package com.extracta.app.feature.details

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.EntityType
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.entity.action.AndroidEntityActionLauncher
import com.extracta.app.entity.action.EntityAction
import com.extracta.app.entity.action.EntityActionLaunchResult
import com.extracta.app.entity.action.EntityActionResolver
import com.extracta.app.ui.formatDocumentDate
import com.extracta.app.ui.documentImageRequest
import com.extracta.app.ui.DOCUMENT_PREVIEW_MAX_SIZE_PX
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.launch

const val RETRY_OCR_TEST_TAG = "retry_ocr"
const val COPY_TEXT_TEST_TAG = "copy_extracted_text"
const val DETECTED_ENTITIES_TEST_TAG = "detected_entities"
const val ENTITY_CARD_TEST_TAG_PREFIX = "entity_card_"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsScreen(
    uiState: DocumentDetailsUiState,
    onNavigateBack: () -> Unit,
    onUpdateTitle: (String) -> Unit = {},
    onRetryOcr: () -> Unit = {},
    onDeleteDocument: () -> Unit = {},
    onEntityAction: ((EntityAction) -> EntityActionLaunchResult)? = null,
) {
    val title = (uiState as? DocumentDetailsUiState.Content)?.document?.title ?: "Document"
    var editingTitle by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val actionResolver = remember { EntityActionResolver() }
    val actionLauncher = remember(context) { AndroidEntityActionLauncher(context) }
    var selectedEntity by remember { mutableStateOf<DetectedEntity?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var shareMenuExpanded by remember { mutableStateOf(false) }
    val shareLauncher = remember(context) { DocumentShareLauncher(context) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState is DocumentDetailsUiState.Content) {
                        IconButton(onClick = { editingTitle = true }) {
                            Icon(Icons.Outlined.Edit, "Edit title")
                        }
                        Box {
                            IconButton(onClick = { shareMenuExpanded = true }) {
                                Icon(Icons.Outlined.Share, "Share document")
                            }
                            DropdownMenu(
                                expanded = shareMenuExpanded,
                                onDismissRequest = { shareMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share extracted text") },
                                    enabled = uiState.document.text.isNotBlank(),
                                    onClick = {
                                        shareMenuExpanded = false
                                        val result = shareLauncher.shareText(uiState.document)
                                        result.failureMessage?.let { message ->
                                            scope.launch { snackbarHostState.showSnackbar(message) }
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Share original image") },
                                    enabled = uiState.document.imagePath != null,
                                    onClick = {
                                        shareMenuExpanded = false
                                        val result = shareLauncher.shareImage(uiState.document)
                                        result.failureMessage?.let { message ->
                                            scope.launch { snackbarHostState.showSnackbar(message) }
                                        }
                                    },
                                )
                            }
                        }
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Outlined.Delete, "Delete document")
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        when (uiState) {
            DocumentDetailsUiState.Loading -> LoadingDetails(contentPadding)
            DocumentDetailsUiState.NotFound -> MessageDetails(
                title = "Document not found",
                message = "It may have been removed from this temporary collection.",
                contentPadding = contentPadding,
            )
            is DocumentDetailsUiState.Error -> MessageDetails(
                title = "Document unavailable",
                message = uiState.message,
                contentPadding = contentPadding,
            )
            is DocumentDetailsUiState.Content -> DocumentDetails(
                document = uiState.document,
                entities = uiState.entities,
                contentPadding = contentPadding,
                onRetryOcr = onRetryOcr,
                onCopyText = { text ->
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("Extracted text", text))
                    scope.launch { snackbarHostState.showSnackbar("Text copied") }
                },
                onEntityClick = { selectedEntity = it },
            )
        }
    }
    selectedEntity?.let { entity ->
        EntityActionsBottomSheet(
            entity = entity,
            actions = actionResolver.resolve(entity),
            onDismiss = { selectedEntity = null },
            onAction = { action ->
                val result = onEntityAction?.invoke(action) ?: actionLauncher.launch(action)
                selectedEntity = null
                val message = when (result) {
                    EntityActionLaunchResult.Launched -> null
                    EntityActionLaunchResult.Copied -> "Detail copied"
                    EntityActionLaunchResult.NoHandler -> "No app is available for this action"
                }
                if (message != null) scope.launch { snackbarHostState.showSnackbar(message) }
            },
        )
    }
    if (editingTitle && uiState is DocumentDetailsUiState.Content) {
        EditTitleDialog(
            initialTitle = uiState.document.title,
            onDismiss = { editingTitle = false },
            onSave = {
                onUpdateTitle(it)
                editingTitle = false
            },
        )
    }
    if (confirmingDelete && uiState is DocumentDetailsUiState.Content) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete document?") },
            text = { Text("This removes the document, its extracted data, and its saved image from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmingDelete = false
                        onDeleteDocument()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EditTitleDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit title") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onSave(title.trim()) }, enabled = title.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LoadingDetails(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading document" },
        )
    }
}

@Composable
private fun MessageDetails(
    title: String,
    message: String,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun DocumentDetails(
    document: Document,
    entities: List<DetectedEntity>,
    contentPadding: PaddingValues,
    onRetryOcr: () -> Unit,
    onCopyText: (String) -> Unit,
    onEntityClick: (DetectedEntity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.large,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val path = document.imagePath
                if (path == null) {
                    MissingDocumentImage()
                } else {
                    SubcomposeAsyncImage(
                        model = documentImageRequest(
                            context = LocalContext.current,
                            path = path,
                            maxSizePx = DOCUMENT_PREVIEW_MAX_SIZE_PX,
                        ),
                        contentDescription = "${document.title} image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        success = { SubcomposeAsyncImageContent() },
                        loading = { MissingDocumentImage() },
                        error = { MissingDocumentImage() },
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Updated ${formatDocumentDate(document.updatedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = document.ocrStatus.label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        item {
            OcrTextSection(document, onRetryOcr, onCopyText)
        }
        if (document.ocrStatus == OcrStatus.COMPLETED) {
            item { DetectedEntitiesSection(entities, onEntityClick) }
        }
    }
}

@Composable
private fun DetectedEntitiesSection(
    entities: List<DetectedEntity>,
    onEntityClick: (DetectedEntity) -> Unit,
) {
    Column(
        modifier = Modifier.testTag(DETECTED_ENTITIES_TEST_TAG),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Detected details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        if (entities.isEmpty()) {
            Text(
                text = "No supported details were found.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            entities.forEach { entity ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEntityClick(entity) }
                        .semantics(mergeDescendants = true) {
                            stateDescription = "Opens available actions"
                        }
                        .testTag("$ENTITY_CARD_TEST_TAG_PREFIX${entity.start}"),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = entity.type.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        SelectionContainer {
                            Text(entity.originalText, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrTextSection(
    document: Document,
    onRetryOcr: () -> Unit,
    onCopyText: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Extracted text",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        when (document.ocrStatus) {
            OcrStatus.PENDING,
            OcrStatus.PROCESSING -> {
                CircularProgressIndicator(
                    modifier = Modifier.semantics {
                        contentDescription = "Extracting document text"
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                Text(
                    text = "Extracting text on this device…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OcrStatus.FAILED -> {
                Text("Text extraction failed. You can try again.")
                Button(
                    onClick = onRetryOcr,
                    modifier = Modifier.testTag(RETRY_OCR_TEST_TAG),
                ) { Text("Retry OCR") }
            }
            OcrStatus.NOT_STARTED -> {
                Text("Text has not been extracted from this image yet.")
                Button(
                    onClick = onRetryOcr,
                    modifier = Modifier.testTag(RETRY_OCR_TEST_TAG),
                ) { Text("Extract text") }
            }
            OcrStatus.COMPLETED -> {
                if (document.text.isBlank()) {
                    Text(
                        text = "No text was found in this image.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SelectionContainer {
                        Text(text = document.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    Button(
                        onClick = { onCopyText(document.text) },
                        modifier = Modifier.testTag(COPY_TEXT_TEST_TAG),
                    ) { Text("Copy all") }
                }
            }
        }
    }
}

@Composable
private fun MissingDocumentImage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Document image unavailable",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private val OcrStatus.label: String
    get() = when (this) {
        OcrStatus.NOT_STARTED -> "OCR not started"
        OcrStatus.PENDING -> "OCR pending"
        OcrStatus.PROCESSING -> "OCR processing"
        OcrStatus.COMPLETED -> "OCR complete"
        OcrStatus.FAILED -> "OCR failed"
    }

private val DocumentShareResult.failureMessage: String?
    get() = when (this) {
        DocumentShareResult.LAUNCHED -> null
        DocumentShareResult.NO_HANDLER -> "No app is available for sharing"
        DocumentShareResult.IMAGE_UNAVAILABLE -> "The content is unavailable for sharing"
    }
