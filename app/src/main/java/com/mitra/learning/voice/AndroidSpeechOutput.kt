package com.mitra.learning.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
    private var voiceStyle: VoiceStyle = VoiceStyle.CARTOON_ADVENTURE
    private val selectedVoiceByLocale = mutableMapOf<String, Voice?>()

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

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = SpeechOutputState.Error("અવાજ વગાડી શકાયો નહીં.")
            }

            override fun onDone(utteranceId: String?) {
                _state.value = SpeechOutputState.Ready
            }
        })
    }

    override suspend fun speakGujarati(text: String) = speak(text, GUJARATI_LOCALE_TAG)

    override suspend fun speak(text: String, languageTag: String) {
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
            val locale = Locale.forLanguageTag(languageTag.ifBlank { GUJARATI_LOCALE_TAG })
            val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                _state.value = SpeechOutputState.Error("આ ભાષાનો અવાજ ફોનમાં ઉપલબ્ધ નથી; લખાણ ચાલુ રહેશે.")
                return@withContext
            }
            selectBestInstalledVoice(locale)
            applyVoiceStyle()
            val parameters = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(
                makeExpressive(text),
                TextToSpeech.QUEUE_FLUSH,
                parameters,
                UUID.randomUUID().toString(),
            )
        }
    }

    override fun setStyle(style: VoiceStyle) {
        voiceStyle = style
        selectedVoiceByLocale.clear()
        applyVoiceStyle()
    }

    private fun applyVoiceStyle() {
        tts?.setPitch(voiceStyle.pitch)
        tts?.setSpeechRate(voiceStyle.rate)
    }

    /**
     * Android TTS quality varies by device. Prefer the highest-quality installed local
     * voice for the requested language so Mitra sounds less robotic and starts faster.
     */
    private fun selectBestInstalledVoice(locale: Locale) {
        val engine = tts ?: return
        val key = "${locale.language}-${locale.country}-${voiceStyle.name}"
        val selected = if (selectedVoiceByLocale.containsKey(key)) {
            selectedVoiceByLocale[key]
        } else {
            val best = engine.voices.orEmpty()
                .asSequence()
                .filter { it.locale.language.equals(locale.language, ignoreCase = true) }
                .maxByOrNull { voiceScore(it, locale) }
            selectedVoiceByLocale[key] = best
            best
        }
        if (selected != null) engine.voice = selected
    }

    private fun voiceScore(voice: Voice, requested: Locale): Int {
        var score = 0
        if (voice.locale.language.equals(requested.language, true)) score += 100
        if (requested.country.isNotBlank() && voice.locale.country.equals(requested.country, true)) score += 30
        if (!voice.isNetworkConnectionRequired) score += 35
        score += when (voice.quality) {
            Voice.QUALITY_VERY_HIGH -> 40
            Voice.QUALITY_HIGH -> 30
            Voice.QUALITY_NORMAL -> 15
            else -> 0
        }
        score += when (voice.latency) {
            Voice.LATENCY_VERY_LOW -> 20
            Voice.LATENCY_LOW -> 15
            Voice.LATENCY_NORMAL -> 8
            else -> 0
        }
        val name = voice.name.lowercase()
        if (voiceStyle == VoiceStyle.CARTOON_ADVENTURE || voiceStyle == VoiceStyle.ENERGETIC_HERO) {
            if ("enhanced" in name || "premium" in name || "wavenet" in name || "neural" in name) score += 12
        }
        return score
    }

    private fun makeExpressive(text: String): String = when (voiceStyle) {
        VoiceStyle.CARTOON_ADVENTURE,
        VoiceStyle.ENERGETIC_HERO,
        VoiceStyle.PLAYFUL_HERO -> text
            .replace("।", "! ")
            .replace(Regex("!{2,}"), "!")
            .replace(Regex("\\s+"), " ")
            .trim()
        VoiceStyle.WARM,
        VoiceStyle.CALM_STORYTELLER -> text
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

        if (supported) {
            selectBestInstalledVoice(Locale("gu", "IN"))
            applyVoiceStyle()
        }
        _state.value = if (supported) SpeechOutputState.Ready else SpeechOutputState.Unavailable
        initialized.completeIfNeeded(supported)
    }

    private fun CompletableDeferred<Boolean>.completeIfNeeded(value: Boolean) {
        if (!isCompleted) complete(value)
    }

    private companion object {
        const val GUJARATI_LOCALE_TAG = "gu-IN"
    }
}
