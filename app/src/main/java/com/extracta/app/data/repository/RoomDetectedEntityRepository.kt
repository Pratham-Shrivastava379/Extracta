package com.extracta.app.data.repository

import com.extracta.app.data.local.DetectedEntityDao
import com.extracta.app.data.local.toDomain
import com.extracta.app.data.local.toEntity
import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.repository.DetectedEntityRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomDetectedEntityRepository @Inject constructor(
    private val dao: DetectedEntityDao,
) : DetectedEntityRepository {
    override fun observeForDocument(documentId: DocumentId): Flow<List<DetectedEntity>> =
        dao.observeByDocument(documentId.value).map { rows -> rows.map { it.toDomain() } }

    override suspend fun replaceForDocument(
        documentId: DocumentId,
        entities: List<DetectedEntity>,
    ) {
        dao.replaceForDocument(documentId.value, entities.map { it.toEntity(documentId) })
    }
}
