package com.kedevjimmy.pesatrack.ui

import android.content.Context
import android.content.ContentValues
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.BudgetStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.screens.BudgetScreen
import com.kedevjimmy.pesatrack.ui.screens.PlanScreen
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A04: invented local histories exercise the actual screen calculations. */
@RunWith(AndroidJUnit4::class)
class BudgetSpendingParityUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        BudgetStore.get(context).save(
            CategoryStore.get(context).canonicalId(TransactionCategory.TRANSPORT.name),
            500_000L
        )
    }

    @After fun teardown() {
        compose.runOnIdle { }
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test fun planBudgetIncludesExpenseFee() {
        insert("BUDGETFEE01", MoneyDirection.EXPENSE, 100_000L, 2_000L)
        compose.setContent { PesaTrackTheme { PlanScreen(PaddingValues()) } }
        awaitBudgetLoaded()
        compose.onNodeWithText("KES 1,020 / KES 5,000").performScrollTo().assertExists()
    }

    @Test fun budgetsDoNotCountIncomeAsCategorySpending() {
        // A user-authoritative category alone cannot turn a receipt into spending.
        insert("BUDGETINC01", MoneyDirection.INCOME, 100_000L, 0L)
        compose.setContent { PesaTrackTheme { BudgetScreen(PaddingValues(), onBack = {}) } }
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Your monthly budgets"))
        awaitBudgetLoaded()
        compose.onNodeWithText("KES 0 / KES 5,000").performScrollTo().assertExists()
    }

    @Test fun planBudgetSubtractsTrustedTransferDirectionRefund() {
        insert("BUDGETPAY01", MoneyDirection.EXPENSE, 100_000L, 0L)
        insert("BUDGETREF01", MoneyDirection.INCOME, 20_000L, 0L)
        val store = TransactionStore.get(context)
        val original = requireNotNull(store.findByTransactionCode("BUDGETPAY01"))
        val refund = requireNotNull(store.findByTransactionCode("BUDGETREF01"))
        PesaTrackDatabase.get(context).writableDatabase.update(
            "transactions",
            ContentValues().apply {
                put("direction", MoneyDirection.TRANSFER.name)
                put("relation_type", TransactionStore.RELATION_REFUND)
                put("related_transaction_id", original.id)
            },
            "id = ?", arrayOf(refund.id.toString())
        )
        compose.setContent { PesaTrackTheme { PlanScreen(PaddingValues()) } }
        awaitBudgetLoaded()
        compose.onNodeWithText("KES 800 / KES 5,000").performScrollTo().assertExists()
    }
    private fun awaitBudgetLoaded() {
        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("KES 5,000", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun insert(code: String, direction: MoneyDirection, amount: Long, fee: Long) {
        val now = System.currentTimeMillis()
        check(TransactionStore.get(context).insertIfNew(
            parsed = ParsedMpesaTransaction(
                transactionCode = code,
                type = if (direction == MoneyDirection.INCOME) MpesaTransactionType.RECEIVED
                    else MpesaTransactionType.SENT_MONEY,
                amountMinor = amount,
                counterparty = "Invented budget merchant",
                counterpartyPhone = null,
                feeMinor = fee,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage = "Invented budget fixture $code"
            ),
            sourcePackage = null,
            sourceKind = "test",
            receivedAtEpochMs = now,
            occurredAtEpochMs = now,
            rebuildPayCycles = false,
            runRelationshipReconciliation = false,
            runMirrorCorrelation = false,
            directionOverride = direction,
            categoryOverride = TransactionCategory.TRANSPORT,
            reviewStateOverride = ReviewState.USER_CLASSIFIED
        ))
    }
}



