package com.ciphervault.app

import android.app.Application
import com.ciphervault.app.backup.VaultBackupManager
import com.ciphervault.app.crypto.BiometricAuthManager
import com.ciphervault.app.crypto.CryptoManager
import com.ciphervault.app.data.VaultConfig
import com.ciphervault.app.data.db.AppDatabase
import com.ciphervault.app.data.repository.FileRepository
import com.ciphervault.app.totp.RecoveryCodeManager
import com.ciphervault.app.totp.TotpManager
import java.io.File

class CipherVaultApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var cryptoManager: CryptoManager
        private set

    lateinit var fileRepository: FileRepository
        private set

    lateinit var vaultConfig: VaultConfig
        private set

    lateinit var totpManager: TotpManager
        private set

    lateinit var recoveryCodeManager: RecoveryCodeManager
        private set

    lateinit var backupManager: VaultBackupManager
        private set

    lateinit var biometricAuthManager: BiometricAuthManager
        private set

    /**
     * In-memory session master key.
     * Set on successful authentication, wiped on lock.
     * Never persisted to disk in plaintext.
     */
    var sessionMasterKey: ByteArray? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        cryptoManager = CryptoManager()
        database = AppDatabase.getInstance(this)
        vaultConfig = VaultConfig(this)
        biometricAuthManager = BiometricAuthManager(this)
        totpManager = TotpManager(this, cryptoManager)
        recoveryCodeManager = RecoveryCodeManager(this)

        fileRepository = FileRepository(
            context = this,
            encryptedFileDao = database.encryptedFileDao(),
            cryptoManager = cryptoManager
        )

        backupManager = VaultBackupManager(
            context = this,
            cryptoManager = cryptoManager,
            vaultConfig = vaultConfig,
            fileRepository = fileRepository,
            encryptedFileDao = database.encryptedFileDao(),
            totpManager = totpManager,
            recoveryCodeManager = recoveryCodeManager
        )
    }

    /** Securely wipes the session master key from memory and purges temporary decrypted cache. */
    fun lockVault() {
        sessionMasterKey?.fill(0)
        sessionMasterKey = null

        // Auto-purge decrypted cache files on lock for zero data leakage
        try {
            val decryptedDir = File(cacheDir, "decrypted")
            if (decryptedDir.exists()) {
                decryptedDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    companion object {
        lateinit var instance: CipherVaultApp
            private set
    }
}
