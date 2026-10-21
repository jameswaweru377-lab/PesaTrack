package com.kedevjimmy.pesatrack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
    fun tabSelectionReturnsToExpectedRoots() {
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
            compose.onAllNodes(hasText("Available after commitments"))
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
            compose.onAllNodes(hasText("Available after commitments"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(
            hasText("Activity") and hasClickAction()
        ).performClick()

        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("Search transactions"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(hasText("Search transactions"))
            .assertIsDisplayed()
    }
}
