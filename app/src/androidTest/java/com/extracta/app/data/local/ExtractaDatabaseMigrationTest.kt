package com.extracta.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class ExtractaDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ExtractaDatabase::class.java,
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    @Throws(IOException::class)
    fun migrateOneToTwoPreservesDocumentsAndCreatesEntityStorage() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO documents " +
                    "(id, title, text, createdAtEpochMillis, updatedAtEpochMillis, imagePath, ocrStatus) " +
                    "VALUES (1, 'Existing', 'Text', 10, 20, '/owned/image.jpg', 'COMPLETED')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            ExtractaDatabase.MIGRATION_1_2,
        ).close()

        val database = Room.databaseBuilder(context, ExtractaDatabase::class.java, TEST_DATABASE)
            .addMigrations(ExtractaDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        assertEquals("Existing", runBlocking { database.documentDao().observeById(1).first()?.title })
        database.close()
    }

    private companion object {
        const val TEST_DATABASE = "extracta-migration-test"
    }
}
