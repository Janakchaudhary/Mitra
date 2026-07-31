package com.mitra.learning.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechInputContractTest {
    @Test
    fun stopListening_implementsSuspendUnitContract() = runTest {
        val input: SpeechInput = FakeSpeechInput()

        val result: Unit = input.stopListening()

        assertEquals(Unit, result)
    }

    private class FakeSpeechInput : SpeechInput {
        private val mutableState = MutableStateFlow<SpeechInputState>(SpeechInputState.Idle)
        override val state: StateFlow<SpeechInputState> = mutableState
        override val isAvailable: Boolean = true

        override suspend fun startListening(): Unit = Unit
        override suspend fun stopListening(): Unit = Unit
        override fun cancel(): Unit = Unit
        override fun close(): Unit = Unit
    }
}
