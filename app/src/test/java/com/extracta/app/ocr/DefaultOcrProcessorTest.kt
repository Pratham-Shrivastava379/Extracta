package com.extracta.app.ocr

import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.domain.repository.DocumentRepository
import java.time.Instant
import com.extracta.app.testutil.FakeEntityProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultOcrProcessorTest {
    @Test
    fun successfulRecognitionPersistsProcessingThenCompletedText() = runTest {
        val repository = RecordingOcrRepository(document())
        val recognizer = FakeTextRecognizer("  Receipt total 441.00  ")
        val entityProcessor = FakeEntityProcessor()
        val processor = DefaultOcrProcessor(repository, recognizer, entityProcessor, this)

        processor.enqueue(DocumentId(1))
        advanceUntilIdle()

        assertEquals(
            listOf(OcrStatus.PROCESSING, OcrStatus.COMPLETED),
            repository.updates.map { it.first },
        )
        assertEquals("Receipt total 441.00", repository.documents.value.single().text)
        assertEquals("/owned/document.jpg", recognizer.paths.single())
        assertEquals(listOf(DocumentId(1)), entityProcessor.enqueuedIds)
    }

    @Test
    fun recognitionFailurePersistsFailedState() = runTest {
        val repository = RecordingOcrRepository(document())
        val processor = DefaultOcrProcessor(
            repository,
            FakeTextRecognizer(error = IllegalStateException("recognizer failed")),
            FakeEntityProcessor(),
            this,
        )

        processor.enqueue(DocumentId(1))
        advanceUntilIdle()

        assertEquals(
            listOf(OcrStatus.PROCESSING, OcrStatus.FAILED),
            repository.updates.map { it.first },
        )
        assertEquals("", repository.documents.value.single().text)
    }

    @Test
    fun repeatedEnqueueWhileActiveRunsRecognizerOnce() = runTest {
        val repository = RecordingOcrRepository(document())
        val recognizer = FakeTextRecognizer("Text")
        val processor = DefaultOcrProcessor(repository, recognizer, FakeEntityProcessor(), this)

        processor.enqueue(DocumentId(1))
        processor.enqueue(DocumentId(1))
        advanceUntilIdle()

        assertEquals(1, recognizer.paths.size)
    }

    @Test
    fun resumePendingProcessesLegacyNotStartedDocumentOnlyOnce() = runTest {
        val repository = RecordingOcrRepository(document(status = OcrStatus.NOT_STARTED))
        val recognizer = FakeTextRecognizer("Recovered")
        val entityProcessor = FakeEntityProcessor()
        val processor = DefaultOcrProcessor(repository, recognizer, entityProcessor, this)

        processor.resumePending()
        processor.resumePending()
        advanceUntilIdle()

        assertEquals(1, recognizer.paths.size)
        assertEquals(OcrStatus.COMPLETED, repository.documents.value.single().ocrStatus)
        assertEquals(1, entityProcessor.resumeCalls)
    }

    @Test
    fun resumePendingRestartsInterruptedProcessingDocument() = runTest {
        val repository = RecordingOcrRepository(document(status = OcrStatus.PROCESSING))
        val recognizer = FakeTextRecognizer("Recovered after restart")
        val processor = DefaultOcrProcessor(
            repository,
            recognizer,
            FakeEntityProcessor(),
            this,
        )

        processor.resumePending()
        advanceUntilIdle()

        assertEquals(1, recognizer.paths.size)
        assertEquals(OcrStatus.COMPLETED, repository.documents.value.single().ocrStatus)
        assertEquals("Recovered after restart", repository.documents.value.single().text)
    }

    private fun document(status: OcrStatus = OcrStatus.PENDING) = Document(
        id = DocumentId(1),
        title = "Document",
        text = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        imagePath = "/owned/document.jpg",
        ocrStatus = status,
    )
}

private class FakeTextRecognizer(
    private val result: String = "",
    private val error: Throwable? = null,
) : TextRecognizer {
    val paths = mutableListOf<String>()

    override suspend fun recognize(imagePath: String): String {
        paths += imagePath
        error?.let { throw it }
        return result
    }
}

private class RecordingOcrRepository(initial: Document) : DocumentRepository {
    val documents = MutableStateFlow(listOf(initial))
    val updates = mutableListOf<Pair<OcrStatus, String>>()

    override fun observeDocuments(): Flow<List<Document>> = documents
    override fun observeDocument(id: DocumentId): Flow<Document?> =
        documents.map { items -> items.firstOrNull { it.id == id } }

    override suspend fun importImage(sourceUri: String, title: String): DocumentId = DocumentId(1)
    override suspend fun saveCapturedImage(sourcePath: String, title: String): DocumentId = DocumentId(1)
    override suspend fun updateTitle(id: DocumentId, title: String) = Unit

    override suspend fun updateOcr(id: DocumentId, status: OcrStatus, text: String) {
        updates += status to text
        documents.value = documents.value.map { document ->
            if (document.id == id) document.copy(ocrStatus = status, text = text) else document
        }
    }
}
