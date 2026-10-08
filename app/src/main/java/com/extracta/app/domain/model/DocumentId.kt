package com.extracta.app.domain.model

@JvmInline
value class DocumentId(val value: Long) {
    init {
        require(value > 0) { "Document IDs must be positive." }
    }
}
