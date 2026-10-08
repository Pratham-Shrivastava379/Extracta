package com.extracta.app.feature.details

import androidx.lifecycle.SavedStateHandle
import com.extracta.app.navigation.DOCUMENT_ID_ARGUMENT
import com.extracta.app.testutil.FakeDocumentRepository
import com.extracta.app.testutil.MainDispatcherRule
import com.extracta.app.testutil.FakeOcrProcessor
import com.extracta.app.testutil.FakeDetectedEntityRepository
import com.extracta.app.testutil.documentFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.EntityType

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun matchingIdProducesDocumentContent() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = DocumentDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(DOCUMENT_ID_ARGUMENT to 7L)),
            repository = FakeDocumentRepository(listOf(documentFixture(id = 7))),
            entityRepository = FakeDetectedEntityRepository(),
            ocrProcessor = FakeOcrProcessor(),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DocumentDetailsUiState.Content)
        assertEquals(7L, (state as DocumentDetailsUiState.Content).document.id.value)
    }

    @Test
    fun unknownIdProducesNotFoundState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = DocumentDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(DOCUMENT_ID_ARGUMENT to 8L)),
            repository = FakeDocumentRepository(),
            entityRepository = FakeDetectedEntityRepository(),
            ocrProcessor = FakeOcrProcessor(),
        )

        advanceUntilIdle()

        assertEquals(DocumentDetailsUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun missingIdProducesErrorState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = DocumentDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = FakeDocumentRepository(),
            entityRepository = FakeDetectedEntityRepository(),
            ocrProcessor = FakeOcrProcessor(),
        )

        assertTrue(viewModel.uiState.value is DocumentDetailsUiState.Error)
    }

    @Test
    fun updatingTitleIsReflectedByRepositoryFlow() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeDocumentRepository(listOf(documentFixture(id = 7)))
        val viewModel = DocumentDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(DOCUMENT_ID_ARGUMENT to 7L)),
            repository = repository,
            entityRepository = FakeDetectedEntityRepository(),
            ocrProcessor = FakeOcrProcessor(),
        )

        viewModel.updateTitle("Phase 3 verified")
        advanceUntilIdle()

        val state = viewModel.uiState.value as DocumentDetailsUiState.Content
        assertEquals("Phase 3 verified", state.document.title)
    }

    @Test
    fun retryQueuesCurrentDocumentForOcr() {
        val processor = FakeOcrProcessor()
        val viewModel = DocumentDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(DOCUMENT_ID_ARGUMENT to 7L)),
            repository = FakeDocumentRepository(listOf(documentFixture(id = 7))),
            entityRepository = FakeDetectedEntityRepository(),
            ocrProcessor = processor,
        )

        viewModel.retryOcr()

        assertEquals(listOf(com.extracta.app.domain.model.DocumentId(7)), processor.enqueuedIds)
    }

    @Test
    fun detectedEntitiesAreIncludedInContent() = runTest(mainDispatcherRule.dispatcher) {
        val entity = DetectedEntity(EntityType.EMAIL, "a@b.co", "a@b.co", 0, 6)
        val viewModel = DocumentDetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(DOCUMENT_ID_ARGUMENT to 7L)),
            repository = FakeDocumentRepository(listOf(documentFixture(id = 7))),
            entityRepository = FakeDetectedEntityRepository(listOf(entity)),
            ocrProcessor = FakeOcrProcessor(),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as DocumentDetailsUiState.Content
        assertEquals(listOf(entity), state.entities)
    }
}
