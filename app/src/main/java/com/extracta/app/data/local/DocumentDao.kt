package com.extracta.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query(
        "SELECT * FROM documents " +
            "WHERE title LIKE :query ESCAPE '\\' COLLATE NOCASE " +
            "OR text LIKE :query ESCAPE '\\' COLLATE NOCASE " +
            "ORDER BY updatedAtEpochMillis DESC, id DESC",
    )
    fun observeNewest(query: String): Flow<List<DocumentEntity>>

    @Query(
        "SELECT * FROM documents " +
            "WHERE title LIKE :query ESCAPE '\\' COLLATE NOCASE " +
            "OR text LIKE :query ESCAPE '\\' COLLATE NOCASE " +
            "ORDER BY updatedAtEpochMillis ASC, id ASC",
    )
    fun observeOldest(query: String): Flow<List<DocumentEntity>>

    @Query(
        "SELECT * FROM documents " +
            "WHERE title LIKE :query ESCAPE '\\' COLLATE NOCASE " +
            "OR text LIKE :query ESCAPE '\\' COLLATE NOCASE " +
            "ORDER BY title COLLATE NOCASE ASC, updatedAtEpochMillis DESC, id DESC",
    )
    fun observeByTitle(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Query("UPDATE documents SET title = :title, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Long): Int

    @Query(
        "UPDATE documents SET text = :text, ocrStatus = :ocrStatus, " +
            "updatedAtEpochMillis = :updatedAt WHERE id = :id",
    )
    suspend fun updateOcr(
        id: Long,
        text: String,
        ocrStatus: String,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
