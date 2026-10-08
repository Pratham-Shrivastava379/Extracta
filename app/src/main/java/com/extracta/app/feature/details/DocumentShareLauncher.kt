package com.extracta.app.feature.details

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.extracta.app.domain.model.Document
import java.io.File

enum class DocumentShareResult {
    LAUNCHED,
    NO_HANDLER,
    IMAGE_UNAVAILABLE,
}

class DocumentShareLauncher(private val context: Context) {
    fun shareText(document: Document): DocumentShareResult {
        val intent = createTextShareIntent(document)
            ?: return DocumentShareResult.IMAGE_UNAVAILABLE
        return launch(intent, "Share extracted text")
    }

    fun shareImage(document: Document): DocumentShareResult {
        val intent = createImageShareIntent(document)
            ?: return DocumentShareResult.IMAGE_UNAVAILABLE
        return launch(intent, "Share document image")
    }

    fun createTextShareIntent(document: Document): Intent? {
        if (document.text.isBlank()) return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, document.title)
            putExtra(Intent.EXTRA_TEXT, document.text)
        }
    }

    fun createImageShareIntent(document: Document): Intent? {
        val file = document.imagePath?.let(::File)?.takeIf(File::isFile)
            ?: return null
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }.getOrNull() ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uri) ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, document.title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun launch(intent: Intent, chooserTitle: String): DocumentShareResult {
        if (intent.resolveActivity(context.packageManager) == null) {
            return DocumentShareResult.NO_HANDLER
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
            DocumentShareResult.LAUNCHED
        }.getOrElse { DocumentShareResult.NO_HANDLER }
    }
}
