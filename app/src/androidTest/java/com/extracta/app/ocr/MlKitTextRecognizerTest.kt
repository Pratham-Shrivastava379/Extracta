package com.extracta.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MlKitTextRecognizerTest {
    @Test
    fun bundledModelRecognizesGeneratedTextOffline() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageFile = File(context.cacheDir, "ocr-smoke-test.jpg")
        val bitmap = Bitmap.createBitmap(1600, 600, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 170f
            }
            canvas.drawText("EXTRACTA OCR 441.00", 50f, 340f, paint)
            imageFile.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
            }

            val result = MlKitTextRecognizer(context).recognize(imageFile.absolutePath)

            assertTrue("Expected generated text in: $result", result.contains("EXTRACTA"))
            assertTrue("Expected generated amount in: $result", result.contains("441"))
        } finally {
            bitmap.recycle()
            imageFile.delete()
        }
    }
}
