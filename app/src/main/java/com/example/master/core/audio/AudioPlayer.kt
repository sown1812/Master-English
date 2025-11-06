package com.example.master.core.audio

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lightweight MediaPlayer wrapper supporting local and remote audio playback.
 */
class AudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    fun play(url: String?, onError: ((Throwable) -> Unit)? = null) {
        stop()
        if (url.isNullOrBlank()) {
            onError?.invoke(IllegalArgumentException("Audio url is empty"))
            return
        }

        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setDataSource(url)
            player.setOnPreparedListener {
                it.start()
                _isPlaying.value = true
            }
            player.setOnCompletionListener {
                _isPlaying.value = false
                stop()
            }
            player.setOnErrorListener { _, what, extra ->
                _isPlaying.value = false
                stop()
                onError?.invoke(IllegalStateException("Audio error $what $extra"))
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            _isPlaying.value = false
            stop()
            onError?.invoke(e)
        }
    }

    fun playLocal(resId: Int) {
        stop()
        mediaPlayer = MediaPlayer.create(appContext, resId).apply {
            setOnCompletionListener {
                _isPlaying.value = false
                stop()
            }
            start()
        }
        _isPlaying.value = true
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
            }
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.reset()
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }
}
