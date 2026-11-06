package com.ciphervault.app.data

import android.content.Context
import android.util.Base64

/**
 * Persists vault configuration in SharedPreferences.
 *
 * Stores:
 * - PBKDF2 salt
 * - Wrapped (encrypted) master key
 * - Key verification token
 * - Key type (PIN or password)
 * - TOTP configuration status
 */
class VaultConfig(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the vault has been created or imported. */
    var isInitialized: Boolean
        get() = prefs.getBoolean(KEY_INITIALIZED, false)
        set(value) = prefs.edit().putBoolean(KEY_INITIALIZED, value).apply()

    /** The type of key protecting the vault: "pin" or "password". */
    var keyType: String
        get() = prefs.getString(KEY_TYPE, "pin") ?: "pin"
        set(value) = prefs.edit().putString(KEY_TYPE, value).apply()

    /** PBKDF2 salt (32 bytes). */
    var salt: ByteArray
        get() = decodeB64(prefs.getString(KEY_SALT, null))
        set(value) = prefs.edit().putString(KEY_SALT, encodeB64(value)).apply()

    /** Master key encrypted with the PBKDF2-derived key. */
    var wrappedMasterKey: ByteArray
        get() = decodeB64(prefs.getString(KEY_WRAPPED_MASTER, null))
        set(value) = prefs.edit().putString(KEY_WRAPPED_MASTER, encodeB64(value)).apply()

    /** Known plaintext encrypted with the derived key — used to verify PIN/password. */
    var keyVerification: ByteArray
        get() = decodeB64(prefs.getString(KEY_VERIFICATION, null))
        set(value) = prefs.edit().putString(KEY_VERIFICATION, encodeB64(value)).apply()

    /** Whether TOTP 2FA has been configured for backups. */
    var isTotpConfigured: Boolean
        get() = prefs.getBoolean(KEY_TOTP_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_TOTP_CONFIGURED, value).apply()

    /** Configured PIN length (e.g. 4 or 6). Defaults to 6. */
    var pinLength: Int
        get() = prefs.getInt(KEY_PIN_LENGTH, 6)
        set(value) = prefs.edit().putInt(KEY_PIN_LENGTH, value).apply()

    /** Whether Biometric unlock (Fingerprint / Face) is enabled. */
    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    /** Master key wrapped by Android KeyStore hardware-backed biometric key. */
    var biometricWrappedMasterKey: ByteArray
        get() = decodeB64(prefs.getString(KEY_BIOMETRIC_WRAPPED_KEY, null))
        set(value) = prefs.edit().putString(KEY_BIOMETRIC_WRAPPED_KEY, encodeB64(value)).apply()

    /** Initialization Vector for biometric master key wrapping. */
    var biometricIv: ByteArray
        get() = decodeB64(prefs.getString(KEY_BIOMETRIC_IV, null))
        set(value) = prefs.edit().putString(KEY_BIOMETRIC_IV, encodeB64(value)).apply()

    /** Whether to auto-lock the vault when the app is placed in background. */
    var autoLockOnBackground: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LOCK, value).apply()

    /** Whether screen capture / app switcher preview is blocked via FLAG_SECURE. */
    var screenProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_PROTECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_PROTECTION, value).apply()

    /** Delay before auto-locking in background (in milliseconds). Default is 0. */
    var autoLockDelayMillis: Long
        get() = prefs.getLong(KEY_AUTO_LOCK_DELAY, 0L)
        set(value) = prefs.edit().putLong(KEY_AUTO_LOCK_DELAY, value).apply()

    /** Whether original files are automatically deleted after encryption. */
    var deleteOriginalAfterEncryption: Boolean
        get() = prefs.getBoolean(KEY_DELETE_ORIGINAL, false)
        set(value) = prefs.edit().putBoolean(KEY_DELETE_ORIGINAL, value).apply()

    /** Wipes all vault configuration. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encodeB64(data: ByteArray): String =
        Base64.encodeToString(data, Base64.NO_WRAP)

    private fun decodeB64(encoded: String?): ByteArray =
        if (encoded.isNullOrEmpty()) ByteArray(0)
        else Base64.decode(encoded, Base64.NO_WRAP)

    companion object {
        private const val PREFS_NAME = "vault_config"
        private const val KEY_INITIALIZED = "initialized"
        private const val KEY_TYPE = "key_type"
        private const val KEY_SALT = "salt"
        private const val KEY_WRAPPED_MASTER = "wrapped_master_key"
        private const val KEY_VERIFICATION = "key_verification"
        private const val KEY_TOTP_CONFIGURED = "totp_configured"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_BIOMETRIC_WRAPPED_KEY = "biometric_wrapped_key"
        private const val KEY_BIOMETRIC_IV = "biometric_iv"
        private const val KEY_AUTO_LOCK = "auto_lock_background"
        private const val KEY_AUTO_LOCK_DELAY = "auto_lock_delay"
        private const val KEY_SCREEN_PROTECTION = "screen_protection"
        private const val KEY_DELETE_ORIGINAL = "delete_original"
    }
}

