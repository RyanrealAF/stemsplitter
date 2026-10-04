package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.StemBassColor
import com.example.ui.theme.StemDrumsColor
import com.example.ui.theme.StemGuitarColor
import com.example.ui.theme.StemOtherColor
import com.example.ui.theme.StemPianoColor
import com.example.ui.theme.StemVocalsColor

enum class StemType(
    val title: String,
    val subtitle: String,
    val color: Color,
    val gmChannel: Int,
    val defaultOrder: Int
) {
    VOCALS("Vocals", "Lead & Harmony Vocals", StemVocalsColor, 0, 1),
    DRUMS("Drums", "Kick, Snare, Hats & Percussion", StemDrumsColor, 9, 2), // GM Percussion (Ch 10)
    BASS("Bass", "Sub, Synth & Electric Bassline", StemBassColor, 1, 3),
    GUITAR("Guitar", "Acoustic & Electric Strum/Lead", StemGuitarColor, 2, 4),
    PIANO("Piano", "Keys, Acoustic Grand & Chords", StemPianoColor, 3, 5),
    OTHER("Other", "Synths, Ambient Textures & FX", StemOtherColor, 4, 6);

    companion object {
        fun matchFromFilename(name: String): StemType {
            val lower = name.lowercase()
            return when {
                lower.contains("vocal") || lower.contains("vox") || lower.contains("voice") -> VOCALS
                lower.contains("drum") || lower.contains("beat") || lower.contains("percussion") -> DRUMS
                lower.contains("bass") -> BASS
                lower.contains("guitar") || lower.contains("gtr") -> GUITAR
                lower.contains("piano") || lower.contains("keys") -> PIANO
                else -> OTHER
            }
        }
    }
}
