package com.mitra.learning.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private val systemRecognizerAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    private val onDeviceRecognizerAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)

    override val isAvailable: Boolean
        get() = systemRecognizerAvailable || onDeviceRecognizerAvailable

    private var recognizer: SpeechRecognizer? = null
    private var recognitionInProgress = false
    private var ignoreCancelledClientError = false
    private var usingOnDeviceOnlyRecognizer = false

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

        // Do not cancel an idle recognizer before every start. Some Android
        // implementations report ERROR_CLIENT for that cancel and race it with
        // the new listening session.
        if (recognitionInProgress) {
            ignoreCancelledClientError = true
            recognizer?.cancel()
            recognitionInProgress = false
            delay(RESTART_SETTLE_DELAY_MS)
        }

        val speechRecognizer = recognizer ?: createRecognizer().also {
            recognizer = it
            it.setRecognitionListener(listener)
        }

        _state.value = SpeechInputState.Listening
        runCatching {
            speechRecognizer.startListening(recognizerIntent(languageTag))
            recognitionInProgress = true
        }.onFailure {
            recognitionInProgress = false
            destroyRecognizer()
            _state.value = SpeechInputState.Error(
                messageGujarati = "માઇક્રોફોન શરૂ થઈ શક્યો નહીં. ફરી માઇક્રોફોન દબાવો.",
                automaticRetry = false,
            )
        }
    }

    override suspend fun stopListening(): Unit = withContext(Dispatchers.Main.immediate) {
        if (recognitionInProgress) {
            recognizer?.stopListening()
        }
        Unit
    }

    override fun cancel() {
        if (recognitionInProgress) {
            ignoreCancelledClientError = true
            recognizer?.cancel()
        }
        recognitionInProgress = false
        _state.value = SpeechInputState.Idle
    }

    override fun close() {
        destroyRecognizer()
        _state.value = SpeechInputState.Idle
    }

    /**
     * Prefer the normal system recognizer. It may use an installed offline pack
     * or Google's network recognizer and therefore works on more phones and for
     * more languages. createOnDeviceSpeechRecognizer() only proves that some
     * on-device recognition exists; it does not prove that Gujarati is installed.
     */
    private fun createRecognizer(): SpeechRecognizer {
        usingOnDeviceOnlyRecognizer = !systemRecognizerAvailable && onDeviceRecognizerAvailable
        return if (usingOnDeviceOnlyRecognizer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        } else {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
        recognitionInProgress = false
        ignoreCancelledClientError = false
        usingOnDeviceOnlyRecognizer = false
    }

    private fun recognizerIntent(languageTag: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

            // Forcing offline recognition caused continuous Gujarati language
            // errors on devices that support on-device speech but do not have
            // the Gujarati model. Only force offline when no system recognizer
            // exists and the app had to create an on-device-only recognizer.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, usingOnDeviceOnlyRecognizer)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "જવાબ બોલો")
        }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            recognitionInProgress = true
            _state.value = SpeechInputState.Listening
        }

        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            recognitionInProgress = false
        }

        override fun onError(error: Int) {
            recognitionInProgress = false

            if (ignoreCancelledClientError && error == SpeechRecognizer.ERROR_CLIENT) {
                ignoreCancelledClientError = false
                return
            }
            ignoreCancelledClientError = false

            val details = errorDetails(error)
            if (details.recreateRecognizer) {
                destroyRecognizer()
            }
            _state.value = SpeechInputState.Error(
                messageGujarati = details.message,
                recoverable = details.recoverable,
                automaticRetry = details.automaticRetry,
                errorCode = error,
            )
        }

        override fun onResults(results: Bundle?) {
            recognitionInProgress = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()

            _state.value = if (text.isNullOrBlank()) {
                SpeechInputState.Error(
                    messageGujarati = "તમારો અવાજ સ્પષ્ટ સાંભળાયો નહીં. માઇક્રોફોન ફરી દબાવી ધીમે બોલો.",
                    automaticRetry = false,
                )
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

    private data class ErrorDetails(
        val message: String,
        val recoverable: Boolean = true,
        val automaticRetry: Boolean = false,
        val recreateRecognizer: Boolean = false,
    )

    private fun errorDetails(error: Int): ErrorDetails = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> ErrorDetails(
            message = "માઇક્રોફોનમાં સમસ્યા આવી. બીજી એપ માઇક્રોફોન વાપરતી હોય તો બંધ કરીને ફરી પ્રયત્ન કરો.",
            recreateRecognizer = true,
        )
        SpeechRecognizer.ERROR_CLIENT -> ErrorDetails(
            message = "અવાજ ઓળખવાનું અટકી ગયું. માઇક્રોફોન ફરી દબાવો.",
            automaticRetry = true,
            recreateRecognizer = true,
        )
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ErrorDetails(
            message = "માઇક્રોફોનની પરવાનગી આપો અથવા લખીને જવાબ આપો.",
            recoverable = false,
        )
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        -> ErrorDetails(
            message = "અવાજ ઓળખવા નેટવર્ક મળ્યું નથી. ઇન્ટરનેટ ચાલુ કરો અથવા Gujarati speech pack ડાઉનલોડ કરો.",
            recreateRecognizer = true,
        )
        SpeechRecognizer.ERROR_NO_MATCH -> ErrorDetails(
            message = "તમારો અવાજ સ્પષ્ટ સાંભળાયો નહીં. માઇક્રોફોન ફરી દબાવી ધીમે બોલો.",
        )
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> ErrorDetails(
            message = "માઇક્રોફોન થોડો વ્યસ્ત છે. મિત્ર ફરી સાંભળવાનો પ્રયત્ન કરે છે…",
            automaticRetry = true,
            recreateRecognizer = true,
        )
        SpeechRecognizer.ERROR_SERVER -> ErrorDetails(
            message = "અવાજ સેવા સાથે જોડાણ થયું નથી. માઇક્રોફોન ફરી દબાવો.",
            recreateRecognizer = true,
        )
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ErrorDetails(
            message = "કંઈ સાંભળાયું નહીં. માઇક્રોફોન ફરી દબાવીને જવાબ બોલો.",
        )
        ERROR_TOO_MANY_REQUESTS_CODE -> ErrorDetails(
            message = "માઇક્રોફોન વારંવાર શરૂ થયો છે. બે સેકન્ડ પછી ફરી દબાવો.",
            recreateRecognizer = true,
        )
        ERROR_SERVER_DISCONNECTED_CODE -> ErrorDetails(
            message = "અવાજ સેવા ડિસ્કનેક્ટ થઈ. માઇક્રોફોન ફરી દબાવો.",
            recreateRecognizer = true,
        )
        ERROR_LANGUAGE_NOT_SUPPORTED_CODE,
        ERROR_LANGUAGE_UNAVAILABLE_CODE,
        -> ErrorDetails(
            message = "આ ફોનમાં પસંદ કરેલી ભાષાનો voice model ઉપલબ્ધ નથી. Google app અથવા Speech Services માં Gujarati અને English ભાષા ઉમેરો.",
            recreateRecognizer = true,
        )
        ERROR_CANNOT_CHECK_SUPPORT_CODE -> ErrorDetails(
            message = "ફોન voice ભાષા તપાસી શક્યો નહીં. Google Speech Services અપડેટ કરીને ફરી પ્રયત્ન કરો.",
            recreateRecognizer = true,
        )
        ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS_CODE -> ErrorDetails(
            message = "Voice language download ની સ્થિતિ મળી નહીં. Speech Services માં ભાષા તપાસો.",
            recreateRecognizer = true,
        )
        else -> ErrorDetails(
            message = "અવાજ ઓળખવામાં સમસ્યા આવી (કોડ $error). માઇક્રોફોન ફરી દબાવો.",
            recreateRecognizer = true,
        )
    }

    private companion object {
        const val GUJARATI_LOCALE_TAG = "gu-IN"
        const val RESTART_SETTLE_DELAY_MS = 180L

        // Newer SpeechRecognizer error constants are represented by their
        // documented integer values so this minSdk 26 app does not reference
        // newer framework fields at runtime.
        const val ERROR_TOO_MANY_REQUESTS_CODE = 10
        const val ERROR_SERVER_DISCONNECTED_CODE = 11
        const val ERROR_LANGUAGE_NOT_SUPPORTED_CODE = 12
        const val ERROR_LANGUAGE_UNAVAILABLE_CODE = 13
        const val ERROR_CANNOT_CHECK_SUPPORT_CODE = 14
        const val ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS_CODE = 15
    }
}
