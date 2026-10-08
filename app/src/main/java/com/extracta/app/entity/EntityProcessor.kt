package com.extracta.app.entity

import com.extracta.app.domain.model.DocumentId

interface EntityProcessor {
    fun enqueue(documentId: DocumentId)

    fun resumeCompleted()
}
