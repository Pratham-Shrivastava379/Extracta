package com.extracta.app.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.repository.DocumentRepository
import com.extracta.app.domain.repository.DetectedEntityRepository
import com.extracta.app.navigation.DOCUMENT_ID_ARGUMENT
import com.extracta.app.ocr.OcrProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class DocumentDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DocumentRepository,
    private val entityRepository: DetectedEntityRepository,
    private val ocrProcessor: OcrProcessor,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DocumentDetailsUiState>(
        DocumentDetailsUiState.Loading,
    )
    val uiState: StateFlow<DocumentDetailsUiState> = _uiState.asStateFlow()
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()
    private val documentId = savedStateHandle.get<Long>(DOCUMENT_ID_ARGUMENT)
        ?.takeIf { it > 0 }
        ?.let(::DocumentId)

    init {
        if (documentId == null) {
            _uiState.value = DocumentDetailsUiState.Error("The document ID is invalid.")
        } else {
            viewModelScope.launch {
                combine(
                    repository.observeDocument(documentId),
                    entityRepository.observeForDocument(documentId),
                ) { document, entities -> document to entities }
                    .catch { error ->
                        _uiState.value = DocumentDetailsUiState.Error(
                            error.message ?: "The document could not be loaded.",
                        )
                    }
                    .collect { (document, entities) ->
                        _uiState.value = document?.let {
                            DocumentDetailsUiState.Content(it, entities)
                        }
                            ?: DocumentDetailsUiState.NotFound
                    }
            }
        }
    }

    fun updateTitle(title: String) {
        val id = documentId ?: return
        viewModelScope.launch {
            runCatching { repository.updateTitle(id, title) }
                .onFailure { error ->
                    _uiState.value = DocumentDetailsUiState.Error(
                        error.message ?: "The title could not be updated.",
                    )
                }
        }
    }

    fun retryOcr() {
        documentId?.let(ocrProcessor::enqueue)
    }

    fun deleteDocument() {
        val id = documentId ?: return
        viewModelScope.launch {
            runCatching { repository.deleteDocument(id) }
                .onSuccess { _deleted.value = true }
                .onFailure { error ->
                    _uiState.value = DocumentDetailsUiState.Error(
                        error.message ?: "The document could not be deleted.",
                    )
                }
        }
    }
}
