package com.extracta.app.domain.repository

import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.DocumentSortOrder
import com.extracta.app.domain.model.OcrStatus
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeDocuments(): Flow<List<Document>>

    fun observeDocuments(query: String, sortOrder: DocumentSortOrder): Flow<List<Document>> =
        observeDocuments()

    fun observeDocument(id: DocumentId): Flow<Document?>

    suspend fun importImage(sourceUri: String, title: String): DocumentId

    suspend fun saveCapturedImage(sourcePath: String, title: String): DocumentId

    suspend fun updateTitle(id: DocumentId, title: String)

    suspend fun deleteDocument(id: DocumentId) {
        error("Document deletion is not supported.")
    }

    suspend fun updateOcr(id: DocumentId, status: OcrStatus, text: String)
}
