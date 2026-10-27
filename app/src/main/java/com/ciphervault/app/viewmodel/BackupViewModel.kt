package com.ciphervault.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.backup.BackupStats
import com.ciphervault.app.totp.QrCodeGenerator
import com.ciphervault.app.totp.TotpVerifyResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the backup screen — handles TOTP setup, QR code generation,
 * recovery codes, and backup file creation.
 */
class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CipherVaultApp

    private var totpSecret: ByteArray? = null

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap

    private val _otpAuthUri = MutableStateFlow<String?>(null)
    val otpAuthUri: StateFlow<String?> = _otpAuthUri

    private val _recoveryCodes = MutableStateFlow<List<String>>(emptyList())
    val recoveryCodes: StateFlow<List<String>> = _recoveryCodes

    private val _isTotpVerified = MutableStateFlow(false)
    val isTotpVerified: StateFlow<Boolean> = _isTotpVerified

    private val _isTotpConfigured = MutableStateFlow(app.vaultConfig.isTotpConfigured)
    val isTotpConfigured: StateFlow<Boolean> = _isTotpConfigured

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _backupUri = MutableStateFlow<Uri?>(null)
    val backupUri: StateFlow<Uri?> = _backupUri

    private val _backupStats = MutableStateFlow<BackupStats?>(null)
    val backupStats: StateFlow<BackupStats?> = _backupStats

    init {
        loadBackupStats()
    }

    /**
     * Loads storage and compression statistics for the vault.
     */
    fun loadBackupStats() {
        viewModelScope.launch {
            try {
                _backupStats.value = app.backupManager.getBackupStats()
            } catch (_: Exception) {}
        }
    }

    // ═══════════════════════════════════════
    //  TOTP Setup
    // ═══════════════════════════════════════

    /**
     * Generates a new TOTP secret, stores it, and creates the QR code.
     */
    fun setupTotp() {
        val secret = app.totpManager.generateAndStoreSecret()
        totpSecret = secret

        val uri = app.totpManager.buildOtpAuthUri(secret)
        _otpAuthUri.value = uri
        _qrBitmap.value = QrCodeGenerator.generate(uri, 512)
    }

    /**
     * Verifies the user's first TOTP code to confirm setup is correct.
     */
    fun verifyTotpSetup(code: String): Boolean {
        val secret = totpSecret ?: app.totpManager.getLocalSecret() ?: return false

        return when (val result = app.totpManager.verifyCode(secret, code)) {
            is TotpVerifyResult.Valid -> {
                app.vaultConfig.isTotpConfigured = true
                _isTotpConfigured.value = true
                _isTotpVerified.value = true

                // Generate recovery codes
                _recoveryCodes.value = app.recoveryCodeManager.generateCodes()
                _message.value = "2FA verified successfully"
                true
            }
            is TotpVerifyResult.Invalid -> {
                _message.value = "Invalid code. ${result.attemptsRemaining} attempts remaining."
                false
            }
            is TotpVerifyResult.LockedOut -> {
                _message.value = "Too many attempts. Wait ${result.secondsRemaining}s."
                false
            }
            is TotpVerifyResult.ReplayDetected -> {
                _message.value = "Code already used. Wait for the next code."
                false
            }
        }
    }

    // ═══════════════════════════════════════
    //  Backup Creation & Export
    // ═══════════════════════════════════════

    /**
     * Creates a .cvault backup file in cache and provides a URI for sharing.
     */
    fun createShareBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val masterKey = app.sessionMasterKey
                    ?: throw IllegalStateException("Vault is locked")

                val uri = app.backupManager.createBackup(masterKey)
                _backupUri.value = uri
                _message.value = "Backup ready for sharing"
                loadBackupStats()
            } catch (e: Exception) {
                _message.value = "Backup failed: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    /**
     * Exports the .cvault backup directly to a user-selected destination file on phone storage.
     */
    fun exportToFile(destinationUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val masterKey = app.sessionMasterKey
                    ?: throw IllegalStateException("Vault is locked")

                app.backupManager.exportBackupToUri(destinationUri, masterKey)
                    .onSuccess {
                        _message.value = "Backup saved to phone storage successfully!"
                        loadBackupStats()
                    }
                    .onFailure {
                        _message.value = "Failed to save: ${it.message}"
                    }
            } catch (e: Exception) {
                _message.value = "Export failed: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() { _message.value = null }
    fun clearBackupUri() { _backupUri.value = null }
}
