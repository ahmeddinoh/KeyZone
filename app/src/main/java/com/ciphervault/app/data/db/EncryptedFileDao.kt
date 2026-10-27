package com.ciphervault.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for encrypted file metadata operations.
 */
@Dao
interface EncryptedFileDao {

    @Query("SELECT * FROM encrypted_files ORDER BY encryptedAt DESC")
    fun getAllFiles(): Flow<List<EncryptedFileEntity>>

    @Query("SELECT * FROM encrypted_files WHERE id = :id")
    suspend fun getFileById(id: Long): EncryptedFileEntity?

    @Query("SELECT * FROM encrypted_files WHERE category = :category ORDER BY encryptedAt DESC")
    fun getFilesByCategory(category: String): Flow<List<EncryptedFileEntity>>

    @Query("SELECT * FROM encrypted_files WHERE originalFileName LIKE '%' || :query || '%' ORDER BY encryptedAt DESC")
    fun searchFiles(query: String): Flow<List<EncryptedFileEntity>>

    @Insert
    suspend fun insertFile(file: EncryptedFileEntity): Long

    @Delete
    suspend fun deleteFile(file: EncryptedFileEntity)

    @Query("DELETE FROM encrypted_files")
    suspend fun deleteAll()

    @Query("UPDATE encrypted_files SET originalFileName = :newName WHERE id = :id")
    suspend fun renameFile(id: Long, newName: String)

    @Query("UPDATE encrypted_files SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String)

    @Query("SELECT COUNT(*) FROM encrypted_files")
    fun getFileCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(originalSize), 0) FROM encrypted_files")
    fun getTotalOriginalSize(): Flow<Long>
}
