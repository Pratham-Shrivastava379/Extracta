package com.extracta.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.extracta.app.domain.repository.DocumentRepository
import com.extracta.app.domain.model.DocumentSortOrder
import com.extracta.app.ocr.OcrProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

private const val SEARCH_DEBOUNCE_MILLIS = 300L

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: DocumentRepository,
    private val ocrProcessor: OcrProcessor,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _query = MutableStateFlow(savedStateHandle[QUERY_KEY] ?: "")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _sortOrder = MutableStateFlow(
        savedStateHandle.get<String>(SORT_ORDER_KEY)
            ?.let { saved -> DocumentSortOrder.entries.firstOrNull { it.name == saved } }
            ?: DocumentSortOrder.NEWEST,
    )
    val sortOrder: StateFlow<DocumentSortOrder> = _sortOrder.asStateFlow()

    init {
        ocrProcessor.resumePending()
        viewModelScope.launch {
            _query
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .combine(_sortOrder) { query, sortOrder -> query to sortOrder }
                .flatMapLatest { (query, sortOrder) ->
                    repository.observeDocuments(query, sortOrder)
                }
                .catch { error ->
                    _uiState.value = HomeUiState.Error(
                        error.message ?: "Documents could not be loaded.",
                    )
                }
                .collect { documents ->
                    _uiState.value = if (documents.isEmpty()) {
                        if (_query.value.isBlank()) HomeUiState.Empty else HomeUiState.NoResults
                    } else {
                        HomeUiState.Content(documents)
                    }
                }
        }
    }

    fun updateQuery(query: String) {
        _query.value = query
        savedStateHandle[QUERY_KEY] = query
    }

    fun updateSortOrder(sortOrder: DocumentSortOrder) {
        _sortOrder.value = sortOrder
        savedStateHandle[SORT_ORDER_KEY] = sortOrder.name
    }

    fun importImage(sourceUri: String) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            runCatching { repository.importImage(sourceUri, "Imported document") }
                .onSuccess { id ->
                    ocrProcessor.enqueue(id)
                    _message.value = "Image imported; extracting text"
                }
                .onFailure { _message.value = it.message ?: "Image could not be imported." }
            _isImporting.value = false
        }
    }

    fun importCancelled() {
        _message.value = "Import cancelled"
    }

    fun messageShown() {
        _message.value = null
    }

    private companion object {
        const val QUERY_KEY = "home_search_query"
        const val SORT_ORDER_KEY = "home_sort_order"
    }
}
