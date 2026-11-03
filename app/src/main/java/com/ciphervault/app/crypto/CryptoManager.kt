package com.ciphervault.app.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
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
    fun encrypt(inputStream: InputStream, outputStream: OutputStream, key: ByteArray, totalSize: Long = 0L, onProgress: ((Float) -> Unit)? = null) {
        val secretKey = SecretKeySpec(key, "AES")
        
        outputStream.write(FLAG_CHUNKED_V3.toInt())
        
        val baseIv = ByteArray(12)
        SecureRandom().nextBytes(baseIv)
        outputStream.write(baseIv.size)
        outputStream.write(baseIv)
        
        val buffer = ByteArray(CHUNK_SIZE)
        var totalRead = 0L
        var chunkIndex = 0
        
        while (true) {
            var readOffset = 0
            while (readOffset < CHUNK_SIZE) {
                val read = inputStream.read(buffer, readOffset, CHUNK_SIZE - readOffset)
                if (read == -1) break
                readOffset += read
            }
            
            if (readOffset == 0) break
            
            val chunkIv = baseIv.copyOf()
            val indexBytes = java.nio.ByteBuffer.allocate(4).putInt(chunkIndex).array()
            for (i in 0..3) {
                chunkIv[8 + i] = (chunkIv[8 + i].toInt() xor indexBytes[i].toInt()).toByte()
            }
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, chunkIv))
            
            val encryptedChunk = cipher.doFinal(buffer, 0, readOffset)
            
            val lenBytes = java.nio.ByteBuffer.allocate(4).putInt(encryptedChunk.size).array()
            outputStream.write(lenBytes)
            outputStream.write(encryptedChunk)
            
            totalRead += readOffset
            if (totalSize > 0L) {
                onProgress?.invoke(totalRead.toFloat() / totalSize)
            }
            chunkIndex++
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

        if (firstByte.toByte() == FLAG_CHUNKED_V3) {
            val ivSize = inputStream.read()
            if (ivSize < 0) throw IllegalStateException("Invalid encrypted data: cannot read IV size")
            val baseIv = readFully(inputStream, ivSize)
            
            val secretKey = SecretKeySpec(key, "AES")
            var chunkIndex = 0
            
            while (true) {
                val lenBytes = ByteArray(4)
                var readOffset = 0
                while (readOffset < 4) {
                    val read = inputStream.read(lenBytes, readOffset, 4 - readOffset)
                    if (read == -1) break
                    readOffset += read
                }
                if (readOffset == 0) break // EOF at chunk boundary
                if (readOffset < 4) throw IllegalStateException("Corrupt chunk size")
                
                val chunkLen = java.nio.ByteBuffer.wrap(lenBytes).int
                if (chunkLen < 0 || chunkLen > CHUNK_SIZE + 64) throw IllegalStateException("Invalid chunk size")
                
                val encryptedChunk = readFully(inputStream, chunkLen)
                
                val chunkIv = baseIv.copyOf()
                val indexBytes = java.nio.ByteBuffer.allocate(4).putInt(chunkIndex).array()
                for (i in 0..3) {
                    chunkIv[8 + i] = (chunkIv[8 + i].toInt() xor indexBytes[i].toInt()).toByte()
                }
                
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, chunkIv))
                
                val decryptedChunk = cipher.doFinal(encryptedChunk)
                outputStream.write(decryptedChunk)
                
                chunkIndex++
            }
            outputStream.flush()
            return
        }

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
            val cipherInputStream = javax.crypto.CipherInputStream(inputStream, cipher)
            GZIPInputStream(cipherInputStream).use { gzipIn ->
                val buffer = ByteArray(8192)
                var read: Int
                while (gzipIn.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
            }
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
    //  Helpers
    // ══════════════════════════════════════════════

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
        private const val CHUNK_SIZE = 1024 * 1024 // 1MB

        // Format version / compression flags
        const val FLAG_COMPRESSED_V2: Byte = 0xF1.toByte()
        const val FLAG_CHUNKED_V3: Byte = 0xF2.toByte()
    }
}
