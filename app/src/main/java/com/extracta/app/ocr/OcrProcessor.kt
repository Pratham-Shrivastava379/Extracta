package com.extracta.app.ocr

import com.extracta.app.domain.model.DocumentId

interface OcrProcessor {
    fun enqueue(documentId: DocumentId)

    fun resumePending()
}
