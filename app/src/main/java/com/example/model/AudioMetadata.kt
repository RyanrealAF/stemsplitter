package com.example.model

data class AudioMetadata(
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val mimeType: String,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val bitrate: Int? = null
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                "%.2f MB".format(mb)
            } else {
                "%.1f KB".format(kb)
            }
        }

    val formattedSpecs: String
        get() {
            val parts = mutableListOf<String>()
            if (sampleRate != null && sampleRate > 0) {
                parts.add("${sampleRate / 1000} kHz")
            }
            if (channels != null && channels > 0) {
                parts.add(if (channels == 1) "Mono" else if (channels == 2) "Stereo" else "$channels ch")
            }
            if (bitrate != null && bitrate > 0) {
                parts.add("${bitrate / 1000} kbps")
            }
            return if (parts.isEmpty()) formattedSize else parts.joinToString(" • ") + " • $formattedSize"
        }
}
