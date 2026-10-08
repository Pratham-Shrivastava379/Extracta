package com.extracta.app.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.DocumentSortOrder

const val SCAN_DOCUMENT_TEST_TAG = "scan_document"
const val IMPORT_IMAGE_TEST_TAG = "import_image"
const val SEARCH_DOCUMENTS_TEST_TAG = "search_documents"
const val SORT_DOCUMENTS_TEST_TAG = "sort_documents"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onScanClick: () -> Unit,
    onDocumentClick: (DocumentId) -> Unit,
    onImportImage: (String) -> Unit = {},
    onImportCancelled: () -> Unit = {},
    isImporting: Boolean = false,
    message: String? = null,
    onMessageShown: () -> Unit = {},
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    sortOrder: DocumentSortOrder = DocumentSortOrder.NEWEST,
    onSortOrderChange: (DocumentSortOrder) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) onImportCancelled() else onImportImage(uri.toString())
    }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Extracta", modifier = Modifier.semantics { heading() }) })
                SearchAndSortBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    sortOrder = sortOrder,
                    onSortOrderChange = onSortOrderChange,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    },
                    icon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                    text = { Text(if (isImporting) "Importing…" else "Import image") },
                    modifier = Modifier.testTag(IMPORT_IMAGE_TEST_TAG),
                )
                ExtendedFloatingActionButton(
                    onClick = onScanClick,
                    icon = { Icon(Icons.Outlined.DocumentScanner, contentDescription = null) },
                    text = { Text("Scan document") },
                    modifier = Modifier.testTag(SCAN_DOCUMENT_TEST_TAG),
                )
            }
        },
    ) { innerPadding ->
        when (uiState) {
            HomeUiState.Loading -> LoadingDocuments(innerPadding)
            HomeUiState.Empty -> EmptyDocuments(innerPadding)
            HomeUiState.NoResults -> NoSearchResults(innerPadding)
            is HomeUiState.Content -> DocumentList(
                documents = uiState.documents,
                contentPadding = innerPadding,
                onDocumentClick = onDocumentClick,
            )
            is HomeUiState.Error -> ErrorDocuments(
                message = uiState.message,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun SearchAndSortBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortOrder: DocumentSortOrder,
    onSortOrderChange: (DocumentSortOrder) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).testTag(SEARCH_DOCUMENTS_TEST_TAG),
            singleLine = true,
            label = { Text("Search documents") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                }
            } else {
                null
            },
        )
        Box {
            IconButton(
                onClick = { sortMenuExpanded = true },
                modifier = Modifier.testTag(SORT_DOCUMENTS_TEST_TAG),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort: ${sortOrder.label}")
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
            ) {
                DocumentSortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.label) },
                        onClick = {
                            onSortOrderChange(order)
                            sortMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingDocuments(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading documents" },
        )
    }
}

@Composable
private fun DocumentList(
    documents: List<Document>,
    contentPadding: PaddingValues,
    onDocumentClick: (DocumentId) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = documents, key = { it.id.value }) { document ->
            DocumentCard(
                document = document,
                onClick = { onDocumentClick(document.id) },
            )
        }
    }
}

@Composable
private fun EmptyDocuments(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.DocumentScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "No documents yet",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        Text(
            text = "Scan or import a document to extract searchable text on your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 88.dp),
        )
    }
}

@Composable
private fun NoSearchResults(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = "No matching documents",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        Text(
            text = "Try another title or phrase from the extracted text.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 88.dp),
        )
    }
}

@Composable
private fun ErrorDocuments(
    message: String,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = "Documents unavailable",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 88.dp),
        )
    }
}
