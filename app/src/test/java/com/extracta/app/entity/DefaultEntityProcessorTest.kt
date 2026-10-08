package com.extracta.app.entity

import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.EntityType
import com.extracta.app.domain.repository.DetectedEntityRepository
import com.extracta.app.testutil.FakeDocumentRepository
import com.extracta.app.testutil.documentFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultEntityProcessorTest {
    @Test
    fun completedDocumentIsExtractedAndRepeatedProcessingReplacesResults() = runTest {
        val document = documentFixture(text = "Email help@example.test")
        val entityRepository = RecordingEntityRepository()
        val processor = DefaultEntityProcessor(
            FakeDocumentRepository(listOf(document)),
            entityRepository,
            DefaultEntityExtractor(),
            this,
        )

        processor.enqueue(document.id)
        advanceUntilIdle()
        processor.enqueue(document.id)
        advanceUntilIdle()

        assertEquals(2, entityRepository.replaceCalls)
        assertEquals(EntityType.EMAIL, entityRepository.entities.value.single().type)
    }

    @Test
    fun recoveryRunsOnceAndExtractionFailurePreservesExistingEntities() = runTest {
        val document = documentFixture(text = "text")
        val old = DetectedEntity(EntityType.EMAIL, "a@b.co", "a@b.co", 0, 6)
        val entityRepository = RecordingEntityRepository(listOf(old))
        val processor = DefaultEntityProcessor(
            FakeDocumentRepository(listOf(document)),
            entityRepository,
            EntityExtractor { error("parser failure") },
            this,
        )

        processor.resumeCompleted()
        processor.resumeCompleted()
        advanceUntilIdle()

        assertEquals(0, entityRepository.replaceCalls)
        assertEquals(listOf(old), entityRepository.entities.value)
    }
}

private class RecordingEntityRepository(
    initial: List<DetectedEntity> = emptyList(),
) : DetectedEntityRepository {
    val entities = MutableStateFlow(initial)
    var replaceCalls = 0

    override fun observeForDocument(documentId: DocumentId): Flow<List<DetectedEntity>> = entities

    override suspend fun replaceForDocument(
        documentId: DocumentId,
        entities: List<DetectedEntity>,
    ) {
        replaceCalls += 1
        this.entities.value = entities
    }
}
