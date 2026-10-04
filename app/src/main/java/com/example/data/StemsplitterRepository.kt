package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.audio.MidiInspector
import com.example.model.AudioMetadata
import com.example.model.LogEntry
import com.example.model.ProcessingStage
import com.example.model.StemItem
import com.example.model.StemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.zip.ZipInputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class ReturnedFile(
    val url: String?,
    val filename: String
)

class StemsplitterRepository(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.MINUTES)
        .build()

    var baseUrl: String = "https://ryanrealaf-stemsplitter.hf.space"

    val uploadEndpoint: String
        get() = "$baseUrl/gradio_api/upload"

    val processEndpoint: String
        get() = "$baseUrl/gradio_api/call/process_sync"

    suspend fun checkServerHealth(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(baseUrl)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404 || response.code == 200 || response.code == 302) {
                    Pair(true, "Endpoint reachable (HTTP ${response.code})")
                } else {
                    Pair(false, "Server responded with HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.message ?: "Unknown"}")
        }
    }

    suspend fun uploadAudioFile(
        file: File,
        onLog: (LogEntry) -> Unit
    ): String = withContext(Dispatchers.IO) {
        onLog(LogEntry(layer = "NETWORK", message = "Connecting to upload endpoint: $uploadEndpoint"))
        val body = file.asRequestBody("application/octet-stream".toMediaType())
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files", file.name, body)
            .build()

        val request = Request.Builder()
            .url(uploadEndpoint)
            .post(multipart)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Upload failed with HTTP ${response.code}: ${response.message}")
            }
            val text = response.body?.string() ?: throw IOException("Empty response from upload endpoint")
            val array = JSONArray(text)
            val serverPath = array.getString(0)
            onLog(LogEntry(layer = "NETWORK", message = "Upload successful. Server token: $serverPath"))
            serverPath
        }
    }

    suspend fun initiateProcessing(
        serverPath: String,
        originalName: String,
        onLog: (LogEntry) -> Unit
    ): String = withContext(Dispatchers.IO) {
        onLog(LogEntry(layer = "SEPARATOR", message = "Dispatching process_sync for HTDemucs 6s + Basic Pitch..."))

        val fileData = JSONObject().apply {
            put("path", serverPath)
            put("orig_name", originalName)
            put("meta", JSONObject().apply {
                put("_type", "gradio.FileData")
            })
        }

        val payload = JSONObject().apply {
            put("audio_path", fileData)
        }

        val request = Request.Builder()
            .url(processEndpoint)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Processing request failed: HTTP ${response.code} ${response.message}")
            }
            val text = response.body?.string() ?: throw IOException("Empty response from process endpoint")
            val eventId = JSONObject(text).getString("event_id")
            onLog(LogEntry(layer = "SEPARATOR", message = "Inference session registered: eventId=$eventId"))
            eventId
        }
    }

    suspend fun streamInferenceResults(
        eventId: String,
        onLog: (LogEntry) -> Unit,
        onStageUpdate: (ProcessingStage, String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val streamUrl = "$baseUrl/gradio_api/call/process_sync/$eventId"
        onLog(LogEntry(layer = "SEPARATOR", message = "Opening SSE stream: $streamUrl"))

        val request = Request.Builder()
            .url(streamUrl)
            .get()
            .header("Accept", "text/event-stream")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("SSE stream failed: HTTP ${response.code}")
            }

            val reader = response.body?.charStream()?.buffered()
                ?: throw IOException("No event stream returned by server")

            reader.use { bufferedReader ->
                var lastProgressReport = System.currentTimeMillis()
                while (coroutineContext.isActive) {
                    val line = bufferedReader.readLine() ?: break

                    if (!line.startsWith("data:")) continue

                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty()) continue
                    if (data == "[DONE]") {
                        onLog(LogEntry(layer = "SEPARATOR", message = "Engine stream closed [DONE]"))
                        break
                    }

                    // Check if intermediate progress or final array
                    try {
                        JSONArray(data)
                        // This is the completed output array
                        return@withContext data
                    } catch (_: Exception) {
                        // Intermediate status or progress message
                        val now = System.currentTimeMillis()
                        if (now - lastProgressReport > 1500) {
                            lastProgressReport = now
                            val cleanMsg = data.take(80)
                            onLog(LogEntry(layer = "SEPARATOR", message = "Engine status: $cleanMsg"))
                            if (cleanMsg.contains("pitch", ignoreCase = true) || cleanMsg.contains("midi", ignoreCase = true)) {
                                onStageUpdate(ProcessingStage.TRANSCRIBING_BASIC_PITCH, "Transcribing notes with Basic Pitch...")
                            } else {
                                onStageUpdate(ProcessingStage.SEPARATING_HTDEMUCS, "Separating 6 audio stems with HTDemucs...")
                            }
                        }
                    }
                }
            }
        }

        throw IOException("Inference ended without returning final stem data.")
    }

    fun extractReturnedFiles(resultJson: String): List<ReturnedFile> {
        val results = mutableListOf<ReturnedFile>()
        try {
            val outputs = JSONArray(resultJson)
            for (i in 0 until outputs.length()) {
                findFilesRecursively(outputs.opt(i), results)
            }
        } catch (_: Exception) {}
        return results.distinctBy { it.url ?: it.filename }
    }

    private fun findFilesRecursively(value: Any?, results: MutableList<ReturnedFile>) {
        when (value) {
            is JSONObject -> {
                val url = value.optString("url", "")
                val path = value.optString("path", "")
                val filename = value.optString("orig_name", "")

                if (url.isNotEmpty() || path.isNotEmpty()) {
                    val finalUrl = if (url.isNotEmpty()) {
                        if (url.startsWith("http")) url else "$baseUrl$url"
                    } else null

                    val finalName = when {
                        filename.isNotEmpty() -> filename
                        url.isNotEmpty() -> {
                            try { File(URL(url).path).name } catch (_: Exception) { "stem_output" }
                        }
                        path.isNotEmpty() -> File(path).name
                        else -> "stem_output"
                    }

                    results.add(ReturnedFile(url = finalUrl, filename = finalName))
                }

                val keys = value.keys()
                while (keys.hasNext()) {
                    val child = value.opt(keys.next())
                    if (child is JSONObject || child is JSONArray) {
                        findFilesRecursively(child, results)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    findFilesRecursively(value.opt(i), results)
                }
            }
        }
    }

    suspend fun saveReturnedFile(
        returnedFile: ReturnedFile,
        subfolder: String = "Stemsplitter"
    ): Pair<File?, Uri?> = withContext(Dispatchers.IO) {
        val url = returnedFile.url ?: return@withContext Pair(null, null)
        val safeFilename = returnedFile.filename.replace(Regex("[^A-Za-z0-9._-]"), "_")

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeFilename)
            put(MediaStore.Downloads.MIME_TYPE, AudioUtils.guessMimeType(safeFilename))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/$subfolder")
            }
        }

        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create output file in Downloads")

        val stemsDir = File(context.filesDir, "stems").apply { mkdirs() }
        val localFile = File(stemsDir, safeFilename)

        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Download failed: HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("Empty download")

                contentResolver.openOutputStream(uri).use { mediaStoreOut ->
                    requireNotNull(mediaStoreOut) { "Could not open output stream for MediaStore URI" }
                    FileOutputStream(localFile).use { localOut ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                mediaStoreOut.write(buffer, 0, bytesRead)
                                localOut.write(buffer, 0, bytesRead)
                            }
                            mediaStoreOut.flush()
                            localOut.flush()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            try {
                contentResolver.delete(uri, null, null)
            } catch (_: Exception) {}
            localFile.delete()
            throw e
        }

        Pair(localFile, uri)
    }


    /**
     * Extracts the user-facing MIDI/analysis artifacts from the production ZIP returned
     * by the Hugging Face Space. The Space exposes the ZIP as one output while the
     * individual MIDI files are bundled inside it.
     */
    suspend fun extractBundleArtifacts(
        bundleFile: File,
        subfolder: String = "Stemsplitter"
    ): List<Pair<ReturnedFile, File?>> = withContext(Dispatchers.IO) {
        if (!bundleFile.exists()) return@withContext emptyList()

        val results = mutableListOf<Pair<ReturnedFile, File?>>()
        val stemsDir = File(context.filesDir, "stems").apply { mkdirs() }

        ZipInputStream(FileInputStream(bundleFile).buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }

                val entryName = entry.name.replace('\\', '/')
                val lower = entryName.lowercase()
                val isMidi = lower.endsWith(".mid") || lower.endsWith(".midi")
                val isJson = lower.endsWith(".json")

                if (!isMidi && !isJson) {
                    zip.closeEntry()
                    continue
                }

                val rawName = entryName.substringAfterLast('/')
                val safeName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                if (safeName.isBlank()) {
                    zip.closeEntry()
                    continue
                }

                val localFile = File(stemsDir, safeName)
                FileOutputStream(localFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var count: Int
                    while (zip.read(buffer).also { count = it } != -1) {
                        output.write(buffer, 0, count)
                    }
                }

                saveLocalArtifactToDownloads(localFile, safeName, subfolder)

                results.add(
                    ReturnedFile(url = null, filename = safeName) to localFile
                )
                zip.closeEntry()
            }
        }

        results
    }

    private fun saveLocalArtifactToDownloads(
        localFile: File,
        filename: String,
        subfolder: String
    ): Uri {
        val safeFilename = filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeFilename)
            put(MediaStore.Downloads.MIME_TYPE, AudioUtils.guessMimeType(safeFilename))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/$subfolder")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IOException("Could not create Downloads artifact: $safeFilename")

        try {
            resolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "Could not open Downloads output stream" }
                localFile.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }

        return uri
    }

    fun groupFilesIntoStems(
        downloadedFiles: List<Pair<ReturnedFile, File?>>,
        audioMetadata: AudioMetadata?
    ): List<StemItem> {
        // Group by StemType
        val stemsMap = mutableMapOf<StemType, MutableList<Pair<ReturnedFile, File?>>>()
        for (stemType in StemType.values()) {
            stemsMap[stemType] = mutableListOf()
        }

        for (item in downloadedFiles) {
            val (returned, _) = item
            val type = StemType.matchFromFilename(returned.filename)
            stemsMap[type]?.add(item)
        }

        val resultList = mutableListOf<StemItem>()

        for (stemType in StemType.values()) {
            val filesForStem = stemsMap[stemType] ?: emptyList()
            var audioFile: File? = null
            var audioReturned: ReturnedFile? = null
            var midiFile: File? = null
            var midiReturned: ReturnedFile? = null

            for ((returned, file) in filesForStem) {
                val fn = returned.filename.lowercase()
                if (fn.endsWith(".mid") || fn.endsWith(".midi")) {
                    midiFile = file
                    midiReturned = returned
                } else if (fn.endsWith(".wav") || fn.endsWith(".mp3") || fn.endsWith(".flac") || fn.endsWith(".ogg") || fn.endsWith(".m4a")) {
                    audioFile = file
                    audioReturned = returned
                }
            }

            // Inspect MIDI if present. A malformed/partial MIDI file must not
            // invalidate the rest of a completed separation.
            var notesCount = 0
            var bpm = 120
            if (midiFile != null && midiFile.exists()) {
                try {
                    val info = MidiInspector.parse(midiFile)
                    notesCount = info.totalNotes
                    bpm = info.bpm
                } catch (_: Exception) {
                    notesCount = 0
                    bpm = 120
                }
            }

            val size = audioFile?.length() ?: midiFile?.length() ?: 0L
            val duration = audioMetadata?.durationMs ?: 0L

            resultList.add(
                StemItem(
                    id = UUID.randomUUID().toString(),
                    stemType = stemType,
                    audioFilename = audioReturned?.filename ?: "${stemType.title.lowercase()}.wav",
                    audioUrl = audioReturned?.url,
                    localAudioPath = audioFile?.absolutePath,
                    midiFilename = midiReturned?.filename ?: "${stemType.title.lowercase()}.mid",
                    midiUrl = midiReturned?.url,
                    localMidiPath = midiFile?.absolutePath,
                    durationMs = duration,
                    fileSizeBytes = size,
                    isDownloaded = audioFile != null && audioFile.exists(),
                    midiNotesCount = notesCount,
                    estimatedBpm = bpm
                )
            )
        }

        return resultList.sortedBy { it.stemType.defaultOrder }
    }
}
