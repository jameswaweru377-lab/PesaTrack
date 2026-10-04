package com.kedevjimmy.pesatrack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.screens.ReviewDiscoveryPage
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingDiscoveryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun userCanContinueWithoutFillingSalaryOrAccountForms() {
        var continued = false
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                ReviewDiscoveryPage(
                    sources = emptyList(), ownershipCandidates = emptyList(),
                    selectedOwnershipEntityKeys = emptySet(), salarySuggestion = null,
                    salaryAccepted = false, importResult = null, applying = false,
                    onSalaryAccepted = {}, onToggleAccount = {}, onManualSalary = {},
                    onContinue = { continued = true }
                )
            }
        }
        compose.onNodeWithText("Set up salary manually").assertDoesNotExist()
        compose.onNodeWithText("Continue").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(continued) }
        compose.onNodeWithText("Personalize accounts and salary (optional)").performClick()
        compose.onNodeWithText("Set up salary manually").assertExists()
    }
}
