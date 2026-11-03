package com.ciphervault.app.viewmodel

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ciphervault.app.CipherVaultApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manages application-level authentication state.
 *
 * The PIN/password IS the encryption key (via PBKDF2 derivation) — not just a lock screen.
 * Without the correct PIN/password, the master key cannot be unwrapped and data is irrecoverable.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CipherVaultApp
    private val vaultConfig = app.vaultConfig
    private val cryptoManager = app.cryptoManager

    private val _appState = MutableStateFlow(determineInitialState())
    val appState: StateFlow<AppState> = _appState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    init {
        viewModelScope.launch {
            app.vaultLockedEvent.collect { timestamp ->
                if (timestamp > 0L && _appState.value == AppState.AUTHENTICATED) {
                    _appState.value = AppState.LOCKED
                }
            }
        }
    }

    private var failedAttempts = 0
    private var lockoutJob: Job? = null
    private val _lockoutSecondsRemaining = MutableStateFlow(0)
    val lockoutSecondsRemaining: StateFlow<Int> = _lockoutSecondsRemaining

    private fun determineInitialState(): AppState =
        if (vaultConfig.isInitialized) AppState.LOCKED else AppState.WELCOME

    /** The vault's key type: "pin" or "password". */
    val keyType: String get() = vaultConfig.keyType

    /** Configured PIN length (e.g. 4 or 6). Defaults to 6. */
    val pinLength: Int get() = vaultConfig.pinLength

    /** Whether device hardware supports biometric authentication. */
    val isBiometricAvailable: Boolean
        get() = app.biometricAuthManager.canAuthenticate()

    /** Whether biometric unlock is currently enabled and configured. */
    val isBiometricEnabled: Boolean
        get() = vaultConfig.isBiometricEnabled && vaultConfig.biometricWrappedMasterKey.isNotEmpty()

    /**
     * Enables or disables biometric unlock.
     * When enabling, wraps the active session master key with the hardware-backed KeyStore key.
     */
    fun setBiometricEnabled(enabled: Boolean): Boolean {
        if (enabled) {
            val masterKey = app.sessionMasterKey ?: return false
            return try {
                val (wrapped, iv) = app.biometricAuthManager.wrapMasterKey(masterKey)
                vaultConfig.biometricWrappedMasterKey = wrapped
                vaultConfig.biometricIv = iv
                vaultConfig.isBiometricEnabled = true
                true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to enable biometric: ${e.message}"
                false
            }
        } else {
            app.biometricAuthManager.deleteBiometricKey()
            vaultConfig.biometricWrappedMasterKey = ByteArray(0)
            vaultConfig.biometricIv = ByteArray(0)
            vaultConfig.isBiometricEnabled = false
            return true
        }
    }

    /**
     * Prompts for biometric unlock and unwraps the session master key.
     */
    fun unlockWithBiometric(activity: FragmentActivity) {
        if (!isBiometricEnabled || _lockoutSecondsRemaining.value > 0) return
        _isProcessing.value = true
        try {
            app.biometricAuthManager.promptBiometric(
                activity = activity,
                title = "Unlock KeyZone",
                subtitle = "Touch sensor to unlock with biometric key",
                onSuccess = {
                    try {
                        val masterKey = app.biometricAuthManager.unwrapMasterKey(
                            vaultConfig.biometricWrappedMasterKey,
                            vaultConfig.biometricIv
                        )
                        app.sessionMasterKey = masterKey
                        failedAttempts = 0
                        _errorMessage.value = null
                        _isProcessing.value = false
                        _appState.value = AppState.AUTHENTICATED
                    } catch (_: Exception) {
                        _errorMessage.value = "Biometric decryption failed. Use PIN/Password."
                        _isProcessing.value = false
                    }
                },
                onError = { error ->
                    if (error.isEmpty()) {
                        _errorMessage.value = null
                    } else {
                        _errorMessage.value = error
                    }
                    _isProcessing.value = false
                }
            )
        } catch (_: Exception) {
            _isProcessing.value = false
            _errorMessage.value = "Biometric prompt failed to start."
        }
    }

    // ═══════════════════════════════════════
    //  Vault Creation
    // ═══════════════════════════════════════

    /**
     * Creates a new vault protected by the given PIN/password.
     *
     * 1. Generates a random PBKDF2 salt
     * 2. Derives a 256-bit wrapping key from the PIN/password
     * 3. Generates a random 256-bit master key
     * 4. Wraps (encrypts) the master key with the derived key
     * 5. Creates a verification token
     * 6. Stores everything in VaultConfig
     * 7. Sets the master key as the session key
     */
    fun createVault(keyType: String, password: String): Boolean {
        if (password.length < 4) {
            _errorMessage.value = "Must be at least 4 characters"
            return false
        }

        _isProcessing.value = true

        val salt = cryptoManager.generateSalt()
        val derivedKey = cryptoManager.deriveKey(password, salt)
        val masterKey = cryptoManager.generateMasterKey()
        val wrappedKey = cryptoManager.wrapKey(masterKey, derivedKey)
        val verification = cryptoManager.createKeyVerification(derivedKey)

        vaultConfig.keyType = keyType
        if (keyType == "pin") {
            vaultConfig.pinLength = password.length
        }
        vaultConfig.salt = salt
        vaultConfig.wrappedMasterKey = wrappedKey
        vaultConfig.keyVerification = verification
        vaultConfig.isInitialized = true

        app.sessionMasterKey = masterKey
        derivedKey.fill(0)

        _errorMessage.value = null
        _isProcessing.value = false
        _appState.value = AppState.AUTHENTICATED
        return true
    }

    // ═══════════════════════════════════════
    //  Vault Unlock & Rate Limiting
    // ═══════════════════════════════════════

    /**
     * Unlocks the vault by verifying the PIN/password and unwrapping the master key.
     * Enforces attempt rate limiting and lockout backoff.
     */
    fun unlock(password: String): Boolean {
        if (_lockoutSecondsRemaining.value > 0) {
            _errorMessage.value = "Too many attempts. Wait ${_lockoutSecondsRemaining.value}s."
            return false
        }

        _isProcessing.value = true
        try {
            val derivedKey = cryptoManager.deriveKey(password, vaultConfig.salt)

            if (!cryptoManager.verifyKey(vaultConfig.keyVerification, derivedKey)) {
                derivedKey.fill(0)
                failedAttempts++
                if (failedAttempts >= 5) {
                    val lockoutDuration = if (failedAttempts >= 10) 60 else 30
                    startLockout(lockoutDuration)
                    _errorMessage.value = "Too many attempts. Locked out for ${lockoutDuration}s."
                } else {
                    val remaining = 5 - failedAttempts
                    _errorMessage.value = "Incorrect ${vaultConfig.keyType} ($remaining attempts left)"
                }
                _isProcessing.value = false
                return false
            }

            val masterKey = cryptoManager.unwrapKey(vaultConfig.wrappedMasterKey, derivedKey)
            app.sessionMasterKey = masterKey
            derivedKey.fill(0)
            failedAttempts = 0

            _errorMessage.value = null
            _isProcessing.value = false
            _appState.value = AppState.AUTHENTICATED
            return true
        } catch (_: Exception) {
            failedAttempts++
            _errorMessage.value = "Incorrect ${vaultConfig.keyType}"
            _isProcessing.value = false
            return false
        }
    }

    private fun startLockout(seconds: Int) {
        lockoutJob?.cancel()
        _lockoutSecondsRemaining.value = seconds
        lockoutJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _lockoutSecondsRemaining.value = remaining
            }
        }
    }

    // ═══════════════════════════════════════
    //  Change Key (Rotation)
    // ═══════════════════════════════════════

    /**
     * Changes the vault PIN/password key while preserving all encrypted files.
     * Re-wraps the master key with the newly derived key.
     */
    fun changeKey(currentKey: String, newKeyType: String, newKey: String): Boolean {
        if (newKey.length < 4) {
            _errorMessage.value = "New key must be at least 4 characters"
            return false
        }
        val currentDerived = cryptoManager.deriveKey(currentKey, vaultConfig.salt)
        if (!cryptoManager.verifyKey(vaultConfig.keyVerification, currentDerived)) {
            _errorMessage.value = "Current ${vaultConfig.keyType} is incorrect"
            currentDerived.fill(0)
            return false
        }

        val masterKey = app.sessionMasterKey ?: cryptoManager.unwrapKey(vaultConfig.wrappedMasterKey, currentDerived)
        currentDerived.fill(0)

        val newSalt = cryptoManager.generateSalt()
        val newDerived = cryptoManager.deriveKey(newKey, newSalt)
        val newWrapped = cryptoManager.wrapKey(masterKey, newDerived)
        val newVerification = cryptoManager.createKeyVerification(newDerived)
        newDerived.fill(0)

        vaultConfig.keyType = newKeyType
        vaultConfig.salt = newSalt
        vaultConfig.wrappedMasterKey = newWrapped
        vaultConfig.keyVerification = newVerification
        _errorMessage.value = null
        return true
    }

    // ═══════════════════════════════════════
    //  State Management
    // ═══════════════════════════════════════

    /** Locks the vault and wipes the session key from memory. */
    fun lock() {
        app.lockVault()
        _appState.value = AppState.LOCKED
    }

    /** Sets state to AUTHENTICATED (e.g., after successful import). */
    fun setAuthenticated() {
        _appState.value = AppState.AUTHENTICATED
    }

    /** Clears the displayed error message. */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Factory reset: securely wipes all vault files, clears database,
     * deletes biometric keys, and resets state back to WELCOME.
     */
    fun wipeVaultAndReset() {
        viewModelScope.launch {
            _isProcessing.value = true
            app.lockVault()
            try {
                val vaultDir = app.fileRepository.vaultDir
                if (vaultDir.exists()) {
                    vaultDir.listFiles()?.forEach { it.delete() }
                }
                app.database.encryptedFileDao().deleteAll()
            } catch (_: Exception) {}

            try {
                app.biometricAuthManager.deleteBiometricKey()
            } catch (_: Exception) {}

            vaultConfig.clear()
            failedAttempts = 0
            _lockoutSecondsRemaining.value = 0
            _errorMessage.value = null
            _isProcessing.value = false
            _appState.value = AppState.WELCOME
        }
    }

    /**
     * Application-level state machine.
     */
    enum class AppState {
        /** No vault exists — show the welcome screen (Create / Import). */
        WELCOME,
        /** Vault exists but is locked — show PIN/password entry. */
        LOCKED,
        /** Vault is unlocked — show the main app. */
        AUTHENTICATED
    }
}
