package com.extracta.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.extracta.app.data.local.DocumentEntity
import com.extracta.app.data.local.ExtractaDatabase
import com.extracta.app.data.storage.LocalImageStorage
import com.extracta.app.domain.model.DocumentId
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RoomDocumentDeletionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: ExtractaDatabase
    private lateinit var imageFile: File
    private val clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DATABASE_NAME)
        database = Room.databaseBuilder(context, ExtractaDatabase::class.java, TEST_DATABASE_NAME)
            .allowMainThreadQueries()
            .build()
        val directory = File(context.filesDir, "document_images").apply { mkdirs() }
        imageFile = File(directory, "delete-test.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DATABASE_NAME)
        imageFile.delete()
    }

    @Test
    fun deleteRemovesRoomRecordAndOwnedImage() = runTest {
        val id = database.documentDao().insert(
            DocumentEntity(
                title = "Delete test",
                text = "Synthetic text",
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1,
                imagePath = imageFile.absolutePath,
                ocrStatus = "COMPLETED",
            ),
        )
        val repository = RoomDocumentRepository(
            documentDao = database.documentDao(),
            imageStorage = LocalImageStorage(context),
            clock = clock,
        )

        repository.deleteDocument(DocumentId(id))

        assertNull(repository.observeDocument(DocumentId(id)).first())
        assertFalse(imageFile.exists())
    }

    @Test
    fun renameSurvivesDatabaseCloseAndReopen() = runTest {
        val id = database.documentDao().insert(
            DocumentEntity(
                title = "Original title",
                text = "Synthetic text",
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1,
                imagePath = imageFile.absolutePath,
                ocrStatus = "COMPLETED",
            ),
        )
        RoomDocumentRepository(database.documentDao(), LocalImageStorage(context), clock)
            .updateTitle(DocumentId(id), "Persistent title")

        database.close()
        database = Room.databaseBuilder(context, ExtractaDatabase::class.java, TEST_DATABASE_NAME)
            .allowMainThreadQueries()
            .build()

        assertEquals("Persistent title", database.documentDao().observeById(id).first()?.title)
    }

    private companion object {
        const val TEST_DATABASE_NAME = "phase-8-management-test.db"
    }
}
