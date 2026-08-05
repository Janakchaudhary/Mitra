package com.mitra.learning.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.security.ParentAccessManager
import com.mitra.learning.security.ParentPinRepository
import com.mitra.learning.settings.LearningSettingsRepository
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

class ParentPinViewModel(
    private val repository: ParentPinRepository,
    private val accessManager: ParentAccessManager,
    private val settingsRepository: LearningSettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentPinUiState())
    val state: StateFlow<ParentPinUiState> = _state.asStateFlow()

    fun updatePin(value: String) {
        val clean = value.filter(Char::isDigit).take(6)
        _state.value = _state.value.copy(pin = clean, error = null)
        if (clean.length in 4..6) tryAutoUnlock(clean)
    }

    private fun tryAutoUnlock(pin: String) {
        if (_state.value.checking || _state.value.unlocked) return
        viewModelScope.launch {
            val expectedLength = repository.expectedLength()
            if (expectedLength != null && pin.length != expectedLength) return@launch
            _state.value = _state.value.copy(checking = true, error = null)
            val ok = repository.verify(pin)
            if (ok) accessManager.unlock(settingsRepository.get().parentAccessMinutes)
            _state.value = _state.value.copy(
                checking = false,
                unlocked = ok,
                error = if (!ok && pin.length == 6) "Incorrect PIN" else null,
            )
        }
    }

    fun unlockWithDeviceCredential() = viewModelScope.launch {
        accessManager.unlock(settingsRepository.get().parentAccessMinutes)
        _state.value = _state.value.copy(unlocked = true, checking = false, error = null)
    }

    fun onDeviceUnlockError(message: String) {
        _state.value = _state.value.copy(checking = false, error = message)
    }

    fun unlock() = viewModelScope.launch {
        val pin = _state.value.pin
        if (pin.length !in 4..6) {
            _state.value = _state.value.copy(error = "Enter the parent PIN")
            return@launch
        }
        _state.value = _state.value.copy(checking = true, error = null)
        val ok = repository.verify(pin)
        if (ok) {
            accessManager.unlock(settingsRepository.get().parentAccessMinutes)
        }
        _state.value = _state.value.copy(
            checking = false,
            unlocked = ok,
            error = if (ok) null else "Incorrect PIN",
        )
    }
}
