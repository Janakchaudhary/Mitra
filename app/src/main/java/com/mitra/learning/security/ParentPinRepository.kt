package com.mitra.learning.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.securityDataStore by preferencesDataStore(name = "security")

class ParentPinRepository(private val context: Context) {
    private val saltKey = stringPreferencesKey("parent_pin_salt")
    private val hashKey = stringPreferencesKey("parent_pin_hash")
    private val lengthKey = intPreferencesKey("parent_pin_length")

    suspend fun hasPin(): Boolean {
        val prefs = context.securityDataStore.data.first()
        return !prefs[saltKey].isNullOrBlank() && !prefs[hashKey].isNullOrBlank()
    }

    suspend fun setPin(pin: String) {
        require(pin.length in 4..6 && pin.all(Char::isDigit)) { "PIN must be 4 to 6 digits" }
        val encoded = PinCodec.encode(pin.toCharArray())
        context.securityDataStore.edit { prefs ->
            prefs[saltKey] = encoded.saltBase64
            prefs[hashKey] = encoded.hashBase64
            prefs[lengthKey] = pin.length
        }
    }

    suspend fun verify(pin: String): Boolean {
        val prefs = context.securityDataStore.data.first()
        val salt = prefs[saltKey] ?: return false
        val hash = prefs[hashKey] ?: return false
        val verified = runCatching {
            PinCodec.verify(pin.toCharArray(), EncodedPin(salt, hash))
        }.getOrDefault(false)
        if (verified && prefs[lengthKey] == null) {
            context.securityDataStore.edit { it[lengthKey] = pin.length }
        }
        return verified
    }

    suspend fun expectedLength(): Int? = context.securityDataStore.data.first()[lengthKey]

    suspend fun clear() {
        context.securityDataStore.edit { it.clear() }
    }
}
