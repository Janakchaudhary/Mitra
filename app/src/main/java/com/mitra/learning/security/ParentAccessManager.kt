package com.mitra.learning.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParentAccessManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private var expiryJob: Job? = null
    private var backgroundLockJob: Job? = null

    fun unlock(minutes: Int) {
        expiryJob?.cancel()
        backgroundLockJob?.cancel()
        _unlocked.value = true
        expiryJob = scope.launch {
            delay(minutes.coerceIn(1, 30) * 60_000L)
            _unlocked.value = false
        }
    }

    /**
     * Relock after a short background grace period. The grace avoids losing parent context when
     * Android briefly leaves Mitra to show the system PDF picker or another permission surface.
     */
    fun onAppBackgrounded(graceMillis: Long = 30_000L) {
        if (!_unlocked.value) return
        backgroundLockJob?.cancel()
        backgroundLockJob = scope.launch {
            delay(graceMillis.coerceAtLeast(0L))
            _unlocked.value = false
        }
    }

    fun onAppForegrounded() {
        backgroundLockJob?.cancel()
        backgroundLockJob = null
    }

    fun lock() {
        expiryJob?.cancel()
        backgroundLockJob?.cancel()
        expiryJob = null
        backgroundLockJob = null
        _unlocked.value = false
    }
}
