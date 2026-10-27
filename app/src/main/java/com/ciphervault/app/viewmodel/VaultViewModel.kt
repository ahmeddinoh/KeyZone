package com.ciphervault.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.data.db.EncryptedFileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    SIZE_DESC("Largest first"),
    SIZE_ASC("Smallest first")
}

/**
 * ViewModel managing vault file operations.
 * Retrieves the master key from the application session for all crypto operations.
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CipherVaultApp
    private val repository = app.fileRepository

    /** The session master key — must be set before any encrypt/decrypt calls. */
    private val masterKey: ByteArray
        get() = app.sessionMasterKey
            ?: throw IllegalStateException("Vault is locked — no session key available")

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption

    val rawFiles: StateFlow<List<EncryptedFileEntity>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<EncryptedFileEntity>> = combine(rawFiles, _sortOption) { fileList, sort ->
        when (sort) {
            SortOption.DATE_DESC -> fileList.sortedByDescending { it.encryptedAt }
            SortOption.DATE_ASC -> fileList.sortedBy { it.encryptedAt }
            SortOption.NAME_ASC -> fileList.sortedBy { it.originalFileName.lowercase() }
            SortOption.NAME_DESC -> fileList.sortedByDescending { it.originalFileName.lowercase() }
            SortOption.SIZE_DESC -> fileList.sortedByDescending { it.originalSize }
            SortOption.SIZE_ASC -> fileList.sortedBy { it.originalSize }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fileCount: StateFlow<Int> = repository.fileCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSize: StateFlow<Long> = repository.totalOriginalSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    private val _decryptedUri = MutableStateFlow<Uri?>(null)
    val decryptedUri: StateFlow<Uri?> = _decryptedUri

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun encryptFile(uri: Uri, fileName: String, mimeType: String?, deleteOriginal: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.encryptFile(uri, fileName, mimeType, masterKey, deleteOriginal)
                .onSuccess { _toastMessage.value = "\"${it.originalFileName}\" encrypted" }
                .onFailure { _toastMessage.value = "Encryption failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun decryptFile(entity: EncryptedFileEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.decryptFile(entity, masterKey)
                .onSuccess {
                    _decryptedUri.value = it
                    _toastMessage.value = "\"${entity.originalFileName}\" decrypted"
                }
                .onFailure { _toastMessage.value = "Decryption failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun exportDecryptedFile(entity: EncryptedFileEntity, destinationUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.exportDecryptedFileToUri(entity, destinationUri, masterKey)
                .onSuccess { _toastMessage.value = "Saved \"${entity.originalFileName}\" to phone files" }
                .onFailure { _toastMessage.value = "Export failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun renameFile(entity: EncryptedFileEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameFile(entity, newName.trim())
            _toastMessage.value = "Renamed to \"${newName.trim()}\""
        }
    }

    fun deleteFile(entity: EncryptedFileEntity) {
        viewModelScope.launch {
            repository.deleteFile(entity)
            _toastMessage.value = "\"${entity.originalFileName}\" deleted"
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearDecryptedCache()
            _toastMessage.value = "Decrypted cache cleared"
        }
    }

    fun clearToast() { _toastMessage.value = null }
    fun clearDecryptedUri() { _decryptedUri.value = null }
}
