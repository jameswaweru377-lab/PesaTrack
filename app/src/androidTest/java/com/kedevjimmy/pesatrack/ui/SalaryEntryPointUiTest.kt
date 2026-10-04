package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.ui.screens.HomeScreen
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreen
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalaryEntryPointUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @After fun clean() { compose.runOnIdle { }; PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }
    @Test fun homeOffersSalaryEntryBeforeFirstPayCycle() {
        var opened = false
        compose.setContent { PesaTrackTheme { HomeScreen(PaddingValues(), onSalaryClick = { opened = true }) } }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Set up / add salary").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Set up / add salary").performClick()
        assertTrue(opened)
    }
    @Test fun payCycleViewHasExplicitEmptyStateAndCalendarSwitch() {
        compose.setContent { PesaTrackTheme { ActivityScreen(PaddingValues()) } }
        compose.onNodeWithText("Pay cycle").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Set up / add salary").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pay-cycle spending").assertIsDisplayed()
        compose.onNodeWithText("Calendar month").performClick()
        compose.onNodeWithText("Pay-cycle spending").assertDoesNotExist()
    }
}
