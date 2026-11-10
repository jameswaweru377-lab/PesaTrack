package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

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
    fun profileShowsCompactEditorialIdentityAndEveryShortcut() {
        showProfile()

        listOf(
            "Personal info",
            "My accounts",
            "Pay cycle",
            "Your goals",
            "Better\nmoney habits",
            "Make sense of your money",
            "Your money, your plans",
            "Only on this device",
            "PesaTrack"
        ).forEach { compose.onNodeWithText(it).assertExists() }
        listOf("App lock & security", "Notifications", "Appearance", "Help & support",
            "About PesaTrack", "Past transactions", "Funds")
            .forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        compose.onNodeWithContentDescription("Back").assertExists()
        compose.onNodeWithContentDescription("Change cover photo").assertExists()
        compose.onNodeWithContentDescription("Change profile photo").assertExists()
    }

    @Test
    fun allFourCompactShortcutsStayVisibleWithoutHorizontalScrolling() {
        showProfile()
        listOf("Personal info", "My accounts", "Pay cycle", "Your goals")
            .forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun shortcutCardsAreCompactRectanglesAndQuickIconsStayGrouped() {
        showProfile()
        listOf("personal", "accounts", "pay_cycle", "goals").forEach { id ->
            val bounds = compose.onNodeWithTag("profile_shortcut_$id")
                .fetchSemanticsNode().boundsInRoot
            assertTrue("$id shortcut must be wider than it is tall", bounds.width > bounds.height * 1.2f)
        }
        val first = compose.onNodeWithTag("profile_quick_personal")
            .fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("profile_quick_privacy")
            .fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Quick actions must form a centered compact cluster",
            last.right - first.left < root.width * .8f)
    }

    @Test
    fun largeSystemFontDoesNotMoveIdentityOverAvatar() {
        compose.setContent {
            val original = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(original.density, 2f)) {
                PesaTrackTheme {
                    ProfileCoverHeader("James Waweru", "🌿", {}, {})
                }
            }
        }
        val avatar = compose.onNodeWithContentDescription("Change profile photo")
            .fetchSemanticsNode().boundsInRoot
        val name = compose.onNodeWithText("James Waweru")
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Profile name must remain below the avatar at 200% font", name.top >= avatar.bottom)
    }

    @Test
    fun largeSystemFontKeepsMoneyWellnessActionVisible() {
        compose.setContent {
            val original = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(original.density, 2f)) {
                PesaTrackTheme {
                    ProfileNairobiWellnessFeature(presentProfileMoneyHabits(null), {})
                }
            }
        }
        val heading = compose.onNodeWithText("Better\nmoney habits")
            .fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithText("Review insights")
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Wellness action must remain below its heading", action.top >= heading.bottom)
        compose.onNodeWithText("Review insights").assertIsDisplayed()
    }

    @Test
    fun personalInformationOpensLocalFields() {
        showProfile()
        compose.onNodeWithText("Personal info").performScrollTo().performClick()
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
        compose.onNodeWithText("Better\nmoney habits").performScrollTo().performClick()
        compose.onNodeWithText("Not enough information yet").assertIsDisplayed()
        compose.onNodeWithText("WHAT WE CAN SEE").performScrollTo().assertIsDisplayed()
    }
}
