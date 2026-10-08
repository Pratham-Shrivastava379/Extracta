package com.extracta.app.feature.capture

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class CameraXController(context: Context) {
    private val appContext = context.applicationContext
    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                runCatching {
                    val cameraProvider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(previewView.display.rotation)
                        .build()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture,
                    )
                    provider = cameraProvider
                    imageCapture = capture
                }.onSuccess { onReady() }.onFailure(onError)
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun capture(
        destination: File,
        onCaptured: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val capture = imageCapture
        if (capture == null) {
            onError(IllegalStateException("The camera is not ready."))
            return
        }
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(destination).build(),
            ContextCompat.getMainExecutor(appContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onCaptured(destination.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    destination.delete()
                    onError(exception)
                }
            },
        )
    }

    fun unbind() {
        provider?.unbindAll()
        provider = null
        imageCapture = null
    }
}
