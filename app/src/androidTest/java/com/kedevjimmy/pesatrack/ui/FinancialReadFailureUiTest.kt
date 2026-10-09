package com.kedevjimmy.pesatrack.ui

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreen
import com.kedevjimmy.pesatrack.ui.screens.DebtsScreen
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.assertThrows
import org.junit.runner.RunWith

/** Force a real isolated SQLite read failure, restore its schema, then retry the UI. */
@RunWith(AndroidJUnit4::class)
class FinancialReadFailureUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @After fun cleanup() {
        compose.runOnIdle { }
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }

    @Test fun activityReadFailureOffersRetryWithoutPublishingEmptyFinancialSummary() {
        val activityStore = TransactionStore.get(context)
        activityStore.activityMonths()
        val recreate = removeTable("transactions")
        assertThrows(SQLiteException::class.java) { activityStore.activityMonths() }
        compose.setContent { PesaTrackTheme { ActivityScreen(PaddingValues()) } }
        try {
            waitFor("Activity could not load")
        } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
            android.util.Log.e("FinancialReadFailureUiTest", compose.onRoot(useUnmergedTree = true).printToString())
            throw failure
        }
        compose.onNodeWithText("Money in").assertDoesNotExist()
        PesaTrackDatabase.get(context).writableDatabase.execSQL(recreate)
        activityStore.activityMonths()
        compose.onNodeWithText("Try again").performClick()
        waitFor("Money in")
        compose.onNodeWithText("Activity could not load").assertDoesNotExist()
    }

    @Test fun debtReadFailureCannotClaimUserHasNoOutstandingDebt() {
        val recreate = removeTable("debt_accounts")
        compose.setContent { PesaTrackTheme { DebtsScreen(PaddingValues(), {}) } }
        waitFor("Debt could not load")
        compose.onNodeWithText("You're clear here").assertDoesNotExist()
        PesaTrackDatabase.get(context).writableDatabase.execSQL(recreate)
        compose.onNodeWithText("Try again").performClick()
        waitFor("No outstanding debt is currently recorded.")
        compose.onNodeWithText("Debt could not load").assertDoesNotExist()
    }

    private fun removeTable(table: String): String {
        val db = PesaTrackDatabase.get(context).writableDatabase
        val schema = db.rawQuery("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)).use { it.moveToFirst(); it.getString(0) }
        db.execSQL("DROP TABLE $table")
        return schema
    }
    private fun waitFor(text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
