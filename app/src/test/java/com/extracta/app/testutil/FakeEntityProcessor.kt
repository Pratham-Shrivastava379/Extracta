package com.extracta.app.testutil

import com.extracta.app.domain.model.DocumentId
import com.extracta.app.entity.EntityProcessor

class FakeEntityProcessor : EntityProcessor {
    val enqueuedIds = mutableListOf<DocumentId>()
    var resumeCalls = 0

    override fun enqueue(documentId: DocumentId) {
        enqueuedIds += documentId
    }

    override fun resumeCompleted() {
        resumeCalls += 1
    }
}
