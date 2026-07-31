package com.mitra.learning.ui.learning

import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.engine.LearningEngine
import com.mitra.learning.learning.model.AnswerFeedback
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.model.SessionPlan
import com.mitra.learning.learning.model.SessionSummary
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechInputState
import com.mitra.learning.voice.SpeechOutput
import com.mitra.learning.voice.SpeechOutputState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearningSessionVoiceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `session speaks first question and submits recognized answer`() = runTest {
        val engine = FakeLearningEngine()
        val input = FakeSpeechInput()
        val output = FakeSpeechOutput()
        val vm = LearningSessionViewModel(engine, input, output)

        advanceUntilIdle()
        assertTrue(output.spoken.contains("૪ પછી કયો અંક આવે?"))

        input.emit(SpeechInputState.Result("૫"))
        advanceUntilIdle()

        assertEquals("૫", engine.lastSubmittedAnswer)
        assertTrue(vm.state.value.awaitingNext)
        assertEquals(AttemptResult.CORRECT, vm.state.value.lastResult)
        // Tutor feedback may include friendly Gujarati around the keyword (for example, "હા! સાચું.").
        assertTrue(output.spoken.any { it.contains("સાચું") })
    }

    @Test
    fun `spoken stop requests session exit without submitting answer`() = runTest {
        val engine = FakeLearningEngine()
        val input = FakeSpeechInput()
        val output = FakeSpeechOutput()
        val vm = LearningSessionViewModel(engine, input, output)

        advanceUntilIdle()
        input.emit(SpeechInputState.Result("બસ"))
        advanceUntilIdle()

        assertTrue(vm.state.value.exitRequested)
        assertEquals(null, engine.lastSubmittedAnswer)
    }

    private class FakeSpeechInput : SpeechInput {
        private val mutableState = MutableStateFlow<SpeechInputState>(SpeechInputState.Idle)
        override val state: StateFlow<SpeechInputState> = mutableState
        override val isAvailable: Boolean = true

        override suspend fun startListening() {
            mutableState.value = SpeechInputState.Listening
        }

        override suspend fun stopListening() = Unit
        override fun cancel() { mutableState.value = SpeechInputState.Idle }
        override fun close() = Unit

        fun emit(value: SpeechInputState) {
            mutableState.value = value
        }
    }

    private class FakeSpeechOutput : SpeechOutput {
        private val mutableState = MutableStateFlow<SpeechOutputState>(SpeechOutputState.Ready)
        override val state: StateFlow<SpeechOutputState> = mutableState
        val spoken = mutableListOf<String>()

        override suspend fun speakGujarati(text: String) {
            spoken += text
            mutableState.value = SpeechOutputState.Speaking
            mutableState.value = SpeechOutputState.Ready
        }

        override fun stop() = Unit
        override fun close() = Unit
    }

    private class FakeLearningEngine : LearningEngine {
        var lastSubmittedAnswer: String? = null

        override suspend fun startSession(questionCount: Int): SessionPlan = SessionPlan(
            sessionId = "session-1",
            concept = ConceptEntity(
                id = "counting",
                subject = "Math",
                standard = 2,
                language = "gu-IN",
                titleGujarati = "ગણતરી",
                titleEnglish = "Counting",
                descriptionGujarati = "ગણતરી શીખો",
                difficulty = 1,
                expectedLearningOutcome = "૧ થી ૨૦ ગણવું",
                sortOrder = 1,
                builtIn = true,
                bookId = null,
                chapterId = null,
                sourcePageStart = null,
                sourcePageEnd = null,
            ),
            questions = listOf(
                LearningQuestion(
                    id = "q1",
                    promptGujarati = "૪ પછી કયો અંક આવે?",
                    expectedAnswer = 5,
                )
            ),
        )

        override suspend fun submitAnswer(
            sessionId: String,
            conceptId: String,
            question: LearningQuestion,
            answerText: String,
            hintsUsed: Int,
        ): AnswerFeedback {
            lastSubmittedAnswer = answerText
            return AnswerFeedback(
                result = AttemptResult.CORRECT,
                messageGujarati = "હા! સાચું.",
                expectedAnswer = question.expectedAnswer,
                mastery = 0.08f,
            )
        }

        override suspend fun skipQuestion(
            sessionId: String,
            conceptId: String,
            question: LearningQuestion,
        ): AnswerFeedback = AnswerFeedback(
            result = AttemptResult.SKIPPED,
            messageGujarati = "ઠીક છે.",
            expectedAnswer = question.expectedAnswer,
            mastery = 0f,
        )

        override suspend fun completeSession(
            sessionId: String,
            conceptTitleGujarati: String,
        ): SessionSummary = SessionSummary(conceptTitleGujarati, 1, 1, 0.08f)

        override suspend fun stopSession(sessionId: String) = Unit
    }
}
