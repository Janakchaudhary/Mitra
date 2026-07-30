package com.mitra.learning.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.security.ParentPinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SetupPinUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val saving: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null,
)

class SetupPinViewModel(
    private val repository: ParentPinRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SetupPinUiState())
    val state: StateFlow<SetupPinUiState> = _state.asStateFlow()

    fun updatePin(value: String) {
        _state.value = _state.value.copy(pin = value.filter(Char::isDigit).take(6), error = null)
    }

    fun updateConfirmation(value: String) {
        _state.value = _state.value.copy(confirmPin = value.filter(Char::isDigit).take(6), error = null)
    }

    fun save() {
        val current = _state.value
        when {
            current.pin.length !in 4..6 -> _state.value = current.copy(error = "Use a 4–6 digit PIN")
            current.pin != current.confirmPin -> _state.value = current.copy(error = "PINs do not match")
            else -> viewModelScope.launch {
                _state.value = current.copy(saving = true, error = null)
                runCatching { repository.setPin(current.pin) }
                    .onSuccess { _state.value = _state.value.copy(saving = false, completed = true) }
                    .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
            }
        }
    }
}
