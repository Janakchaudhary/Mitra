package com.mitra.learning.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mitra.learning.voice.VoiceStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class LearningSettings(
    val sessionMinutes: Int = 60,
    val dailyMinutes: Int = 180,
    val parentAccessMinutes: Int = 5,
    val voiceStyle: VoiceStyle = VoiceStyle.CARTOON_ADVENTURE,
) {
    fun normalized(): LearningSettings = copy(
        sessionMinutes = sessionMinutes.coerceIn(15, 60),
        dailyMinutes = dailyMinutes.coerceIn(30, 180),
        parentAccessMinutes = parentAccessMinutes.coerceIn(1, 30),
    )
}

private val Context.learningSettingsDataStore by preferencesDataStore(name = "learning_settings")

class LearningSettingsRepository(private val context: Context) {
    private val sessionMinutesKey = intPreferencesKey("session_minutes")
    private val dailyMinutesKey = intPreferencesKey("daily_minutes")
    private val parentAccessMinutesKey = intPreferencesKey("parent_access_minutes")
    private val voiceStyleKey = stringPreferencesKey("voice_style")
    private val settingsVersionKey = intPreferencesKey("settings_version")

    val settings: Flow<LearningSettings> = context.learningSettingsDataStore.data.map { prefs ->
        val legacy = (prefs[settingsVersionKey] ?: 1) < CURRENT_SETTINGS_VERSION
        LearningSettings(
            // Milestone 22 intentionally upgrades the previous 30/60-minute defaults
            // to one 60-minute session and a three-hour daily allowance.
            sessionMinutes = if (legacy) 60 else prefs[sessionMinutesKey] ?: 60,
            dailyMinutes = if (legacy) 180 else prefs[dailyMinutesKey] ?: 180,
            parentAccessMinutes = prefs[parentAccessMinutesKey] ?: 5,
            voiceStyle = if (legacy) {
                VoiceStyle.CARTOON_ADVENTURE
            } else {
                runCatching {
                    VoiceStyle.valueOf(prefs[voiceStyleKey] ?: VoiceStyle.CARTOON_ADVENTURE.name)
                }.getOrDefault(VoiceStyle.CARTOON_ADVENTURE)
            },
        ).normalized()
    }

    suspend fun get(): LearningSettings = settings.first()

    suspend fun save(value: LearningSettings) {
        val clean = value.normalized()
        context.learningSettingsDataStore.edit { prefs ->
            prefs[sessionMinutesKey] = clean.sessionMinutes
            prefs[dailyMinutesKey] = clean.dailyMinutes
            prefs[parentAccessMinutesKey] = clean.parentAccessMinutes
            prefs[voiceStyleKey] = clean.voiceStyle.name
            prefs[settingsVersionKey] = CURRENT_SETTINGS_VERSION
        }
    }

    suspend fun reset() {
        context.learningSettingsDataStore.edit { it.clear() }
    }

    private companion object {
        const val CURRENT_SETTINGS_VERSION = 2
    }
}
