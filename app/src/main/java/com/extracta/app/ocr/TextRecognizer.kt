package com.extracta.app.ocr

interface TextRecognizer {
    suspend fun recognize(imagePath: String): String
}
