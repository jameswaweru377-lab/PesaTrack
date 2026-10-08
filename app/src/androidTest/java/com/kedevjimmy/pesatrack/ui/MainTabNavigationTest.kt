package com.kedevjimmy.pesatrack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import com.kedevjimmy.pesatrack.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainTabNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tabSelectionAndAndroidBackReturnToExpectedRoot() {
        compose.setContent {
            PesaTrackTheme {
                PesaTrackApp(
                    themeMode = ThemeMode.SYSTEM,
                    onThemeModeChange = {},
                    setupComplete = true,
                    onSetupComplete = {}
                )
            }
        }

        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("Tracked funds"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(
            hasText("Activity") and hasClickAction()
        ).performClick()

        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("Search transactions"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(
            hasText("Home") and hasClickAction()
        ).performClick()

        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("Tracked funds"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(
            hasText("Activity") and hasClickAction()
        ).performClick()

        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("Search transactions"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        pressBack()

        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("Tracked funds"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(hasText("Tracked funds"))
            .assertIsDisplayed()
    }
}
