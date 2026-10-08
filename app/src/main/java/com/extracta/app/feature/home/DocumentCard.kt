package com.extracta.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.ui.formatDocumentDate
import com.extracta.app.ui.documentImageRequest
import com.extracta.app.ui.THUMBNAIL_MAX_SIZE_PX
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

fun documentCardTestTag(id: DocumentId): String = "document_card_${id.value}"

@Composable
fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(documentCardTestTag(document.id)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                val path = document.imagePath
                if (path == null) {
                    MissingThumbnail()
                } else {
                    SubcomposeAsyncImage(
                        model = documentImageRequest(context, path, THUMBNAIL_MAX_SIZE_PX),
                        contentDescription = "${document.title} thumbnail",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        success = { SubcomposeAsyncImageContent() },
                        loading = { MissingThumbnail() },
                        error = { MissingThumbnail() },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatDocumentDate(document.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = document.text.ifBlank { "No extracted text yet" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MissingThumbnail() {
    Icon(
        imageVector = Icons.Outlined.Description,
        contentDescription = "Document image unavailable",
        modifier = Modifier.size(36.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}
