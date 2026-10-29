package com.iptvtv.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Session-wide lock for adult content. Starts locked on every app start;
 * unlocked by entering the PIN, and stays unlocked until the process ends.
 */
class NsfwGate(private val settings: SettingsStore) {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    suspend fun tryUnlock(pin: String): Boolean {
        val ok = settings.checkPin(pin)
        if (ok) _unlocked.value = true
        return ok
    }

    fun lock() {
        _unlocked.value = false
    }
}
