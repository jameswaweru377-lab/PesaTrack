package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatementEmptyLandingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyStateExplainsPrivacyAndImportsInLightTheme() = assertEmptyState(false)

    @Test fun emptyStateExplainsPrivacyAndImportsInDarkTheme() = assertEmptyState(true)

    @Test fun primaryImportActionStartsTheExistingImportFlow() {
        var importCount = 0
        compose.setContent {
            PesaTrackTheme(darkTheme = false) {
                StatementEmptyLanding(
                    statementFileCount = 0,
                    onImport = { importCount++ },
                    onExplainReview = {},
                    onBankCsv = {}
                )
            }
        }

        compose.onNodeWithText("Import a statement").performClick()
        assertEquals(1, importCount)
        compose.onNodeWithText("M-Pesa PDF").performClick()
        assertEquals(2, importCount)
    }

    private fun assertEmptyState(dark: Boolean) {
        compose.setContent {
            PesaTrackTheme(darkTheme = dark) {
                StatementEmptyLanding(
                    statementFileCount = 0,
                    onImport = {},
                    onExplainReview = {},
                    onBankCsv = {}
                )
            }
        }
        compose.onNodeWithText("0 files").assertIsDisplayed()
        compose.onNodeWithText("Private by design").assertIsDisplayed()
        compose.onNodeWithText("Bring a statement into view").assertIsDisplayed()
        compose.onNodeWithText("Bank CSV").assertDoesNotExist()
        compose.onNodeWithText("M-Pesa PDF").assertIsDisplayed()
    }
}
