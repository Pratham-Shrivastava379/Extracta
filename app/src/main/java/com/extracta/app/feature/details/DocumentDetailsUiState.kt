package com.extracta.app.feature.details

import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DetectedEntity

sealed interface DocumentDetailsUiState {
    data object Loading : DocumentDetailsUiState
    data object NotFound : DocumentDetailsUiState
    data class Content(
        val document: Document,
        val entities: List<DetectedEntity> = emptyList(),
    ) : DocumentDetailsUiState
    data class Error(val message: String) : DocumentDetailsUiState
}
