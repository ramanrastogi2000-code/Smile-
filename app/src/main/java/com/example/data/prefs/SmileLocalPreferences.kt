package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.SmileCryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.smileDataStore by preferencesDataStore(name = "smile_local_prefs")

data class LocalPrefsState(
    val darkMode: Boolean = false,
    val languageCode: String = "EN",
    val wallpaperId: String = "smile_yellow",
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String = "",
    val mutedStatusUids: Set<String> = emptySet(),
    val lastBackupEpochMs: Long = 0L
)

class SmileLocalPreferences(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language_code")
        val WALLPAPER_ID = stringPreferencesKey("wallpaper_id")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        val MUTED_STATUS_UIDS = stringSetPreferencesKey("muted_status_uids")
        val LAST_BACKUP_MS = longPreferencesKey("last_backup_ms")
        fun privateKeyKey(uid: String) = stringPreferencesKey("ec_priv_$uid")
        fun publicKeyKey(uid: String) = stringPreferencesKey("ec_pub_$uid")
    }

    val state: Flow<LocalPrefsState> = context.smileDataStore.data.map { prefs ->
        LocalPrefsState(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            languageCode = prefs[Keys.LANGUAGE] ?: "EN",
            wallpaperId = prefs[Keys.WALLPAPER_ID] ?: "smile_yellow",
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockPinHash = prefs[Keys.APP_LOCK_PIN_HASH] ?: "",
            mutedStatusUids = prefs[Keys.MUTED_STATUS_UIDS] ?: emptySet(),
            lastBackupEpochMs = prefs[Keys.LAST_BACKUP_MS] ?: 0L
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.smileDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(code: String) {
        context.smileDataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setWallpaper(wallpaperId: String) {
        context.smileDataStore.edit { it[Keys.WALLPAPER_ID] = wallpaperId }
    }

    suspend fun setAppLock(enabled: Boolean, pinHash: String? = null) {
        context.smileDataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
            if (pinHash != null) prefs[Keys.APP_LOCK_PIN_HASH] = pinHash
        }
    }

    suspend fun toggleMutedStatusUser(targetUid: String) {
        context.smileDataStore.edit { prefs ->
            val current = prefs[Keys.MUTED_STATUS_UIDS]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(targetUid)) current.remove(targetUid)
            prefs[Keys.MUTED_STATUS_UIDS] = current
        }
    }

    suspend fun recordBackupTimestamp(epochMs: Long) {
        context.smileDataStore.edit { it[Keys.LAST_BACKUP_MS] = epochMs }
    }

    /**
     * Ensures the authenticated user has a persisted ECDH P-256 keypair wrapped with AndroidKeyStore.
     * Returns (publicKeyBase64, privateKeyBytes).
     */
    suspend fun getOrCreateKeyPair(uid: String): Pair<String, ByteArray> {
        val prefs = context.smileDataStore.data.first()
        val existingPrivWrapped = prefs[Keys.privateKeyKey(uid)]
        val existingPub = prefs[Keys.publicKeyKey(uid)]
        if (!existingPrivWrapped.isNullOrBlank() && !existingPub.isNullOrBlank()) {
            val unwrapped = SmileCryptoManager.unwrapPrivateKeyFromStorage(existingPrivWrapped)
            if (unwrapped != null) {
                return existingPub to unwrapped
            }
        }
        val kp = SmileCryptoManager.generateEcKeyPair()
        val pubB64 = android.util.Base64.encodeToString(kp.public.encoded, android.util.Base64.NO_WRAP)
        val privWrapped = SmileCryptoManager.wrapPrivateKeyForStorage(kp.private.encoded)
        context.smileDataStore.edit { editPrefs ->
            editPrefs[Keys.privateKeyKey(uid)] = privWrapped
            editPrefs[Keys.publicKeyKey(uid)] = pubB64
        }
        return pubB64 to kp.private.encoded
    }

    suspend fun getPrivateKeyBytes(uid: String): ByteArray? {
        val wrapped = context.smileDataStore.data.first()[Keys.privateKeyKey(uid)] ?: return null
        return SmileCryptoManager.unwrapPrivateKeyFromStorage(wrapped)
    }
}
