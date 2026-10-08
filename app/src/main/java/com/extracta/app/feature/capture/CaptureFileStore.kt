package com.extracta.app.feature.capture

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface CaptureFileStore {
    fun cleanup()

    fun create(): File

    fun delete(path: String)
}

@Singleton
class LocalCaptureFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : CaptureFileStore {
    override fun cleanup() {
        val directory = File(context.cacheDir, CAPTURE_DIRECTORY)
        directory.listFiles()?.filter(File::isFile)?.forEach(File::delete)
    }

    override fun create(): File {
        val directory = File(context.cacheDir, CAPTURE_DIRECTORY)
        check(directory.exists() || directory.mkdirs()) { "Could not create camera cache." }
        return File.createTempFile("capture-", ".jpg", directory)
    }

    override fun delete(path: String) {
        val file = File(path)
        val directory = File(context.cacheDir, CAPTURE_DIRECTORY).canonicalFile
        if (file.canonicalFile.parentFile == directory) file.delete()
    }

    private companion object {
        const val CAPTURE_DIRECTORY = "camera_captures"
    }
}
