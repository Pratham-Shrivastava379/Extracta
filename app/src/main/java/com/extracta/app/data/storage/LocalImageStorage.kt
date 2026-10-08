package com.extracta.app.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LocalImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageStorage {
    override suspend fun copyFrom(sourceUri: String): String = withContext(Dispatchers.IO) {
        val source = Uri.parse(sourceUri)
        val directory = File(context.filesDir, IMAGE_DIRECTORY)
        check(directory.exists() || directory.mkdirs()) { "Could not create image storage." }

        val extension = context.contentResolver.extensionFor(source)
        val destination = File(directory, "${UUID.randomUUID()}.$extension")
        try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw IOException("The selected image could not be opened.")
            input.use { stream ->
                destination.outputStream().use(stream::copyTo)
            }
            destination.absolutePath
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }

    override suspend fun copyCapturedFile(sourcePath: String): String = withContext(Dispatchers.IO) {
        val source = File(sourcePath).canonicalFile
        val captureDirectory = File(context.cacheDir, CAPTURE_DIRECTORY).canonicalFile
        require(source.parentFile == captureDirectory && source.isFile) {
            "The captured image is not an Extracta temporary file."
        }
        val directory = File(context.filesDir, IMAGE_DIRECTORY)
        check(directory.exists() || directory.mkdirs()) { "Could not create image storage." }
        val destination = File(directory, "${UUID.randomUUID()}.jpg")
        try {
            source.inputStream().use { input ->
                destination.outputStream().use(input::copyTo)
            }
            destination.absolutePath
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }

    override suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        val ownedDirectory = File(context.filesDir, IMAGE_DIRECTORY).canonicalFile
        if (file.canonicalFile.parentFile == ownedDirectory) file.delete()
        Unit
    }

    private fun ContentResolver.extensionFor(uri: Uri): String =
        getType(uri)?.substringAfter('/', missingDelimiterValue = "jpg")
            ?.substringBefore('+')
            ?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,10}")) }
            ?: "jpg"

    private companion object {
        const val IMAGE_DIRECTORY = "document_images"
        const val CAPTURE_DIRECTORY = "camera_captures"
    }
}
