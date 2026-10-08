package com.extracta.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DocumentEntity::class, DetectedEntityEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ExtractaDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun detectedEntityDao(): DetectedEntityDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS detected_entities (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        documentId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        originalText TEXT NOT NULL,
                        normalizedValue TEXT NOT NULL,
                        startOffset INTEGER NOT NULL,
                        endOffset INTEGER NOT NULL,
                        FOREIGN KEY(documentId) REFERENCES documents(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_detected_entities_documentId " +
                        "ON detected_entities(documentId)",
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_detected_entities_documentId_startOffset_endOffset_type " +
                        "ON detected_entities(documentId, startOffset, endOffset, type)",
                )
            }
        }
    }
}
