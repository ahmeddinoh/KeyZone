package com.ciphervault.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.ciphervault.app.crypto.CryptoManager
import com.ciphervault.app.data.db.EncryptedFileDao
import com.ciphervault.app.data.db.EncryptedFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Repository coordinating encryption/decryption operations and database metadata.
 * All encryption methods require the session master key as a parameter —
 * the key is never stored or cached in this class.
 */
class FileRepository(
    private val context: Context,
    private val encryptedFileDao: EncryptedFileDao,
    private val cryptoManager: CryptoManager
) {
    val allFiles: Flow<List<EncryptedFileEntity>> = encryptedFileDao.getAllFiles()
    val fileCount: Flow<Int> = encryptedFileDao.getFileCount()
    val totalOriginalSize: Flow<Long> = encryptedFileDao.getTotalOriginalSize()

    /** Directory where encrypted files are stored (app-private). */
    val vaultDir: File
        get() = File(context.filesDir, "vault").also { if (!it.exists()) it.mkdirs() }

    private val decryptedCacheDir: File
        get() = File(context.cacheDir, "decrypted").also { if (!it.exists()) it.mkdirs() }

    /**
     * Encrypts a file and stores it in the vault.
     *
     * @param uri Content URI of the file to encrypt
     * @param originalFileName Display name of the original file
     * @param mimeType MIME type of the original file
     * @param masterKey The session master key for AES-256-GCM encryption
     * @param deleteOriginal If true, attempts to delete the unencrypted source file via SAF
     */
    suspend fun encryptFile(
        uri: Uri,
        originalFileName: String,
        mimeType: String?,
        masterKey: ByteArray,
        deleteOriginal: Boolean = false
    ): Result<EncryptedFileEntity> = withContext(Dispatchers.IO) {
        try {
            val encryptedFileName = UUID.randomUUID().toString() + ".enc"
            val encryptedFile = File(vaultDir, encryptedFileName)

            // Query original file size
            var originalSize = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        originalSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open the selected file"))

            encryptedFile.outputStream().use { outputStream ->
                inputStream.use { input ->
                    cryptoManager.encrypt(input, outputStream, masterKey)
                }
            }

            if (deleteOriginal) {
                try {
                    android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
                } catch (_: Exception) {}
            }

            val entity = EncryptedFileEntity(
                originalFileName = originalFileName,
                encryptedFileName = encryptedFileName,
                originalSize = originalSize,
                mimeType = mimeType
            )
            val id = encryptedFileDao.insertFile(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decrypts a file from the vault and returns a shareable content URI.
     *
     * @param entity The encrypted file metadata
     * @param masterKey The session master key for AES-256-GCM decryption
     */
    suspend fun decryptFile(
        entity: EncryptedFileEntity,
        masterKey: ByteArray
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(vaultDir, entity.encryptedFileName)
            if (!encryptedFile.exists()) {
                return@withContext Result.failure(Exception("Encrypted file not found on disk"))
            }

            val decryptedFile = File(decryptedCacheDir, entity.originalFileName)

            encryptedFile.inputStream().use { input ->
                decryptedFile.outputStream().use { output ->
                    cryptoManager.decrypt(input, output, masterKey)
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                decryptedFile
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decrypts a file directly into a destination URI selected by user via SAF.
     */
    suspend fun exportDecryptedFileToUri(
        entity: EncryptedFileEntity,
        destinationUri: Uri,
        masterKey: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(vaultDir, entity.encryptedFileName)
            if (!encryptedFile.exists()) {
                return@withContext Result.failure(Exception("Encrypted file not found on disk"))
            }

            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext Result.failure(Exception("Cannot open destination location"))

            encryptedFile.inputStream().use { input ->
                outputStream.use { output ->
                    cryptoManager.decrypt(input, output, masterKey)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(entity: EncryptedFileEntity, newName: String) = withContext(Dispatchers.IO) {
        encryptedFileDao.renameFile(entity.id, newName)
    }

    suspend fun deleteFile(entity: EncryptedFileEntity) = withContext(Dispatchers.IO) {
        File(vaultDir, entity.encryptedFileName).let { if (it.exists()) it.delete() }
        encryptedFileDao.deleteFile(entity)
    }

    fun searchFiles(query: String): Flow<List<EncryptedFileEntity>> =
        encryptedFileDao.searchFiles(query)

    suspend fun getVaultSizeOnDisk(): Long = withContext(Dispatchers.IO) {
        vaultDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun getDecryptedCacheSize(): Long = withContext(Dispatchers.IO) {
        decryptedCacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clearDecryptedCache() = withContext(Dispatchers.IO) {
        decryptedCacheDir.listFiles()?.forEach { it.delete() }
    }
}
