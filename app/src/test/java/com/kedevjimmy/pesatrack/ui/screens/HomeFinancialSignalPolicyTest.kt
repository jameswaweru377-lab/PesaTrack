package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.MonthlyBudget
import org.junit.Assert.*
import org.junit.Test

/** Entirely invented minor-unit amounts, no personal financial fixtures. */
class HomeFinancialSignalPolicyTest {
    private fun complete() = HomeFinancialSignalsInput(
        budgetLimitMinor=1_000_000L,
        budgetSpentMinor=450_000L,
        uncategorizedShare=.05,
        currentSpentMinor=800_000L,
        previousAlignedSpentMinor=900_000L,
        incomeMinor=2_000_000L,
        debtOutstandingMinor=1_000_000L,
        unknownDebtBalances=false,
        feesMinor=10_000L,
        currentTrustedRows=35,
        previousTrustedRows=30,
        pendingReviews=0,
        confirmedAccountCount=2,
        hasConfirmedSalaryCycle=true,
        debtAccountCount=1,
        remainingCommitmentsMinor=100_000L,
        confirmedEmergencyReserveMinor=900_000L,
        confirmedEssentialCostsMinor=300_000L,
        confirmedMonthlyDebtPaymentsMinor=100_000L,
        elapsedMonthFraction=.60,
        isCurrentMonth=true
    )

    @Test fun financialHealthTotalsUseOnlyMonthAlignedBudgets() {
        val totals = financialHealthBudgetTotals(
            budgets = listOf(
                MonthlyBudget("transport", 100_000L, periodLabel = "This week"),
                MonthlyBudget("FOOD", 500_000L, periodLabel = "This month")
            ),
            spendingByCategory = mapOf("TRANSPORT" to 40_000L, "FOOD" to 250_000L),
            canonicalCategoryId = { it.uppercase() }
        )

        assertEquals(500_000L, totals.limitMinor)
        assertEquals(250_000L, totals.spentMinor)
    }

    @Test fun weeklyBudgetAloneCannotCreateAMonthlySpendingPaceRating() {
        val totals = financialHealthBudgetTotals(
            budgets = listOf(MonthlyBudget("TRANSPORT", 100_000L, periodLabel = "This week")),
            spendingByCategory = mapOf("TRANSPORT" to 40_000L),
            canonicalCategoryId = { it }
        )

        assertNull(totals.limitMinor)
        assertNull(totals.spentMinor)
        val health = evaluateHomeFinancialSignals(
            complete().copy(
                budgetLimitMinor = totals.limitMinor,
                budgetSpentMinor = totals.spentMinor
            )
        )
        assertNull(health.indicators[3].score)
    }

    @Test fun unavailableBudgetSpendingEvidenceIsNotTreatedAsZero() {
        val totals = financialHealthBudgetTotals(
            budgets = listOf(MonthlyBudget("FOOD", 500_000L, periodLabel = "This month")),
            spendingByCategory = null,
            canonicalCategoryId = { it }
        )

        assertEquals(500_000L, totals.limitMinor)
        assertNull(totals.spentMinor)
        val health = evaluateHomeFinancialSignals(
            complete().copy(
                budgetLimitMinor = totals.limitMinor,
                budgetSpentMinor = totals.spentMinor
            )
        )
        assertNull(health.indicators[3].score)
    }

    @Test fun fullEvidenceOffersAQualifiedSummaryAndFourPillars() {
        val result=evaluateHomeFinancialSignals(complete())
        assertTrue(result.dataSufficient)
        assertEquals("On track",result.status)
        assertNotNull(result.score)
        assertEquals(listOf("Cash flow","Savings","Debt","Spending"),
            result.indicators.map { it.title })
        assertEquals(100,result.confidence.score)
        assertTrue(result.actions.any { it.title.contains("commitments") })
    }

    @Test fun aMissingEmergencyReserveCannotBecomeHealthyZero() {
        val result=evaluateHomeFinancialSignals(
            complete().copy(confirmedEmergencyReserveMinor=null))
        assertFalse(result.dataSufficient)
        assertNull(result.score)
        assertNull(result.indicators[1].score)
        assertTrue(result.actions.any { it.title=="Review emergency savings" })
    }

    @Test fun transfersAndUnverifiedBankBalancesCannotBeEmergencySavings() {
        val result=evaluateHomeFinancialSignals(
            complete().copy(confirmedEssentialCostsMinor=null,
                confirmedEmergencyReserveMinor=5_000_000L))
        assertNull(result.indicators[1].score)
        assertNull(result.score)
    }

    @Test fun debtPrincipalIsNotAProxyForMonthlyRepayments() {
        val samePayments=evaluateHomeFinancialSignals(complete())
        val hugePrincipal=evaluateHomeFinancialSignals(
            complete().copy(debtOutstandingMinor=90_000_000L))
        assertEquals(samePayments.indicators[2].score, hugePrincipal.indicators[2].score)
        assertNotNull(samePayments.indicators[2].score)
    }

    @Test fun existingDebtWithZeroOrUnknownRequiredPaymentsCannotLookAffordable() {
        listOf<Long?>(null,0L).forEach { payment ->
            val result=evaluateHomeFinancialSignals(
                complete().copy(confirmedMonthlyDebtPaymentsMinor=payment))
            assertNull(result.indicators[2].score)
            assertNull(result.score)
            assertTrue(result.actions.any { it.destination=="debts" })
        }
    }

    @Test fun explicitlyNoRepaymentsAndNoRecordedActiveDebtIsNotInferredAutomatically() {
        val unknown=evaluateHomeFinancialSignals(
            complete().copy(debtAccountCount=0,confirmedMonthlyDebtPaymentsMinor=null))
        assertNull(unknown.indicators[2].score)
        val reported=evaluateHomeFinancialSignals(
            complete().copy(debtAccountCount=0,confirmedMonthlyDebtPaymentsMinor=0L))
        assertNotNull(reported.indicators[2].score)
        assertTrue(reported.indicators[2].reason.contains("Unrecorded"))
    }

    @Test fun limitedOrUnreviewedActivityCannotProduceAFullRating() {
        listOf(
            complete().copy(currentTrustedRows=0),
            complete().copy(previousTrustedRows=0),
            complete().copy(pendingReviews=1),
            complete().copy(confirmedAccountCount=0),
            complete().copy(hasConfirmedSalaryCycle=false),
            complete().copy(uncategorizedShare=.80)
        ).forEach { input ->
            val result=evaluateHomeFinancialSignals(input)
            assertNull(result.score)
            assertFalse(result.dataSufficient)
        }
    }

    @Test fun aSingleFinancialReviewMakesOverallProvisionalButKeepsUsefulSignals() {
        val result=evaluateHomeFinancialSignals(complete().copy(pendingReviews=1))
        assertNull(result.score)
        assertNotNull(result.indicators[0].score)
        assertNotNull(result.indicators[3].score)
        assertTrue(result.indicators[0].reason.contains("Provisional"))
        assertTrue(result.actions.any { it.destination=="activity" })
    }

    @Test fun spendingIsAssessedAgainstElapsedMonthNotJustAbsoluteBudget() {
        val mid=evaluateHomeFinancialSignals(complete())
        val quick=evaluateHomeFinancialSignals(complete().copy(elapsedMonthFraction=.30))
        assertTrue(quick.indicators[3].score!! < mid.indicators[3].score!!)
        val early=evaluateHomeFinancialSignals(complete().copy(elapsedMonthFraction=.10))
        assertNull(early.indicators[3].score)
        assertNull(early.score)
    }

    @Test fun risingSpendingAloneDoesNotImplyStressWhenIncomeCanCoverIt() {
        val high=evaluateHomeFinancialSignals(
            complete().copy(currentSpentMinor=1_200_000L,incomeMinor=4_000_000L))
        assertTrue(high.indicators[0].score!! >= 80)
    }

    @Test fun actualExpenseAboveRecordedIncomeTriggersActionNotGuesswork() {
        val result=evaluateHomeFinancialSignals(
            complete().copy(currentSpentMinor=3_000_000L))
        assertTrue(result.actions.any { it.title=="Recheck outgoing costs" })
        assertTrue(result.indicators[0].score!! < 65)
    }

    @Test fun corruptedMoneyOrUnknownPeriodsDoNotProduceFinancialRatings() {
        val invalid=evaluateHomeFinancialSignals(
            complete().copy(budgetLimitMinor=0L, confirmedEssentialCostsMinor=-1L,
                confirmedMonthlyDebtPaymentsMinor=-1L,elapsedMonthFraction=Double.NaN))
        assertNull(invalid.score)
        assertNull(invalid.indicators[1].score)
        assertNull(invalid.indicators[2].score)
        assertNull(invalid.indicators[3].score)
        val historical=evaluateHomeFinancialSignals(complete().copy(isCurrentMonth=false))
        assertNull(historical.score)
        assertTrue(historical.indicators.all { it.score == null })
    }
}
