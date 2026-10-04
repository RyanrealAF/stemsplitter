package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val activeStemId: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val progress: Float = 0f,
    val isLooping: Boolean = false
)

class StemAudioPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun playStem(stemId: String, file: File) {
        if (!file.exists()) return

        if (_playbackState.value.activeStemId == stemId && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            } else {
                mediaPlayer?.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressTracker()
            }
            return
        }

        stop()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                prepare()
                isLooping = _playbackState.value.isLooping
                start()
            }
            mediaPlayer = player

            val duration = player.duration
            _playbackState.value = PlaybackState(
                activeStemId = stemId,
                isPlaying = true,
                currentPositionMs = 0,
                durationMs = duration,
                progress = 0f,
                isLooping = player.isLooping
            )

            player.setOnCompletionListener {
                if (!player.isLooping) {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration,
                        progress = 1f
                    )
                }
            }

            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun seekTo(progress: Float) {
        val player = mediaPlayer ?: return
        val targetMs = (progress * player.duration).toInt().coerceIn(0, player.duration)
        player.seekTo(targetMs)
        _playbackState.value = _playbackState.value.copy(
            currentPositionMs = targetMs,
            progress = progress
        )
    }

    fun toggleLoop() {
        val newLoop = !_playbackState.value.isLooping
        mediaPlayer?.isLooping = newLoop
        _playbackState.value = _playbackState.value.copy(isLooping = newLoop)
    }

    fun pause() {
        mediaPlayer?.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playbackState.value = PlaybackState(isLooping = _playbackState.value.isLooping)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val pos = player.currentPosition
                    val dur = player.duration.coerceAtLeast(1)
                    val p = pos.toFloat() / dur.toFloat()
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur,
                        progress = p.coerceIn(0f, 1f)
                    )
                }
                delay(80)
            }
        }
    }

    fun release() {
        stop()
    }
}
