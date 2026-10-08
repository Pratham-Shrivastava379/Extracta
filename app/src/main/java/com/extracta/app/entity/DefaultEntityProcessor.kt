package com.extracta.app.entity

import com.extracta.app.di.ApplicationScope
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.domain.repository.DetectedEntityRepository
import com.extracta.app.domain.repository.DocumentRepository
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class DefaultEntityProcessor @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val entityRepository: DetectedEntityRepository,
    private val extractor: EntityExtractor,
    @ApplicationScope private val scope: CoroutineScope,
) : EntityProcessor {
    private val jobs = mutableMapOf<DocumentId, Job>()
    private val resumedCompletedWork = AtomicBoolean(false)

    override fun enqueue(documentId: DocumentId) {
        synchronized(jobs) {
            if (jobs[documentId]?.isActive == true) return
            val job = scope.launch { process(documentId) }
            jobs[documentId] = job
            job.invokeOnCompletion {
                synchronized(jobs) {
                    if (jobs[documentId] === job) jobs.remove(documentId)
                }
            }
        }
    }

    override fun resumeCompleted() {
        if (!resumedCompletedWork.compareAndSet(false, true)) return
        scope.launch {
            documentRepository.observeDocuments().first()
                .filter { it.ocrStatus == OcrStatus.COMPLETED }
                .forEach { enqueue(it.id) }
        }
    }

    private suspend fun process(documentId: DocumentId) {
        runCatching {
            val document = documentRepository.observeDocument(documentId).first()
                ?: return
            if (document.ocrStatus != OcrStatus.COMPLETED) return
            val entities = extractor.extract(document.text)
            entityRepository.replaceForDocument(documentId, entities)
        }
        // Derived-data failures intentionally leave the completed OCR document untouched.
    }
}
