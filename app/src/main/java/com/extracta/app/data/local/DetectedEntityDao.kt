package com.extracta.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectedEntityDao {
    @Query(
        "SELECT * FROM detected_entities WHERE documentId = :documentId " +
            "ORDER BY startOffset ASC, endOffset ASC, id ASC",
    )
    fun observeByDocument(documentId: Long): Flow<List<DetectedEntityEntity>>

    @Query("DELETE FROM detected_entities WHERE documentId = :documentId")
    suspend fun deleteByDocument(documentId: Long)

    @Insert
    suspend fun insertAll(entities: List<DetectedEntityEntity>)

    @Transaction
    suspend fun replaceForDocument(documentId: Long, entities: List<DetectedEntityEntity>) {
        deleteByDocument(documentId)
        if (entities.isNotEmpty()) insertAll(entities)
    }
}
