package com.extracta.app.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.mutableStateOf
import com.extracta.app.domain.model.Document
import com.extracta.app.domain.model.DocumentId
import com.extracta.app.domain.model.OcrStatus
import com.extracta.app.ui.theme.ExtractaTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenStatesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingAndErrorStatesAreVisible() {
        val state = mutableStateOf<HomeUiState>(HomeUiState.Loading)

        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                HomeScreen(state.value, {}, {})
            }
        }
        composeRule.onNodeWithContentDescription("Loading documents").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = HomeUiState.Error("Temporary failure")
        }
        composeRule.onNodeWithText("Documents unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Temporary failure").assertIsDisplayed()
    }

    @Test
    fun contentHandlesLongTextMissingImageAndClick() {
        val document = Document(
            id = DocumentId(42),
            title = "A deliberately long document title that must remain safe in a compact card",
            text = "Long recognized text ".repeat(30),
            createdAt = Instant.parse("2026-10-01T10:00:00Z"),
            updatedAt = Instant.parse("2026-10-01T11:00:00Z"),
            imagePath = null,
            ocrStatus = OcrStatus.COMPLETED,
        )
        var selectedId: DocumentId? = null

        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                HomeScreen(
                    uiState = HomeUiState.Content(listOf(document)),
                    onScanClick = {},
                    onDocumentClick = { selectedId = it },
                )
            }
        }

        composeRule.onNodeWithText(document.title).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Document image unavailable").assertIsDisplayed()
        composeRule.onNodeWithTag(documentCardTestTag(document.id)).performClick()
        composeRule.runOnIdle { assertEquals(document.id, selectedId) }
    }

    @Test
    fun searchAndNoResultsStateAreVisible() {
        val query = mutableStateOf("")
        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                HomeScreen(
                    uiState = HomeUiState.NoResults,
                    onScanClick = {},
                    onDocumentClick = {},
                    query = query.value,
                    onQueryChange = { query.value = it },
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_DOCUMENTS_TEST_TAG).performTextInput("invoice")
        composeRule.onNodeWithText("No matching documents").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("invoice", query.value) }
    }
}
