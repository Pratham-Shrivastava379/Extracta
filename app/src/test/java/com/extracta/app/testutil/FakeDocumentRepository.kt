package com.extracta.app.testutil

import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.DocumentSortOrder
import com.extracta.app.domain.repository.DocumentRepository
import com.extracta.app.domain.model.OcrStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

class FakeDocumentRepository(
    initialDocuments: List<Document> = emptyList(),
) : DocumentRepository {
    private val documents = MutableStateFlow(initialDocuments)

    override fun observeDocuments(): Flow<List<Document>> = documents

    override fun observeDocuments(query: String, sortOrder: DocumentSortOrder): Flow<List<Document>> =
        documents.map { items ->
            val filtered = items.filter {
                it.title.contains(query.trim(), ignoreCase = true) ||
                    it.text.contains(query.trim(), ignoreCase = true)
            }
            when (sortOrder) {
                DocumentSortOrder.NEWEST -> filtered.sortedByDescending { it.updatedAt }
                DocumentSortOrder.OLDEST -> filtered.sortedBy { it.updatedAt }
                DocumentSortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
            }
        }

    override fun observeDocument(id: DocumentId): Flow<Document?> =
        documents.map { items -> items.firstOrNull { it.id == id } }

    override suspend fun importImage(sourceUri: String, title: String): DocumentId =
        addImage(sourceUri, title)

    override suspend fun saveCapturedImage(sourcePath: String, title: String): DocumentId =
        addImage(sourcePath, title)

    private fun addImage(path: String, title: String): DocumentId {
        val id = DocumentId((documents.value.maxOfOrNull { it.id.value } ?: 0L) + 1L)
        val now = Instant.now()
        documents.value += Document(
            id = id,
            title = title,
            text = "",
            createdAt = now,
            updatedAt = now,
            imagePath = path,
            ocrStatus = OcrStatus.PENDING,
        )
        return id
    }

    override suspend fun updateTitle(id: DocumentId, title: String) {
        documents.value = documents.value.map { document ->
            if (document.id == id) document.copy(title = title) else document
        }
    }

    override suspend fun updateOcr(id: DocumentId, status: OcrStatus, text: String) {
        documents.value = documents.value.map { document ->
            if (document.id == id) document.copy(ocrStatus = status, text = text) else document
        }
    }

    override suspend fun deleteDocument(id: DocumentId) {
        documents.value = documents.value.filterNot { it.id == id }
    }

    fun emit(documents: List<Document>) {
        this.documents.value = documents
    }
}
