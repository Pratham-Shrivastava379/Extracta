package com.extracta.app.data.repository

import com.extracta.app.domain.model.DocumentId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryDocumentRepositoryTest {
    private val repository = InMemoryDocumentRepository()

    @Test
    fun documentsAreSeededInNewestFirstOrder() = runTest {
        val documents = repository.observeDocuments().first()

        assertEquals(3, documents.size)
        assertEquals("Quarterly planning notes", documents.first().title)
        assertEquals(documents.sortedByDescending { it.updatedAt }, documents)
    }

    @Test
    fun documentCanBeObservedByTypedId() = runTest {
        val document = repository.observeDocument(DocumentId(2)).first()
        val missing = repository.observeDocument(DocumentId(99)).first()

        assertEquals("Coffee shop receipt", document?.title)
        assertNull(missing)
    }
}
