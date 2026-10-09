package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.interpretation.*
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.ui.screens.AccountsScreen
import com.kedevjimmy.pesatrack.ui.screens.DebtsScreen
import com.kedevjimmy.pesatrack.ui.screens.HomeScreen
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** A zero estimate from partial repayment is not confirmed debt settlement. */
@RunWith(AndroidJUnit4::class)
class EstimatedDebtSettlementUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
        seedEstimatedZero()
    }
    @After fun cleanup() {
        compose.runOnIdle { }
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }

    @Test fun accountsRetainsZeroEstimateAsAnActiveDebtAwaitingConfirmation() {
        compose.setContent { PesaTrackTheme { AccountsScreen(PaddingValues()) } }
        waitFor("Known balances")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("1 active account"))
        compose.onNodeWithText("1 active account").assertExists()
        compose.onNodeWithText("No active tracked debt").assertDoesNotExist()
        compose.onNodeWithText("None").assertDoesNotExist()
    }

    @Test fun homeRetainsZeroEstimateAsAnActiveDebtAwaitingConfirmation() {
        compose.setContent { PesaTrackTheme { HomeScreen(PaddingValues()) } }
        waitFor("Tracked funds")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("1 active debt account"))
        compose.onNodeWithText("1 active debt account").assertExists()
        compose.onNodeWithText("0 active debt accounts").assertDoesNotExist()
        compose.onNodeWithText("None").assertDoesNotExist()
    }

    @Test fun debtPageKeepsZeroEstimateOutOfSettledDebts() {
        compose.setContent { PesaTrackTheme { DebtsScreen(PaddingValues(), {}) } }
        waitFor("Total outstanding debt")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Synthetic repayment lender"))
        compose.onNodeWithText("Synthetic repayment lender").assertExists()
        compose.onNodeWithText("Settled debts (1)").assertDoesNotExist()
        compose.onNodeWithText("You're clear here").assertDoesNotExist()
    }

    private fun seedEstimatedZero() {
        val at = System.currentTimeMillis() - 180_000L
        val reference = FinancialAccountReference(entityKey = "synthetic:repayment-lender",
            displayName = "Synthetic repayment lender", providerKey = "synthetic:repayment-lender",
            accountTypeHint = AccountType.OTHER, ownershipHint = AccountOwnershipHint.USER_OWNED,
            nature = FinancialAccountNature.LIABILITY)
        val store = DebtStore.get(context)
        store.recordObservedOutstanding(reference, 10_000L, at)
        store.recordRepayment(DebtRepaymentEventFact(amountMinor = 10_000L, liability = reference,
            fundingAccount = null, completion = DebtCompletion.PARTIAL, outstandingAfterMinor = null,
            availableCreditAfterMinor = null, walletBalanceAfterMinor = null,
            confidence = InterpretationConfidence.HIGH), null, "invented-partial-repayment", null,
            "TEST", at + 60_000L)
        val account = store.accounts().single()
        assertEquals(0L, account.outstandingMinor)
        assertEquals(DebtBalanceQuality.ESTIMATED, account.balanceQuality)
        assertFalse(account.isSettled)
    }
    private fun waitFor(text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
