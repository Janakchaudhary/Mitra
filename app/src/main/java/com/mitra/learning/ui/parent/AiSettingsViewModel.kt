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
    val provider: AiProviderType = AiProviderType.OPENAI,
    val baseUrl: String = AiProviderConfig.DEFAULT_OPENAI_BASE_URL,
    val model: String = AiProviderConfig.DEFAULT_OPENAI_MODEL,
    val cloudflareAccountId: String = "",
    val credentialDraft: String = "",
    val hasStoredOpenAiKey: Boolean = false,
    val hasStoredCloudflareToken: Boolean = false,
    val saving: Boolean = false,
    val testing: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val hasStoredCredential: Boolean
        get() = when (provider) {
            AiProviderType.CLOUDFLARE -> hasStoredCloudflareToken
            AiProviderType.OPENAI -> hasStoredOpenAiKey
            AiProviderType.MOCK -> false
        }
}

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
                remoteEnabled = config.remoteEnabled,
                provider = config.provider.takeUnless { it == AiProviderType.MOCK } ?: AiProviderType.OPENAI,
                baseUrl = config.baseUrl,
                model = config.model,
                cloudflareAccountId = config.cloudflareAccountId,
                hasStoredOpenAiKey = hasSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY),
                hasStoredCloudflareToken = hasSecret(AndroidKeystoreSecretStore.CLOUDFLARE_API_TOKEN),
            )
        }
    }

    fun setRemoteEnabled(value: Boolean) = _state.update { it.copy(remoteEnabled = value, message = null, error = null) }

    fun setProvider(value: AiProviderType) {
        if (value == AiProviderType.MOCK) return
        _state.update { old ->
            val oldDefault = AiProviderConfig.defaultModel(old.provider)
            val nextModel = if (old.model.isBlank() || old.model == oldDefault) {
                AiProviderConfig.defaultModel(value)
            } else if (old.provider != value) {
                AiProviderConfig.defaultModel(value)
            } else old.model
            old.copy(
                provider = value,
                model = nextModel,
                credentialDraft = "",
                message = null,
                error = null,
            )
        }
    }

    fun setBaseUrl(value: String) = _state.update { it.copy(baseUrl = value.take(200), message = null, error = null) }
    fun setModel(value: String) = _state.update { it.copy(model = value.take(120), message = null, error = null) }
    fun setCloudflareAccountId(value: String) = _state.update {
        it.copy(cloudflareAccountId = value.filter { it.isLetterOrDigit() }.take(40), message = null, error = null)
    }
    fun setCredential(value: String) = _state.update { it.copy(credentialDraft = value.take(500), message = null, error = null) }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, message = null, error = null) }
            runCatching { persistCurrentState() }
                .onSuccess {
                    refreshSecrets("AI settings saved.", saving = false)
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
                refreshSecrets(result, testing = false)
            }.onFailure { failure ->
                _state.update { it.copy(testing = false, error = failure.message ?: "Connection test failed.") }
            }
        }
    }

    fun clearCredential() {
        val key = when (_state.value.provider) {
            AiProviderType.OPENAI -> AndroidKeystoreSecretStore.OPENAI_API_KEY
            AiProviderType.CLOUDFLARE -> AndroidKeystoreSecretStore.CLOUDFLARE_API_TOKEN
            AiProviderType.MOCK -> return
        }
        secretStore.removeSecret(key)
        refreshSecrets("Saved credential removed.")
    }

    private suspend fun persistCurrentState() {
        val current = _state.value
        require(current.model.isNotBlank()) { "Model is required." }

        when (current.provider) {
            AiProviderType.OPENAI -> {
                val baseUrl = current.baseUrl.trim().trimEnd('/')
                val uri = runCatching { URI(baseUrl) }.getOrNull()
                val host = uri?.host.orEmpty().lowercase()
                require(uri?.scheme == "https") { "OpenAI base URL must use HTTPS." }
                require(host == "api.openai.com" || host.endsWith(".api.openai.com")) {
                    "OpenAI credentials are only sent to official OpenAI API domains."
                }
                saveDraftIfPresent(AndroidKeystoreSecretStore.OPENAI_API_KEY)
                if (current.remoteEnabled) {
                    require(hasSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY)) {
                        "Enter an OpenAI API key before enabling OpenAI."
                    }
                }
                repository.save(
                    AiProviderConfig(
                        provider = AiProviderType.OPENAI,
                        remoteEnabled = current.remoteEnabled,
                        baseUrl = baseUrl,
                        model = current.model.trim(),
                        cloudflareAccountId = current.cloudflareAccountId.trim(),
                    )
                )
            }

            AiProviderType.CLOUDFLARE -> {
                val accountId = current.cloudflareAccountId.trim()
                if (current.remoteEnabled) {
                    require(accountId.matches(Regex("[A-Fa-f0-9]{32}"))) {
                        "Enter the 32-character Cloudflare Account ID shown in Workers AI → Use REST API."
                    }
                }
                require(current.model.trim().startsWith("@cf/")) {
                    "Cloudflare free mode only allows Cloudflare-hosted @cf/... Workers AI models."
                }
                saveDraftIfPresent(AndroidKeystoreSecretStore.CLOUDFLARE_API_TOKEN)
                if (current.remoteEnabled) {
                    require(hasSecret(AndroidKeystoreSecretStore.CLOUDFLARE_API_TOKEN)) {
                        "Enter a Cloudflare Workers AI API token before enabling Cloudflare."
                    }
                }
                repository.save(
                    AiProviderConfig(
                        provider = AiProviderType.CLOUDFLARE,
                        remoteEnabled = current.remoteEnabled,
                        baseUrl = AiProviderConfig.DEFAULT_OPENAI_BASE_URL,
                        model = current.model.trim(),
                        cloudflareAccountId = accountId,
                    )
                )
            }

            AiProviderType.MOCK -> repository.save(AiProviderConfig(remoteEnabled = false))
        }
    }

    private fun saveDraftIfPresent(key: String) {
        _state.value.credentialDraft.trim().takeIf { it.isNotBlank() }?.let { secretStore.saveSecret(key, it) }
    }

    private fun hasSecret(key: String): Boolean = !secretStore.readSecret(key).isNullOrBlank()

    private fun refreshSecrets(message: String, saving: Boolean? = null, testing: Boolean? = null) {
        _state.update {
            it.copy(
                saving = saving ?: it.saving,
                testing = testing ?: it.testing,
                credentialDraft = "",
                hasStoredOpenAiKey = hasSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY),
                hasStoredCloudflareToken = hasSecret(AndroidKeystoreSecretStore.CLOUDFLARE_API_TOKEN),
                message = message,
                error = null,
            )
        }
    }
}
