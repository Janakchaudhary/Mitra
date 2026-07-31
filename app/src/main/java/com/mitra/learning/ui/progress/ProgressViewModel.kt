package com.mitra.learning.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.repository.ProgressRepository
import com.mitra.learning.learning.progress.ProgressAnalyzer
import com.mitra.learning.learning.progress.ProgressDashboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressUiState(
    val loading: Boolean = true,
    val dashboard: ProgressDashboard? = null,
    val error: String? = null,
)

class ProgressViewModel(
    private val repository: ProgressRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                ProgressAnalyzer.analyze(
                    concepts = repository.concepts(),
                    mastery = repository.mastery(),
                    prerequisites = repository.prerequisites(),
                    sessions = repository.sessions(),
                    attempts = repository.attempts(),
                )
            }.onSuccess { dashboard ->
                _state.value = ProgressUiState(loading = false, dashboard = dashboard)
            }.onFailure { error ->
                _state.value = ProgressUiState(
                    loading = false,
                    error = error.message ?: "Progress could not be loaded.",
                )
            }
        }
    }
}
