package com.extracta.app.feature.home

import com.extracta.app.domain.model.Document

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data object NoResults : HomeUiState
    data class Content(val documents: List<Document>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
