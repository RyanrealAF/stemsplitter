package com.example

import com.example.model.AudioMetadata
import com.example.model.StemType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAudioMetadataFormatting() {
        val meta = AudioMetadata(
            displayName = "demo_song.wav",
            sizeBytes = 15 * 1024 * 1024,
            durationMs = 185000,
            mimeType = "audio/wav",
            sampleRate = 44100,
            channels = 2
        )
        assertEquals("03:05", meta.formattedDuration)
        assertEquals("15.00 MB", meta.formattedSize)
        assertEquals(StemType.VOCALS, StemType.matchFromFilename("mix_vocals.wav"))
        assertEquals(StemType.DRUMS, StemType.matchFromFilename("drums.wav"))
    }
}
