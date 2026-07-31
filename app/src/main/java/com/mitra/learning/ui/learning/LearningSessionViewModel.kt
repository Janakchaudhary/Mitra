package com.mitra.learning.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.learning.engine.LearningEngine
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.model.SessionPlan
import com.mitra.learning.learning.model.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearningSessionUiState(
    val loading: Boolean = true,
    val sessionId: String? = null,
    val conceptId: String? = null,
    val conceptTitleGujarati: String = "",
    val questions: List<LearningQuestion> = emptyList(),
    val questionIndex: Int = 0,
    val answer: String = "",
    val feedback: String? = null,
    val lastResult: AttemptResult? = null,
    val awaitingNext: Boolean = false,
    val summary: SessionSummary? = null,
    val error: String? = null,
) {
    val currentQuestion: LearningQuestion?
        get() = questions.getOrNull(questionIndex)

    val completed: Boolean
        get() = summary != null
}

class LearningSessionViewModel(
    private val engine: LearningEngine,
) : ViewModel() {
    private val _state = MutableStateFlow(LearningSessionUiState())
    val state: StateFlow<LearningSessionUiState> = _state.asStateFlow()

    init {
        start()
    }

    fun updateAnswer(value: String) {
        if (_state.value.awaitingNext) return
        _state.update { it.copy(answer = value.take(12), error = null) }
    }

    fun submit() {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        val conceptId = current.conceptId ?: return
        val question = current.currentQuestion ?: return
        if (current.awaitingNext || current.loading) return
        if (current.answer.isBlank()) {
            _state.update { it.copy(error = "જવાબ લખો અથવા છોડો દબાવો.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                engine.submitAnswer(
                    sessionId = sessionId,
                    conceptId = conceptId,
                    question = question,
                    answerText = current.answer,
                )
            }.onSuccess { feedback ->
                _state.update {
                    it.copy(
                        loading = false,
                        feedback = feedback.messageGujarati,
                        lastResult = feedback.result,
                        awaitingNext = true,
                    )
                }
            }.onFailure { failure ->
                _state.update { it.copy(loading = false, error = failure.message ?: "કંઈક ખોટું થયું.") }
            }
        }
    }

    fun skip() {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        val conceptId = current.conceptId ?: return
        val question = current.currentQuestion ?: return
        if (current.awaitingNext || current.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { engine.skipQuestion(sessionId, conceptId, question) }
                .onSuccess { feedback ->
                    _state.update {
                        it.copy(
                            loading = false,
                            feedback = feedback.messageGujarati,
                            lastResult = feedback.result,
                            awaitingNext = true,
                        )
                    }
                }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, error = failure.message ?: "કંઈક ખોટું થયું.") }
                }
        }
    }

    fun next() {
        val current = _state.value
        if (!current.awaitingNext || current.loading) return

        if (current.questionIndex >= current.questions.lastIndex) {
            complete()
        } else {
            _state.update {
                it.copy(
                    questionIndex = it.questionIndex + 1,
                    answer = "",
                    feedback = null,
                    lastResult = null,
                    awaitingNext = false,
                    error = null,
                )
            }
        }
    }

    fun stop(onStopped: () -> Unit) {
        val id = _state.value.sessionId
        if (id == null) {
            onStopped()
            return
        }
        viewModelScope.launch {
            runCatching { engine.stopSession(id) }
            onStopped()
        }
    }

    private fun start() {
        viewModelScope.launch {
            runCatching { engine.startSession() }
                .onSuccess { plan -> applyPlan(plan) }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, error = failure.message ?: "સત્ર શરૂ થઈ શક્યું નહીં.") }
                }
        }
    }

    private fun applyPlan(plan: SessionPlan?) {
        if (plan == null) {
            _state.update { it.copy(loading = false, error = "હજુ શીખવા માટે કોઈ વિષય તૈયાર નથી.") }
            return
        }
        _state.value = LearningSessionUiState(
            loading = false,
            sessionId = plan.sessionId,
            conceptId = plan.concept.id,
            conceptTitleGujarati = plan.concept.titleGujarati,
            questions = plan.questions,
        )
    }

    private fun complete() {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { engine.completeSession(sessionId, current.conceptTitleGujarati) }
                .onSuccess { summary ->
                    _state.update { it.copy(loading = false, summary = summary, awaitingNext = false) }
                }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, error = failure.message ?: "સત્ર પૂરુ થઈ શક્યું નહીં.") }
                }
        }
    }
}
