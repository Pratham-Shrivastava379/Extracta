package com.extracta.app.feature.capture

sealed interface CameraUiState {
    data object LoadingPermission : CameraUiState
    data object RequestingPermission : CameraUiState
    data object PermissionGranted : CameraUiState
    data object StartingCamera : CameraUiState
    data object Preview : CameraUiState
    data object Capturing : CameraUiState
    data object PermissionDenied : CameraUiState
    data object PermissionPermanentlyDenied : CameraUiState
    data object CameraUnavailable : CameraUiState
    data class Captured(
        val imagePath: String,
        val message: String? = null,
    ) : CameraUiState
    data class Saving(val imagePath: String) : CameraUiState
    data class Error(val message: String) : CameraUiState
}
