package com.extracta.app.data.storage

interface ImageStorage {
    suspend fun copyFrom(sourceUri: String): String

    suspend fun copyCapturedFile(sourcePath: String): String

    suspend fun delete(path: String)
}
