package com.mitra.learning.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.security.ParentPinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParentPinUiState(
    val pin: String = "",
    val checking: Boolean = false,
    val unlocked: Boolean = false,
    val error: String? = null,
)

class ParentPinViewModel(private val repository: ParentPinRepository) : ViewModel() {
    private val _state = MutableStateFlow(ParentPinUiState())
    val state: StateFlow<ParentPinUiState> = _state.asStateFlow()

    fun updatePin(value: String) {
        _state.value = _state.value.copy(pin = value.filter(Char::isDigit).take(6), error = null)
    }

    fun unlock() = viewModelScope.launch {
        val pin = _state.value.pin
        if (pin.length !in 4..6) {
            _state.value = _state.value.copy(error = "Enter the parent PIN")
            return@launch
        }
        _state.value = _state.value.copy(checking = true, error = null)
        val ok = repository.verify(pin)
        _state.value = _state.value.copy(
            checking = false,
            unlocked = ok,
            error = if (ok) null else "Incorrect PIN",
        )
    }
}
