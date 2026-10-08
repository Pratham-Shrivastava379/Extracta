package com.extracta.app.feature.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import java.io.File
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentShareLauncherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val imageDirectory = File(context.filesDir, "document_images")
    private val imageFile = File(imageDirectory, "share-test.jpg")
    private val launcher = DocumentShareLauncher(context)

    @After
    fun tearDown() {
        imageFile.delete()
    }

    @Test
    fun textIntentContainsOnlyTheRequestedDocumentContent() {
        val intent = launcher.createTextShareIntent(document(text = "Synthetic OCR text"))

        assertNotNull(intent)
        assertEquals(Intent.ACTION_SEND, intent?.action)
        assertEquals("text/plain", intent?.type)
        assertEquals("Synthetic document", intent?.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("Synthetic OCR text", intent?.getStringExtra(Intent.EXTRA_TEXT))
        assertNotNull(intent?.resolveActivity(context.packageManager))
        assertNull(launcher.createTextShareIntent(document(text = "")))
    }

    @Test
    fun imageIntentUsesReadablePrivateFileProviderUri() {
        imageDirectory.mkdirs()
        imageFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        val intent = launcher.createImageShareIntent(document(imagePath = imageFile.absolutePath))
        val uri = intent?.sharedStreamUri()

        assertNotNull(intent)
        assertEquals(Intent.ACTION_SEND, intent?.action)
        assertEquals("${context.packageName}.files", uri?.authority)
        assertTrue(intent?.flags?.and(Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertNotNull(intent?.resolveActivity(context.packageManager))
        assertEquals(listOf<Byte>(1, 2, 3, 4), context.contentResolver.openInputStream(uri!!)?.use { it.readBytes().toList() })
    }

    @Test
    fun imageIntentRejectsMissingAndNonOwnedFiles() {
        assertNull(launcher.createImageShareIntent(document(imagePath = imageFile.absolutePath)))
        val outsideFile = File(context.cacheDir, "outside.jpg").apply { writeBytes(byteArrayOf(5)) }
        try {
            assertNull(launcher.createImageShareIntent(document(imagePath = outsideFile.absolutePath)))
        } finally {
            outsideFile.delete()
        }
    }

    private fun document(
        text: String = "Synthetic OCR text",
        imagePath: String? = null,
    ) = Document(
        id = DocumentId(91),
        title = "Synthetic document",
        text = text,
        createdAt = Instant.parse("2026-10-07T10:00:00Z"),
        updatedAt = Instant.parse("2026-10-07T10:00:00Z"),
        imagePath = imagePath,
        ocrStatus = OcrStatus.COMPLETED,
    )

    @Suppress("DEPRECATION")
    private fun Intent.sharedStreamUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }
}
