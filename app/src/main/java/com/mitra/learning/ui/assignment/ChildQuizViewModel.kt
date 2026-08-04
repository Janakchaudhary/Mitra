package com.mitra.learning.ui.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.learning.assignment.ParentQuizPlan
import com.mitra.learning.learning.assignment.ParentQuizRepository
import com.mitra.learning.study.practice.MitraPracticeEvaluator
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechInputState
import com.mitra.learning.voice.SpeechOutput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChildQuizUiState(
    val loading: Boolean = true,
    val plan: ParentQuizPlan? = null,
    val questionIndex: Int = 0,
    val answer: String = "",
    val attempts: Int = 0,
    val marks: Int = 0,
    val listening: Boolean = false,
    val awaitingNext: Boolean = false,
    val completed: Boolean = false,
    val feedback: String? = null,
    val error: String? = null,
) {
    val currentQuestion get() = plan?.questions?.getOrNull(questionIndex)
}

class ChildQuizViewModel(
    private val repository: ParentQuizRepository,
    private val speechInput: SpeechInput,
    private val speechOutput: SpeechOutput,
) : ViewModel() {
    private val _state = MutableStateFlow(ChildQuizUiState())
    val state: StateFlow<ChildQuizUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val plan = repository.load()
            _state.value = ChildQuizUiState(loading = false, plan = plan, error = if (plan == null) "Parentએ હજી કસોટી બનાવી નથી." else null)
        }
        viewModelScope.launch {
            speechInput.state.collect { input ->
                when (input) {
                    SpeechInputState.Idle -> _state.value = _state.value.copy(listening = false)
                    SpeechInputState.Listening -> _state.value = _state.value.copy(listening = true, error = null)
                    is SpeechInputState.Partial -> _state.value = _state.value.copy(listening = true, answer = input.text)
                    is SpeechInputState.Result -> {
                        _state.value = _state.value.copy(listening = false, answer = input.text)
                        submit()
                    }
                    is SpeechInputState.Error -> _state.value = _state.value.copy(listening = false, error = input.messageGujarati)
                }
            }
        }
    }

    fun updateAnswer(value: String) {
        if (_state.value.awaitingNext || _state.value.completed) return
        _state.value = _state.value.copy(answer = value.take(250), feedback = null, error = null)
    }

    fun startVoice() = viewModelScope.launch {
        val question = _state.value.currentQuestion ?: return@launch
        speechOutput.stop()
        speechInput.startListening(question.recognitionLanguageTag)
    }

    fun stopVoice() = viewModelScope.launch { speechInput.stopListening() }

    fun replayQuestion() {
        val question = _state.value.currentQuestion ?: return
        viewModelScope.launch { speechOutput.speak(question.spokenPrompt, if (question.recognitionLanguageTag == "en-IN") "en-IN" else "gu-IN") }
    }

    fun submit() {
        val current = _state.value
        val question = current.currentQuestion ?: return
        val raw = current.answer.trim()
        if (raw.isBlank() || current.awaitingNext || current.completed) return
        val evaluation = MitraPracticeEvaluator.evaluate(question.toChallenge(), raw, current.marks)
        val attempt = current.attempts + 1
        if (!evaluation.correct && attempt < 2) {
            val feedback = evaluation.feedbackGujarati
            _state.value = current.copy(attempts = attempt, feedback = feedback, answer = "")
            viewModelScope.launch { speechOutput.speakGujarati("$feedback. ફરી પ્રયત્ન કરો.") }
            return
        }

        val newMarks = current.marks + if (evaluation.correct) 1 else 0
        val feedback = if (evaluation.correct) {
            evaluation.feedbackGujarati
        } else {
            "ચાલો સમજીએ. ${question.correctionGujarati}"
        }
        _state.value = current.copy(
            marks = newMarks,
            attempts = attempt,
            feedback = feedback,
            awaitingNext = true,
            listening = false,
        )
        viewModelScope.launch {
            speechOutput.speakGujarati(feedback)
            delay(if (evaluation.correct) 1_600 else 3_000)
            moveNext(newMarks)
        }
    }

    private fun moveNext(newMarks: Int) {
        val current = _state.value
        val total = current.plan?.questions?.size ?: return
        val nextIndex = current.questionIndex + 1
        if (nextIndex >= total) {
            val closing = "કસોટી પૂરી! $total માંથી $newMarks ગુણ મળ્યા. તમારી મહેનત માટે શાબાશ."
            _state.value = current.copy(completed = true, awaitingNext = false, marks = newMarks, feedback = closing)
            viewModelScope.launch { speechOutput.speakGujarati(closing) }
        } else {
            _state.value = current.copy(
                questionIndex = nextIndex,
                answer = "",
                attempts = 0,
                feedback = null,
                awaitingNext = false,
                marks = newMarks,
            )
            replayQuestion()
        }
    }

    override fun onCleared() {
        speechInput.cancel()
        speechOutput.stop()
        super.onCleared()
    }
}
