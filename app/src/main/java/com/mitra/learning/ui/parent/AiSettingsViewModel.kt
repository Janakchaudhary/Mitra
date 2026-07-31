package com.mitra.learning.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.ai.ConfigurableAiGateway
import com.mitra.learning.ai.settings.AiProviderConfig
import com.mitra.learning.ai.settings.AiProviderType
import com.mitra.learning.ai.settings.AiSettingsRepository
import com.mitra.learning.security.AndroidKeystoreSecretStore
import com.mitra.learning.security.SecretStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI

data class AiSettingsUiState(
    val loading: Boolean = true,
    val remoteEnabled: Boolean = false,
    val baseUrl: String = AiProviderConfig.DEFAULT_OPENAI_BASE_URL,
    val model: String = AiProviderConfig.DEFAULT_OPENAI_MODEL,
    val apiKeyDraft: String = "",
    val hasStoredApiKey: Boolean = false,
    val saving: Boolean = false,
    val testing: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class AiSettingsViewModel(
    private val repository: AiSettingsRepository,
    private val secretStore: SecretStore,
    private val gateway: ConfigurableAiGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(AiSettingsUiState())
    val state: StateFlow<AiSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val config = repository.getConfig()
            _state.value = AiSettingsUiState(
                loading = false,
                remoteEnabled = config.provider == AiProviderType.OPENAI && config.remoteEnabled,
                baseUrl = config.baseUrl,
                model = config.model,
                hasStoredApiKey = !secretStore.readSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY).isNullOrBlank(),
            )
        }
    }

    fun setRemoteEnabled(value: Boolean) = _state.update { it.copy(remoteEnabled = value, message = null, error = null) }
    fun setBaseUrl(value: String) = _state.update { it.copy(baseUrl = value.take(200), message = null, error = null) }
    fun setModel(value: String) = _state.update { it.copy(model = value.take(100), message = null, error = null) }
    fun setApiKey(value: String) = _state.update { it.copy(apiKeyDraft = value.take(300), message = null, error = null) }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, message = null, error = null) }
            runCatching { persistCurrentState() }
                .onSuccess {
                    _state.update {
                        it.copy(
                            saving = false,
                            apiKeyDraft = "",
                            hasStoredApiKey = !secretStore.readSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY).isNullOrBlank(),
                            message = "AI settings saved.",
                        )
                    }
                }
                .onFailure { failure ->
                    _state.update { it.copy(saving = false, error = failure.message ?: "Could not save AI settings.") }
                }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.update { it.copy(testing = true, message = null, error = null) }
            runCatching {
                persistCurrentState()
                gateway.testConfiguredProvider()
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        testing = false,
                        apiKeyDraft = "",
                        hasStoredApiKey = !secretStore.readSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY).isNullOrBlank(),
                        message = result,
                    )
                }
            }.onFailure { failure ->
                _state.update { it.copy(testing = false, error = failure.message ?: "Connection test failed.") }
            }
        }
    }

    fun clearApiKey() {
        secretStore.removeSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY)
        _state.update {
            it.copy(apiKeyDraft = "", hasStoredApiKey = false, message = "Saved API key removed.", error = null)
        }
    }

    private suspend fun persistCurrentState() {
        val current = _state.value
        val baseUrl = current.baseUrl.trim().trimEnd('/')
        val uri = runCatching { URI(baseUrl) }.getOrNull()
        val host = uri?.host.orEmpty().lowercase()
        require(uri?.scheme == "https") { "AI base URL must use HTTPS." }
        require(host == "api.openai.com" || host.endsWith(".api.openai.com")) {
            "Milestone 5 only sends the API key to official OpenAI API domains."
        }
        require(current.model.isNotBlank()) { "Model is required." }
        current.apiKeyDraft.trim().takeIf { it.isNotBlank() }?.let { key ->
            secretStore.saveSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY, key)
        }
        if (current.remoteEnabled) {
            require(!secretStore.readSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY).isNullOrBlank()) {
                "Enter an API key before enabling remote AI."
            }
        }
        repository.save(
            AiProviderConfig(
                provider = if (current.remoteEnabled) AiProviderType.OPENAI else AiProviderType.MOCK,
                remoteEnabled = current.remoteEnabled,
                baseUrl = baseUrl,
                model = current.model,
            )
        )
    }
}
