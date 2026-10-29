package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementUnifiedOverviewPolicyTest {
    private fun month(
        account: String, key: String, status: StatementSourceCoveragePolicy.Status,
        currency: String = "KES", repeats: Int = 0, unresolved: Int = 0
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
        safeMoneyInMinor = if (status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW) null else 8_000L,
        safeMoneyOutMinor = if (status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW) null else 5_000L
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
        assertFalse("An all-accounts money total is not safe before transfer pairing",
            result.javaClass.declaredFields.any { it.name.contains("money", ignoreCase = true) ||
                it.name.contains("spending", ignoreCase = true) })
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
    }
}
