package com.extracta.app.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.extracta.app.MainActivity
import com.extracta.app.feature.home.SCAN_DOCUMENT_TEST_TAG
import com.extracta.app.feature.home.IMPORT_IMAGE_TEST_TAG
import com.extracta.app.feature.home.SEARCH_DOCUMENTS_TEST_TAG
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

class ExtractaAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun collectionShowsCaptureActionsRegardlessOfPersistedDocuments() {
        composeRule.onNodeWithTag(IMPORT_IMAGE_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(SCAN_DOCUMENT_TEST_TAG).assertExists()
    }

    @Test
    fun searchQuerySurvivesActivityRecreation() {
        composeRule.onNodeWithTag(SEARCH_DOCUMENTS_TEST_TAG)
            .performTextReplacement("restored query")

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithTag(SEARCH_DOCUMENTS_TEST_TAG)
            .assertTextContains("restored query")
    }
}
