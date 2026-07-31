package com.mitra.learning.ai.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiSettingsDataStore by preferencesDataStore(name = "ai_settings")

class AiSettingsRepository(private val context: Context) {
    private val providerKey = stringPreferencesKey("provider")
    private val enabledKey = booleanPreferencesKey("remote_enabled")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val modelKey = stringPreferencesKey("model")

    val config: Flow<AiProviderConfig> = context.aiSettingsDataStore.data.map { prefs ->
        AiProviderConfig(
            provider = runCatching {
                AiProviderType.valueOf(prefs[providerKey] ?: AiProviderType.MOCK.name)
            }.getOrDefault(AiProviderType.MOCK),
            remoteEnabled = prefs[enabledKey] ?: false,
            baseUrl = prefs[baseUrlKey].orEmpty().ifBlank { AiProviderConfig.DEFAULT_OPENAI_BASE_URL },
            model = prefs[modelKey].orEmpty().ifBlank { AiProviderConfig.DEFAULT_OPENAI_MODEL },
        )
    }

    suspend fun getConfig(): AiProviderConfig = config.first()

    suspend fun save(config: AiProviderConfig) {
        val cleanBaseUrl = config.baseUrl.trim().trimEnd('/').ifBlank { AiProviderConfig.DEFAULT_OPENAI_BASE_URL }
        val cleanModel = config.model.trim().ifBlank { AiProviderConfig.DEFAULT_OPENAI_MODEL }
        context.aiSettingsDataStore.edit { prefs ->
            prefs[providerKey] = config.provider.name
            prefs[enabledKey] = config.remoteEnabled
            prefs[baseUrlKey] = cleanBaseUrl
            prefs[modelKey] = cleanModel
        }
    }
}
