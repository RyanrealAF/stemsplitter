package com.example.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.example.model.AudioMetadata
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AudioUtils {

    fun inspectAudioUri(context: Context, uri: Uri): Pair<AudioMetadata, File> {
        val contentResolver = context.contentResolver

        // 1. Resolve Display Name & Size from MediaStore
        var displayName = "input_audio"
        var sizeBytes = 0L

        contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) displayName = name
                }
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (sizeIndex != -1) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        // Clean filename
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destinationFile = File(context.cacheDir, "input_${System.currentTimeMillis()}_$safeName")

        // 2. Stream to disk using bounded 64KB buffer (Memory Architecture requirement)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        } ?: throw IOException("Could not open audio stream from URI: $uri")

        if (destinationFile.length() == 0L) {
            destinationFile.delete()
            throw IOException("Selected audio file is empty (0 bytes).")
        }

        if (sizeBytes <= 0L) {
            sizeBytes = destinationFile.length()
        }

        // 3. Inspect Audio Tracks and Technical Specs via MediaMetadataRetriever
        var durationMs = 0L
        var bitrate: Int? = null
        var sampleRate: Int? = null
        var channels: Int? = null
        val detectedMime = contentResolver.getType(uri) ?: guessMimeType(safeName)

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(destinationFile.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durStr != null) durationMs = durStr.toLongOrNull() ?: 0L

            val bitStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (bitStr != null) bitrate = bitStr.toIntOrNull()

            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            if (hasAudio == null && durationMs == 0L) {
                // Warning: might not be audio, but let's check with MediaExtractor
            }
        } catch (_: Exception) {
            // Ignore retriever exception and fallback to MediaExtractor
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        // Extract detailed codec track info (sample rate, channel count)
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(destinationFile.absolutePath)
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (durationMs <= 0L && format.containsKey(MediaFormat.KEY_DURATION)) {
                        durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000L
                    }
                    break
                }
            }
        } catch (_: Exception) {
            // Some formats might not be parsed by native extractor
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }

        val metadata = AudioMetadata(
            displayName = displayName,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            mimeType = detectedMime,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = bitrate
        )

        return Pair(metadata, destinationFile)
    }

    fun guessMimeType(filename: String): String {
        return when {
            filename.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            filename.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            filename.endsWith(".flac", ignoreCase = true) -> "audio/flac"
            filename.endsWith(".m4a", ignoreCase = true) || filename.endsWith(".aac", ignoreCase = true) -> "audio/mp4"
            filename.endsWith(".ogg", ignoreCase = true) || filename.endsWith(".opus", ignoreCase = true) -> "audio/ogg"
            filename.endsWith(".mid", ignoreCase = true) || filename.endsWith(".midi", ignoreCase = true) -> "audio/midi"
            filename.endsWith(".zip", ignoreCase = true) -> "application/zip"
            filename.endsWith(".json", ignoreCase = true) -> "application/json"
            else -> "application/octet-stream"
        }
    }
}
