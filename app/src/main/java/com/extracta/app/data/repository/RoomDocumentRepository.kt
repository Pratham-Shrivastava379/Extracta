package com.extracta.app.data.repository

import com.extracta.app.data.local.DocumentDao
import com.extracta.app.data.local.DocumentEntity
import com.extracta.app.data.local.toDatabaseValue
import com.extracta.app.data.local.toDomain
import com.extracta.app.data.storage.ImageStorage
import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.DocumentSortOrder
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.domain.repository.DocumentRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomDocumentRepository @Inject constructor(
    private val documentDao: DocumentDao,
    private val imageStorage: ImageStorage,
    private val clock: Clock,
) : DocumentRepository {
    override fun observeDocuments(): Flow<List<Document>> =
        observeDocuments("", DocumentSortOrder.NEWEST)

    override fun observeDocuments(
        query: String,
        sortOrder: DocumentSortOrder,
    ): Flow<List<Document>> {
        val pattern = "%${query.trim().escapeLikePattern()}%"
        val source = when (sortOrder) {
            DocumentSortOrder.NEWEST -> documentDao.observeNewest(pattern)
            DocumentSortOrder.OLDEST -> documentDao.observeOldest(pattern)
            DocumentSortOrder.TITLE -> documentDao.observeByTitle(pattern)
        }
        return source.map { entities -> entities.map(DocumentEntity::toDomain) }
    }

    override fun observeDocument(id: DocumentId): Flow<Document?> =
        documentDao.observeById(id.value).map { it?.toDomain() }

    override suspend fun importImage(sourceUri: String, title: String): DocumentId {
        val ownedPath = imageStorage.copyFrom(sourceUri)
        return createDocument(ownedPath, title)
    }

    override suspend fun saveCapturedImage(sourcePath: String, title: String): DocumentId {
        val ownedPath = imageStorage.copyCapturedFile(sourcePath)
        return createDocument(ownedPath, title)
    }

    private suspend fun createDocument(ownedPath: String, title: String): DocumentId {
        return try {
            val now = clock.instant().toEpochMilli()
            val id = documentDao.insert(
                DocumentEntity(
                    title = title.trim().ifBlank { "Imported document" },
                    text = "",
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    imagePath = ownedPath,
                    ocrStatus = OcrStatus.PENDING.toDatabaseValue(),
                ),
            )
            DocumentId(id)
        } catch (error: Throwable) {
            imageStorage.delete(ownedPath)
            throw error
        }
    }

    override suspend fun updateTitle(id: DocumentId, title: String) {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Document title cannot be blank." }
        check(documentDao.updateTitle(id.value, normalizedTitle, clock.instant().toEpochMilli()) == 1) {
            "Document ${id.value} does not exist."
        }
    }

    override suspend fun deleteDocument(id: DocumentId) {
        val document = checkNotNull(documentDao.getById(id.value)) {
            "Document ${id.value} does not exist."
        }
        check(documentDao.deleteById(id.value) == 1) { "Document ${id.value} could not be deleted." }
        document.imagePath?.let { imageStorage.delete(it) }
    }

    override suspend fun updateOcr(id: DocumentId, status: OcrStatus, text: String) {
        check(
            documentDao.updateOcr(
                id = id.value,
                text = text,
                ocrStatus = status.toDatabaseValue(),
                updatedAt = clock.instant().toEpochMilli(),
            ) == 1,
        ) { "Document ${id.value} does not exist." }
    }
}

private fun String.escapeLikePattern(): String = buildString(length) {
    this@escapeLikePattern.forEach { character ->
        if (character == '\\' || character == '%' || character == '_') append('\\')
        append(character)
    }
}
