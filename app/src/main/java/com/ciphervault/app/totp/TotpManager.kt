package com.ciphervault.app.totp

import android.content.Context
import android.util.Base64
import com.ciphervault.app.crypto.CryptoManager

/**
 * High-level TOTP management with replay protection and rate limiting.
 *
 * The TOTP secret is stored locally encrypted with Android KeyStore (hardware-backed).
 * For backup files, the secret is encrypted with the vault master key for portability.
 *
 * Rate limiting: 5 consecutive failed attempts → 30-second lockout.
 * Replay protection: Rejects codes whose counter ≤ the last successfully verified counter.
 */
class TotpManager(private val context: Context, private val cryptoManager: CryptoManager) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── Rate Limiting ───

    private var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    private var lockoutUntil: Long
        get() = prefs.getLong(KEY_LOCKOUT_UNTIL, 0)
        set(value) = prefs.edit().putLong(KEY_LOCKOUT_UNTIL, value).apply()

    // ─── Replay Protection ───

    private var lastVerifiedCounter: Long
        get() = prefs.getLong(KEY_LAST_COUNTER, -1)
        set(value) = prefs.edit().putLong(KEY_LAST_COUNTER, value).apply()

    // ═══════════════════════════════════════
    //  Secret Management
    // ═══════════════════════════════════════

    /**
     * Generates a new 160-bit TOTP secret and stores it locally
     * encrypted with Android KeyStore (hardware-backed).
     *
     * @return The raw secret bytes (needed for QR code generation)
     */
    fun generateAndStoreSecret(): ByteArray {
        val secret = TotpEngine.generateSecret()
        storeSecretLocally(secret)
        return secret
    }

    /**
     * Retrieves the locally stored TOTP secret (decrypted from KeyStore).
     * Returns null if no secret is stored.
     */
    fun getLocalSecret(): ByteArray? {
        val encoded = prefs.getString(KEY_SECRET, null) ?: return null
        return try {
            val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
            cryptoManager.decryptWithKeyStore(encrypted)
        } catch (_: Exception) {
            null
        }
    }

    /** Stores a TOTP secret locally encrypted with Android KeyStore. */
    fun storeSecretLocally(secret: ByteArray) {
        val encrypted = cryptoManager.encryptWithKeyStore(secret)
        prefs.edit()
            .putString(KEY_SECRET, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    /** Encrypts the TOTP secret with the vault master key (for backup portability). */
    fun encryptSecretForBackup(secret: ByteArray, masterKey: ByteArray): ByteArray =
        cryptoManager.encryptBytes(secret, masterKey)

    /** Decrypts a TOTP secret from a backup using the vault master key. */
    fun decryptSecretFromBackup(encrypted: ByteArray, masterKey: ByteArray): ByteArray =
        cryptoManager.decryptBytes(encrypted, masterKey)

    // ═══════════════════════════════════════
    //  OTP Auth URI
    // ═══════════════════════════════════════

    /**
     * Builds the standard `otpauth://totp/...` URI for scanning into Google Authenticator.
     *
     * Format: `otpauth://totp/{Issuer}:{Account}?secret={Base32}&issuer={Issuer}&algorithm=SHA1&digits=6&period=30`
     */
    fun buildOtpAuthUri(secret: ByteArray, accountName: String = "Vault"): String {
        val base32Secret = Base32.encode(secret)
        val issuer = ISSUER
        return "otpauth://totp/$issuer:$accountName" +
                "?secret=$base32Secret" +
                "&issuer=$issuer" +
                "&algorithm=SHA1" +
                "&digits=6" +
                "&period=30"
    }

    // ═══════════════════════════════════════
    //  Verification
    // ═══════════════════════════════════════

    /**
     * Verifies a 6-digit TOTP code with:
     * - ±1 step clock drift tolerance (checks counter -1, 0, +1)
     * - Replay protection (rejects already-used counters)
     * - Rate limiting (5 failures → 30s lockout)
     */
    fun verifyCode(secret: ByteArray, code: String): TotpVerifyResult {
        val now = System.currentTimeMillis()

        // Check lockout
        if (now < lockoutUntil) {
            val remaining = ((lockoutUntil - now) / 1000).coerceAtLeast(1)
            return TotpVerifyResult.LockedOut(remaining)
        }

        val result = TotpEngine.verify(secret, code, now)

        if (result.valid) {
            // Replay protection
            if (result.counter <= lastVerifiedCounter) {
                return TotpVerifyResult.ReplayDetected
            }
            lastVerifiedCounter = result.counter
            failedAttempts = 0
            return TotpVerifyResult.Valid
        }

        // Rate limiting
        val attempts = failedAttempts + 1
        failedAttempts = attempts

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockoutUntil = now + LOCKOUT_DURATION_MS
            failedAttempts = 0
            return TotpVerifyResult.LockedOut(LOCKOUT_DURATION_MS / 1000)
        }

        return TotpVerifyResult.Invalid(MAX_FAILED_ATTEMPTS - attempts)
    }

    /** Resets rate limiting and replay protection state. */
    fun resetState() {
        failedAttempts = 0
        lockoutUntil = 0
        lastVerifiedCounter = -1
    }

    /** Whether any TOTP secret is stored. */
    fun hasSecret(): Boolean = prefs.contains(KEY_SECRET)

    companion object {
        private const val PREFS_NAME = "totp_manager"
        private const val KEY_SECRET = "encrypted_secret"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_LAST_COUNTER = "last_counter"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
        private const val ISSUER = "CipherVault"
    }
}

/** Result of a TOTP verification attempt. */
sealed class TotpVerifyResult {
    /** Code is valid and has not been replayed. */
    data object Valid : TotpVerifyResult()
    /** Code is invalid. */
    data class Invalid(val attemptsRemaining: Int) : TotpVerifyResult()
    /** Too many failures — user is temporarily locked out. */
    data class LockedOut(val secondsRemaining: Long) : TotpVerifyResult()
    /** Code was already used within the current time window. */
    data object ReplayDetected : TotpVerifyResult()
}
