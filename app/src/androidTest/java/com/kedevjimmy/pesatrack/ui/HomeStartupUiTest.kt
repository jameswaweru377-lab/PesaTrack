package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.ui.screens.HomeScreen
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeStartupUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After fun tearDown() {
        compose.runOnIdle { }
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test fun emptyHistoryLoadsHomeWithoutStayingOnSpinner() {
        showHome()
        waitForText("Tracked funds")
        compose.onNodeWithText("Preparing your money picture").assertDoesNotExist()
    }

    @Test fun startupFailureShowsRetryAndRetryCanLoadHome() {
        val db = PesaTrackDatabase.get(context).writableDatabase
        val createTransactions = db.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'transactions'",
            null
        ).use { cursor -> cursor.moveToFirst(); cursor.getString(0) }
        db.execSQL("DROP TABLE transactions")
        showHome()
        waitForText("Your money picture could not load")
        compose.onNodeWithText("Preparing your money picture").assertDoesNotExist()
        db.execSQL(createTransactions)
        compose.onNodeWithText("Try again").performClick()
        waitForText("Tracked funds")
        compose.onNodeWithText("Your money picture could not load").assertDoesNotExist()
    }

    private fun showHome() {
        compose.setContent {
            PesaTrackTheme(darkTheme = true) {
                HomeScreen(scaffoldPadding = PaddingValues())
            }
        }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
