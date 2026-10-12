package com.kedevjimmy.pesatrack.ui

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreen
import com.kedevjimmy.pesatrack.ui.screens.DebtsScreen
import com.kedevjimmy.pesatrack.ui.screens.DebtsScreenCache
import com.kedevjimmy.pesatrack.ui.screens.PersonalReceivablesScreen
import com.kedevjimmy.pesatrack.ui.screens.PersonalReceivablesScreenCache
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
        compose.onNodeWithTag("activity-summary-toggle").assertDoesNotExist()
        PesaTrackDatabase.get(context).writableDatabase.execSQL(recreate)
        activityStore.activityMonths()
        compose.onNodeWithText("Try again").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("activity-summary-toggle").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Activity could not load").assertDoesNotExist()
        compose.onNodeWithTag("activity-summary-toggle").performClick()
        waitFor("Money in")
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

    @Test fun returningToCachedDebtsDoesNotReloadOrShowABar() {
        val cache = DebtsScreenCache().apply {
            revision = LedgerUpdateBus.state.value.revision
        }
        val recreate = removeTable("debt_accounts")
        compose.setContent {
            PesaTrackTheme {
                DebtsScreen(PaddingValues(), onBack = {}, stateCache = cache)
            }
        }
        compose.onNodeWithText("No outstanding debt is currently recorded.").assertExists()
        compose.onNodeWithText("Preparing your debt information").assertDoesNotExist()
        // An actual ledger change must invalidate the cache, but retain
        // the last verified cards while the new read is being attempted.
        compose.runOnIdle { LedgerUpdateBus.notifyChanged() }
        waitFor("Could not refresh your debts.")
        compose.onNodeWithText(
            "No outstanding debts were recorded at the last successful refresh."
        ).assertExists()
        PesaTrackDatabase.get(context).writableDatabase.execSQL(recreate)
        compose.onNodeWithText("Try again").performClick()
        waitFor("No outstanding debt is currently recorded.")
        compose.onNodeWithText("Could not refresh your debts.").assertDoesNotExist()
    }

    @Test fun returningToCachedReceivablesNeverStartsFullWidthLoader() {
        val cache = PersonalReceivablesScreenCache().apply {
            revision = LedgerUpdateBus.state.value.revision
        }
        compose.setContent {
            PesaTrackTheme {
                PersonalReceivablesScreen(PaddingValues(), onLiabilities = {}, stateCache = cache)
            }
        }
        compose.onNodeWithText("No personal loans recorded yet. Record a loan or open a payment in Activity to link it.")
            .assertExists()
        compose.onNodeWithText("Preparing your personal debts").assertDoesNotExist()
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
