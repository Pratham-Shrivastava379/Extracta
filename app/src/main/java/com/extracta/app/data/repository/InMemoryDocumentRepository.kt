package com.extracta.app.data.repository

import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.DocumentSortOrder
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.domain.repository.DocumentRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class InMemoryDocumentRepository @Inject constructor() : DocumentRepository {
    private val documents = MutableStateFlow(sampleDocuments)

    override fun observeDocuments(): Flow<List<Document>> =
        observeDocuments("", DocumentSortOrder.NEWEST)

    override fun observeDocuments(query: String, sortOrder: DocumentSortOrder): Flow<List<Document>> =
        documents.map { items ->
            val filtered = items.filter {
                it.title.contains(query.trim(), ignoreCase = true) ||
                    it.text.contains(query.trim(), ignoreCase = true)
            }
            when (sortOrder) {
                DocumentSortOrder.NEWEST -> filtered.sortedWith(
                    compareByDescending<Document> { it.updatedAt }.thenByDescending { it.id.value },
                )
                DocumentSortOrder.OLDEST -> filtered.sortedWith(
                    compareBy<Document> { it.updatedAt }.thenBy { it.id.value },
                )
                DocumentSortOrder.TITLE -> filtered.sortedWith(
                    compareBy<Document, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
                        .thenByDescending { it.updatedAt }
                        .thenByDescending { it.id.value },
                )
            }
        }

    override fun observeDocument(id: DocumentId): Flow<Document?> =
        documents.map { items -> items.firstOrNull { it.id == id } }

    override suspend fun importImage(sourceUri: String, title: String): DocumentId {
        return addImage(sourceUri, title)
    }

    override suspend fun saveCapturedImage(sourcePath: String, title: String): DocumentId {
        return addImage(sourcePath, title)
    }

    private fun addImage(imagePath: String, title: String): DocumentId {
        val nextId = DocumentId((documents.value.maxOfOrNull { it.id.value } ?: 0L) + 1L)
        val now = Instant.now()
        documents.value += Document(
            id = nextId,
            title = title.trim().ifBlank { "Imported document" },
            text = "",
            createdAt = now,
            updatedAt = now,
            imagePath = imagePath,
            ocrStatus = OcrStatus.PENDING,
        )
        return nextId
    }

    override suspend fun updateTitle(id: DocumentId, title: String) {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Document title cannot be blank." }
        documents.value = documents.value.map { document ->
            if (document.id == id) {
                document.copy(title = normalizedTitle, updatedAt = Instant.now())
            } else {
                document
            }
        }
    }

    override suspend fun updateOcr(id: DocumentId, status: OcrStatus, text: String) {
        documents.value = documents.value.map { document ->
            if (document.id == id) {
                document.copy(text = text, ocrStatus = status, updatedAt = Instant.now())
            } else {
                document
            }
        }
    }

    override suspend fun deleteDocument(id: DocumentId) {
        check(documents.value.any { it.id == id }) { "Document ${id.value} does not exist." }
        documents.value = documents.value.filterNot { it.id == id }
    }

    private companion object {
        val sampleDocuments = listOf(
            Document(
                id = DocumentId(1),
                title = "Quarterly planning notes",
                text = "Product review, delivery milestones, and follow-up actions for the next quarter.",
                createdAt = Instant.parse("2026-10-04T09:30:00Z"),
                updatedAt = Instant.parse("2026-10-04T10:15:00Z"),
                imagePath = null,
                ocrStatus = OcrStatus.COMPLETED,
            ),
            Document(
                id = DocumentId(2),
                title = "Coffee shop receipt",
                text = "Subtotal 420.00\nTax 21.00\nTotal 441.00",
                createdAt = Instant.parse("2026-10-03T14:05:00Z"),
                updatedAt = Instant.parse("2026-10-03T14:05:00Z"),
                imagePath = null,
                ocrStatus = OcrStatus.COMPLETED,
            ),
            Document(
                id = DocumentId(3),
                title = "Conference flyer",
                text = "Offline-first Android meetup\nSaturday, 10:00 AM\nCommunity Hall",
                createdAt = Instant.parse("2026-10-01T07:45:00Z"),
                updatedAt = Instant.parse("2026-10-01T07:45:00Z"),
                imagePath = null,
                ocrStatus = OcrStatus.NOT_STARTED,
            ),
        )
    }
}
