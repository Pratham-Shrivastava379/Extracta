package com.extracta.app.data.local

import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentMapperTest {
    @Test
    fun entityMapsToPersistenceAgnosticDomainModel() {
        val entity = DocumentEntity(
            id = 42,
            title = "Receipt",
            text = "Total 100",
            createdAtEpochMillis = 1_000,
            updatedAtEpochMillis = 2_000,
            imagePath = "/owned/receipt.jpg",
            ocrStatus = "COMPLETED",
        )

        val document = entity.toDomain()

        assertEquals(DocumentId(42), document.id)
        assertEquals(Instant.ofEpochMilli(1_000), document.createdAt)
        assertEquals(Instant.ofEpochMilli(2_000), document.updatedAt)
        assertEquals(OcrStatus.COMPLETED, document.ocrStatus)
    }

    @Test
    fun unknownPersistedStatusMapsToFailed() {
        val entity = DocumentEntity(
            id = 1,
            title = "Document",
            text = "",
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
            imagePath = "/owned/image.jpg",
            ocrStatus = "UNKNOWN_FUTURE_STATUS",
        )

        assertEquals(OcrStatus.FAILED, entity.toDomain().ocrStatus)
    }
}
