package com.mitra.learning.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AndroidSpeechInput(
    context: Context,
) : SpeechInput {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow<SpeechInputState>(SpeechInputState.Idle)
    override val state: StateFlow<SpeechInputState> = _state.asStateFlow()

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    private var recognizer: SpeechRecognizer? = null

    override suspend fun startListening() = startListening(GUJARATI_LOCALE_TAG)

    override suspend fun startListening(languageTag: String) = withContext(Dispatchers.Main.immediate) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            _state.value = SpeechInputState.Error(
                messageGujarati = "માઇક્રોફોનની પરવાનગી આપો અથવા લખીને જવાબ આપો.",
                recoverable = false,
            )
            return@withContext
        }

        if (!isAvailable) {
            _state.value = SpeechInputState.Error(
                messageGujarati = "આ ફોનમાં અવાજ ઓળખવાની સુવિધા ઉપલબ્ધ નથી. લખીને જવાબ આપો.",
                recoverable = false,
            )
            return@withContext
        }

        val speechRecognizer = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also {
            recognizer = it
            it.setRecognitionListener(listener)
        }

        speechRecognizer.cancel()
        _state.value = SpeechInputState.Listening
        speechRecognizer.startListening(recognizerIntent(languageTag))
    }

    override suspend fun stopListening(): Unit = withContext(Dispatchers.Main.immediate) {
        recognizer?.stopListening()
        Unit
    }

    override fun cancel() {
        recognizer?.cancel()
        _state.value = SpeechInputState.Idle
    }

    override fun close() {
        recognizer?.destroy()
        recognizer = null
        _state.value = SpeechInputState.Idle
    }

    private fun recognizerIntent(languageTag: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "જવાબ બોલો")
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechInputState.Listening
        }

        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            _state.value = SpeechInputState.Error(
                messageGujarati = errorMessageGujarati(error),
                recoverable = error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            )
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()

            _state.value = if (text.isNullOrBlank()) {
                SpeechInputState.Error("અવાજ સમજાયો નહીં. ફરી બોલો અથવા લખીને જવાબ આપો.")
            } else {
                SpeechInputState.Result(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotBlank()) {
                _state.value = SpeechInputState.Partial(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun errorMessageGujarati(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "માઇક્રોફોનમાં સમસ્યા આવી. ફરી પ્રયત્ન કરો."
        SpeechRecognizer.ERROR_CLIENT -> "અવાજ ઓળખવાનું અટકી ગયું. ફરી પ્રયત્ન કરો."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "માઇક્રોફોનની પરવાનગી આપો અથવા લખીને જવાબ આપો."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "અવાજ ઓળખવા માટે નેટવર્ક મળ્યું નહીં. લખીને જવાબ આપી શકો."
        SpeechRecognizer.ERROR_NO_MATCH -> "અવાજ સમજાયો નહીં. ફરી બોલો અથવા લખીને જવાબ આપો."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "માઇક્રોફોન વ્યસ્ત છે. થોડું પછી ફરી બોલો."
        SpeechRecognizer.ERROR_SERVER -> "અવાજ સેવા હાલમાં ઉપલબ્ધ નથી. લખીને જવાબ આપો."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "કંઈ સાંભળાયું નહીં. માઇક્રોફોન દબાવીને ફરી બોલો."
        else -> "અવાજ સમજવામાં સમસ્યા આવી. લખીને જવાબ આપી શકો."
    }

    private companion object {
        const val GUJARATI_LOCALE_TAG = "gu-IN"
    }
}
