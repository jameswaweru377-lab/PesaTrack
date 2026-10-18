package com.kedevjimmy.pesatrack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.screens.PesaTrackWelcomePage
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PesaTrackWelcomePageTest {
    @get:Rule val compose = createComposeRule()

    @Test fun userCanStartWithoutTriggeringImportOrConsentUntilNextStep() {
        var events = 0
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                PesaTrackWelcomePage(onGetStarted = { events++ }, onSkip = {})
            }
        }
        compose.onNodeWithText("Welcome to PesaTrack").assertIsDisplayed()
        compose.onNodeWithText("Welcome to Home").assertDoesNotExist()
        compose.onNodeWithText("Bring in M-Pesa and bank activity").assertIsDisplayed()
        compose.onNodeWithText("Get started").performClick()
        compose.runOnIdle { assertEquals(1, events) }
    }

    @Test fun userCanSkipWithoutGrantingSmsPermission() {
        var skipped = false
        compose.setContent {
            PesaTrackTheme(darkTheme = false) {
                PesaTrackWelcomePage(onGetStarted = {}, onSkip = { skipped = true })
            }
        }
        compose.onNodeWithText("Skip for now").performClick()
        compose.runOnIdle { assertEquals(true, skipped) }
    }
}
