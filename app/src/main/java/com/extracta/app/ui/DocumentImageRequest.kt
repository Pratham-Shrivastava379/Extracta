package com.extracta.app.ui

import android.content.Context
import coil.request.ImageRequest
import coil.size.Precision
import java.io.File

const val THUMBNAIL_MAX_SIZE_PX = 320
const val DOCUMENT_PREVIEW_MAX_SIZE_PX = 1600

fun documentImageRequest(
    context: Context,
    path: String,
    maxSizePx: Int,
): ImageRequest {
    require(maxSizePx > 0) { "Image request size must be positive." }
    return ImageRequest.Builder(context)
        .data(File(path))
        .size(maxSizePx)
        .precision(Precision.INEXACT)
        .allowHardware(true)
        .build()
}
