package com.extracta.app.testutil

import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.repository.DetectedEntityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDetectedEntityRepository(
    initialEntities: List<DetectedEntity> = emptyList(),
) : DetectedEntityRepository {
    private val entities = MutableStateFlow(initialEntities)

    override fun observeForDocument(documentId: DocumentId): Flow<List<DetectedEntity>> = entities

    override suspend fun replaceForDocument(
        documentId: DocumentId,
        entities: List<DetectedEntity>,
    ) {
        this.entities.value = entities
    }
}
