package com.ciphervault.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.backup.BackupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the 2-step vault import workflow:
 * 1. Select .cvault backup file
 * 2. Enter PIN / password (the encryption key)
 * 3. Enter 6-digit TOTP code from Authenticator App (or recovery code)
 *
 * Once imported, the app unlocks immediately and on future app launches,
 * only the PIN / password is required (no TOTP code needed for normal app runs).
 */
class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CipherVaultApp
    private val backupManager = app.backupManager

    enum class ImportStep {
        SELECT_FILE,
        ENTER_KEY,
        ENTER_TOTP,
        SUCCESS
    }

    private val _step = MutableStateFlow(ImportStep.SELECT_FILE)
    val step: StateFlow<ImportStep> = _step

    private val _backupUri = MutableStateFlow<Uri?>(null)
    val backupUri: StateFlow<Uri?> = _backupUri

    private val _backupInfo = MutableStateFlow<BackupInfo?>(null)
    val backupInfo: StateFlow<BackupInfo?> = _backupInfo

    private val _enteredKey = MutableStateFlow("")
    val enteredKey: StateFlow<String> = _enteredKey

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Called when the user picks a backup file.
     */
    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            backupManager.inspectBackup(uri)
                .onSuccess { info ->
                    _backupUri.value = uri
                    _backupInfo.value = info
                    _step.value = ImportStep.ENTER_KEY
                }
                .onFailure {
                    _errorMessage.value = "Failed to read backup file: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    /**
     * Step 1 verification: Validates the entered PIN or password against the backup.
     */
    fun submitKey(key: String, onValid: () -> Unit = {}) {
        val uri = _backupUri.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            backupManager.verifyKeyForBackup(uri, key)
                .onSuccess { isValid ->
                    if (isValid) {
                        _enteredKey.value = key
                        val info = _backupInfo.value
                        if (info != null && info.hasTotp) {
                            _step.value = ImportStep.ENTER_TOTP
                            onValid()
                        } else {
                            // If backup had no TOTP configured, import directly
                            executeImport(uri, key, "")
                        }
                    } else {
                        val keyType = _backupInfo.value?.keyType ?: "key"
                        _errorMessage.value = "Incorrect $keyType. Please try again."
                    }
                }
                .onFailure {
                    _errorMessage.value = "Verification error: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    /**
     * Step 2 verification: Validates the TOTP 2FA code and completes import.
     */
    fun submitTotp(code: String, onSuccess: () -> Unit) {
        val uri = _backupUri.value ?: return
        val key = _enteredKey.value
        executeImport(uri, key, code, onSuccess)
    }

    private fun executeImport(
        uri: Uri,
        key: String,
        totpCode: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            backupManager.importBackup(uri, key, totpCode)
                .onSuccess { masterKey ->
                    // Set active session master key
                    app.sessionMasterKey = masterKey
                    _step.value = ImportStep.SUCCESS
                    onSuccess()
                }
                .onFailure {
                    _errorMessage.value = it.message ?: "Failed to import vault"
                }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun reset() {
        _step.value = ImportStep.SELECT_FILE
        _backupUri.value = null
        _backupInfo.value = null
        _enteredKey.value = ""
        _errorMessage.value = null
        _isLoading.value = false
    }
}
