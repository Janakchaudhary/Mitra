package com.mitra.learning.voice

import kotlinx.coroutines.flow.StateFlow

sealed interface SpeechOutputState {
    data object Initializing : SpeechOutputState
    data object Ready : SpeechOutputState
    data object Speaking : SpeechOutputState
    data object Unavailable : SpeechOutputState
    data class Error(val messageGujarati: String) : SpeechOutputState
}

interface SpeechOutput {
    val state: StateFlow<SpeechOutputState>

    suspend fun speakGujarati(text: String)
    suspend fun speak(text: String, languageTag: String) = speakGujarati(text)
    fun setStyle(style: VoiceStyle) = Unit
    fun stop()
    fun close()
}
