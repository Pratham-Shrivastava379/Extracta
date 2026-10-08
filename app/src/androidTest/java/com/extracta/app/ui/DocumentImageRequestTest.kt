package com.extracta.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.request.SuccessResult
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DocumentImageRequestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var source: File

    @Before
    fun setUp() {
        source = File(context.cacheDir, "large-display-source.jpg")
        val bitmap = Bitmap.createBitmap(SOURCE_SIZE, SOURCE_SIZE, Bitmap.Config.RGB_565)
        bitmap.eraseColor(Color.WHITE)
        source.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output))
        }
        bitmap.recycle()
    }

    @After
    fun tearDown() {
        source.delete()
    }

    @Test
    fun largeSourceIsDownsampledForThumbnailAndPreview() = runTest {
        val imageLoader = ImageLoader(context)
        try {
            val thumbnail = imageLoader.execute(
                documentImageRequest(context, source.absolutePath, THUMBNAIL_MAX_SIZE_PX),
            ) as SuccessResult
            val preview = imageLoader.execute(
                documentImageRequest(context, source.absolutePath, DOCUMENT_PREVIEW_MAX_SIZE_PX),
            ) as SuccessResult

            assertEquals(THUMBNAIL_MAX_SIZE_PX, thumbnail.drawable.intrinsicWidth)
            assertEquals(THUMBNAIL_MAX_SIZE_PX, thumbnail.drawable.intrinsicHeight)
            assertTrue(preview.drawable.intrinsicWidth <= DOCUMENT_PREVIEW_MAX_SIZE_PX)
            assertTrue(preview.drawable.intrinsicHeight <= DOCUMENT_PREVIEW_MAX_SIZE_PX)
        } finally {
            imageLoader.shutdown()
        }
    }

    private companion object {
        const val SOURCE_SIZE = 4096
    }
}
