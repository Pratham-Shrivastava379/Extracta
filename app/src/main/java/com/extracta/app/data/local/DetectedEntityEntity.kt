package com.extracta.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detected_entities",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("documentId"),
        Index(value = ["documentId", "startOffset", "endOffset", "type"], unique = true),
    ],
)
data class DetectedEntityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val type: String,
    val originalText: String,
    val normalizedValue: String,
    val startOffset: Int,
    val endOffset: Int,
)
