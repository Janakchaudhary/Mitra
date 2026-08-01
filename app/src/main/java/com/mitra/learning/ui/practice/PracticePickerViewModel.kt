package com.mitra.learning.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PracticeChoice(
    val conceptId: String,
    val titleGujarati: String,
    val subject: String,
    val mastery: Float,
    val fromBook: Boolean,
)

data class PracticePickerUiState(
    val loading: Boolean = true,
    val choices: List<PracticeChoice> = emptyList(),
    val error: String? = null,
)

class PracticePickerViewModel(
    private val repository: ProgressRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PracticePickerUiState())
    val state: StateFlow<PracticePickerUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val mastery = repository.mastery().associateBy { it.conceptId }
            repository.concepts()
                .filter { it.practiceReady }
                .map { concept ->
                    PracticeChoice(
                        conceptId = concept.id,
                        titleGujarati = concept.titleGujarati,
                        subject = concept.subject,
                        mastery = mastery[concept.id]?.mastery ?: 0f,
                        fromBook = !concept.builtIn,
                    )
                }
                .sortedWith(compareBy<PracticeChoice> { it.subject }.thenBy { it.mastery }.thenBy { it.titleGujarati })
        }.onSuccess { choices ->
            _state.value = PracticePickerUiState(loading = false, choices = choices)
        }.onFailure { error ->
            _state.value = PracticePickerUiState(loading = false, error = error.message ?: "Practice list could not be loaded")
        }
    }
}
