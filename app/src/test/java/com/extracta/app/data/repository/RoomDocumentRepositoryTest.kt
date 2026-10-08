package com.extracta.app.data.repository

import com.extracta.app.data.local.DocumentDao
import com.extracta.app.data.local.DocumentEntity
import com.extracta.app.data.storage.ImageStorage
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.domain.model.DocumentSortOrder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomDocumentRepositoryTest {
    private val dao = FakeDocumentDao()
    private val storage = FakeImageStorage()
    private val clock = Clock.fixed(Instant.parse("2026-10-06T08:00:00Z"), ZoneOffset.UTC)
    private val repository = RoomDocumentRepository(dao, storage, clock)

    @Test
    fun importCopiesImageAndCreatesNotStartedDocument() = runTest {
        val id = repository.importImage("content://picker/image/1", "  Receipt  ")

        val document = repository.observeDocument(id).first()
        assertEquals("content://picker/image/1", storage.copiedSource)
        assertEquals("Receipt", document?.title)
        assertEquals("", document?.text)
        assertEquals("/owned/unique-image.jpg", document?.imagePath)
        assertEquals(OcrStatus.PENDING, document?.ocrStatus)
        assertEquals(clock.instant(), document?.createdAt)
    }

    @Test
    fun repeatedImportsReceiveDistinctDatabaseIds() = runTest {
        val first = repository.importImage("content://picker/image/1", "Image")
        val second = repository.importImage("content://picker/image/1", "Image")

        assertEquals(DocumentId(1), first)
        assertEquals(DocumentId(2), second)
    }

    @Test
    fun capturedImageIsCopiedAndCreatesNotStartedDocument() = runTest {
        val id = repository.saveCapturedImage("/cache/camera_captures/capture.jpg", "Scan")

        val document = repository.observeDocument(id).first()
        assertEquals("/cache/camera_captures/capture.jpg", storage.capturedSource)
        assertEquals("/owned/unique-capture.jpg", document?.imagePath)
        assertEquals(OcrStatus.PENDING, document?.ocrStatus)
    }

    @Test
    fun titleUpdateIsTrimmedAndObserved() = runTest {
        val id = repository.importImage("content://picker/image/1", "Image")

        repository.updateTitle(id, "  Renamed  ")

        assertEquals("Renamed", repository.observeDocument(id).first()?.title)
    }

    @Test
    fun ocrResultAndStatusArePersisted() = runTest {
        val id = repository.importImage("content://picker/image/1", "Image")

        repository.updateOcr(id, OcrStatus.COMPLETED, "Recognized text")

        val document = repository.observeDocument(id).first()
        assertEquals(OcrStatus.COMPLETED, document?.ocrStatus)
        assertEquals("Recognized text", document?.text)
    }

    @Test
    fun failedInsertDeletesCopiedFile() = runTest {
        dao.failInsert = true

        val result = runCatching {
            repository.importImage("content://picker/image/1", "Image")
        }

        assertTrue(result.isFailure)
        assertEquals(listOf("/owned/unique-image.jpg"), storage.deletedPaths)
    }

    @Test
    fun searchEscapesSqlWildcardCharacters() = runTest {
        repository.observeDocuments("50%_off\\today", DocumentSortOrder.NEWEST).first()

        assertEquals("%50\\%\\_off\\\\today%", dao.lastSearchPattern)
    }

    @Test
    fun deleteRemovesDatabaseRecordThenOwnedImage() = runTest {
        val id = repository.importImage("content://picker/image/1", "Image")

        repository.deleteDocument(id)

        assertEquals(null, repository.observeDocument(id).first())
        assertEquals(listOf("/owned/unique-image.jpg"), storage.deletedPaths)
    }
}

private class FakeImageStorage : ImageStorage {
    var copiedSource: String? = null
    var capturedSource: String? = null
    val deletedPaths = mutableListOf<String>()

    override suspend fun copyFrom(sourceUri: String): String {
        copiedSource = sourceUri
        return "/owned/unique-image.jpg"
    }

    override suspend fun copyCapturedFile(sourcePath: String): String {
        capturedSource = sourcePath
        return "/owned/unique-capture.jpg"
    }

    override suspend fun delete(path: String) {
        deletedPaths += path
    }
}

private class FakeDocumentDao : DocumentDao {
    private val documents = MutableStateFlow<List<DocumentEntity>>(emptyList())
    var failInsert = false
    var lastSearchPattern: String? = null

    override fun observeAll(): Flow<List<DocumentEntity>> = documents

    override fun observeNewest(query: String): Flow<List<DocumentEntity>> = documents.also {
        lastSearchPattern = query
    }

    override fun observeOldest(query: String): Flow<List<DocumentEntity>> = documents.also {
        lastSearchPattern = query
    }

    override fun observeByTitle(query: String): Flow<List<DocumentEntity>> = documents.also {
        lastSearchPattern = query
    }

    override fun observeById(id: Long): Flow<DocumentEntity?> =
        documents.map { items -> items.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): DocumentEntity? =
        documents.value.firstOrNull { it.id == id }

    override suspend fun insert(document: DocumentEntity): Long {
        if (failInsert) error("insert failed")
        val id = (documents.value.maxOfOrNull { it.id } ?: 0L) + 1L
        documents.value += document.copy(id = id)
        return id
    }

    override suspend fun updateTitle(id: Long, title: String, updatedAt: Long): Int {
        if (documents.value.none { it.id == id }) return 0
        documents.value = documents.value.map {
            if (it.id == id) it.copy(title = title, updatedAtEpochMillis = updatedAt) else it
        }
        return 1
    }

    override suspend fun updateOcr(
        id: Long,
        text: String,
        ocrStatus: String,
        updatedAt: Long,
    ): Int {
        if (documents.value.none { it.id == id }) return 0
        documents.value = documents.value.map {
            if (it.id == id) {
                it.copy(
                    text = text,
                    ocrStatus = ocrStatus,
                    updatedAtEpochMillis = updatedAt,
                )
            } else {
                it
            }
        }
        return 1
    }

    override suspend fun deleteById(id: Long): Int {
        if (documents.value.none { it.id == id }) return 0
        documents.value = documents.value.filterNot { it.id == id }
        return 1
    }
}
