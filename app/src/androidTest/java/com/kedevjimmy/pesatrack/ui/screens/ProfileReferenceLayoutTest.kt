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

/** Layout contract for the simplified private Profile detail screen. */
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
    fun profileShowsPrivateIdentityAndNoDuplicateSettings() {
        showProfile()

        listOf(
            "Personal information",
            "Pay cycle",
            "Better money habits",
            "Edit profile",
            "Only on this device"
        ).forEach { compose.onNodeWithText(it).assertExists() }
        listOf("App lock & security", "Notifications", "Appearance", "Help & support",
            "About PesaTrack", "Past transactions", "Funds")
            .forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        compose.onNodeWithContentDescription("Back to More").assertExists()
        compose.onNodeWithContentDescription("Change cover photo").assertExists()
        compose.onNodeWithContentDescription("Change profile photo").assertExists()
    }

    @Test
    fun personalInformationOpensLocalFields() {
        showProfile()
        compose.onNodeWithText("Personal information").performScrollTo().performClick()
        compose.onNodeWithText("Display name").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("City or town (optional)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("About me (optional)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save changes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun payCycleShortcutUsesItsDedicatedDestination() {
        var opened = false
        compose.setContent {
            PesaTrackTheme {
                ProfileScreen(scaffoldPadding = PaddingValues(), onBack = {},
                    onSettingsClick = {}, onPayCycleClick = { opened = true })
            }
        }
        compose.onNodeWithText("Pay cycle").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(opened) }
    }

    @Test
    fun moneyHabitsHasExplainedEmptyStateAndAction() {
        showProfile()
        compose.onNodeWithText("Better money habits").performScrollTo().performClick()
        compose.onNodeWithText("Not enough information yet").assertIsDisplayed()
        compose.onNodeWithText("WHAT WE CAN SEE").performScrollTo().assertIsDisplayed()
    }
}
