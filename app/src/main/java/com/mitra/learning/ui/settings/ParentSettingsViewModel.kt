package com.mitra.learning.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.reset.AppDataResetService
import com.mitra.learning.data.backup.MitraBackupService
import com.mitra.learning.settings.LearningSettings
import com.mitra.learning.settings.LearningSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mitra.learning.voice.SpeechOutput
import com.mitra.learning.voice.VoiceStyle


data class ParentSettingsUiState(
    val loading: Boolean = true,
    val sessionMinutes: Int = 20,
    val dailyMinutes: Int = 30,
    val parentAccessMinutes: Int = 5,
    val voiceStyle: VoiceStyle = VoiceStyle.WARM,
    val busy: Boolean = false,
    val message: String? = null,
    val fullResetCompleted: Boolean = false,
)

class ParentSettingsViewModel(
    private val settingsRepository: LearningSettingsRepository,
    private val resetService: AppDataResetService,
    private val speechOutput: SpeechOutput,
    private val backupService: MitraBackupService? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentSettingsUiState())
    val state: StateFlow<ParentSettingsUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val settings = settingsRepository.get()
        _state.value = ParentSettingsUiState(
            loading = false,
            sessionMinutes = settings.sessionMinutes,
            dailyMinutes = settings.dailyMinutes,
            parentAccessMinutes = settings.parentAccessMinutes,
            voiceStyle = settings.voiceStyle,
        )
    }

    fun setSessionMinutes(value: Int) { _state.value = _state.value.copy(sessionMinutes = value, message = null) }
    fun setDailyMinutes(value: Int) { _state.value = _state.value.copy(dailyMinutes = value, message = null) }
    fun setParentAccessMinutes(value: Int) { _state.value = _state.value.copy(parentAccessMinutes = value, message = null) }
    fun setVoiceStyle(value: VoiceStyle) {
        _state.value = _state.value.copy(voiceStyle = value, message = null)
        speechOutput.setStyle(value)
    }

    fun previewVoice() = viewModelScope.launch {
        speechOutput.setStyle(_state.value.voiceStyle)
        speechOutput.speakGujarati("નમસ્તે! હું મિત્ર છું. આજે આપણે મજા સાથે શીખીશું!")
    }

    fun save() = viewModelScope.launch {
        val current = _state.value
        _state.value = current.copy(busy = true, message = null)
        runCatching {
            settingsRepository.save(
                LearningSettings(
                    sessionMinutes = current.sessionMinutes,
                    dailyMinutes = current.dailyMinutes,
                    parentAccessMinutes = current.parentAccessMinutes,
                    voiceStyle = current.voiceStyle,
                )
            )
        }.onSuccess {
            speechOutput.setStyle(current.voiceStyle)
            _state.value = _state.value.copy(busy = false, message = "Settings saved")
        }.onFailure { error ->
            _state.value = _state.value.copy(busy = false, message = error.message ?: "Could not save settings")
        }
    }


    fun exportBackup(uri: Uri) = viewModelScope.launch {
        val service = backupService ?: return@launch
        _state.value = _state.value.copy(busy = true, message = null)
        service.exportTo(uri)
            .onSuccess { _state.value = _state.value.copy(busy = false, message = "Backup saved. Keep it private.") }
            .onFailure { error -> _state.value = _state.value.copy(busy = false, message = error.message ?: "Backup failed") }
    }

    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        val service = backupService ?: return@launch
        _state.value = _state.value.copy(busy = true, message = "Restoring backup…")
        service.restoreFrom(uri)
            .onSuccess {
                _state.value = _state.value.copy(busy = false, message = "Backup restored. Mitra will restart.")
                service.restartApp()
            }
            .onFailure { error -> _state.value = _state.value.copy(busy = false, message = error.message ?: "Restore failed") }
    }

    fun resetProgress() = runReset("Learning progress reset") { resetService.resetLearningProgress() }
    fun resetBookAnalysis() = runReset("Book analysis removed. PDFs are still on this phone.") { resetService.resetBookAnalysis() }

    fun resetEverything() = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, message = null)
        runCatching { resetService.resetEverything() }
            .onSuccess { _state.value = _state.value.copy(busy = false, fullResetCompleted = true) }
            .onFailure { error ->
                _state.value = _state.value.copy(busy = false, message = error.message ?: "Reset failed")
            }
    }

    private fun runReset(success: String, block: suspend () -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true, message = null)
        runCatching { block() }
            .onSuccess { _state.value = _state.value.copy(busy = false, message = success) }
            .onFailure { error ->
                _state.value = _state.value.copy(busy = false, message = error.message ?: "Operation failed")
            }
    }
}
