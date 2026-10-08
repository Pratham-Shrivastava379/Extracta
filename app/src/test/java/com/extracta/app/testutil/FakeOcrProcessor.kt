package com.extracta.app.testutil

import com.extracta.app.domain.model.DocumentId
import com.extracta.app.ocr.OcrProcessor

class FakeOcrProcessor : OcrProcessor {
    val enqueuedIds = mutableListOf<DocumentId>()
    var resumeCalls = 0

    override fun enqueue(documentId: DocumentId) {
        enqueuedIds += documentId
    }

    override fun resumePending() {
        resumeCalls += 1
    }
}
