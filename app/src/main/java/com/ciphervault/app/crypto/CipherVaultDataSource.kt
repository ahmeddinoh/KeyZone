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

@androidx.media3.common.util.UnstableApi
class CipherVaultDataSource(
    private val encryptedFile: File,
    private val masterKey: ByteArray
) : BaseDataSource(true) {

    private var inputStream: FileInputStream? = null
    private var opened = false
    
    // For V1 format
    private var cipher: Cipher? = null
    private var cipherFinished = false
    
    // For V3 format
    private var isV3 = false
    private var baseIv = ByteArray(0)
    private var chunkIndex = 0
    private val chunkOffsets = mutableListOf<Long>()
    private val chunkFileOffsets = mutableListOf<Long>()

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

            val firstByte = stream.read()
            if (firstByte < 0) throw java.io.IOException("Invalid stream")

            if (firstByte.toByte() == CryptoManager.FLAG_CHUNKED_V3) {
                isV3 = true
                val ivSize = stream.read()
                if (ivSize < 0) throw java.io.IOException("Invalid stream header")
                baseIv = readFully(stream, ivSize)
                
                val dataStartOffset = stream.channel.position()
                var currentFileOffset = dataStartOffset
                var calculatedActualDataSize = 0L
                
                chunkOffsets.clear()
                chunkFileOffsets.clear()
                
                val lenBytes = ByteArray(4)
                while (currentFileOffset < fileLength) {
                    stream.channel.position(currentFileOffset)
                    
                    var readOffset = 0
                    while (readOffset < 4) {
                        val read = stream.read(lenBytes, readOffset, 4 - readOffset)
                        if (read == -1) break
                        readOffset += read
                    }
                    if (readOffset < 4) break
                    
                    val chunkLen = java.nio.ByteBuffer.wrap(lenBytes).int
                    if (chunkLen < 0 || chunkLen > 1024 * 1024 + 64) throw java.io.IOException("Invalid chunk size during indexing")
                    
                    chunkOffsets.add(calculatedActualDataSize)
                    chunkFileOffsets.add(currentFileOffset)
                    
                    val chunkPlaintextLen = chunkLen - (CryptoManager.GCM_TAG_LENGTH / 8)
                    if (chunkPlaintextLen < 0) throw java.io.IOException("Invalid encrypted chunk length")
                    
                    calculatedActualDataSize += chunkPlaintextLen
                    currentFileOffset += 4 + chunkLen
                }
                
                val actualDataSize = calculatedActualDataSize
                
                currentPosition = 0
                if (dataSpec.position > 0) {
                    if (dataSpec.position > actualDataSize) {
                        throw java.io.EOFException("Seek position ${dataSpec.position} exceeds data size $actualDataSize")
                    }
                    
                    var targetChunk = chunkOffsets.binarySearch(dataSpec.position)
                    if (targetChunk < 0) {
                        targetChunk = -targetChunk - 2
                    }
                    
                    val offsetInChunk = (dataSpec.position - chunkOffsets[targetChunk]).toInt()
                    val fileOffset = chunkFileOffsets[targetChunk]
                    stream.channel.position(fileOffset)
                    
                    chunkIndex = targetChunk
                    currentPosition = dataSpec.position
                    
                    if (offsetInChunk > 0) {
                        val dummyBuffer = ByteArray(0)
                        readV3(dummyBuffer, 0, 0, isSkipping = true) 
                        
                        if (pendingBuffer.size > offsetInChunk) {
                            pendingBuffer = pendingBuffer.copyOfRange(offsetInChunk, pendingBuffer.size)
                            pendingOffset = 0
                        } else {
                            pendingBuffer = ByteArray(0)
                            pendingOffset = 0
                        }
                    }
                } else {
                    stream.channel.position(dataStartOffset)
                }
                
                bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                    dataSpec.length
                } else {
                    actualDataSize - dataSpec.position
                }
            } else {
                isV3 = false
                val isCompressed: Boolean
                val ivSize: Int
                if (firstByte.toByte() == CryptoManager.FLAG_COMPRESSED_V2) {
                    isCompressed = true
                    ivSize = stream.read()
                } else {
                    isCompressed = false
                    ivSize = firstByte
                }

                if (isCompressed) {
                    throw java.io.IOException("Compressed media streaming is not supported")
                }

                val iv = readFully(stream, ivSize)
                val headerSize = 1 + ivSize
                val encryptedPayloadSize = fileLength - headerSize
                val tagSize = CryptoManager.GCM_TAG_LENGTH / 8 // 16 bytes
                val actualDataSize = encryptedPayloadSize - tagSize

                if (actualDataSize < 0) throw java.io.IOException("Invalid encrypted file size")

                cipher = Cipher.getInstance(CryptoManager.TRANSFORMATION).apply {
                    init(
                        Cipher.DECRYPT_MODE,
                        SecretKeySpec(masterKey, "AES"),
                        GCMParameterSpec(CryptoManager.GCM_TAG_LENGTH, iv)
                    )
                }
                cipherFinished = false
                
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
            }

            pendingBuffer = ByteArray(0)
            pendingOffset = 0

        } catch (e: Exception) {
            closeSilently()
            throw (e as? java.io.IOException) ?: java.io.IOException("Failed to initialize cipher data source", e)
        }

        opened = true
        transferStarted(dataSpec)
        return bytesToRead
    }
    
    private fun readFully(stream: InputStream, size: Int): ByteArray {
        val bytes = ByteArray(size)
        var readOffset = 0
        while (readOffset < size) {
            val read = stream.read(bytes, readOffset, size - readOffset)
            if (read < 0) throw java.io.EOFException("Premature EOF")
            readOffset += read
        }
        return bytes
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
            if (!isV3) finishCipherAndVerifyTag()
            return C.RESULT_END_OF_INPUT
        }

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

        return if (isV3) {
            readV3(buffer, offset, length, isSkipping)
        } else {
            readV1(buffer, offset, length, isSkipping)
        }
    }

    private fun readV3(buffer: ByteArray, offset: Int, length: Int, isSkipping: Boolean): Int {
        val stream = inputStream ?: return C.RESULT_END_OF_INPUT
        
        val lenBytes = ByteArray(4)
        var readOffset = 0
        while (readOffset < 4) {
            val read = stream.read(lenBytes, readOffset, 4 - readOffset)
            if (read == -1) break
            readOffset += read
        }
        if (readOffset == 0) return C.RESULT_END_OF_INPUT
        if (readOffset < 4) throw java.io.IOException("Corrupt chunk size")
        
        val chunkLen = java.nio.ByteBuffer.wrap(lenBytes).int
        if (chunkLen < 0 || chunkLen > 1024 * 1024 + 64) throw java.io.IOException("Invalid chunk size")
        
        val encryptedChunk = readFully(stream, chunkLen)
        
        val chunkIv = baseIv.copyOf()
        val indexBytes = java.nio.ByteBuffer.allocate(4).putInt(chunkIndex).array()
        for (i in 0..3) {
            chunkIv[8 + i] = (chunkIv[8 + i].toInt() xor indexBytes[i].toInt()).toByte()
        }
        
        val chunkCipher = Cipher.getInstance(CryptoManager.TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(masterKey, "AES"),
                GCMParameterSpec(CryptoManager.GCM_TAG_LENGTH, chunkIv)
            )
        }
        
        val decrypted = try {
            chunkCipher.doFinal(encryptedChunk)
        } catch (e: Exception) {
            throw java.io.IOException("Error decrypting chunk $chunkIndex", e)
        }
        chunkIndex++
        
        if (decrypted.isNotEmpty()) {
            val toCopy = minOf(length, decrypted.size)
            System.arraycopy(decrypted, 0, buffer, offset, toCopy)
            if (decrypted.size > toCopy) {
                pendingBuffer = decrypted
                pendingOffset = toCopy
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

    private fun readV1(buffer: ByteArray, offset: Int, length: Int, isSkipping: Boolean): Int {
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
        if (opened) {
            opened = false
            transferEnded()
        }
        closeSilently()
    }

    private fun closeSilently() {
        try {
            inputStream?.close()
        } catch (_: Exception) {
        } finally {
            inputStream = null
            cipher = null
            cipherFinished = false
            isV3 = false
            baseIv = ByteArray(0)
            chunkIndex = 0
            pendingBuffer = ByteArray(0)
            pendingOffset = 0
        }
    }
}