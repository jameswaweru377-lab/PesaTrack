package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementUnifiedOverviewPolicyTest {
    private fun month(
        account: String, key: String, status: StatementSourceCoveragePolicy.Status,
        currency: String = "KES", repeats: Int = 0, unresolved: Int = 0,
        incoming: Long = 8_000L, outgoing: Long = 5_000L
    ) = StatementSourceCoveragePolicy.MonthCoverage(
        accountId = account,
        currency = currency,
        monthKey = key,
        sourceCount = if (repeats > 0 || unresolved > 0) 2 else 1,
        observedLegCount = 2,
        remainingObservedLegCount = 2 - repeats,
        confirmedRepeatCopies = repeats,
        unresolvedCandidateCount = unresolved,
        status = status,
        safeMoneyInMinor = if (status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW) null else incoming,
        safeMoneyOutMinor = if (status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW) null else outgoing
    )

    @Test fun monthsOnFileCountsUnionAcrossAccountsNotTransactionLegs() {
        val result = StatementUnifiedOverviewPolicy.project(
            2026, listOf(
                month("wallet", "2026-01", StatementSourceCoveragePolicy.Status.VERIFIED_REPEAT, repeats = 1),
                month("bank", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE),
                month("bank", "2026-02", StatementSourceCoveragePolicy.Status.COMPLEMENTARY)
            )
        )
        assertEquals(2, result.accountRows.size)
        assertEquals(2, result.monthsOnFile)
        assertEquals(0, result.monthsNeedingReview)
        assertEquals(1, result.verifiedRepeatCopies)
        assertEquals(24, result.accountMonths.size)
        assertEquals(setOf("bank", "wallet"), result.accountRows.map { it.accountId }.toSet())
    }

    @Test fun missingMonthIsNoImportNotZeroCashFlow() {
        val result = StatementUnifiedOverviewPolicy.project(2026, listOf(
            month("wallet", "2026-09", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE)
        ))
        val missing = result.accountMonths.single { it.accountId == "wallet" && it.month == 10 }
        assertEquals(StatementUnifiedOverviewPolicy.MonthState.NO_IMPORT, missing.state)
        assertEquals(0, missing.observedLegs)
        assertEquals(1, result.monthsOnFile)
        assertEquals(11, result.accountMonths.count { it.state == StatementUnifiedOverviewPolicy.MonthState.NO_IMPORT })
    }

    @Test fun uncertainBankOverlapWithholdsGlobalAmountAndRequiresReview() {
        val result = StatementUnifiedOverviewPolicy.project(2026, listOf(
            month("bank", "2026-09", StatementSourceCoveragePolicy.Status.NEEDS_REVIEW, unresolved = 2),
            month("wallet", "2026-09", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE)
        ))
        assertEquals(1, result.monthsOnFile)
        assertEquals(1, result.monthsNeedingReview)
        assertEquals(2, result.unresolvedCandidates)
        assertEquals(StatementUnifiedOverviewPolicy.MonthState.NEEDS_REVIEW,
            result.accountMonths.single { it.accountId == "bank" && it.month == 9 }.state)
        assertEquals(StatementUnifiedOverviewPolicy.CashStatus.NEEDS_REVIEW, result.cashSummary.status)
        assertNull(result.cashSummary.moneyInMinor)
        assertNull(result.cashSummary.moneyOutMinor)
    }

    @Test fun safeAccountCashLegsSumAcrossImportedPeriodsWithoutCountingRepeatedCopies() {
        val result = StatementUnifiedOverviewPolicy.project(2026, listOf(
            month("wallet", "2026-01", StatementSourceCoveragePolicy.Status.VERIFIED_REPEAT,
                repeats = 1, incoming = 12_000L, outgoing = 4_000L),
            month("bank", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE,
                incoming = 9_000L, outgoing = 6_000L),
            month("bank", "2026-02", StatementSourceCoveragePolicy.Status.COMPLEMENTARY,
                incoming = 2_000L, outgoing = 1_000L),
            month("wallet", "2025-12", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE,
                incoming = 300_000L, outgoing = 300_000L)
        ))
        assertEquals(StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE, result.cashSummary.status)
        assertEquals(23_000L, result.cashSummary.moneyInMinor)
        assertEquals(11_000L, result.cashSummary.moneyOutMinor)
        assertEquals(1, result.verifiedRepeatCopies)
    }

    @Test fun differentCurrenciesWithholdInsteadOfAddingUnconvertibleMoney() {
        val result = StatementUnifiedOverviewPolicy.project(2026, listOf(
            month("wallet", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE),
            month("bank", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE,
                currency = "USD")
        ))
        assertEquals(StatementUnifiedOverviewPolicy.CashStatus.UNSUPPORTED_CURRENCY,
            result.cashSummary.status)
        assertNull(result.cashSummary.moneyInMinor)
        assertNull(result.cashSummary.moneyOutMinor)
    }

    @Test fun unverifiedNullAmountsWithholdAllTotalsDespiteOnFileStatus() {
        val incomplete = month("wallet", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE)
            .copy(safeMoneyInMinor = null)
        val result = StatementUnifiedOverviewPolicy.project(2026, listOf(incomplete))
        assertEquals(StatementUnifiedOverviewPolicy.CashStatus.NEEDS_REVIEW, result.cashSummary.status)
        assertNull(result.cashSummary.moneyInMinor)
        assertNull(result.cashSummary.moneyOutMinor)
    }

    @Test fun duplicateCoverageKeysCannotAccidentallyDoubleAllAccountTotals() {
        val repeated = month("wallet", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE)
        val result = StatementUnifiedOverviewPolicy.project(2026, listOf(repeated, repeated))
        assertEquals(StatementUnifiedOverviewPolicy.CashStatus.NEEDS_REVIEW, result.cashSummary.status)
        assertNull(result.cashSummary.moneyInMinor)
    }

    @Test fun overflowingAmountSumIsWithheldInsteadOfWrappingNegative() {
        val result = StatementUnifiedOverviewPolicy.project(2026, listOf(
            month("wallet", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE,
                incoming = Long.MAX_VALUE),
            month("bank", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE,
                incoming = 1L)
        ))
        assertEquals(StatementUnifiedOverviewPolicy.CashStatus.NEEDS_REVIEW, result.cashSummary.status)
        assertNull(result.cashSummary.moneyInMinor)
    }

    @Test fun differentCurrenciesAndPreviousYearsDoNotInflateMonthCounts() {
        val result = StatementUnifiedOverviewPolicy.project(2026, listOf(
            month("bank", "2026-01", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE),
            month("bank", "2026-01", StatementSourceCoveragePolicy.Status.NEEDS_REVIEW,
                currency = "USD", unresolved = 1),
            month("bank", "2025-12", StatementSourceCoveragePolicy.Status.SINGLE_SOURCE)
        ))
        assertEquals(1, result.monthsOnFile)
        assertEquals(1, result.monthsNeedingReview)
        assertEquals(1, result.unresolvedCandidates)
        assertEquals(1, result.accountRows.size)
    }

    @Test fun emptyReadModelProducesNoAccountsNotTwelveInventedMonths() {
        val result = StatementUnifiedOverviewPolicy.project(2026, emptyList())
        assertTrue(result.accountRows.isEmpty())
        assertTrue(result.accountMonths.isEmpty())
        assertEquals(0, result.monthsOnFile)
        assertEquals(0, result.monthsNeedingReview)
        assertEquals(StatementUnifiedOverviewPolicy.CashStatus.NO_IMPORT, result.cashSummary.status)
        assertNull(result.cashSummary.moneyInMinor)
        assertNull(result.cashSummary.moneyOutMinor)
    }
}
