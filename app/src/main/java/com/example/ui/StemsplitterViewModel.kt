package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MidiFileInfo
import com.example.audio.MidiInspector
import com.example.audio.PlaybackState
import com.example.audio.StemAudioPlayer
import com.example.data.AudioUtils
import com.example.data.ReturnedFile
import com.example.data.StemsplitterRepository
import com.example.model.AudioMetadata
import com.example.model.LogEntry
import com.example.model.ProcessingStage
import com.example.model.SeparationJob
import com.example.model.StemItem
import com.example.model.StemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class StemsplitterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StemsplitterRepository(application)
    private val audioPlayer = StemAudioPlayer(application, viewModelScope)

    private val _currentJob = MutableStateFlow<SeparationJob?>(null)
    val currentJob: StateFlow<SeparationJob?> = _currentJob.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _serverStatus = MutableStateFlow<Pair<Boolean?, String>>(Pair(null, "Not checked"))
    val serverStatus: StateFlow<Pair<Boolean?, String>> = _serverStatus.asStateFlow()

    private val _selectedMidiInfo = MutableStateFlow<Pair<StemType, MidiFileInfo>?>(null)
    val selectedMidiInfo: StateFlow<Pair<StemType, MidiFileInfo>?> = _selectedMidiInfo.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = audioPlayer.playbackState

    private var activeProcessingJob: Job? = null
    private var selectedLocalFile: File? = null

    init {
        checkServer()
    }

    fun checkServer() {
        viewModelScope.launch {
            _serverStatus.value = Pair(null, "Testing connection...")
            val result = repository.checkServerHealth()
            _serverStatus.value = result
            addLog(LogEntry(layer = "NETWORK", message = "Health check: ${result.second}"))
        }
    }

    fun setCustomServerUrl(url: String) {
        val clean = url.trim().removeSuffix("/")
        repository.baseUrl = clean
        checkServer()
    }

    fun getServerUrl(): String = repository.baseUrl

    fun onAudioSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                audioPlayer.stop()
                val (metadata, localFile) = AudioUtils.inspectAudioUri(getApplication(), uri)
                selectedLocalFile = localFile

                val job = SeparationJob(
                    id = UUID.randomUUID().toString(),
                    audioName = metadata.displayName,
                    metadata = metadata,
                    stage = ProcessingStage.INPUT_VALIDATED,
                    statusMessage = "Audio validated: ${metadata.formattedSpecs}",
                    logs = listOf(
                        LogEntry(
                            layer = "INPUT",
                            message = "Loaded ${metadata.displayName} (${metadata.formattedSize}, ${metadata.formattedDuration})"
                        ),
                        LogEntry(
                            layer = "DECODER",
                            message = "Specs: ${metadata.sampleRate ?: 44100} Hz, ${metadata.channels ?: 2} channels, MIME: ${metadata.mimeType}"
                        )
                    )
                )
                _currentJob.value = job

            } catch (e: Exception) {
                addLog(LogEntry(layer = "INPUT", message = "Failed to inspect audio: ${e.message}", isError = true))
                _currentJob.value = SeparationJob(
                    id = UUID.randomUUID().toString(),
                    audioName = "Error",
                    stage = ProcessingStage.ERROR,
                    statusMessage = "Input error: ${e.message}",
                    errorMessage = e.message,
                    errorLayer = "INPUT"
                )
            }
        }
    }

    fun startSeparation() {
        val localFile = selectedLocalFile
        val job = _currentJob.value
        if (localFile == null || !localFile.exists()) {
            addLog(LogEntry(layer = "INPUT", message = "No valid audio file selected.", isError = true))
            return
        }

        activeProcessingJob?.cancel()
        activeProcessingJob = viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            try {
                // Step 1: Uploading
                updateStage(ProcessingStage.UPLOADING, "Uploading audio...", 0.15f)
                addLog(LogEntry(layer = "INPUT", message = "Beginning streaming transfer of ${localFile.name} (${localFile.length() / 1024} KB)"))

                val serverPath = repository.uploadAudioFile(localFile) { log ->
                    addLog(log)
                }

                // Step 2: Sending to Stemsplitter
                updateStage(ProcessingStage.SEPARATING_HTDEMUCS, "Sending to Stemsplitter...", 0.30f)
                val eventId = repository.initiateProcessing(serverPath, localFile.name) { log ->
                    addLog(log)
                }

                // Step 3: Processing audio
                updateStage(ProcessingStage.SEPARATING_HTDEMUCS, "Processing audio...", 0.50f)
                val resultJson = repository.streamInferenceResults(
                    eventId = eventId,
                    onLog = { log -> addLog(log) },
                    onStageUpdate = { stage, status ->
                        val p = if (stage == ProcessingStage.TRANSCRIBING_BASIC_PITCH) 0.65f else 0.45f
                        updateStage(stage, status, p)
                    }
                )

                // Step 4: Finding returned files
                updateStage(ProcessingStage.SAVING_STEMS, "Finding returned files...", 0.80f)
                addLog(LogEntry(layer = "STORAGE", message = "Parsing output FileData payloads..."))
                val files = repository.extractReturnedFiles(resultJson)
                addLog(LogEntry(layer = "STORAGE", message = "Discovered ${files.size} output assets from neural engine."))

                // Step 5: Saving each file
                val savedFiles = mutableListOf<Pair<ReturnedFile, File?>>()
                var downloaded = 0
                for (file in files.distinctBy { it.url ?: it.filename }) {
                    val url = file.url ?: continue
                    downloaded++
                    val p = 0.80f + (0.18f * (downloaded.toFloat() / files.size.coerceAtLeast(1)))
                    updateStage(ProcessingStage.SAVING_STEMS, "Saving ${file.filename}...", p)
                    addLog(LogEntry(layer = "STORAGE", message = "Saving ${file.filename}..."))
                    try {
                        val (f, _) = repository.saveReturnedFile(file)
                        savedFiles.add(Pair(file, f))
                    } catch (e: Exception) {
                        addLog(LogEntry(layer = "STORAGE", message = "Failed saving ${file.filename}: ${e.message}", isError = true))
                    }
                }

                // Step 6: Group into 6 clean stems
                val stems = repository.groupFilesIntoStems(savedFiles, job?.metadata)
                addLog(LogEntry(layer = "MIDI", message = "MIDI transcription parsed. Vocals, Drums, Bass, Guitar, Piano, Other ready."))

                _currentJob.update { current ->
                    current?.copy(
                        stage = ProcessingStage.COMPLETED,
                        statusMessage = "Complete. Saved ${savedFiles.size} files to Downloads/Stemsplitter/",
                        progressPercent = 1.0f,
                        stems = stems,
                        savedCount = savedFiles.size,
                        totalFiles = files.size,
                        completedAt = System.currentTimeMillis()
                    )
                }

                addLog(LogEntry(layer = "STORAGE", message = "Complete. Saved ${savedFiles.size} files to Downloads/Stemsplitter/"))

            } catch (e: Exception) {
                val err = "Error: ${e.message ?: "Unknown error"}"
                addLog(LogEntry(layer = "SEPARATOR", message = err, isError = true))
                _currentJob.update { current ->
                    current?.copy(
                        stage = ProcessingStage.ERROR,
                        statusMessage = err,
                        errorMessage = err,
                        errorLayer = if (err.contains("HTTP") || err.contains("connect")) "NETWORK" else "SEPARATOR"
                    )
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun cancelProcessing() {
        activeProcessingJob?.cancel()
        activeProcessingJob = null
        _isProcessing.value = false
        addLog(LogEntry(layer = "CANCELLATION", message = "User cancelled active processing session."))
        _currentJob.update { current ->
            current?.copy(
                stage = ProcessingStage.IDLE,
                statusMessage = "Processing cancelled by user.",
                progressPercent = 0f
            )
        }
    }

    fun playStem(stem: StemItem) {
        val path = stem.localAudioPath ?: return
        val file = File(path)
        audioPlayer.playStem(stem.id, file)
    }

    fun seekPlayback(progress: Float) {
        audioPlayer.seekTo(progress)
    }

    fun toggleLoop() {
        audioPlayer.toggleLoop()
    }

    fun stopPlayback() {
        audioPlayer.stop()
    }

    fun inspectMidi(stem: StemItem) {
        val path = stem.localMidiPath ?: return
        val file = File(path)
        val info = MidiInspector.parse(file)
        _selectedMidiInfo.value = Pair(stem.stemType, info)
    }

    fun closeMidiInspection() {
        _selectedMidiInfo.value = null
    }

    fun shareStemAudio(stem: StemItem) {
        val path = stem.localAudioPath ?: return
        shareFile(File(path), AudioUtils.guessMimeType(stem.audioFilename))
    }

    fun shareStemMidi(stem: StemItem) {
        val path = stem.localMidiPath ?: return
        shareFile(File(path), "audio/midi")
    }

    private fun shareFile(file: File, mimeType: String) {
        if (!file.exists()) return
        val context = getApplication<Application>()
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Export ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            addLog(LogEntry(layer = "STORAGE", message = "Failed to open export chooser: ${e.message}", isError = true))
        }
    }

    private fun updateStage(stage: ProcessingStage, status: String, progress: Float) {
        _currentJob.update { current ->
            current?.copy(
                stage = stage,
                statusMessage = status,
                progressPercent = progress
            )
        }
    }

    private fun addLog(entry: LogEntry) {
        _currentJob.update { current ->
            if (current == null) {
                SeparationJob(
                    id = UUID.randomUUID().toString(),
                    audioName = "Session",
                    logs = listOf(entry)
                )
            } else {
                current.copy(logs = current.logs + entry)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        activeProcessingJob?.cancel()
    }
}
