package com.extracta.app.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.repository.DocumentRepository
import com.extracta.app.ocr.OcrProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val captureFiles: CaptureFileStore,
    private val ocrProcessor: OcrProcessor,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.LoadingPermission)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _savedDocumentId = MutableStateFlow<DocumentId?>(null)
    val savedDocumentId: StateFlow<DocumentId?> = _savedDocumentId.asStateFlow()

    private var temporaryPath: String? = null

    init {
        captureFiles.cleanup()
    }

    fun requestPermission() {
        if (_uiState.value == CameraUiState.LoadingPermission ||
            _uiState.value == CameraUiState.PermissionDenied
        ) {
            _uiState.value = CameraUiState.RequestingPermission
        }
    }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean = false) {
        val current = _uiState.value
        val awaitingPermission = current == CameraUiState.LoadingPermission ||
            current == CameraUiState.RequestingPermission ||
            current == CameraUiState.PermissionDenied ||
            current == CameraUiState.PermissionPermanentlyDenied
        if (!awaitingPermission) return
        _uiState.value = when {
            granted -> CameraUiState.PermissionGranted
            permanentlyDenied -> CameraUiState.PermissionPermanentlyDenied
            else -> CameraUiState.PermissionDenied
        }
    }

    fun cameraUnavailable() {
        _uiState.value = CameraUiState.CameraUnavailable
    }

    fun startPreview() {
        if (_uiState.value == CameraUiState.PermissionGranted ||
            _uiState.value is CameraUiState.Error
        ) {
            _uiState.value = CameraUiState.StartingCamera
        }
    }

    fun onCameraReady() {
        if (_uiState.value == CameraUiState.StartingCamera ||
            _uiState.value == CameraUiState.PermissionGranted
        ) {
            _uiState.value = CameraUiState.Preview
        }
    }

    fun onCameraError(error: Throwable) {
        discardTemporaryFile()
        _uiState.value = CameraUiState.Error(error.message ?: "The camera could not be started.")
    }

    fun beginCapture(): java.io.File? {
        if (_uiState.value != CameraUiState.Preview) return null
        return runCatching { captureFiles.create() }
            .onSuccess { file ->
                temporaryPath = file.absolutePath
                _uiState.value = CameraUiState.Capturing
            }
            .onFailure(::onCameraError)
            .getOrNull()
    }

    fun onImageCaptured(path: String) {
        if (_uiState.value != CameraUiState.Capturing || path != temporaryPath) {
            captureFiles.delete(path)
            return
        }
        _uiState.value = CameraUiState.Captured(path)
    }

    fun onCaptureError(error: Throwable) {
        discardTemporaryFile()
        _uiState.value = CameraUiState.Error(error.message ?: "The photo could not be captured.")
    }

    fun retake() {
        if (_uiState.value !is CameraUiState.Captured) return
        discardTemporaryFile()
        _uiState.value = CameraUiState.PermissionGranted
    }

    fun save() {
        val captured = _uiState.value as? CameraUiState.Captured ?: return
        _uiState.value = CameraUiState.Saving(captured.imagePath)
        viewModelScope.launch {
            runCatching {
                repository.saveCapturedImage(captured.imagePath, "Scanned document")
            }.onSuccess { id ->
                discardTemporaryFile()
                ocrProcessor.enqueue(id)
                _savedDocumentId.value = id
            }.onFailure { error ->
                _uiState.value = CameraUiState.Captured(
                    imagePath = captured.imagePath,
                    message = error.message ?: "The scan could not be saved.",
                )
            }
        }
    }

    fun savedDocumentHandled() {
        _savedDocumentId.value = null
    }

    fun cancel() {
        discardTemporaryFile()
    }

    private fun discardTemporaryFile() {
        temporaryPath?.let(captureFiles::delete)
        temporaryPath = null
    }

    override fun onCleared() {
        discardTemporaryFile()
    }
}
