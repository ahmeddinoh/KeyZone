package com.ciphervault.app.totp

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Manages single-use backup recovery codes for TOTP 2FA.
 *
 * Codes are generated in XXXXX-XXXXX format (10 hex digits, dash-separated).
 * Only SHA-256 hashes of the codes are stored — the plaintext is shown once.
 * Each code is consumed (deleted from the hash set) on successful use.
 */
class RecoveryCodeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Generates a new set of single-use recovery codes.
     *
     * @param count Number of codes to generate (default: 8)
     * @return Plaintext recovery codes (show to user once, then discard)
     */
    fun generateCodes(count: Int = CODE_COUNT): List<String> {
        val random = SecureRandom()
        val codes = (1..count).map {
            val bytes = ByteArray(5) // 40 bits → 10 hex chars
            random.nextBytes(bytes)
            val hex = bytes.joinToString("") { "%02X".format(it) }
            "${hex.substring(0, 5)}-${hex.substring(5, 10)}"
        }

        // Store only the hashes
        val hashes = codes.map { hashCode(it) }.toSet()
        prefs.edit().putStringSet(KEY_HASHES, hashes).apply()

        return codes
    }

    /**
     * Verifies and consumes a recovery code. A consumed code cannot be reused.
     *
     * @param code Recovery code (with or without dashes)
     * @return true if valid and consumed
     */
    fun verifyAndConsume(code: String): Boolean {
        val normalized = normalizeCode(code)
        val hash = hashCode(normalized)
        val hashes = prefs.getStringSet(KEY_HASHES, emptySet())?.toMutableSet() ?: return false

        if (hashes.contains(hash)) {
            hashes.remove(hash)
            prefs.edit().putStringSet(KEY_HASHES, hashes).apply()
            return true
        }
        return false
    }

    /** Number of remaining unused recovery codes. */
    fun remainingCount(): Int =
        prefs.getStringSet(KEY_HASHES, emptySet())?.size ?: 0

    /** Returns hashed codes for inclusion in backup files. */
    fun getHashedCodes(): Set<String> =
        prefs.getStringSet(KEY_HASHES, emptySet()) ?: emptySet()

    /** Imports hashed codes from a backup file. */
    fun importHashedCodes(hashes: Set<String>) {
        prefs.edit().putStringSet(KEY_HASHES, hashes).apply()
    }

    /** Clears all stored recovery codes. */
    fun clear() {
        prefs.edit().remove(KEY_HASHES).apply()
    }

    private fun normalizeCode(code: String): String {
        val stripped = code.replace("-", "").replace(" ", "").uppercase()
        return if (stripped.length == 10) {
            "${stripped.substring(0, 5)}-${stripped.substring(5, 10)}"
        } else {
            code.uppercase()
        }
    }

    private fun hashCode(code: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(code.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    companion object {
        private const val PREFS_NAME = "recovery_codes"
        private const val KEY_HASHES = "code_hashes"
        private const val CODE_COUNT = 8
    }
}
