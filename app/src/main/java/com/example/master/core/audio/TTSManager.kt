package com.example.master.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Wrapper around Android TextToSpeech API with lifecycle-safe helpers.
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var speechRate = 1.0f
    private var pitch = 1.0f

    override fun onInit(status: Int) {
        _isReady.value = status == TextToSpeech.SUCCESS
        if (_isReady.value) {
            setLanguage(Locale.US)
        }
    }

    fun setLanguage(locale: Locale): Boolean {
        if (!_isReady.value) return false
        val availability = tts.isLanguageAvailable(locale)
        return if (availability == TextToSpeech.LANG_AVAILABLE ||
            availability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        ) {
            tts.language = locale
            true
        } else {
            false
        }
    }

    fun setSpeed(speed: Float) {
        speechRate = speed.coerceIn(0.5f, 2.0f)
        if (_isReady.value) {
            tts.setSpeechRate(speechRate)
        }
    }

    fun setPitch(pitchFactor: Float) {
        pitch = pitchFactor.coerceIn(0.5f, 2.0f)
        if (_isReady.value) {
            tts.setPitch(pitch)
        }
    }

    fun speak(
        text: String,
        language: Locale = tts.language ?: Locale.US,
        speed: Float? = null,
        pitchFactor: Float? = null,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH
    ) {
        if (!_isReady.value) return
        if (tts.language != language) {
            setLanguage(language)
        }
        tts.setSpeechRate((speed ?: speechRate).coerceIn(0.5f, 2.0f))
        tts.setPitch((pitchFactor ?: pitch).coerceIn(0.5f, 2.0f))
        tts.speak(text, queueMode, null, text.hashCode().toString())
    }

    fun stop() {
        if (_isReady.value) {
            tts.stop()
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        _isReady.value = false
    }

    fun release() {
        shutdown()
    }
}
