package com.extracta.app.testutil

import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import java.time.Instant

fun documentFixture(
    id: Long = 1,
    title: String = "Test document",
    text: String = "Recognized text",
): Document = Document(
    id = DocumentId(id),
    title = title,
    text = text,
    createdAt = Instant.parse("2026-10-01T10:00:00Z"),
    updatedAt = Instant.parse("2026-10-01T11:00:00Z"),
    imagePath = null,
    ocrStatus = OcrStatus.COMPLETED,
)
