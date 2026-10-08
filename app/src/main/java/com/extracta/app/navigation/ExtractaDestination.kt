package com.extracta.app.navigation

import com.extracta.app.domain.model.DocumentId

const val DOCUMENT_ID_ARGUMENT = "documentId"

sealed class ExtractaDestination(val route: String) {
    data object Home : ExtractaDestination("home")
    data object Capture : ExtractaDestination("capture")
    data object DocumentDetails : ExtractaDestination("document/{$DOCUMENT_ID_ARGUMENT}") {
        fun route(documentId: DocumentId): String = "document/${documentId.value}"
    }
}
