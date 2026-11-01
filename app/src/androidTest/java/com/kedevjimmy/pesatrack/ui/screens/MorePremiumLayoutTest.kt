package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** All approved More destinations must stay functional after the visual redesign. */
@RunWith(AndroidJUnit4::class)
class MorePremiumLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun approvedWorkspaceTilesKeepTheirExistingNavigation() {
        var destination = ""
        compose.setContent {
            PesaTrackTheme {
                MoreScreen(
                    scaffoldPadding = PaddingValues(0.dp),
                    onProfileClick = { destination = "profile" },
                    onProfileSectionClick = { destination = it },
                    onAccountsClick = { destination = "accounts" },
                    onImportClick = { destination = "imports" },
                    onStatementActivityClick = { destination = "statements" },
                    onDataHealthClick = { destination = "diagnostics" },
                    onSettingsClick = { destination = "settings" },
                    onReportsClick = { destination = "reports" }
                )
            }
        }

        compose.onNodeWithText("Money workspace").assertIsDisplayed()
        compose.onNodeWithText("Statements").performClick()
        compose.runOnIdle { assertEquals("statements", destination) }
        compose.onNodeWithText("Imports").performClick()
        compose.runOnIdle { assertEquals("imports", destination) }
        compose.onNodeWithText("Accounts").performClick()
        compose.runOnIdle { assertEquals("accounts", destination) }
        compose.onNodeWithText("Reports").performClick()
        compose.runOnIdle { assertEquals("reports", destination) }
    }

    @Test
    fun newPreferenceAndSupportShortcutsOpenRealDestinations() {
        var destination = ""
        compose.setContent {
            PesaTrackTheme {
                MoreScreen(
                    scaffoldPadding = PaddingValues(0.dp),
                    onProfileClick = { destination = "profile" },
                    onProfileSectionClick = { destination = it },
                    onAccountsClick = { destination = "accounts" },
                    onImportClick = { destination = "imports" },
                    onStatementActivityClick = { destination = "statements" },
                    onDataHealthClick = { destination = "diagnostics" },
                    onSettingsClick = { destination = "settings" },
                    onReportsClick = { destination = "reports" }
                )
            }
        }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Notifications"))
        compose.onNodeWithText("Notifications").performClick()
        compose.runOnIdle { assertEquals("notifications", destination) }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("App lock & security"))
        compose.onNodeWithText("App lock & security").performClick()
        compose.runOnIdle { assertEquals("security", destination) }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Advanced troubleshooting"))
        compose.onNodeWithText("Advanced troubleshooting").performClick()
        compose.runOnIdle { assertEquals("diagnostics", destination) }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("About PesaTrack"))
        compose.onNodeWithText("About PesaTrack").performClick()
        compose.runOnIdle { assertEquals("about", destination) }
    }
}
