package com.extracta.app.entity

import com.extracta.app.domain.model.DetectedEntity

fun interface EntityExtractor {
    fun extract(text: String): List<DetectedEntity>
}
