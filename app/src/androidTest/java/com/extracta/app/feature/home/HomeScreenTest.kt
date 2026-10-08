package com.extracta.app.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.extracta.app.ui.theme.ExtractaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateAndScanActionAreVisibleAndClickable() {
        var scanClickCount = 0

        composeRule.setContent {
            ExtractaTheme(dynamicColor = false) {
                HomeScreen(
                    uiState = HomeUiState.Empty,
                    onScanClick = { scanClickCount++ },
                    onDocumentClick = {},
                )
            }
        }

        composeRule.onNodeWithText("No documents yet").assertIsDisplayed().assert(isHeading())
        composeRule.onNodeWithTag(SCAN_DOCUMENT_TEST_TAG).assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, scanClickCount) }
    }
}
