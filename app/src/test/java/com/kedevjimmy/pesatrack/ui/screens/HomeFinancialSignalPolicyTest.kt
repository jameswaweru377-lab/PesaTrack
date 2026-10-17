package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.*
import org.junit.Test

class HomeFinancialSignalPolicyTest {
    private fun example() = HomeFinancialSignalsInput(
        budgetLimitMinor = 1_000_000,
        budgetSpentMinor = 700_000,
        uncategorizedShare = .05,
        currentSpentMinor = 800_000,
        previousAlignedSpentMinor = 900_000,
        incomeMinor = 2_000_000,
        debtOutstandingMinor = 100_000,
        unknownDebtBalances = false,
        feesMinor = 10_000,
        currentTrustedRows = 35,
        previousTrustedRows = 30,
        pendingReviews = 0,
        confirmedAccountCount = 2,
        hasConfirmedSalaryCycle = true
    )

    @Test fun healthySufficientDataCanBeRated() {
        val result = evaluateHomeFinancialSignals(example())
        assertTrue(result.dataSufficient)
        assertNotNull(result.score)
        assertEquals("On track",result.status)
        assertEquals(4,result.indicators.size)
    }

    @Test fun missingSalaryNeverProducesAtRisk() {
        val result = evaluateHomeFinancialSignals(
            example().copy(hasConfirmedSalaryCycle=false,currentSpentMinor=9_000_000))
        assertNull(result.score)
        assertEquals("Limited data",result.status)
    }

    @Test fun emptyOrUntrustedCoverageSuppressesRiskClassification() {
        val inputs = listOf(
            example().copy(uncategorizedShare=.9),
            example().copy(pendingReviews=8),
            example().copy(unknownDebtBalances=true),
            example().copy(previousTrustedRows=0),
            example().copy(confirmedAccountCount=0),
            example().copy(budgetLimitMinor=null)
        )
        inputs.forEach {
            val result=evaluateHomeFinancialSignals(it)
            assertNull(result.score)
            assertEquals("Limited data",result.status)
        }
    }

    @Test fun corroboratedBudgetAndSpendingPressureMayShowAtRisk() {
        val result=evaluateHomeFinancialSignals(
            example().copy(budgetSpentMinor=2_400_000,
                currentSpentMinor=2_400_000,
                previousAlignedSpentMinor=900_000,
                incomeMinor=2_000_000,
                debtOutstandingMinor=2_000_000,
                feesMinor=120_000))
        assertTrue(result.dataSufficient)
        assertEquals("At risk",result.status)
        assertTrue(result.indicators.count { (it.score?:100)<40 }>=2)
    }

    @Test fun negativeAndZeroAmountsDoNotCauseInvalidScores() {
        val result=evaluateHomeFinancialSignals(
            example().copy(currentSpentMinor=-1, feesMinor=-1,
                budgetLimitMinor=0, debtOutstandingMinor=-1))
        assertEquals("Limited data",result.status)
        assertNull(result.score)
    }
}
