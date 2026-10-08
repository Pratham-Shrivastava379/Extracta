package com.extracta.app.data.local

import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import java.time.Instant

fun DocumentEntity.toDomain(): Document = Document(
    id = DocumentId(id),
    title = title,
    text = text,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    imagePath = imagePath,
    ocrStatus = ocrStatus.toOcrStatus(),
)

internal fun OcrStatus.toDatabaseValue(): String = name

internal fun String.toOcrStatus(): OcrStatus =
    OcrStatus.entries.firstOrNull { it.name == this } ?: OcrStatus.FAILED
