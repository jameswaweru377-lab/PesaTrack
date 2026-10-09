package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Invented histories in a fresh test database, never an installed user's ledger. */
@RunWith(AndroidJUnit4::class)
class FinancialLabHistoryIntegrationTest {
    private lateinit var context: Context
    private val time = 1_790_613_600_000L
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }
    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test fun mshwariAndUnknownLenderHistoriesKeepBalancesAndReplaysSeparate() {
        val store = DebtStore.get(context)
        listOf("M-Shwari", "Nova Credit").forEachIndexed { index, label ->
            val drawdown = FinancialMessageInterpreter.interpret("Finance",
                "$label amount is Ksh 2,500.00. Total $label outstanding amount is Ksh 2,500.00.")
                .events.single() as DebtDrawdownEventFact
            val repayment = FinancialMessageInterpreter.interpret("MPESA",
                "Ksh 500.00 from your M-PESA has been used to partially pay your outstanding $label. " +
                    "Total $label outstanding balance is Ksh 2,000.00.")
                .events.single() as DebtRepaymentEventFact
            repeat(3) { replay ->
                val draw = store.recordDrawdown(drawdown, null, "invented-draw-$index", null, "TEST", time)
                val repay = store.recordRepayment(repayment, null, "invented-repay-$index", null, "TEST", time + 60_000L)
                assertEquals(replay == 0, draw.inserted)
                assertEquals(replay == 0, repay.inserted)
            }
        }
        val accounts = store.accounts().filter { it.lastRepaymentMinor != null }
        assertEquals(2, accounts.size)
        assertEquals(setOf("M-Shwari Loan", "Nova Credit"), accounts.map { it.name }.toSet())
        accounts.forEach {
            assertEquals(200_000L, it.outstandingMinor)
            assertEquals(50_000L, it.lastRepaymentMinor)
            assertEquals(DebtBalanceQuality.OBSERVED, it.balanceQuality)
        }
    }

    @Test fun partialPaymentWithoutBalanceProducesAnEstimateFromPriorDebtNotPaymentAmount() {
        val store = DebtStore.get(context)
        val liability = (FinancialMessageInterpreter.interpret("MPESA",
            "Ksh 500.00 from your M-PESA has been used to partially pay your outstanding M-Shwari.")
            .events.single() as DebtRepaymentEventFact)
        assertNull(liability.outstandingAfterMinor)
        store.recordObservedOutstanding(liability.liability, 250_000L, time)
        repeat(3) { store.recordRepayment(liability, null, "invented-estimated-repay", null, "TEST", time + 60_000L) }
        val account = store.accounts().single { it.lastRepaymentMinor != null }
        assertEquals(200_000L, account.outstandingMinor)
        assertEquals(DebtBalanceQuality.ESTIMATED, account.balanceQuality)
        assertEquals(50_000L, account.lastRepaymentMinor)
    }
}
