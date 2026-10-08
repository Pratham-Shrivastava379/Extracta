package com.extracta.app.feature.home

import androidx.lifecycle.SavedStateHandle
import com.extracta.app.domain.model.DocumentSortOrder
import com.extracta.app.testutil.FakeDocumentRepository
import com.extracta.app.testutil.MainDispatcherRule
import com.extracta.app.testutil.FakeOcrProcessor
import com.extracta.app.testutil.documentFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun emptyRepositoryProducesEmptyState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(HomeUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun repositoryUpdatesProduceContentState() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeDocumentRepository()
        val viewModel = viewModel(repository)

        repository.emit(listOf(documentFixture()))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Content)
        assertEquals("Test document", (state as HomeUiState.Content).documents.single().title)
    }

    @Test
    fun importedImageIsQueuedForOcr() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeDocumentRepository()
        val processor = FakeOcrProcessor()
        val viewModel = viewModel(repository, processor)

        viewModel.importImage("content://picker/image/1")
        advanceUntilIdle()

        assertEquals(listOf(com.extracta.app.domain.model.DocumentId(1)), processor.enqueuedIds)
        assertEquals("Image imported; extracting text", viewModel.message.value)
    }

    @Test
    fun searchIsDebouncedAndShowsNoResults() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeDocumentRepository(listOf(documentFixture()))
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.updateQuery("missing phrase")
        advanceTimeBy(299)
        assertTrue(viewModel.uiState.value is HomeUiState.Content)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(HomeUiState.NoResults, viewModel.uiState.value)
    }

    @Test
    fun searchAndSortSurviveViewModelRecreation() = runTest(mainDispatcherRule.dispatcher) {
        val savedState = SavedStateHandle()
        val repository = FakeDocumentRepository(listOf(documentFixture()))
        val first = viewModel(repository, savedStateHandle = savedState)
        first.updateQuery("Test")
        first.updateSortOrder(DocumentSortOrder.TITLE)

        val recreated = viewModel(repository, savedStateHandle = savedState)
        advanceUntilIdle()

        assertEquals("Test", recreated.query.value)
        assertEquals(DocumentSortOrder.TITLE, recreated.sortOrder.value)
        assertTrue(recreated.uiState.value is HomeUiState.Content)
    }

    private fun viewModel(
        repository: FakeDocumentRepository = FakeDocumentRepository(),
        processor: FakeOcrProcessor = FakeOcrProcessor(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = HomeViewModel(savedStateHandle, repository, processor)
}
