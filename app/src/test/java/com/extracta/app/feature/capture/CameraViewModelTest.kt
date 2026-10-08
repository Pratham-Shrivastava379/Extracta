package com.extracta.app.feature.capture

import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.repository.DocumentRepository
import com.extracta.app.testutil.MainDispatcherRule
import com.extracta.app.testutil.FakeOcrProcessor
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun permissionResultsHaveExplicitStates() {
        val viewModel = CameraViewModel(
            FakeCaptureRepository(),
            FakeCaptureFileStore(),
            FakeOcrProcessor(),
        )

        viewModel.requestPermission()
        assertEquals(CameraUiState.RequestingPermission, viewModel.uiState.value)

        viewModel.onPermissionResult(granted = false)
        assertEquals(CameraUiState.PermissionDenied, viewModel.uiState.value)

        viewModel.onPermissionResult(granted = false, permanentlyDenied = true)
        assertEquals(CameraUiState.PermissionPermanentlyDenied, viewModel.uiState.value)

        viewModel.onPermissionResult(granted = true)
        assertEquals(CameraUiState.PermissionGranted, viewModel.uiState.value)
    }

    @Test
    fun enteringCaptureCleansFilesLeftByAnInterruptedSession() {
        val files = FakeCaptureFileStore()

        CameraViewModel(FakeCaptureRepository(), files, FakeOcrProcessor())

        assertEquals(1, files.cleanupCount)
    }

    @Test
    fun repeatedCaptureTapsCreateOnlyOneTemporaryFile() {
        val files = FakeCaptureFileStore()
        val viewModel = previewViewModel(files = files)

        val first = viewModel.beginCapture()
        val second = viewModel.beginCapture()

        assertEquals("C:\\cache\\camera_captures\\capture-1.jpg", first?.path)
        assertNull(second)
        assertEquals(1, files.createdCount)
        assertEquals(CameraUiState.Capturing, viewModel.uiState.value)
    }

    @Test
    fun retakeDeletesTemporaryCaptureAndRestartsPreview() {
        val files = FakeCaptureFileStore()
        val viewModel = previewViewModel(files = files)
        val path = requireNotNull(viewModel.beginCapture()).absolutePath
        viewModel.onImageCaptured(path)

        viewModel.retake()

        assertEquals(listOf(path), files.deletedPaths)
        assertEquals(CameraUiState.PermissionGranted, viewModel.uiState.value)
    }

    @Test
    fun cancelDeletesTemporaryCapture() {
        val files = FakeCaptureFileStore()
        val viewModel = previewViewModel(files = files)
        val path = requireNotNull(viewModel.beginCapture()).absolutePath
        viewModel.onImageCaptured(path)

        viewModel.cancel()

        assertEquals(listOf(path), files.deletedPaths)
    }

    @Test
    fun permissionRefreshDoesNotReplaceCapturedReview() {
        val viewModel = previewViewModel()
        val path = requireNotNull(viewModel.beginCapture()).absolutePath
        viewModel.onImageCaptured(path)

        viewModel.onPermissionResult(granted = true)

        assertEquals(CameraUiState.Captured(path), viewModel.uiState.value)
    }

    @Test
    fun saveIsSingleShotAndPublishesDocumentId() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCaptureRepository()
        val files = FakeCaptureFileStore()
        val processor = FakeOcrProcessor()
        val viewModel = previewViewModel(repository, files, processor)
        val path = requireNotNull(viewModel.beginCapture()).absolutePath
        viewModel.onImageCaptured(path)

        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, repository.saveCount)
        assertEquals(path, repository.savedPath)
        assertEquals(DocumentId(7), viewModel.savedDocumentId.value)
        assertTrue(path in files.deletedPaths)
        assertEquals(listOf(DocumentId(7)), processor.enqueuedIds)
    }

    @Test
    fun failedSaveReturnsToReviewForRetry() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCaptureRepository(failSave = true)
        val viewModel = previewViewModel(repository)
        val path = requireNotNull(viewModel.beginCapture()).absolutePath
        viewModel.onImageCaptured(path)

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CameraUiState.Captured
        assertEquals(path, state.imagePath)
        assertEquals("save failed", state.message)
    }

    private fun previewViewModel(
        repository: FakeCaptureRepository = FakeCaptureRepository(),
        files: FakeCaptureFileStore = FakeCaptureFileStore(),
        processor: FakeOcrProcessor = FakeOcrProcessor(),
    ): CameraViewModel = CameraViewModel(repository, files, processor).also {
        it.onPermissionResult(granted = true)
        it.startPreview()
        it.onCameraReady()
    }
}

private class FakeCaptureFileStore : CaptureFileStore {
    var createdCount = 0
    var cleanupCount = 0
    val deletedPaths = mutableListOf<String>()

    override fun cleanup() {
        cleanupCount += 1
    }

    override fun create(): File {
        createdCount += 1
        return File("C:\\cache\\camera_captures\\capture-$createdCount.jpg")
    }

    override fun delete(path: String) {
        deletedPaths += path
    }
}

private class FakeCaptureRepository(
    private val failSave: Boolean = false,
) : DocumentRepository {
    var saveCount = 0
    var savedPath: String? = null

    override fun observeDocuments(): Flow<List<Document>> = flowOf(emptyList())
    override fun observeDocument(id: DocumentId): Flow<Document?> = flowOf(null)
    override suspend fun importImage(sourceUri: String, title: String): DocumentId = DocumentId(1)
    override suspend fun updateTitle(id: DocumentId, title: String) = Unit
    override suspend fun updateOcr(
        id: DocumentId,
        status: com.extracta.app.domain.model.OcrStatus,
        text: String,
    ) = Unit

    override suspend fun saveCapturedImage(sourcePath: String, title: String): DocumentId {
        saveCount += 1
        savedPath = sourcePath
        if (failSave) error("save failed")
        return DocumentId(7)
    }
}
