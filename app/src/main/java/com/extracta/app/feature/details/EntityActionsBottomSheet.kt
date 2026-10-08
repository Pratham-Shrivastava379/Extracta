package com.extracta.app.feature.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.entity.action.EntityAction

const val ENTITY_ACTIONS_SHEET_TEST_TAG = "entity_actions_sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityActionsBottomSheet(
    entity: DetectedEntity,
    actions: List<EntityAction>,
    onAction: (EntityAction) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .semantics { paneTitle = "Actions for ${entity.type.label}" }
                .testTag(ENTITY_ACTIONS_SHEET_TEST_TAG),
        ) {
            ListItem(
                headlineContent = { Text(entity.originalText) },
                supportingContent = { Text(entity.type.label) },
            )
            HorizontalDivider()
            actions.forEach { action ->
                ListItem(
                    headlineContent = { Text(action.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onAction(action) }
                        .testTag("entity_action_${action.label}"),
                )
            }
        }
    }
}

internal val com.extracta.app.domain.model.EntityType.label: String
    get() = when (this) {
        com.extracta.app.domain.model.EntityType.EMAIL -> "Email"
        com.extracta.app.domain.model.EntityType.URL -> "Web address"
        com.extracta.app.domain.model.EntityType.PHONE -> "Phone"
        com.extracta.app.domain.model.EntityType.DATE_TIME -> "Date or time"
        com.extracta.app.domain.model.EntityType.MONEY -> "Money"
        com.extracta.app.domain.model.EntityType.ADDRESS -> "Address"
    }
