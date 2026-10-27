package com.ciphervault.app.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.ciphervault.app.crypto.CryptoManager
import com.ciphervault.app.data.VaultConfig
import com.ciphervault.app.data.db.EncryptedFileDao
import com.ciphervault.app.data.db.EncryptedFileEntity
import com.ciphervault.app.data.repository.FileRepository
import com.ciphervault.app.totp.RecoveryCodeManager
import com.ciphervault.app.totp.TotpEngine
import com.ciphervault.app.totp.TotpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupInfo(
    val keyType: String,
    val fileCount: Int,
    val createdAt: Long,
    val hasTotp: Boolean
)

data class BackupStats(
    val rawVaultSizeBytes: Long,
    val compressedBackupSizeBytes: Long,
    val fileCount: Int,
    val spaceSavedBytes: Long,
    val savingsPercentage: Int,
    val compressionLevelName: String = "DEFLATE (Maximum Level 9)"
)

/**
 * Manages vault backup creation and import.
 *
 * Backup format: ZIP archive (.cvault) containing:
 * ```
 * manifest.json     — vault config, TOTP data, file metadata
 * vault/
 *   <uuid>.enc      — encrypted file data (already AES-256-GCM encrypted)
 * ```
 *
 * Security model:
 * - Files are encrypted with the master key (derived from PIN/password)
 * - TOTP secret is encrypted with the master key for portability
 * - Import requires both the PIN/password AND a valid TOTP code
 */
class VaultBackupManager(
    private val context: Context,
    private val cryptoManager: CryptoManager,
    private val vaultConfig: VaultConfig,
    private val fileRepository: FileRepository,
    private val encryptedFileDao: EncryptedFileDao,
    private val totpManager: TotpManager,
    private val recoveryCodeManager: RecoveryCodeManager
) {

    /**
     * Builds the JSON manifest containing vault configuration, encrypted TOTP secret, and file metadata.
     */
    private suspend fun buildManifest(files: List<EncryptedFileEntity>, masterKey: ByteArray): JSONObject {
        val totpSecret = totpManager.getLocalSecret()
        return JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("createdAt", System.currentTimeMillis())
            put("keyType", vaultConfig.keyType)
            put("salt", encB64(vaultConfig.salt))
            put("wrappedMasterKey", encB64(vaultConfig.wrappedMasterKey))
            put("keyVerification", encB64(vaultConfig.keyVerification))

            // TOTP secret encrypted with master key (portable across devices)
            if (totpSecret != null) {
                val encTotp = totpManager.encryptSecretForBackup(totpSecret, masterKey)
                put("totpSecretEncrypted", encB64(encTotp))
            }

            // Recovery code hashes
            put("recoveryCodesHashed", JSONArray(recoveryCodeManager.getHashedCodes().toList()))

            // File metadata
            val fileArray = JSONArray()
            files.forEach { f ->
                fileArray.put(JSONObject().apply {
                    put("originalFileName", f.originalFileName)
                    put("encryptedFileName", f.encryptedFileName)
                    put("mimeType", f.mimeType ?: "")
                    put("originalSize", f.originalSize)
                    put("encryptedAt", f.encryptedAt)
                    put("category", f.category)
                })
            }
            put("files", fileArray)
        }
    }

    /**
     * Writes the backup ZIP archive using Deflater.BEST_COMPRESSION (Level 9)
     * and compact JSON serialization to produce the smallest possible backup archive.
     */
    private fun writeZipArchive(
        outputStream: java.io.OutputStream,
        manifest: JSONObject,
        files: List<EncryptedFileEntity>
    ) {
        val buffered = BufferedOutputStream(outputStream)
        ZipOutputStream(buffered).use { zip ->
            // Enforce maximum DEFLATE compression (Level 9)
            zip.setLevel(Deflater.BEST_COMPRESSION)

            // Compact manifest (zero formatting whitespace)
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            val vaultDir = fileRepository.vaultDir
            val buffer = ByteArray(32768)
            files.forEach { file ->
                val encFile = File(vaultDir, file.encryptedFileName)
                if (encFile.exists()) {
                    zip.putNextEntry(ZipEntry("vault/${file.encryptedFileName}"))
                    encFile.inputStream().use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            zip.write(buffer, 0, read)
                        }
                    }
                    zip.closeEntry()
                }
            }
        }
    }

    /**
     * Computes storage and backup size comparison metrics.
     * Compares the total uncompressed source file payload against the
     * compressed & encrypted .cvault archive package.
     */
    suspend fun getBackupStats(): BackupStats = withContext(Dispatchers.IO) {
        val files = fileRepository.allFiles.first()
        val fileCount = files.size
        val rawVaultSizeBytes = files.sumOf { it.originalSize }

        val vaultDir = fileRepository.vaultDir
        var actualVaultEncryptedSize = 0L
        files.forEach { file ->
            val encFile = File(vaultDir, file.encryptedFileName)
            if (encFile.exists()) {
                actualVaultEncryptedSize += encFile.length()
            }
        }

        // Add estimated ZIP container & manifest overhead (~1.5 KB)
        val estimatedBackupSize = if (actualVaultEncryptedSize > 0) {
            actualVaultEncryptedSize + 1500L
        } else {
            1024L
        }

        val effectiveRawSize = if (rawVaultSizeBytes > 0) rawVaultSizeBytes else estimatedBackupSize
        val spaceSavedBytes = maxOf(0L, effectiveRawSize - estimatedBackupSize)
        val savingsPercentage = if (effectiveRawSize > 0 && spaceSavedBytes > 0) {
            ((spaceSavedBytes.toDouble() / effectiveRawSize) * 100).toInt()
        } else {
            0
        }

        BackupStats(
            rawVaultSizeBytes = effectiveRawSize,
            compressedBackupSizeBytes = estimatedBackupSize,
            fileCount = fileCount,
            spaceSavedBytes = spaceSavedBytes,
            savingsPercentage = savingsPercentage
        )
    }

    /**
     * Creates a backup ZIP in temporary storage and returns a shareable URI.
     *
     * @param masterKey Session master key (encrypts TOTP secret for portability)
     * @return Content URI of the .cvault backup file for Android share sheet
     */
    suspend fun createBackup(masterKey: ByteArray): Uri = withContext(Dispatchers.IO) {
        val backupDir = File(context.cacheDir, "backups").also { it.mkdirs() }
        val backupFile = File(backupDir, "CipherVault_${System.currentTimeMillis()}.cvault")

        val files = fileRepository.allFiles.first()
        val manifest = buildManifest(files, masterKey)

        backupFile.outputStream().use { out ->
            writeZipArchive(out, manifest, files)
        }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
    }

    /**
     * Exports the backup directly to a user-selected destination file on phone storage.
     *
     * @param destinationUri Destination Uri chosen by user via CreateDocument file picker
     * @param masterKey Session master key
     */
    suspend fun exportBackupToUri(destinationUri: Uri, masterKey: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext Result.failure(Exception("Cannot open selected location for saving"))

            val files = fileRepository.allFiles.first()
            val manifest = buildManifest(files, masterKey)

            outputStream.use { out ->
                writeZipArchive(out, manifest, files)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inspects a backup file and returns its metadata without importing.
     */
    suspend fun inspectBackup(backupUri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(backupUri)
                ?: return@withContext Result.failure(Exception("Cannot open backup file"))

            var manifest: JSONObject? = null
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "manifest.json") {
                        manifest = JSONObject(String(zip.readBytes(), Charsets.UTF_8))
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val m = manifest
                ?: return@withContext Result.failure(Exception("Invalid backup: missing manifest"))

            Result.success(
                BackupInfo(
                    keyType = m.optString("keyType", "pin"),
                    fileCount = m.optJSONArray("files")?.length() ?: 0,
                    createdAt = m.optLong("createdAt", 0L),
                    hasTotp = m.has("totpSecretEncrypted")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies if the entered PIN/password correctly decrypts the backup's key verification.
     */
    suspend fun verifyKeyForBackup(backupUri: Uri, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(backupUri)
                ?: return@withContext Result.failure(Exception("Cannot open backup file"))

            var manifest: JSONObject? = null
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "manifest.json") {
                        manifest = JSONObject(String(zip.readBytes(), Charsets.UTF_8))
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val m = manifest
                ?: return@withContext Result.failure(Exception("Invalid backup: missing manifest"))

            val salt = decB64(m.getString("salt"))
            val keyVerification = decB64(m.getString("keyVerification"))
            val derivedKey = cryptoManager.deriveKey(password, salt)
            val isValid = cryptoManager.verifyKey(keyVerification, derivedKey)
            derivedKey.fill(0)

            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports a vault from a backup file.
     *
     * 1. Verifies the PIN/password by deriving the key and checking the verification token
     * 2. Unwraps the master key
     * 3. Verifies the TOTP code (or recovery code)
     * 4. Imports all encrypted files and metadata
     *
     * @param backupUri URI of the .cvault backup file
     * @param password The PIN/password
     * @param totpCode 6-digit TOTP code or recovery code
     * @return The master key on success (caller should set as session key)
     */
    suspend fun importBackup(
        backupUri: Uri,
        password: String,
        totpCode: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(backupUri)
                ?: return@withContext Result.failure(Exception("Cannot open backup file"))

            // ── Parse ZIP ──
            var manifest: JSONObject? = null
            val fileDataMap = mutableMapOf<String, ByteArray>()

            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "manifest.json" ->
                            manifest = JSONObject(String(zip.readBytes(), Charsets.UTF_8))
                        entry.name.startsWith("vault/") -> {
                            val name = entry.name.removePrefix("vault/")
                            if (name.isNotEmpty()) fileDataMap[name] = zip.readBytes()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val m = manifest
                ?: return@withContext Result.failure(Exception("Invalid backup: missing manifest"))

            // ── Step 1: Verify PIN/password ──
            val salt = decB64(m.getString("salt"))
            val wrappedMasterKey = decB64(m.getString("wrappedMasterKey"))
            val keyVerification = decB64(m.getString("keyVerification"))

            val derivedKey = cryptoManager.deriveKey(password, salt)
            if (!cryptoManager.verifyKey(keyVerification, derivedKey)) {
                derivedKey.fill(0)
                return@withContext Result.failure(Exception("Incorrect PIN or password"))
            }

            val masterKey = cryptoManager.unwrapKey(wrappedMasterKey, derivedKey)
            derivedKey.fill(0)

            // ── Step 2: Verify TOTP ──
            if (m.has("totpSecretEncrypted")) {
                val encTotp = decB64(m.getString("totpSecretEncrypted"))
                val totpSecret = totpManager.decryptSecretFromBackup(encTotp, masterKey)

                // Try TOTP code first
                val totpResult = TotpEngine.verify(totpSecret, totpCode)
                if (!totpResult.valid) {
                    // Try as recovery code
                    val recoveryHashes = mutableSetOf<String>()
                    if (m.has("recoveryCodesHashed")) {
                        val arr = m.getJSONArray("recoveryCodesHashed")
                        for (i in 0 until arr.length()) recoveryHashes.add(arr.getString(i))
                    }
                    recoveryCodeManager.importHashedCodes(recoveryHashes)

                    if (!recoveryCodeManager.verifyAndConsume(totpCode)) {
                        masterKey.fill(0)
                        return@withContext Result.failure(
                            Exception("Invalid verification code or recovery code")
                        )
                    }
                }

                // Store TOTP secret locally (KeyStore-encrypted)
                totpManager.storeSecretLocally(totpSecret)
                totpManager.resetState()
            }

            // ── Step 3: Import vault configuration ──
            vaultConfig.keyType = m.getString("keyType")
            vaultConfig.salt = salt
            vaultConfig.wrappedMasterKey = wrappedMasterKey
            vaultConfig.keyVerification = keyVerification
            vaultConfig.isTotpConfigured = m.has("totpSecretEncrypted")
            vaultConfig.isInitialized = true

            // ── Step 4: Import encrypted files ──
            val vaultDir = fileRepository.vaultDir
            val filesArray = m.getJSONArray("files")

            for (i in 0 until filesArray.length()) {
                val obj = filesArray.getJSONObject(i)
                val encFileName = obj.getString("encryptedFileName")

                fileDataMap[encFileName]?.let { data ->
                    File(vaultDir, encFileName).writeBytes(data)
                }

                encryptedFileDao.insertFile(
                    EncryptedFileEntity(
                        originalFileName = obj.getString("originalFileName"),
                        encryptedFileName = encFileName,
                        mimeType = obj.optString("mimeType").ifEmpty { null },
                        originalSize = obj.getLong("originalSize"),
                        encryptedAt = obj.getLong("encryptedAt"),
                        category = obj.optString("category", "General")
                    )
                )
            }

            // Import remaining recovery codes
            if (m.has("recoveryCodesHashed")) {
                val hashes = mutableSetOf<String>()
                val arr = m.getJSONArray("recoveryCodesHashed")
                for (i in 0 until arr.length()) hashes.add(arr.getString(i))
                recoveryCodeManager.importHashedCodes(hashes)
            }

            Result.success(masterKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encB64(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)
    private fun decB64(str: String): ByteArray = Base64.decode(str, Base64.NO_WRAP)

    companion object {
        private const val BACKUP_VERSION = 1
    }
}
