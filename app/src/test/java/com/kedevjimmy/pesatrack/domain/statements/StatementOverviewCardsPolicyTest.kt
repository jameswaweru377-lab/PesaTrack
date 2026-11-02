package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementOverviewCardsPolicyTest {
    private val cash = StatementUnifiedOverviewPolicy.CashSummary(
        StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE, 150_000L, 100_000L)
    private val evidence = SharedAccountingViewPolicy.View(2026, 150_000L, 70_000L,
        150_000L, 100_000L, 30_000L, 2, 1, 0, 0,
        SharedAccountingViewPolicy.Status.INDEPENDENT_SCOPES)

    @Test fun grossMoneyOutIsNeverRenamedToExpenses() {
        val cards = StatementOverviewCardsPolicy.project(cash, evidence)
        assertEquals(150_000L, cards.moneyInMinor)
        assertEquals(100_000L, cards.grossMoneyOutMinor)
        assertNull(cards.verifiedExpensesMinor)
        assertEquals(30_000L, cards.reviewedTransfersMinor)
    }

    @Test fun ambiguousOrMismatchedYearEvidenceWithholdsTransferAmount() {
        assertNull(StatementOverviewCardsPolicy.project(cash, evidence.copy(
            status = SharedAccountingViewPolicy.Status.REVIEW_REQUIRED)).reviewedTransfersMinor)
        assertNull(StatementOverviewCardsPolicy.project(cash, evidence.copy(
            statementGrossOutMinor = 90_000L)).reviewedTransfersMinor)
    }

    @Test fun withheldGrossEvidenceDoesNotInventExpenseOrTransfer() {
        val unavailable = StatementOverviewCardsPolicy.project(
            StatementUnifiedOverviewPolicy.CashSummary(StatementUnifiedOverviewPolicy.CashStatus.NEEDS_REVIEW), evidence)
        assertNull(unavailable.moneyInMinor)
        assertNull(unavailable.grossMoneyOutMinor)
        assertNull(unavailable.verifiedExpensesMinor)
        assertNull(unavailable.reviewedTransfersMinor)
    }
}
