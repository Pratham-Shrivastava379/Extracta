package com.extracta.app.feature.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.camera.view.PreviewView
import coil.compose.AsyncImage
import com.extracta.app.ui.DOCUMENT_PREVIEW_MAX_SIZE_PX
import com.extracta.app.ui.documentImageRequest

const val CAPTURE_BUTTON_TEST_TAG = "capture_button"
const val RETAKE_BUTTON_TEST_TAG = "retake_button"
const val SAVE_BUTTON_TEST_TAG = "save_capture_button"
const val CANCEL_CAPTURE_TEST_TAG = "cancel_capture_button"

@Composable
fun CameraRoute(
    viewModel: CameraViewModel,
    onNavigateBack: () -> Unit,
    onDocumentSaved: (com.extracta.app.domain.model.DocumentId) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedDocumentId by viewModel.savedDocumentId.collectAsStateWithLifecycle()
    val controller = remember { CameraXController(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val permanentlyDenied = !granted && activity?.let {
            !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } == true
        viewModel.onPermissionResult(granted, permanentlyDenied)
    }

    LaunchedEffect(Unit) {
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        when {
            !hasCamera -> viewModel.cameraUnavailable()
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED -> viewModel.onPermissionResult(granted = true)
            else -> {
                viewModel.requestPermission()
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    LaunchedEffect(savedDocumentId) {
        savedDocumentId?.let { id ->
            viewModel.savedDocumentHandled()
            onDocumentSaved(id)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val awaitingPermission = uiState == CameraUiState.LoadingPermission ||
            uiState == CameraUiState.RequestingPermission ||
            uiState == CameraUiState.PermissionDenied ||
            uiState == CameraUiState.PermissionPermanentlyDenied
        if (awaitingPermission && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.onPermissionResult(granted = true)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            controller.unbind()
        }
    }

    CameraScreen(
        uiState = uiState,
        controller = controller,
        onNavigateBack = {
            viewModel.cancel()
            onNavigateBack()
        },
        onRequestPermission = {
            viewModel.requestPermission()
            permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onOpenSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
        onPreviewStarting = viewModel::startPreview,
        onCameraReady = viewModel::onCameraReady,
        onCameraError = viewModel::onCameraError,
        onCapture = {
            viewModel.beginCapture()?.let { destination ->
                controller.capture(
                    destination = destination,
                    onCaptured = viewModel::onImageCaptured,
                    onError = viewModel::onCaptureError,
                )
            }
        },
        onRetake = viewModel::retake,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    uiState: CameraUiState,
    controller: CameraXController? = null,
    onNavigateBack: () -> Unit,
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onPreviewStarting: () -> Unit = {},
    onCameraReady: () -> Unit = {},
    onCameraError: (Throwable) -> Unit = {},
    onCapture: () -> Unit = {},
    onRetake: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState is CameraUiState.Captured || uiState is CameraUiState.Saving) {
                            "Review scan"
                        } else {
                            "Scan document"
                        },
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (uiState) {
            CameraUiState.LoadingPermission,
            CameraUiState.RequestingPermission -> StatusMessage(
                title = "Checking camera access",
                message = "Preparing the camera…",
                loading = true,
                modifier = Modifier.padding(padding),
            )
            CameraUiState.PermissionDenied -> StatusMessage(
                title = "Camera permission needed",
                message = "Allow camera access to scan a document.",
                actionLabel = "Try again",
                onAction = onRequestPermission,
                modifier = Modifier.padding(padding),
            )
            CameraUiState.PermissionPermanentlyDenied -> StatusMessage(
                title = "Camera access is blocked",
                message = "Enable Camera for Extracta in system settings, then return here.",
                actionLabel = "Open settings",
                onAction = onOpenSettings,
                modifier = Modifier.padding(padding),
            )
            CameraUiState.CameraUnavailable -> StatusMessage(
                title = "Camera unavailable",
                message = "This device does not report an available camera.",
                modifier = Modifier.padding(padding),
            )
            is CameraUiState.Error -> StatusMessage(
                title = "Camera error",
                message = uiState.message,
                actionLabel = "Try again",
                onAction = onPreviewStarting,
                modifier = Modifier.padding(padding),
            )
            CameraUiState.PermissionGranted,
            CameraUiState.StartingCamera,
            CameraUiState.Preview,
            CameraUiState.Capturing -> CameraPreviewContent(
                state = uiState,
                controller = controller,
                onPreviewStarting = onPreviewStarting,
                onCameraReady = onCameraReady,
                onCameraError = onCameraError,
                onCapture = onCapture,
                modifier = Modifier.padding(padding),
            )
            is CameraUiState.Captured -> CaptureReview(
                imagePath = uiState.imagePath,
                message = uiState.message,
                saving = false,
                onRetake = onRetake,
                onSave = onSave,
                onCancel = onNavigateBack,
                modifier = Modifier.padding(padding),
            )
            is CameraUiState.Saving -> CaptureReview(
                imagePath = uiState.imagePath,
                saving = true,
                onRetake = {},
                onSave = {},
                onCancel = onNavigateBack,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun CameraPreviewContent(
    state: CameraUiState,
    controller: CameraXController?,
    onPreviewStarting: () -> Unit,
    onCameraReady: () -> Unit,
    onCameraError: (Throwable) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onPreviewStarting() }
    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (controller != null) {
            CameraPreview(
                controller = controller,
                onReady = onCameraReady,
                onError = onCameraError,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Camera preview" },
            )
        }
        if (state == CameraUiState.PermissionGranted || state == CameraUiState.StartingCamera) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        FilledIconButton(
            onClick = onCapture,
            enabled = state == CameraUiState.Preview,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(28.dp)
                .testTag(CAPTURE_BUTTON_TEST_TAG),
        ) {
            if (state == CameraUiState.Capturing) {
                CircularProgressIndicator()
            } else {
                Icon(Icons.Outlined.CameraAlt, contentDescription = "Take photo")
            }
        }
    }
}

@Composable
private fun CameraPreview(
    controller: CameraXController,
    onReady: () -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
    DisposableEffect(lifecycleOwner, previewView) {
        controller.bind(lifecycleOwner, previewView, onReady, onError)
        onDispose(controller::unbind)
    }
}

@Composable
private fun CaptureReview(
    imagePath: String,
    saving: Boolean,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val context = LocalContext.current
    Column(modifier.fillMaxSize()) {
        AsyncImage(
            model = documentImageRequest(context, imagePath, DOCUMENT_PREVIEW_MAX_SIZE_PX),
            contentDescription = "Captured document preview",
            modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
            contentScale = ContentScale.Fit,
        )
        if (message != null) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !saving,
                modifier = Modifier.testTag(CANCEL_CAPTURE_TEST_TAG),
            ) { Text("Cancel") }
            OutlinedButton(
                onClick = onRetake,
                enabled = !saving,
                modifier = Modifier.testTag(RETAKE_BUTTON_TEST_TAG),
            ) { Text("Retake") }
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.testTag(SAVE_BUTTON_TEST_TAG),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = "Saving document" },
                    )
                } else {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (actionLabel != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) {
                Text(actionLabel)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
