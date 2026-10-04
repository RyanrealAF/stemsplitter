package com.example.audio

import java.io.File
import java.io.FileInputStream

data class MidiFileInfo(
    val format: Int = 1,
    val trackCount: Int = 1,
    val timeDivisionPpq: Int = 480,
    val totalNotes: Int = 0,
    val bpm: Int = 120,
    val keySignature: String? = null
)

object MidiInspector {

    fun parse(midiFile: File): MidiFileInfo {
        if (!midiFile.exists() || midiFile.length() < 14) {
            return MidiFileInfo()
        }

        try {
            FileInputStream(midiFile).use { input ->
                val header = ByteArray(14)
                val read = input.read(header)
                if (read < 14) return MidiFileInfo()

                // Check "MThd" signature
                if (header[0] != 0x4D.toByte() || header[1] != 0x54.toByte() ||
                    header[2] != 0x68.toByte() || header[3] != 0x64.toByte()
                ) {
                    return MidiFileInfo()
                }

                val format = ((header[8].toInt() and 0xFF) shl 8) or (header[9].toInt() and 0xFF)
                val tracks = ((header[10].toInt() and 0xFF) shl 8) or (header[11].toInt() and 0xFF)
                val division = ((header[12].toInt() and 0xFF) shl 8) or (header[13].toInt() and 0xFF)
                val ppq = if (division > 0 && (division and 0x8000) == 0) division else 480

                // Quick scan of remainder to count note-on events and find tempo
                var noteCount = 0
                var estimatedBpm = 120
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    for (i in 0 until bytesRead - 4) {
                        val b = buffer[i].toInt() and 0xFF
                        // Note-On is 0x90 to 0x9F
                        if (b in 0x90..0x9F) {
                            val velocity = buffer[i + 2].toInt() and 0x7F
                            if (velocity > 0) {
                                noteCount++
                            }
                        }
                        // Tempo meta-event: FF 51 03 tt tt tt
                        if (b == 0xFF && (buffer[i + 1].toInt() and 0xFF) == 0x51 && (buffer[i + 2].toInt() and 0xFF) == 0x03) {
                            val usPerQuarter = ((buffer[i + 3].toInt() and 0xFF) shl 16) or
                                    ((buffer[i + 4].toInt() and 0xFF) shl 8) or
                                    (buffer[i + 5].toInt() and 0xFF)
                            if (usPerQuarter > 0) {
                                estimatedBpm = (60_000_000L / usPerQuarter).toInt()
                            }
                        }
                    }
                }

                return MidiFileInfo(
                    format = format,
                    trackCount = tracks.coerceAtLeast(1),
                    timeDivisionPpq = ppq,
                    totalNotes = noteCount,
                    bpm = estimatedBpm
                )
            }
        } catch (_: Exception) {
            return MidiFileInfo()
        }
    }
}
