package com.mitra.learning.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AndroidSpeechOutput(
    context: Context,
) : SpeechOutput {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow<SpeechOutputState>(SpeechOutputState.Initializing)
    override val state: StateFlow<SpeechOutputState> = _state.asStateFlow()

    private val initialized = CompletableDeferred<Boolean>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null

    init {
        val engine = TextToSpeech(appContext) { status ->
            mainHandler.post { configureAfterInit(status) }
        }
        tts = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = SpeechOutputState.Speaking
            }

            @Deprecated("Deprecated in Android SDK")
            override fun onError(utteranceId: String?) {
                _state.value = SpeechOutputState.Error("અવાજ વગાડી શકાયો નહીં.")
            }

            override fun onDone(utteranceId: String?) {
                _state.value = SpeechOutputState.Ready
            }
        })
    }

    override suspend fun speakGujarati(text: String) {
        if (text.isBlank()) return

        val ready = if (initialized.isCompleted) {
            runCatching { initialized.await() }.getOrDefault(false)
        } else {
            withTimeoutOrNull(5_000) { initialized.await() } ?: false
        }

        if (!ready) {
            if (_state.value !is SpeechOutputState.Unavailable) {
                _state.value = SpeechOutputState.Unavailable
            }
            return
        }

        withContext(Dispatchers.Main.immediate) {
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                UUID.randomUUID().toString(),
            )
        }
    }

    override fun stop() {
        tts?.stop()
        if (_state.value is SpeechOutputState.Speaking) {
            _state.value = SpeechOutputState.Ready
        }
    }

    override fun close() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _state.value = SpeechOutputState.Unavailable
    }


    private fun configureAfterInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _state.value = SpeechOutputState.Unavailable
            initialized.completeIfNeeded(false)
            return
        }

        val languageResult = tts?.setLanguage(Locale("gu", "IN"))
            ?: TextToSpeech.LANG_NOT_SUPPORTED
        val supported = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED

        _state.value = if (supported) SpeechOutputState.Ready else SpeechOutputState.Unavailable
        initialized.completeIfNeeded(supported)
    }

    private fun CompletableDeferred<Boolean>.completeIfNeeded(value: Boolean) {
        if (!isCompleted) complete(value)
    }
}

