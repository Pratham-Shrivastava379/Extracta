package com.extracta.app.domain.model

import java.time.Instant

data class Document(
    val id: DocumentId,
    val title: String,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val imagePath: String?,
    val ocrStatus: OcrStatus,
)
