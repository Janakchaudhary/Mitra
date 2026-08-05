package com.mitra.learning.voice

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Waits for a real TTS completion callback and falls back to a short reading-time estimate. */
suspend fun SpeechOutput.speakAndAwait(
    text: String,
    languageTag: String = "gu-IN",
    maximumMillis: Long = 12_000L,
) = coroutineScope {
    if (text.isBlank()) return@coroutineScope
    val startSignal = async(start = CoroutineStart.UNDISPATCHED) {
        state.drop(1).first {
            it is SpeechOutputState.Speaking ||
                it is SpeechOutputState.Error ||
                it is SpeechOutputState.Unavailable
        }
    }
    speak(text, languageTag)
    when (withTimeoutOrNull(1_500L) { startSignal.await() }) {
        SpeechOutputState.Speaking -> {
            withTimeoutOrNull(maximumMillis) {
                state.first { it !is SpeechOutputState.Speaking }
            }
        }
        else -> {
            startSignal.cancel()
            delay((text.length * 55L).coerceIn(700L, 4_500L))
        }
    }
}
