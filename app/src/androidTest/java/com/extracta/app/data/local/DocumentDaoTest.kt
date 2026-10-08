package com.extracta.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentDaoTest {
    private lateinit var database: ExtractaDatabase
    private lateinit var dao: DocumentDao
    private lateinit var entityDao: DetectedEntityDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ExtractaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.documentDao()
        entityDao = database.detectedEntityDao()
    }

    @Test
    fun replacesDetectedEntitiesTransactionallyWithoutDuplicates() = runTest {
        val documentId = dao.insert(entity(title = "Entity source", updatedAt = 100))
        val first = detectedEntity(documentId, "EMAIL", "a@b.co", 0, 6)
        val replacement = detectedEntity(documentId, "URL", "https://example.test", 10, 30)

        entityDao.replaceForDocument(documentId, listOf(first))
        assertEquals(listOf("a@b.co"), entityDao.observeByDocument(documentId).first().map { it.originalText })

        entityDao.replaceForDocument(documentId, listOf(replacement))
        val rows = entityDao.observeByDocument(documentId).first()
        assertEquals(1, rows.size)
        assertEquals("URL", rows.single().type)

        entityDao.replaceForDocument(documentId, emptyList())
        assertEquals(emptyList<DetectedEntityEntity>(), entityDao.observeByDocument(documentId).first())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun insertsObservesAndRenamesDocuments() = runTest {
        val olderId = dao.insert(entity(title = "Older", updatedAt = 100))
        val newerId = dao.insert(entity(title = "Newer", updatedAt = 200))

        assertEquals(listOf(newerId, olderId), dao.observeAll().first().map { it.id })

        assertEquals(1, dao.updateTitle(olderId, "Renamed", 300))
        assertEquals("Renamed", dao.observeById(olderId).first()?.title)
        assertEquals(olderId, dao.observeAll().first().first().id)
        assertEquals(0, dao.updateTitle(999, "Missing", 400))
        assertNull(dao.observeById(999).first())

        assertEquals(1, dao.updateOcr(newerId, "Recognized", "COMPLETED", 500))
        val recognized = dao.observeById(newerId).first()
        assertEquals("Recognized", recognized?.text)
        assertEquals("COMPLETED", recognized?.ocrStatus)
        assertEquals(500L, recognized?.updatedAtEpochMillis)
        assertEquals(0, dao.updateOcr(999, "", "FAILED", 600))
    }

    @Test
    fun searchesLiteralWildcardsAndSortsDeterministically() = runTest {
        val percent = dao.insert(entity(title = "Save 50%", updatedAt = 300))
        val underscore = dao.insert(entity(title = "under_score", updatedAt = 100))
        val alpha = dao.insert(entity(title = "Alpha", updatedAt = 200))

        assertEquals(listOf(percent), dao.observeNewest("%50\\%%").first().map { it.id })
        assertEquals(listOf(underscore), dao.observeNewest("%under\\_score%").first().map { it.id })
        assertEquals(
            listOf(underscore, alpha, percent),
            dao.observeOldest("%%").first().map { it.id },
        )
        assertEquals(
            listOf(alpha, percent, underscore),
            dao.observeByTitle("%%").first().map { it.id },
        )
    }

    private fun entity(title: String, updatedAt: Long) = DocumentEntity(
        title = title,
        text = "",
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = updatedAt,
        imagePath = "/owned/$title.jpg",
        ocrStatus = "NOT_STARTED",
    )

    private fun detectedEntity(
        documentId: Long,
        type: String,
        text: String,
        start: Int,
        end: Int,
    ) = DetectedEntityEntity(
        documentId = documentId,
        type = type,
        originalText = text,
        normalizedValue = text,
        startOffset = start,
        endOffset = end,
    )
}
