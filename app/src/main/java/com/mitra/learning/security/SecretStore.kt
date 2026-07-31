package com.mitra.learning.security

interface SecretStore {
    fun saveSecret(key: String, value: String)
    fun readSecret(key: String): String?
    fun removeSecret(key: String)
}
