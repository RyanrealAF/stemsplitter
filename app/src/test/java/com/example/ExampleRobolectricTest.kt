package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.MidiInspector
import com.example.data.AudioUtils
import com.example.model.StemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Stemsplitter", appName)
    }

    @Test
    fun `test stem type matching from filenames`() {
        assertEquals(StemType.VOCALS, StemType.matchFromFilename("song_vocals.wav"))
        assertEquals(StemType.DRUMS, StemType.matchFromFilename("track_drums.mp3"))
        assertEquals(StemType.BASS, StemType.matchFromFilename("deep_bass.flac"))
        assertEquals(StemType.GUITAR, StemType.matchFromFilename("lead_guitar.wav"))
        assertEquals(StemType.PIANO, StemType.matchFromFilename("acoustic_piano.mid"))
        assertEquals(StemType.OTHER, StemType.matchFromFilename("ambient_synths.wav"))
    }

    @Test
    fun `test guess mime type`() {
        assertEquals("audio/wav", AudioUtils.guessMimeType("vocals.wav"))
        assertEquals("audio/midi", AudioUtils.guessMimeType("drums.mid"))
        assertEquals("audio/flac", AudioUtils.guessMimeType("bass.flac"))
        assertEquals("audio/mpeg", AudioUtils.guessMimeType("track.mp3"))
    }

    @Test
    fun `test midi inspector on dummy midi header`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "test.mid")
        FileOutputStream(file).use { out ->
            // MThd header: 'M','T','h','d', len=6, fmt=1, ntrks=1, division=480
            val header = byteArrayOf(
                0x4D, 0x54, 0x68, 0x64,
                0x00, 0x00, 0x00, 0x06,
                0x00, 0x01,
                0x00, 0x01,
                0x01, 0xE0.toByte() // 480
            )
            out.write(header)
        }
        val info = MidiInspector.parse(file)
        assertEquals(1, info.format)
        assertEquals(1, info.trackCount)
        assertEquals(480, info.timeDivisionPpq)
        file.delete()
    }

    @Test
    fun `test launch MainActivity`() {
        val controller = org.robolectric.Robolectric.buildActivity(com.ryanrealaf.stemsplitter.MainActivity::class.java)
        controller.setup()
        assertNotNull(controller.get())
    }
}
