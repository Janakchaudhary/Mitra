package com.mitra.learning.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.mitra.learning.voice.VoiceStyle

data class LearningSettings(
    val sessionMinutes: Int = 20,
    val dailyMinutes: Int = 30,
    val parentAccessMinutes: Int = 5,
    val voiceStyle: VoiceStyle = VoiceStyle.WARM,
) {
    fun normalized(): LearningSettings = copy(
        sessionMinutes = sessionMinutes.coerceIn(10, 30),
        dailyMinutes = dailyMinutes.coerceIn(15, 120),
        parentAccessMinutes = parentAccessMinutes.coerceIn(1, 30),
    )
}

private val Context.learningSettingsDataStore by preferencesDataStore(name = "learning_settings")

class LearningSettingsRepository(private val context: Context) {
    private val sessionMinutesKey = intPreferencesKey("session_minutes")
    private val dailyMinutesKey = intPreferencesKey("daily_minutes")
    private val parentAccessMinutesKey = intPreferencesKey("parent_access_minutes")
    private val voiceStyleKey = androidx.datastore.preferences.core.stringPreferencesKey("voice_style")

    val settings: Flow<LearningSettings> = context.learningSettingsDataStore.data.map { prefs ->
        LearningSettings(
            sessionMinutes = prefs[sessionMinutesKey] ?: 20,
            dailyMinutes = prefs[dailyMinutesKey] ?: 30,
            parentAccessMinutes = prefs[parentAccessMinutesKey] ?: 5,
            voiceStyle = runCatching { VoiceStyle.valueOf(prefs[voiceStyleKey] ?: VoiceStyle.WARM.name) }.getOrDefault(VoiceStyle.WARM),
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
        }
    }

    suspend fun reset() {
        context.learningSettingsDataStore.edit { it.clear() }
    }
}
