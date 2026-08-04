package com.mitra.learning.ui.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.learning.assignment.ParentQuizPlan
import com.mitra.learning.learning.assignment.ParentQuizRepository
import com.mitra.learning.learning.assignment.ParentQuizService
import com.mitra.learning.learning.assignment.ParentQuizTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParentQuizBuilderUiState(
    val title: String = "આજની કસોટી",
    val topic: ParentQuizTopic = ParentQuizTopic.MIXED,
    val questionCount: Int = 20,
    val selectedSkillConceptId: String = "builtin-math-add-under-20",
    val creating: Boolean = false,
    val activePlan: ParentQuizPlan? = null,
    val message: String? = null,
)

class ParentQuizBuilderViewModel(
    private val service: ParentQuizService,
    private val repository: ParentQuizRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentQuizBuilderUiState())
    val state: StateFlow<ParentQuizBuilderUiState> = _state.asStateFlow()

    init { refresh() }

    fun setTitle(value: String) { _state.value = _state.value.copy(title = value.take(60), message = null) }
    fun setTopic(value: ParentQuizTopic) { _state.value = _state.value.copy(topic = value, message = null) }
    fun setQuestionCount(value: Int) { _state.value = _state.value.copy(questionCount = value.coerceIn(5, 25), message = null) }
    fun setSkillConcept(value: String) { _state.value = _state.value.copy(selectedSkillConceptId = value, message = null) }

    fun create() = viewModelScope.launch {
        if (_state.value.creating) return@launch
        val current = _state.value
        _state.value = current.copy(creating = true, message = "પ્રશ્નો તૈયાર થાય છે…")
        runCatching { service.create(current.title, current.topic, current.questionCount, current.selectedSkillConceptId) }
            .onSuccess { plan ->
                _state.value = _state.value.copy(
                    creating = false,
                    activePlan = plan,
                    message = "${plan.questions.size} ગુણની કસોટી બાળકના Home પર તૈયાર છે.",
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(creating = false, message = error.message ?: "કસોટી બની શકી નહીં.")
            }
    }

    fun clear() = viewModelScope.launch {
        repository.clear()
        _state.value = _state.value.copy(activePlan = null, message = "કસોટી દૂર થઈ.")
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(activePlan = repository.load())
    }
}
