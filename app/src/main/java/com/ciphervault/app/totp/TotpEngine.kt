package com.ciphervault.app.totp

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * RFC 6238 TOTP implementation — fully offline.
 *
 * Generates and verifies Time-Based One-Time Passwords using:
 * - HMAC-SHA1 (compatible with Google Authenticator)
 * - 30-second time period
 * - 6-digit codes
 * - 160-bit (20-byte) secret keys
 *
 * All time calculations rely solely on the local device system clock.
 */
object TotpEngine {

    private const val PERIOD_SECONDS = 30L
    private const val DIGITS = 6
    private const val SECRET_SIZE_BYTES = 20 // 160 bits per RFC recommendation
    private const val HMAC_ALGORITHM = "HmacSHA1"
    private val DIGITS_POWER = 10.0.pow(DIGITS.toDouble()).toInt()

    /**
     * Generates a cryptographically secure 160-bit (20-byte) random secret.
     */
    fun generateSecret(): ByteArray {
        val secret = ByteArray(SECRET_SIZE_BYTES)
        SecureRandom().nextBytes(secret)
        return secret
    }

    /**
     * Generates the current 6-digit TOTP code.
     *
     * @param secret The shared TOTP secret (20 bytes)
     * @param timeMillis Current time in milliseconds (defaults to system clock)
     */
    fun generateCode(secret: ByteArray, timeMillis: Long = System.currentTimeMillis()): String {
        val counter = timeMillis / 1000 / PERIOD_SECONDS
        return generateCodeForCounter(secret, counter)
    }

    /**
     * Returns seconds remaining in the current 30-second TOTP period.
     */
    fun secondsRemaining(timeMillis: Long = System.currentTimeMillis()): Int {
        return (PERIOD_SECONDS - ((timeMillis / 1000) % PERIOD_SECONDS)).toInt()
    }

    /**
     * Verifies a TOTP code with ±1 step clock drift tolerance (±30 seconds).
     *
     * Checks the code against counter values: current-1, current, current+1.
     *
     * @param secret The shared TOTP secret
     * @param code The 6-digit code to verify
     * @param timeMillis Current time in milliseconds
     * @param window Number of periods to check on each side (default: 1)
     * @return [VerificationResult] with validity and matched counter
     */
    fun verify(
        secret: ByteArray,
        code: String,
        timeMillis: Long = System.currentTimeMillis(),
        window: Int = 1
    ): VerificationResult {
        val currentCounter = timeMillis / 1000 / PERIOD_SECONDS

        for (offset in -window..window) {
            val counter = currentCounter + offset
            val expectedCode = generateCodeForCounter(secret, counter)
            if (timingSafeEquals(expectedCode, code)) {
                return VerificationResult(valid = true, counter = counter)
            }
        }

        return VerificationResult(valid = false, counter = -1)
    }

    /**
     * Generates the TOTP code for a specific counter value.
     *
     * Implements the dynamic truncation algorithm per RFC 4226 §5.4:
     * 1. Compute HMAC-SHA1(secret, counter)
     * 2. Extract 4 bytes using dynamic offset
     * 3. Reduce to [DIGITS]-digit number
     */
    private fun generateCodeForCounter(secret: ByteArray, counter: Long): String {
        // Counter → 8-byte big-endian
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        // HMAC-SHA1
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secret, HMAC_ALGORITHM))
        val hash = mac.doFinal(counterBytes)

        // Dynamic truncation
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val truncated = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val code = truncated % DIGITS_POWER
        return code.toString().padStart(DIGITS, '0')
    }

    /**
     * Constant-time string comparison to prevent timing side-channel attacks.
     */
    private fun timingSafeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * Result of a TOTP verification attempt.
     *
     * @property valid Whether the code matched
     * @property counter The counter value that matched (-1 if no match)
     */
    data class VerificationResult(
        val valid: Boolean,
        val counter: Long
    )
}
