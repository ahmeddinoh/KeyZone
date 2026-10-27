package com.ciphervault.app.totp

/**
 * RFC 4648 Base32 encoder/decoder.
 * Standard alphabet: A–Z, 2–7, with '=' padding.
 */
object Base32 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    private val DECODE_TABLE = IntArray(128) { -1 }.also { table ->
        ALPHABET.forEachIndexed { index, char ->
            table[char.code] = index
            if (char.isLetter()) table[char.lowercaseChar().code] = index
        }
    }

    /**
     * Encodes a byte array to a Base32 string with padding.
     */
    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val result = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                result.append(ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            result.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        // Pad to 8-character boundary
        while (result.length % 8 != 0) {
            result.append('=')
        }
        return result.toString()
    }

    /**
     * Decodes a Base32 string to a byte array.
     * Handles strings with or without padding.
     */
    fun decode(encoded: String): ByteArray {
        val stripped = encoded.trimEnd('=').uppercase()
        if (stripped.isEmpty()) return ByteArray(0)

        val output = ByteArray(stripped.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var outputIndex = 0

        for (char in stripped) {
            val value = if (char.code < 128) DECODE_TABLE[char.code] else -1
            if (value < 0) throw IllegalArgumentException("Invalid Base32 character: $char")
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[outputIndex++] = (buffer shr bitsLeft).toByte()
            }
        }
        return output.copyOf(outputIndex)
    }
}
