package com.extracta.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val text: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val imagePath: String,
    val ocrStatus: String,
)
