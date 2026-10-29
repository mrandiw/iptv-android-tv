package com.iptvtv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "settings")

/** User preferences. The adult-content PIN is stored as a SHA-256 hash, never as plain text. */
class SettingsStore(private val context: Context) {

    val preferredCountry: Flow<String> = context.dataStore.data.map { it[KEY_COUNTRY] ?: DEFAULT_COUNTRY }

    suspend fun setPreferredCountry(code: String) {
        context.dataStore.edit { it[KEY_COUNTRY] = code }
    }

    suspend fun lastSyncMillis(): Long = context.dataStore.data.first()[KEY_LAST_SYNC] ?: 0L

    val lastSyncFlow: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_SYNC] ?: 0L }

    suspend fun setLastSyncMillis(value: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC] = value }
    }

    suspend fun checkPin(pin: String): Boolean {
        val stored = context.dataStore.data.first()[KEY_PIN_HASH] ?: hash(DEFAULT_PIN)
        return stored == hash(pin)
    }

    suspend fun setPin(pin: String) {
        require(pin.length == PIN_LENGTH && pin.all { it.isDigit() }) { "PIN must be $PIN_LENGTH digits" }
        context.dataStore.edit { it[KEY_PIN_HASH] = hash(pin) }
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        const val DEFAULT_COUNTRY = "ID"
        const val DEFAULT_PIN = "0000"
        const val PIN_LENGTH = 4

        private val KEY_COUNTRY = stringPreferencesKey("preferred_country")
        private val KEY_LAST_SYNC = longPreferencesKey("last_sync_millis")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
    }
}
