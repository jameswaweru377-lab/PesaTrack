package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Layout contract for the approved light Profile reference. */
@RunWith(AndroidJUnit4::class)
class ProfileReferenceLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showProfile() {
        compose.setContent {
            PesaTrackTheme {
                ProfileScreen(
                    scaffoldPadding = PaddingValues(),
                    onBack = {},
                    onSettingsClick = {},
                    onAccountsClick = {},
                    onDataHealthClick = {},
                    onGoalsClick = {},
                    onHistoricalImportClick = {}
                )
            }
        }
    }

    @Test
    fun profileKeepsApprovedDestinationsWithoutFinancialOrSettingsDuplicates() {
        showProfile()

        listOf(
            "Personal information",
            "App lock & security",
            "Notifications",
            "Appearance",
            "Help & support",
            "About PesaTrack",
            "Better money habits",
            "Not enough information yet"
        ).forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Financial overview").assertDoesNotExist()
        compose.onNodeWithText("More settings").assertDoesNotExist()
    }

    @Test
    fun appearanceAndPersonalInformationOpenFromProfile() {
        showProfile()

        compose.onNodeWithText("Appearance").performClick()
        compose.onNodeWithText("Light").assertIsDisplayed()
        compose.onNodeWithText("Text size").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithText("Personal information").performClick()
        compose.onNodeWithText("Display name").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Choose photo").assertIsDisplayed()
        compose.onNodeWithText("Save changes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun securityAndHelpHaveRealDestinations() {
        showProfile()
        compose.onNodeWithText("App lock & security").performScrollTo().performClick()
        compose.onNodeWithText("App lock & security").assertIsDisplayed()
        compose.onNodeWithText("App lock PIN").assertIsDisplayed()
        compose.onNodeWithText("Unlock with biometrics").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Help & support").performScrollTo().performClick()
        compose.onNodeWithText("Search help topics").assertIsDisplayed()
        compose.onNodeWithText("Guided troubleshooting").performClick()
        compose.onNodeWithText("Run safe checks").assertIsDisplayed()
    }

    @Test
    fun moneyHabitsHasExplainedEmptyStateAndAction() {
        showProfile()
        compose.onNodeWithText("Better money habits").performClick()
        compose.onNodeWithText("Not enough information yet").assertIsDisplayed()
        compose.onNodeWithText("What we can see").performScrollTo().assertIsDisplayed()
    }
}
