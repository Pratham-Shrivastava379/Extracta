package com.extracta.app.ocr

import com.extracta.app.di.ApplicationScope
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.domain.repository.DocumentRepository
import com.extracta.app.entity.EntityProcessor
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DefaultOcrProcessor @Inject constructor(
    private val repository: DocumentRepository,
    private val recognizer: TextRecognizer,
    private val entityProcessor: EntityProcessor,
    @ApplicationScope private val scope: CoroutineScope,
) : OcrProcessor {
    private val jobs = mutableMapOf<DocumentId, Job>()
    private val processingMutex = Mutex()
    private val resumedPendingWork = AtomicBoolean(false)

    override fun enqueue(documentId: DocumentId) {
        synchronized(jobs) {
            if (jobs[documentId]?.isActive == true) return
            val job = scope.launch {
                processingMutex.withLock { process(documentId) }
            }
            jobs[documentId] = job
            job.invokeOnCompletion {
                synchronized(jobs) {
                    if (jobs[documentId] === job) jobs.remove(documentId)
                }
            }
        }
    }

    override fun resumePending() {
        if (!resumedPendingWork.compareAndSet(false, true)) return
        entityProcessor.resumeCompleted()
        scope.launch {
            repository.observeDocuments().first()
                .filter { document ->
                    document.ocrStatus == OcrStatus.NOT_STARTED ||
                        document.ocrStatus == OcrStatus.PENDING ||
                        document.ocrStatus == OcrStatus.PROCESSING
                }
                .forEach { enqueue(it.id) }
        }
    }

    private suspend fun process(documentId: DocumentId) {
        runCatching {
            val document = repository.observeDocument(documentId).first()
                ?: error("The document no longer exists.")
            val imagePath = document.imagePath
                ?: error("The document image is unavailable.")
            repository.updateOcr(documentId, OcrStatus.PROCESSING, "")
            val text = recognizer.recognize(imagePath).trim()
            repository.updateOcr(documentId, OcrStatus.COMPLETED, text)
            entityProcessor.enqueue(documentId)
        }.onFailure {
            runCatching { repository.updateOcr(documentId, OcrStatus.FAILED, "") }
        }
    }
}
