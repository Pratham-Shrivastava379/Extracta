package com.extracta.app.feature.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isHeading
import androidx.compose.runtime.mutableStateOf
import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.EntityType
import com.extracta.app.ui.theme.ExtractaTheme
import com.extracta.app.entity.action.EntityAction
import com.extracta.app.entity.action.EntityActionLaunchResult
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class DocumentDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingNotFoundAndErrorStatesAreVisible() {
        val state = mutableStateOf<DocumentDetailsUiState>(DocumentDetailsUiState.Loading)

        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(state.value, {})
            }
        }
        composeRule.onNodeWithContentDescription("Loading document").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = DocumentDetailsUiState.NotFound
        }
        composeRule.onNodeWithText("Document not found").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = DocumentDetailsUiState.Error("Temporary failure")
        }
        composeRule.onNodeWithText("Temporary failure").assertIsDisplayed()
    }

    @Test
    fun contentShowsMetadataMissingImageAndLongText() {
        val longText = "Selectable OCR content. ".repeat(40)
        val document = Document(
            id = DocumentId(9),
            title = "Long document",
            text = longText,
            createdAt = Instant.parse("2026-10-01T10:00:00Z"),
            updatedAt = Instant.parse("2026-10-01T11:00:00Z"),
            imagePath = null,
            ocrStatus = OcrStatus.COMPLETED,
        )

        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(DocumentDetailsUiState.Content(document), {})
            }
        }

        composeRule.onNodeWithText("Document image unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("OCR complete").assertIsDisplayed()
        composeRule.onNodeWithText("Extracted text").assert(isHeading())
        composeRule.onNodeWithText(longText).fetchSemanticsNode()
    }

    @Test
    fun titleCanBeEditedAndSaved() {
        val document = Document(
            id = DocumentId(9),
            title = "Original title",
            text = "",
            createdAt = Instant.parse("2026-10-01T10:00:00Z"),
            updatedAt = Instant.parse("2026-10-01T11:00:00Z"),
            imagePath = null,
            ocrStatus = OcrStatus.NOT_STARTED,
        )
        var savedTitle: String? = null

        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(
                    uiState = DocumentDetailsUiState.Content(document),
                    onNavigateBack = {},
                    onUpdateTitle = { savedTitle = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Edit title").performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Phase 3 verified")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle { org.junit.Assert.assertEquals("Phase 3 verified", savedTitle) }
    }

    @Test
    fun failedOcrCanBeRetried() {
        val document = document(OcrStatus.FAILED)
        var retries = 0
        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(
                    uiState = DocumentDetailsUiState.Content(document),
                    onNavigateBack = {},
                    onRetryOcr = { retries += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Text extraction failed. You can try again.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RETRY_OCR_TEST_TAG).performClick()
        composeRule.runOnIdle { org.junit.Assert.assertEquals(1, retries) }
    }

    @Test
    fun processingAndBlankCompletionHaveDistinctMessages() {
        val state = mutableStateOf(DocumentDetailsUiState.Content(document(OcrStatus.PROCESSING)))
        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(state.value, {})
            }
        }

        composeRule.onNodeWithContentDescription("Extracting document text").assertIsDisplayed()
        composeRule.runOnIdle {
            state.value = DocumentDetailsUiState.Content(document(OcrStatus.COMPLETED))
        }
        composeRule.onNodeWithText("No text was found in this image.").assertIsDisplayed()
    }

    @Test
    fun completedTextCanBeCopied() {
        val completed = document(OcrStatus.COMPLETED).copy(text = "Recognized sample")
        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(DocumentDetailsUiState.Content(completed), {})
            }
        }

        composeRule.onNodeWithTag(COPY_TEXT_TEST_TAG).performClick()
        composeRule.onNodeWithText("Text copied").assertIsDisplayed()
    }

    @Test
    fun completedDocumentShowsReadOnlyDetectedEntities() {
        val completed = document(OcrStatus.COMPLETED).copy(text = "help@example.test")
        val entity = DetectedEntity(
            EntityType.EMAIL,
            "help@example.test",
            "help@example.test",
            0,
            17,
        )
        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(
                    DocumentDetailsUiState.Content(completed, listOf(entity)),
                    {},
                )
            }
        }

        composeRule.onNodeWithTag(DETECTED_ENTITIES_TEST_TAG).assertExists()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNode(
            hasText("help@example.test") and
                hasAnyAncestor(hasTestTag(DETECTED_ENTITIES_TEST_TAG)),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("${ENTITY_CARD_TEST_TAG_PREFIX}0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        )
    }

    @Test
    fun tappingEntityShowsSuitableActionsAndReportsCopy() {
        val completed = document(OcrStatus.COMPLETED).copy(text = "help@example.test")
        val entity = DetectedEntity(EntityType.EMAIL, "help@example.test", "help@example.test", 0, 17)
        var launched: EntityAction? = null
        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(
                    uiState = DocumentDetailsUiState.Content(completed, listOf(entity)),
                    onNavigateBack = {},
                    onEntityAction = {
                        launched = it
                        EntityActionLaunchResult.Copied
                    },
                )
            }
        }

        composeRule.onNodeWithTag("${ENTITY_CARD_TEST_TAG_PREFIX}0").performClick()
        composeRule.onNodeWithTag(ENTITY_ACTIONS_SHEET_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Compose email").assertIsDisplayed()
        composeRule.onNodeWithText("Copy").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(launched is EntityAction.Copy) }
        composeRule.onNodeWithText("Detail copied").assertIsDisplayed()
    }

    @Test
    fun unavailableHandlerIsExplained() {
        val completed = document(OcrStatus.COMPLETED).copy(text = "https://example.test")
        val entity = DetectedEntity(EntityType.URL, "https://example.test", "https://example.test", 0, 20)
        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(
                    uiState = DocumentDetailsUiState.Content(completed, listOf(entity)),
                    onNavigateBack = {},
                    onEntityAction = { EntityActionLaunchResult.NoHandler },
                )
            }
        }

        composeRule.onNodeWithTag("${ENTITY_CARD_TEST_TAG_PREFIX}0").performClick()
        composeRule.onNodeWithText("Open in browser").performClick()
        composeRule.onNodeWithText("No app is available for this action").assertIsDisplayed()
    }

    @Test
    fun deleteRequiresConfirmation() {
        var deletes = 0
        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                DocumentDetailsScreen(
                    uiState = DocumentDetailsUiState.Content(document(OcrStatus.COMPLETED)),
                    onNavigateBack = {},
                    onDeleteDocument = { deletes += 1 },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Delete document").performClick()
        composeRule.onNodeWithText("Delete document?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertEquals(1, deletes) }
    }

    private fun document(status: OcrStatus) = Document(
        id = DocumentId(9),
        title = "OCR document",
        text = "",
        createdAt = Instant.parse("2026-10-01T10:00:00Z"),
        updatedAt = Instant.parse("2026-10-01T11:00:00Z"),
        imagePath = null,
        ocrStatus = status,
    )
}
