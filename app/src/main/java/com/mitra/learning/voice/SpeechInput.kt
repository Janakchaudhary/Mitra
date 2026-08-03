package com.mitra.learning.voice

import kotlinx.coroutines.flow.StateFlow

sealed interface SpeechInputState {
    data object Idle : SpeechInputState
    data object Listening : SpeechInputState
    data class Partial(val text: String) : SpeechInputState
    data class Result(val text: String) : SpeechInputState

    /**
     * @param recoverable true when the child can try the microphone again.
     * @param automaticRetry true only for short-lived recognizer states such as BUSY.
     * Automatic retries must stay rare because repeatedly restarting Android's
     * recognizer can itself cause ERROR_TOO_MANY_REQUESTS.
     */
    data class Error(
        val messageGujarati: String,
        val recoverable: Boolean = true,
        val automaticRetry: Boolean = false,
        val errorCode: Int? = null,
    ) : SpeechInputState
}

interface SpeechInput {
    val state: StateFlow<SpeechInputState>
    val isAvailable: Boolean

    suspend fun startListening()
    suspend fun startListening(languageTag: String) = startListening()
    suspend fun stopListening()
    fun cancel()
    fun close()
}
