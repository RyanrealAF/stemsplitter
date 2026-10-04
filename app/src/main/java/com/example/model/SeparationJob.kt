package com.example.model

data class SeparationJob(
    val id: String,
    val audioName: String,
    val metadata: AudioMetadata? = null,
    val stage: ProcessingStage = ProcessingStage.IDLE,
    val statusMessage: String = "Ready to separate audio",
    val progressPercent: Float = 0f,
    val stems: List<StemItem> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val errorMessage: String? = null,
    val errorLayer: String? = null,
    val savedCount: Int = 0,
    val totalFiles: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val durationSeconds: Long
        get() {
            val end = completedAt ?: System.currentTimeMillis()
            return ((end - startedAt) / 1000).coerceAtLeast(0)
        }
}
