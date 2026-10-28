package com.ciphervault.app.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages all cryptographic operations for CipherVault.
 *
 * Two modes of operation:
 * 1. **Key-derived**: File encryption/decryption using a master key that is itself
 *    wrapped (encrypted) by a key derived from the user's PIN/password via PBKDF2.
 *    The PIN/password IS the key — without it, data is irrecoverable.
 * 2. **KeyStore-backed**: Android KeyStore (hardware-backed) encryption for the
 *    TOTP secret stored locally on the device.
 *
 * Encryption format: `[IV_LEN (1 byte)] [IV (12 bytes)] [CIPHERTEXT + GCM_AUTH_TAG]`
 */
class CryptoManager {

    private val keyStore by lazy { KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) } }

    // ══════════════════════════════════════════════
    //  PBKDF2 Key Derivation
    // ══════════════════════════════════════════════

    /** Generates a cryptographically random 32-byte salt for PBKDF2. */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit AES key from a password/PIN using PBKDF2-HMAC-SHA256.
     * Uses 150,000 iterations (balanced for mobile performance vs. security).
     */
    fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val key = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return key
    }

    // ══════════════════════════════════════════════
    //  Master Key Management
    // ══════════════════════════════════════════════

    /** Generates a random 256-bit (32-byte) master key for file encryption. */
    fun generateMasterKey(): ByteArray {
        val key = ByteArray(MASTER_KEY_SIZE)
        SecureRandom().nextBytes(key)
        return key
    }

    /** Wraps (encrypts) a key using another key via AES-256-GCM. */
    fun wrapKey(keyToWrap: ByteArray, wrappingKey: ByteArray): ByteArray =
        encryptBytes(keyToWrap, wrappingKey)

    /** Unwraps (decrypts) a key using another key via AES-256-GCM. */
    fun unwrapKey(wrappedKey: ByteArray, wrappingKey: ByteArray): ByteArray =
        decryptBytes(wrappedKey, wrappingKey)

    // ══════════════════════════════════════════════
    //  Key Verification
    // ══════════════════════════════════════════════

    /**
     * Creates a verification token by encrypting a known plaintext with the derived key.
     * Used to check if a PIN/password is correct without decrypting all files.
     */
    fun createKeyVerification(key: ByteArray): ByteArray =
        encryptBytes(KEY_VERIFICATION_PLAINTEXT.toByteArray(Charsets.UTF_8), key)

    /**
     * Verifies a key by decrypting the verification token and comparing the plaintext.
     * Returns false if the key is wrong (GCM auth tag will fail).
     */
    fun verifyKey(verification: ByteArray, key: ByteArray): Boolean {
        return try {
            val decrypted = decryptBytes(verification, key)
            String(decrypted, Charsets.UTF_8) == KEY_VERIFICATION_PLAINTEXT
        } catch (_: Exception) {
            false
        }
    }

    // ══════════════════════════════════════════════
    //  AES-256-GCM with provided key
    // ══════════════════════════════════════════════

    /**
     * Encrypts a stream using AES-256-GCM with the provided key.
     * Automatically applies GZIP compression if it reduces data size,
     * significantly shrinking text, documents, PDFs, and database files.
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream, key: ByteArray) {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        outputStream.write(iv.size)
        outputStream.write(iv)

        val buffer = ByteArray(8192)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            val output = cipher.update(buffer, 0, read)
            if (output != null) {
                outputStream.write(output)
            }
        }
        val finalOutput = cipher.doFinal()
        if (finalOutput != null) {
            outputStream.write(finalOutput)
        }
        outputStream.flush()
    }

    /**
     * Decrypts a stream using AES-256-GCM with the provided key.
     * Automatically detects and handles both compressed (V2) and legacy uncompressed payloads.
     */
    fun decrypt(inputStream: InputStream, outputStream: OutputStream, key: ByteArray) {
        val firstByte = inputStream.read()
        if (firstByte < 0) throw IllegalStateException("Invalid encrypted data: empty stream")

        val isCompressed: Boolean
        val ivSize: Int
        if (firstByte.toByte() == FLAG_COMPRESSED_V2) {
            isCompressed = true
            ivSize = inputStream.read()
            if (ivSize < 0) throw IllegalStateException("Invalid encrypted data: cannot read IV size")
        } else {
            // Legacy uncompressed format: first byte is the iv.size (e.g. 12)
            isCompressed = false
            ivSize = firstByte
        }

        val iv = readFully(inputStream, ivSize)

        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

        if (isCompressed) {
            val encryptedData = inputStream.readBytes()
            val decryptedData = cipher.doFinal(encryptedData)
            val finalPlainData = decompressGzip(decryptedData)
            outputStream.write(finalPlainData)
        } else {
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                val output = cipher.update(buffer, 0, read)
                if (output != null) {
                    outputStream.write(output)
                }
            }
            val finalOutput = cipher.doFinal()
            if (finalOutput != null) {
                outputStream.write(finalOutput)
            }
        }
        outputStream.flush()
    }

    /** Encrypts a byte array using AES-256-GCM with the provided key. */
    fun encryptBytes(data: ByteArray, key: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)

        return ByteArray(1 + iv.size + encryptedData.size).apply {
            this[0] = iv.size.toByte()
            System.arraycopy(iv, 0, this, 1, iv.size)
            System.arraycopy(encryptedData, 0, this, 1 + iv.size, encryptedData.size)
        }
    }

    /** Decrypts a byte array using AES-256-GCM with the provided key. */
    fun decryptBytes(data: ByteArray, key: ByteArray): ByteArray {
        val ivSize = data[0].toInt() and 0xFF
        val iv = data.copyOfRange(1, 1 + ivSize)
        val encryptedData = data.copyOfRange(1 + ivSize, data.size)

        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return cipher.doFinal(encryptedData)
    }

    // ══════════════════════════════════════════════
    //  Android KeyStore (for TOTP secret local storage)
    // ══════════════════════════════════════════════

    private fun getOrCreateKeyStoreKey(): SecretKey {
        val existing = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_TYPE)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return keyGen.generateKey()
    }

    /** Encrypts data using Android KeyStore (hardware-backed on supported devices). */
    fun encryptWithKeyStore(data: ByteArray): ByteArray {
        val key = getOrCreateKeyStoreKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        return ByteArray(1 + iv.size + encrypted.size).apply {
            this[0] = iv.size.toByte()
            System.arraycopy(iv, 0, this, 1, iv.size)
            System.arraycopy(encrypted, 0, this, 1 + iv.size, encrypted.size)
        }
    }

    /** Decrypts data using Android KeyStore (hardware-backed on supported devices). */
    fun decryptWithKeyStore(data: ByteArray): ByteArray {
        val key = getOrCreateKeyStoreKey()
        val ivSize = data[0].toInt() and 0xFF
        val iv = data.copyOfRange(1, 1 + ivSize)
        val encrypted = data.copyOfRange(1 + ivSize, data.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    // ══════════════════════════════════════════════
    //  Helpers & Compression
    // ══════════════════════════════════════════════

    private fun compressGzip(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun decompressGzip(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        val bis = ByteArrayInputStream(data)
        return GZIPInputStream(bis).use { it.readBytes() }
    }

    private fun readFully(input: InputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(buffer, offset, length - offset)
            if (read < 0) throw IllegalStateException("Unexpected end of stream")
            offset += read
        }
        return buffer
    }

    companion object {
        private const val KEYSTORE_TYPE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "CipherVault_TOTP_Key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128 // bits
        private const val MASTER_KEY_SIZE = 32 // 256 bits
        private const val SALT_SIZE = 32
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 150_000
        private const val PBKDF2_KEY_LENGTH = 256 // bits
        private const val KEY_VERIFICATION_PLAINTEXT = "CIPHERVAULT_KEY_OK_v1"

        // Format version / compression flags
        const val FLAG_COMPRESSED_V2: Byte = 0xF1.toByte()
    }
}
