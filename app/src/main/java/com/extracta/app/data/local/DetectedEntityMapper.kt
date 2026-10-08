package com.extracta.app.data.local

import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.EntityType

fun DetectedEntityEntity.toDomain() = DetectedEntity(
    type = EntityType.valueOf(type),
    originalText = originalText,
    normalizedValue = normalizedValue,
    start = startOffset,
    end = endOffset,
)

fun DetectedEntity.toEntity(documentId: DocumentId) = DetectedEntityEntity(
    documentId = documentId.value,
    type = type.name,
    originalText = originalText,
    normalizedValue = normalizedValue,
    startOffset = start,
    endOffset = end,
)
