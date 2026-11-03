package com.ciphervault.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ciphervault.app.CipherVaultApp
import com.ciphervault.app.data.db.EncryptedFileEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String?
)

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
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _mediaPreviewUri = MutableStateFlow<Uri?>(null)
    val mediaPreviewUri: StateFlow<Uri?> = _mediaPreviewUri

    private val _viewingNoteContent = MutableStateFlow<Pair<String, String>?>(null)
    val viewingNoteContent: StateFlow<Pair<String, String>?> = _viewingNoteContent
    
    val selectedFilesForEncryption = mutableStateListOf<SelectedFile>()

    private val _viewingCredentialContent = MutableStateFlow<Pair<String, Map<String, String>>?>(null)
    val viewingCredentialContent: StateFlow<Pair<String, Map<String, String>>?> = _viewingCredentialContent

    private val _highlightedFileId = MutableStateFlow<Long?>(null)
    val highlightedFileId: StateFlow<Long?> = _highlightedFileId

    fun setHighlightedFile(id: Long?) {
        _highlightedFileId.value = id
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    private val _encryptionProgress = MutableStateFlow(0f)
    val encryptionProgress: StateFlow<Float> = _encryptionProgress

    suspend fun encryptFileSuspend(uri: Uri, fileName: String, mimeType: String?, deleteOriginal: Boolean = false) {
        _isLoading.value = true
        _encryptionProgress.value = 0f
        repository.encryptFile(uri, fileName, mimeType, masterKey, deleteOriginal) { progress ->
            _encryptionProgress.value = progress
        }
            // Removing the Toast success message as requested by user
            .onFailure { _toastMessage.value = "Encryption failed: ${it.message}" }
        _isLoading.value = false
    }

    fun encryptFile(uri: Uri, fileName: String, mimeType: String?, deleteOriginal: Boolean = false) {
        viewModelScope.launch {
            encryptFileSuspend(uri, fileName, mimeType, deleteOriginal)
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

    fun prepareMediaPreview(entity: EncryptedFileEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.decryptFile(entity, masterKey)
                .onSuccess { uri ->
                    _mediaPreviewUri.value = uri
                }
                .onFailure { _toastMessage.value = "Failed to load media: ${it.message}" }
            _isLoading.value = false
        }
    }

    suspend fun decryptToByteArray(entity: EncryptedFileEntity): ByteArray? {
        var result: ByteArray? = null
        _isLoading.value = true
        repository.decryptFileToByteArray(entity, masterKey)
            .onSuccess { result = it }
            .onFailure { _toastMessage.value = "Decryption failed: ${it.message}" }
        _isLoading.value = false
        return result
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

    fun updateTags(entity: EncryptedFileEntity, tags: String) {
        viewModelScope.launch {
            repository.updateTags(entity, tags.trim())
            _toastMessage.value = "Tags updated"
        }
    }

    fun createSecureNote(context: Context, title: String, content: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tempFile = File(context.cacheDir, "$title.note.txt")
                tempFile.writeText(content)
                val uri = Uri.fromFile(tempFile)
                repository.encryptFile(uri, "$title.note.txt", "text/plain", masterKey, deleteOriginal = false)
                tempFile.delete()
                _toastMessage.value = "Secure note saved"
            } catch (e: Exception) {
                _toastMessage.value = "Failed to save note: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCredential(context: Context, title: String, username: String, pass: String) {
        if (title.isBlank() || pass.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tempFile = File(context.cacheDir, "$title.cred.txt")
                val content = "Username:$username\nPassword:$pass"
                tempFile.writeText(content)
                val uri = Uri.fromFile(tempFile)
                repository.encryptFile(uri, "$title.cred.txt", "text/plain", masterKey, deleteOriginal = false)
                tempFile.delete()
                _toastMessage.value = "Credentials saved"
            } catch (e: Exception) {
                _toastMessage.value = "Failed to save credentials: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun viewSecureNote(entity: EncryptedFileEntity, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.decryptFile(entity, masterKey)
                .onSuccess { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val text = inputStream?.bufferedReader().use { it?.readText() }
                        if (text != null) {
                            if (entity.originalFileName.endsWith(".cred.txt")) {
                                val lines = text.split("\n")
                                val username = lines.find { it.startsWith("Username:") }?.substringAfter("Username:") ?: ""
                                val password = lines.find { it.startsWith("Password:") }?.substringAfter("Password:") ?: ""
                                val map = mapOf("username" to username, "password" to password)
                                _viewingCredentialContent.value = Pair(entity.originalFileName.removeSuffix(".cred.txt"), map)
                            } else {
                                _viewingNoteContent.value = Pair(entity.originalFileName.removeSuffix(".note.txt"), text)
                            }
                        } else {
                            _toastMessage.value = "Content is empty"
                        }
                    } catch (e: Exception) {
                        _toastMessage.value = "Failed to read content"
                    }
                }
                .onFailure { _toastMessage.value = "Decryption failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun clearViewingNote() { _viewingNoteContent.value = null }
    fun clearViewingCredential() { _viewingCredentialContent.value = null }

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
    fun clearMediaPreviewUri() { _mediaPreviewUri.value = null }

    fun deleteDecryptedCacheFile(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The uri looks like content://com.ciphervault.app.fileprovider/cache/decrypted/filename.ext
                val path = uri.path
                if (path != null && path.contains("cache/decrypted/")) {
                    val filename = path.substringAfterLast("/")
                    val cacheDir = File(context.cacheDir, "decrypted")
                    val fileToDelete = File(cacheDir, filename)
                    if (fileToDelete.exists()) {
                        fileToDelete.delete()
                    }
                }
            } catch (e: Exception) {
                // Ignore exceptions during cleanup
            }
        }
    }
}
