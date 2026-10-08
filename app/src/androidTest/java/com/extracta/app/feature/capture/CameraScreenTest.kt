package com.extracta.app.feature.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.isHeading
import com.extracta.app.ui.theme.ExtractaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CameraScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permissionStatesExposeRecoveryActions() {
        var retries = 0
        composeRule.setContent {
            ExtractaTheme {
                CameraScreen(
                    uiState = CameraUiState.PermissionDenied,
                    onNavigateBack = {},
                    onRequestPermission = { retries += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Camera permission needed")
            .assertIsDisplayed()
            .assert(isHeading())
        composeRule.onNodeWithText("Try again").performClick()
        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun reviewProvidesRetakeSaveAndCancel() {
        var retakes = 0
        var saves = 0
        var cancels = 0
        composeRule.setContent {
            ExtractaTheme {
                CameraScreen(
                    uiState = CameraUiState.Captured("/missing/review.jpg"),
                    onNavigateBack = { cancels += 1 },
                    onRetake = { retakes += 1 },
                    onSave = { saves += 1 },
                )
            }
        }

        composeRule.onNodeWithTag(RETAKE_BUTTON_TEST_TAG).performClick()
        composeRule.onNodeWithTag(SAVE_BUTTON_TEST_TAG).performClick()
        composeRule.onNodeWithTag(CANCEL_CAPTURE_TEST_TAG).performClick()
        composeRule.runOnIdle {
            assertEquals(1, retakes)
            assertEquals(1, saves)
            assertEquals(1, cancels)
        }
    }

    @Test
    fun savingDisablesReviewActions() {
        composeRule.setContent {
            ExtractaTheme {
                CameraScreen(
                    uiState = CameraUiState.Saving("/missing/review.jpg"),
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(RETAKE_BUTTON_TEST_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(SAVE_BUTTON_TEST_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(CANCEL_CAPTURE_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun previewAllowsCaptureOnlyWhenReady() {
        composeRule.setContent {
            ExtractaTheme {
                CameraScreen(
                    uiState = CameraUiState.Preview,
                    controller = null,
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(CAPTURE_BUTTON_TEST_TAG).assertIsEnabled()
    }
}
