package com.mitra.learning.voice

import kotlinx.coroutines.flow.StateFlow

sealed interface SpeechInputState {
    data object Idle : SpeechInputState
    data object Listening : SpeechInputState
    data class Partial(val text: String) : SpeechInputState
    data class Result(val text: String) : SpeechInputState
    data class Error(val messageGujarati: String, val recoverable: Boolean = true) : SpeechInputState
}

interface SpeechInput {
    val state: StateFlow<SpeechInputState>
    val isAvailable: Boolean

    suspend fun startListening()
    suspend fun stopListening()
    fun cancel()
    fun close()
}
