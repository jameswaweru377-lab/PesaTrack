package com.kedevjimmy.pesatrack.domain.recurring

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringPatternPolicyV2Test {
    private val zone = TimeZone.getTimeZone("Africa/Nairobi")

    @Test
    fun fixedFifthDoesNotDriftWithMonthLengthsOrTwoDayJitter() {
        val candidate = detect(
            "2026-01-05", "2026-02-06", "2026-03-04", "2026-04-05"
        ).single()
        assertEquals(RecurringPatternCadence.MONTHLY_FIXED_DAY, candidate.cadence)
        assertEquals("2026-05-05", day(candidate.nextExpectedAtEpochMs))
        assertTrue(candidate.reason.contains("5th"))
        assertEquals(RecurringPatternConfidence.HIGH, candidate.confidence)
    }

    @Test
    fun monthEndFollowsRealMonthEndsIncludingLeapFebruary() {
        val candidate = detect(
            "2024-01-31", "2024-02-29", "2024-03-31", "2024-04-30"
        ).single()
        assertEquals(RecurringPatternCadence.MONTHLY_LAST_DAY, candidate.cadence)
        assertEquals("2024-05-31", day(candidate.nextExpectedAtEpochMs))
        assertTrue(candidate.reason.contains("last day"))
    }

    @Test
    fun oneMissingMonthKeepsSuggestionButLowersConfidence() {
        val candidate = detect(
            "2026-01-05", "2026-02-05", "2026-04-05", "2026-05-05"
        ).single()
        assertEquals(RecurringPatternCadence.MONTHLY_FIXED_DAY, candidate.cadence)
        assertEquals("2026-06-05", day(candidate.nextExpectedAtEpochMs))
        assertEquals(RecurringPatternConfidence.MEDIUM, candidate.confidence)
        assertTrue(candidate.reason.contains("one skipped"))
    }

    @Test
    fun fourWeeklyPaymentsGiveWeeklySuggestion() {
        val candidate = detect(
            "2026-01-01", "2026-01-08", "2026-01-15", "2026-01-22"
        ).single()
        assertEquals(RecurringPatternCadence.WEEKLY, candidate.cadence)
        assertEquals("2026-01-29", day(candidate.nextExpectedAtEpochMs))
        assertEquals(RecurringPatternConfidence.HIGH, candidate.confidence)
        assertTrue(candidate.reason.contains("every week"))
    }

    @Test
    fun fourFortnightlyPaymentsAreNotMistakenForWeekly() {
        val candidate = detect(
            "2026-01-01", "2026-01-15", "2026-01-29", "2026-02-12"
        ).single()
        assertEquals(RecurringPatternCadence.FORTNIGHTLY, candidate.cadence)
        assertEquals("2026-02-26", day(candidate.nextExpectedAtEpochMs))
        assertTrue(candidate.reason.contains("two weeks"))
    }

    @Test
    fun oneMissedWeekDoesNotResetTheWeeklyAnchor() {
        val candidate = detect(
            "2026-01-01", "2026-01-08", "2026-01-22", "2026-01-29"
        ).single()
        assertEquals(RecurringPatternCadence.WEEKLY, candidate.cadence)
        assertEquals("2026-02-05", day(candidate.nextExpectedAtEpochMs))
        assertEquals(RecurringPatternConfidence.MEDIUM, candidate.confidence)
    }

    @Test
    fun threeTermsPerYearAreRecognizedWithoutMonthlyAssumptions() {
        val candidate = detect(
            "2025-01-15", "2025-05-15", "2025-09-15", "2026-01-15"
        ).single()
        assertEquals(RecurringPatternCadence.TERMLY, candidate.cadence)
        assertEquals("2026-05-15", day(candidate.nextExpectedAtEpochMs))
        assertTrue(candidate.reason.contains("term"))
    }

    @Test
    fun oneMissedTermKeepsTermlySuggestion() {
        val candidate = detect(
            "2024-01-15", "2024-05-15", "2025-01-15", "2025-05-15"
        ).single()
        assertEquals(RecurringPatternCadence.TERMLY, candidate.cadence)
        assertEquals("2025-09-15", day(candidate.nextExpectedAtEpochMs))
        assertEquals(RecurringPatternConfidence.MEDIUM, candidate.confidence)
    }

    @Test
    fun repeatedShopVisitsAtIrregularIntervalsNeverCountAsRecurring() {
        assertTrue(detect(
            "2026-01-01", "2026-01-03", "2026-01-08", "2026-01-14",
            "2026-01-19", "2026-01-26", category = "SHOPPING"
        ).isEmpty())
    }

    @Test
    fun twoPaymentsAreNotEnoughToSuggestAnyCadence() {
        assertTrue(detect("2026-01-05", "2026-02-05").isEmpty())
    }

    @Test
    fun volatileAmountsDoNotCreateCommitmentSuggestions() {
        assertTrue(detect(
            "2026-01-05", "2026-02-05", "2026-03-05",
            amounts = listOf(100_00L, 990_00L, 100_00L)
        ).isEmpty())
    }

    @Test
    fun staleMonthlyHistoryIsNotSuggestedForever() {
        assertTrue(detect(
            "2025-01-05", "2025-02-05", "2025-03-05", "2025-04-05",
            now = "2025-12-01"
        ).isEmpty())
    }

    @Test
    fun existingLiveDetectorRemainsMonthlyOnlyUntilManualUiIntegration() {
        val observations = observations(
            listOf("2026-01-01", "2026-01-08", "2026-01-15", "2026-01-22")
        )
        assertTrue(RecurringPatternPolicy.detect(observations, epoch("2026-01-25")).isEmpty())
        assertFalse(RecurringPatternPolicy.detectV2(observations, epoch("2026-01-25")).isEmpty())
    }

    private fun detect(
        vararg dates: String,
        amounts: List<Long> = List(dates.size) { 1_500_00L },
        category: String = "RENT",
        now: String = dates.last()
    ): List<RecurringPatternCandidate> = RecurringPatternPolicy.detectV2(
        observations(dates.toList(), amounts, category), epoch(now)
    )

    private fun observations(
        dates: List<String>,
        amounts: List<Long> = List(dates.size) { 1_500_00L },
        category: String = "RENT"
    ): List<RecurringPatternObservation> = dates.mapIndexed { index, date ->
        RecurringPatternObservation(
            transactionId = index.toLong() + 1,
            identityKey = "exam:invented-recurring",
            label = "Exam Fictional Commitment",
            categoryId = category,
            amountMinor = amounts[index],
            occurredAtEpochMs = epoch(date),
            counterparty = "Exam Fictional Recipient",
            counterpartyPhone = null,
            accountId = "exam-account"
        )
    }

    private fun epoch(date: String): Long = formatter().parse(date)!!.time

    private fun day(instant: Long): String = formatter().format(instant)

    private fun formatter() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = zone
        isLenient = false
    }
}
