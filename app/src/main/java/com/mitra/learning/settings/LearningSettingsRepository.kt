package com.mitra.learning.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class LearningSettings(
    val sessionMinutes: Int = 20,
    val dailyMinutes: Int = 30,
    val parentAccessMinutes: Int = 5,
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

    val settings: Flow<LearningSettings> = context.learningSettingsDataStore.data.map { prefs ->
        LearningSettings(
            sessionMinutes = prefs[sessionMinutesKey] ?: 20,
            dailyMinutes = prefs[dailyMinutesKey] ?: 30,
            parentAccessMinutes = prefs[parentAccessMinutesKey] ?: 5,
        ).normalized()
    }

    suspend fun get(): LearningSettings = settings.first()

    suspend fun save(value: LearningSettings) {
        val clean = value.normalized()
        context.learningSettingsDataStore.edit { prefs ->
            prefs[sessionMinutesKey] = clean.sessionMinutes
            prefs[dailyMinutesKey] = clean.dailyMinutes
            prefs[parentAccessMinutesKey] = clean.parentAccessMinutes
        }
    }

    suspend fun reset() {
        context.learningSettingsDataStore.edit { it.clear() }
    }
}
