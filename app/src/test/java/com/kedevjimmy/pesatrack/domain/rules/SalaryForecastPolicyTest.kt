package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryForecastPolicyTest {
    private val day = 86_400_000L
    private val now = 100 * day

    @Test fun noSalaryEvidenceNeverProducesAnAmount() {
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
            confirmedPaydayEpochMs = null,
            confirmedSalaryAmountMinor = null
        ))
        assertEquals(SalaryForecastStatus.NEEDS_DATA, result.status)
        assertNull(result.lowMinor)
        assertTrue(result.missing.any { it.contains("payday", ignoreCase = true) })
    }

    @Test fun confirmedPaydayTakesPriorityOverEstimatedWindow() {
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
            expectedWindow = ExpectedPayWindow(now + 3 * day, now + 10 * day)
        ))
        assertEquals(SalaryForecastStatus.CONFIRMED, result.status)
        assertEquals(now + 5 * day, result.paydayStartEpochMs)
        assertEquals(now + 5 * day, result.paydayEndEpochMs)
        assertEquals(1_000_000L, result.lowMinor)
        assertEquals(2_500_000L, result.highMinor)
        assertTrue(result.excluded.any { it.contains("future salary", ignoreCase = true) })
    }

    @Test fun estimatedWindowWidensBothPaydayAndSpendingUncertainty() {
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
            confirmedPaydayEpochMs = null,
            confirmedSalaryAmountMinor = null,
            expectedWindow = ExpectedPayWindow(now + 3 * day, now + 7 * day)
        ))
        assertEquals(SalaryForecastStatus.ESTIMATED, result.status)
        assertEquals( -200_000L, result.lowMinor)
        assertEquals(3_100_000L, result.highMinor)
    }

    @Test fun noBalanceOrStaleBalanceSuppressesRange() {
        for (balanceTime in listOf(null, now - 2 * day)) {
            val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
                balanceMinor = if (balanceTime == null) null else 5_000_000L,
                balanceAsOfEpochMs = balanceTime
            ))
            assertEquals(SalaryForecastStatus.NEEDS_DATA, result.status)
            assertNull(result.lowMinor)
        }
    }

    @Test fun fewerThanThreeCompleteCyclesAreNotExtrapolated() {
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
            cycles = completeFacts().cycles.take(2)
        ))
        assertEquals(SalaryForecastStatus.NEEDS_DATA, result.status)
        assertTrue(result.missing.any { it.contains("three", ignoreCase = true) })
    }

    @Test fun partialAndUnreviewedCyclesAreNotTrusted() {
        val partial = completeFacts().cycles.first().copy(endEpochMs = null)
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
            cycles = listOf(partial) + completeFacts().cycles.drop(1)
        ))
        assertEquals(SalaryForecastStatus.NEEDS_DATA, result.status)
        val reviewed = SalaryForecastPolicy.evaluate(completeFacts().copy(
            cycles = completeFacts().cycles.mapIndexed { i, x -> if (i == 1) x.copy(unreviewedCount = 1) else x }
        ))
        assertEquals(SalaryForecastStatus.NEEDS_DATA, reviewed.status)
    }

    @Test fun overdueConfirmedDateAndOverdueWindowNeverRollForward() {
        val past = SalaryForecastPolicy.evaluate(completeFacts().copy(
            confirmedPaydayEpochMs = now - day
        ))
        assertEquals(SalaryForecastStatus.NEEDS_DATA, past.status)
        val window = SalaryForecastPolicy.evaluate(completeFacts().copy(
            confirmedPaydayEpochMs = null,
            confirmedSalaryAmountMinor = null,
            expectedWindow = ExpectedPayWindow(now - 10 * day, now - day)
        ))
        assertEquals(SalaryForecastStatus.NEEDS_DATA, window.status)
    }

    @Test fun activeDebtWithoutKnownDueDateSuppressesNumericResult() {
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(hasUnscheduledDebt = true))
        assertEquals(SalaryForecastStatus.NEEDS_DATA, result.status)
        assertTrue(result.missing.any { it.contains("loan", ignoreCase = true) })
    }

    @Test fun duplicatesAreCountedOnceAndNegativeMoneyIsShown() {
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
            balanceMinor = 1_000_000L,
            commitments = listOf(
                SalaryForecastCommitment("rent", "Rent", 1_000_000L, now + 2 * day),
                SalaryForecastCommitment("rent", "Rent", 1_000_000L, now + 2 * day)
            )
        ))
        assertEquals(-3_000_000L, result.lowMinor)
        assertEquals(-1_500_000L, result.highMinor)
        assertEquals(1, result.included.count { it.contains("Rent") })
    }

    @Test fun dueAfterPaydayDoesNotReduceTheForecast() {
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
            commitments = listOf(SalaryForecastCommitment("rent", "Rent", 1_000_000L, now + 8 * day))
        ))
        assertEquals(2_000_000L, result.lowMinor)
        assertEquals(3_500_000L, result.highMinor)
    }

    @Test fun overflowDoesNotDisplayWrappedMoney() {
        val result = SalaryForecastPolicy.evaluate(completeFacts().copy(
            balanceMinor = Long.MIN_VALUE
        ))
        assertEquals(SalaryForecastStatus.NEEDS_DATA, result.status)
        assertFalse(result.missing.isEmpty())
    }

    private fun completeFacts(): SalaryForecastFacts = SalaryForecastFacts(
        nowEpochMs = now,
        confirmedPaydayEpochMs = now + 5 * day,
        confirmedSalaryAmountMinor = 5_200_000L,
        expectedWindow = null,
        balanceMinor = 5_000_000L,
        balanceAsOfEpochMs = now - 60_000L,
        cycles = listOf(9_000_000L, 13_500_000L, 18_000_000L).mapIndexed { index, spend ->
            SalarySpendingCycle(
                startEpochMs = (index * 30L) * day,
                endEpochMs = (index * 30L + 30L) * day,
                variableSpendingMinor = spend
            )
        },
        commitments = listOf(SalaryForecastCommitment("rent", "Rent", 1_000_000L, now + 2 * day))
    )
}
