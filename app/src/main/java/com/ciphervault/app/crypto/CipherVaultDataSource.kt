package com.ciphervault.app.crypto

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * A custom ExoPlayer DataSource that decrypts AES-256-GCM encrypted media files on the fly.
 * Allows instant playback without decrypting the entire file to memory or disk.
 */
@androidx.media3.common.util.UnstableApi
class CipherVaultDataSource(
    private val encryptedFile: File,
    private val masterKey: ByteArray
) : BaseDataSource(true) {

    private var inputStream: InputStream? = null
    private var cipher: Cipher? = null
    private var cipherFinished = false

    private var bytesToRead: Long = C.LENGTH_UNSET.toLong()
    private var bytesReadTotal: Long = 0
    private var currentPosition: Long = 0

    private var pendingBuffer: ByteArray = ByteArray(0)
    private var pendingOffset: Int = 0

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)

        try {
            val fileLength = encryptedFile.length()
            if (fileLength == 0L) {
                throw java.io.IOException("File is empty")
            }

            val stream = FileInputStream(encryptedFile)
            inputStream = stream

            // Read header (1 byte for format flag, then IV size, then IV)
            val firstByte = stream.read()
            if (firstByte < 0) throw java.io.IOException("Invalid stream")

            val isCompressed: Boolean
            val ivSize: Int
            if (firstByte.toByte() == CryptoManager.FLAG_COMPRESSED_V2) {
                isCompressed = true
                ivSize = stream.read()
                if (ivSize < 0) throw java.io.IOException("Invalid stream header")
            } else {
                isCompressed = false
                ivSize = firstByte
            }

            if (isCompressed) {
                throw java.io.IOException("Compressed media streaming is not supported")
            }

            val iv = ByteArray(ivSize)
            var offset = 0
            while (offset < ivSize) {
                val read = stream.read(iv, offset, ivSize - offset)
                if (read < 0) throw java.io.EOFException("Premature EOF reading IV")
                offset += read
            }

            val headerSize = 1 + ivSize
            val encryptedPayloadSize = fileLength - headerSize
            val tagSize = CryptoManager.GCM_TAG_LENGTH / 8 // 16 bytes
            val actualDataSize = encryptedPayloadSize - tagSize

            if (actualDataSize < 0) {
                throw java.io.IOException("Invalid encrypted file size")
            }

            cipher = Cipher.getInstance(CryptoManager.TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(masterKey, "AES"),
                    GCMParameterSpec(CryptoManager.GCM_TAG_LENGTH, iv)
                )
            }
            cipherFinished = false
            pendingBuffer = ByteArray(0)
            pendingOffset = 0

            // Handle seeking (skipping bytes in the cipher stream)
            currentPosition = 0
            if (dataSpec.position > 0) {
                if (dataSpec.position > actualDataSize) {
                    throw java.io.EOFException("Seek position ${dataSpec.position} exceeds data size $actualDataSize")
                }
                skipPlaintext(dataSpec.position)
                currentPosition = dataSpec.position
            }

            bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                actualDataSize - dataSpec.position
            }

        } catch (e: Exception) {
            closeSilently()
            throw (e as? java.io.IOException) ?: java.io.IOException("Failed to initialize cipher data source", e)
        }

        transferStarted(dataSpec)
        return bytesToRead
    }

    private fun skipPlaintext(bytesToSkip: Long) {
        var remaining = bytesToSkip
        val tempBuffer = ByteArray(8192)
        while (remaining > 0) {
            val readAmount = minOf(remaining, tempBuffer.size.toLong()).toInt()
            val read = readInternal(tempBuffer, 0, readAmount, isSkipping = true)
            if (read == C.RESULT_END_OF_INPUT) {
                throw java.io.EOFException("Premature EOF while seeking")
            }
            remaining -= read
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return readInternal(buffer, offset, length, isSkipping = false)
    }

    private fun readInternal(
        buffer: ByteArray,
        offset: Int,
        length: Int,
        isSkipping: Boolean
    ): Int {
        if (length == 0) return 0
        if ((!isSkipping) && bytesToRead == 0L) {
            finishCipherAndVerifyTag()
            return C.RESULT_END_OF_INPUT
        }

        // 1. First, drain any pending decrypted bytes from previous cipher operations
        val pendingAvailable = pendingBuffer.size - pendingOffset
        if (pendingAvailable > 0) {
            val toCopy = minOf(length, pendingAvailable)
            System.arraycopy(pendingBuffer, pendingOffset, buffer, offset, toCopy)
            pendingOffset += toCopy
            if (pendingOffset == pendingBuffer.size) {
                pendingBuffer = ByteArray(0)
                pendingOffset = 0
            }
            if (!isSkipping) {
                if (bytesToRead != C.LENGTH_UNSET.toLong()) {
                    bytesToRead -= toCopy
                }
                bytesReadTotal += toCopy
                currentPosition += toCopy
                bytesTransferred(toCopy)
            }
            return toCopy
        }

        // 2. Loop reading from underlying input stream until decrypted bytes are produced or EOF
        val tempEncrypted = ByteArray(8192)
        val tagSizeBytes = CryptoManager.GCM_TAG_LENGTH / 8
        while (true) {
            val maxStreamRead = if (!isSkipping && bytesToRead != C.LENGTH_UNSET.toLong()) {
                minOf(tempEncrypted.size.toLong(), bytesToRead + tagSizeBytes).toInt()
            } else {
                tempEncrypted.size
            }

            if (maxStreamRead <= 0) {
                finishCipherAndVerifyTag()
                return C.RESULT_END_OF_INPUT
            }

            val actuallyRead = inputStream?.read(tempEncrypted, 0, maxStreamRead) ?: -1

            if (actuallyRead == -1) {
                val finalBytes = finishCipherAndVerifyTag()
                if (finalBytes != null && finalBytes.isNotEmpty()) {
                    val toCopy = minOf(length, finalBytes.size)
                    System.arraycopy(finalBytes, 0, buffer, offset, toCopy)
                    if (finalBytes.size > toCopy) {
                        pendingBuffer = finalBytes.copyOfRange(toCopy, finalBytes.size)
                        pendingOffset = 0
                    }
                    if (!isSkipping) {
                        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
                            bytesToRead -= toCopy
                        }
                        bytesReadTotal += toCopy
                        currentPosition += toCopy
                        bytesTransferred(toCopy)
                    }
                    return toCopy
                }
                return C.RESULT_END_OF_INPUT
            }

            if (actuallyRead > 0) {
                val decrypted = cipher?.update(tempEncrypted, 0, actuallyRead)
                if (decrypted != null && decrypted.isNotEmpty()) {
                    val toCopy = minOf(length, decrypted.size)
                    System.arraycopy(decrypted, 0, buffer, offset, toCopy)
                    if (decrypted.size > toCopy) {
                        pendingBuffer = decrypted.copyOfRange(toCopy, decrypted.size)
                        pendingOffset = 0
                    }
                    if (!isSkipping) {
                        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
                            bytesToRead -= toCopy
                        }
                        bytesReadTotal += toCopy
                        currentPosition += toCopy
                        bytesTransferred(toCopy)
                    }
                    return toCopy
                }
                // If cipher.update returned null/empty (buffering tag), loop continues to read next chunk
            }
        }
    }

    private fun finishCipherAndVerifyTag(): ByteArray? {
        if (!cipherFinished) {
            cipherFinished = true
            return try {
                cipher?.doFinal()
            } catch (e: Exception) {
                throw java.io.IOException("Error during decryption / tag verification failed", e)
            }
        }
        return null
    }

    override fun getUri(): Uri? = Uri.fromFile(encryptedFile)

    override fun close() {
        closeSilently()
        transferEnded()
    }

    private fun closeSilently() {
        try {
            inputStream?.close()
        } catch (_: Exception) {
        } finally {
            inputStream = null
            cipher = null
            cipherFinished = false
            pendingBuffer = ByteArray(0)
            pendingOffset = 0
        }
    }
}
