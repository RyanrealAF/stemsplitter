package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val layer: String, // INPUT, DECODER, NETWORK, SEPARATOR, TRANSCRIBER, MIDI, STORAGE, CANCELLATION
    val message: String,
    val isError: Boolean = false,
    val details: String? = null
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
