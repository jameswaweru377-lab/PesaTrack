package com.kedevjimmy.pesatrack.ui

import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        waitForText("Available after commitments")
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
        waitForText("Available after commitments")
        compose.onNodeWithText("Your money picture could not load").assertDoesNotExist()
    }

    @Test fun homeUsesApprovedSectionsAndDoesNotExposeOldShortcuts() {
        showHome()
        waitForText("Available after commitments")
        val list = compose.onNode(hasScrollToIndexAction())
        list.performScrollToNode(hasText("This month"))
        compose.onNodeWithText("Money in").assertExists()
        compose.onNodeWithText("Money out").assertExists()
        compose.onNodeWithText("Budget left").assertExists()
        list.performScrollToNode(hasText("Upcoming"))
        compose.onNodeWithText("Upcoming").assertExists()
        compose.onNodeWithText("Recent activity").assertExists()
        compose.onNodeWithText("More money tools").assertDoesNotExist()
    }

    @Test fun partialLoanKeepsCashOutflowButProjectsPersonalSpending() {
        val now = System.currentTimeMillis()
        val transactions = TransactionStore.get(context)
        val salary = RuleStore.get(context).save(
            name = "Invented salary", counterpartyContains = "",
            counterpartyExact = "Invented employer", displayLabel = "Invented employer",
            category = TransactionCategory.INCOME, direction = MoneyDirection.INCOME,
            preset = RulePreset.SALARY, startsPayCycle = true,
            accountId = AccountStore.DEFAULT_MPESA_ID,
            payWindowStartDay = 1, payWindowEndDay = 31
        )
        assertTrue(transactions.insertManualIncome(
            1_000_000L, now - 60_000L, "Invented employer", salary
        ))
        assertTrue(transactions.insertIfNew(
            parsed = ParsedMpesaTransaction(
                transactionCode = "LOANHOME01", type = MpesaTransactionType.SENT_MONEY,
                amountMinor = 160_000L, counterparty = "Invented restaurant",
                counterpartyPhone = null, feeMinor = 2_000L, balanceMinor = null,
                occurredAtText = null, rawMessage = "Invented home loan fixture"
            ),
            sourcePackage = null, sourceKind = "test", receivedAtEpochMs = now,
            occurredAtEpochMs = now, rebuildPayCycles = false,
            runRelationshipReconciliation = false, runMirrorCorrelation = false,
            accountIdOverride = AccountStore.DEFAULT_MPESA_ID,
            directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.EATING_OUT,
            reviewStateOverride = ReviewState.USER_CLASSIFIED
        ))
        val payment = requireNotNull(transactions.findByTransactionCode("LOANHOME01"))
        PersonalReceivableStore(context).record(
            null, "Invented borrower", null, PersonalReceivableStore.LENT, 80_000L, now, payment.id
        )
        BudgetStore.get(context).save(
            CategoryStore.get(context).canonicalId(TransactionCategory.EATING_OUT.name), 200_000L
        )
        PayCycleStore.get(context).rebuild()
        assertEquals(160_000L, transactions.getById(payment.id)!!.amountMinor)
        assertEquals(838_000L, PayCycleStore.get(context).snapshot().availableToLiveMinor)
        showHome()
        waitForText("Available after commitments")
        // The reference Home shows actual cash movements and budget availability;
        // the old category breakdown is no longer visible in the approved image.
        compose.onNodeWithText("Money in").assertExists()
        compose.onNodeWithText("Money out").assertExists()
        compose.onNodeWithText("Budget left").assertExists()
        assertEquals(160_000L, transactions.getById(payment.id)!!.amountMinor)
        assertEquals(838_000L, PayCycleStore.get(context).snapshot().availableToLiveMinor)
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
