package com.example.model

data class StemItem(
    val id: String,
    val stemType: StemType,
    val audioFilename: String,
    val audioUrl: String? = null,
    val localAudioPath: String? = null,
    val midiFilename: String? = null,
    val midiUrl: String? = null,
    val localMidiPath: String? = null,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val isDownloaded: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val midiNotesCount: Int = 0,
    val estimatedBpm: Int = 120
) {
    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return ""
            val kb = fileSizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(kb)
        }
}
