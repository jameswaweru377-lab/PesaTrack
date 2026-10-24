package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun profileShowsEveryApprovedDestinationAndLocalDataNote() {
        showProfile()

        listOf(
            "Personal information",
            "Account security",
            "Notifications",
            "Appearance",
            "Financial overview",
            "Help & support",
            "About PesaTrack",
            "More settings",
            "Stored on this phone"
        ).forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun appearanceAndPersonalInformationOpenFromProfile() {
        showProfile()

        compose.onNodeWithText("Appearance").performClick()
        compose.onNodeWithText("Light").assertIsDisplayed()
        compose.onNodeWithText("PesaTrack purple").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithText("Personal information").performClick()
        compose.onNodeWithText("Your name").assertIsDisplayed()
        compose.onNodeWithText("Save details").assertIsDisplayed()
    }
}
