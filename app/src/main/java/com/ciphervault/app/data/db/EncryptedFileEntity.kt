package com.ciphervault.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the metadata of an encrypted file stored in the vault.
 * The actual encrypted content is stored as a file on disk.
 */
@Entity(tableName = "encrypted_files")
data class EncryptedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Original file name before encryption */
    val originalFileName: String,

    /** Name of the encrypted file in the vault directory */
    val encryptedFileName: String,

    /** Original file size in bytes */
    val originalSize: Long,

    /** MIME type of the original file (e.g., "image/jpeg") */
    val mimeType: String? = null,

    /** Timestamp when the file was encrypted */
    val encryptedAt: Long = System.currentTimeMillis(),

    /** User-defined category for organization */
    val category: String = "General",

    /** Comma-separated list of tags */
    val tags: String = ""
)
