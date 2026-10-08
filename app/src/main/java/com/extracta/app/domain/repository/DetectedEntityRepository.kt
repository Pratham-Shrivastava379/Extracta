package com.extracta.app.domain.repository

import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.DocumentId
import kotlinx.coroutines.flow.Flow

interface DetectedEntityRepository {
    fun observeForDocument(documentId: DocumentId): Flow<List<DetectedEntity>>

    suspend fun replaceForDocument(documentId: DocumentId, entities: List<DetectedEntity>)
}
