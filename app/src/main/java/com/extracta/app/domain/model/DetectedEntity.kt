package com.extracta.app.domain.model

data class DetectedEntity(
    val type: EntityType,
    val originalText: String,
    val normalizedValue: String,
    val start: Int,
    val end: Int,
) {
    init {
        require(start >= 0) { "Entity start offset cannot be negative." }
        require(end > start) { "Entity end offset must be after its start offset." }
        require(originalText.isNotEmpty()) { "Entity source text cannot be empty." }
        require(normalizedValue.isNotBlank()) { "Entity normalized value cannot be blank." }
        require(end - start == originalText.length) {
            "Entity offsets must span the original source text."
        }
    }
}
