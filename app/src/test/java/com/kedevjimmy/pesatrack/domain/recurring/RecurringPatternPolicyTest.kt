package com.kedevjimmy.pesatrack.domain.recurring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringPatternPolicyTest {
    private val dayMs =
        24L * 60L * 60L * 1000L

    @Test
    fun stableMonthlyPatternBecomesHighConfidence() {
        val observations =
            listOf(
                observation(
                    id = 1,
                    day = 0,
                    amount = 1_500_00L
                ),
                observation(
                    id = 2,
                    day = 30,
                    amount = 1_500_00L
                ),
                observation(
                    id = 3,
                    day = 60,
                    amount = 1_500_00L
                ),
                observation(
                    id = 4,
                    day = 90,
                    amount = 1_500_00L
                )
            )

        val candidates =
            RecurringPatternPolicy.detect(
                observations =
                    observations,
                nowEpochMs =
                    100L * dayMs
            )

        assertEquals(
            1,
            candidates.size
        )
        val candidate =
            candidates.single()
        assertEquals(
            RecurringPatternConfidence.HIGH,
            candidate.confidence
        )
        assertEquals(
            30,
            candidate.medianIntervalDays
        )
        assertEquals(
            1_500_00L,
            candidate.typicalAmountMinor
        )
        assertEquals(
            4,
            candidate.occurrenceCount
        )
    }

    @Test
    fun threeMonthlyPaymentsCanProduceMediumSuggestion() {
        val observations =
            listOf(
                observation(
                    id = 1,
                    day = 0,
                    amount = 900_00L
                ),
                observation(
                    id = 2,
                    day = 31,
                    amount = 950_00L
                ),
                observation(
                    id = 3,
                    day = 62,
                    amount = 920_00L
                )
            )

        val candidate =
            RecurringPatternPolicy.detect(
                observations =
                    observations,
                nowEpochMs =
                    70L * dayMs
            ).single()

        assertEquals(
            RecurringPatternConfidence.MEDIUM,
            candidate.confidence
        )
        assertEquals(
            31,
            candidate.medianIntervalDays
        )
        assertEquals(
            920_00L,
            candidate.typicalAmountMinor
        )
    }

    @Test
    fun frequentMerchantUsageIsNotTreatedAsMonthlyRecurring() {
        val observations =
            listOf(
                observation(
                    id = 1,
                    day = 0,
                    amount = 500_00L
                ),
                observation(
                    id = 2,
                    day = 7,
                    amount = 500_00L
                ),
                observation(
                    id = 3,
                    day = 14,
                    amount = 500_00L
                ),
                observation(
                    id = 4,
                    day = 30,
                    amount = 500_00L
                )
            )

        assertTrue(
            RecurringPatternPolicy.detect(
                observations =
                    observations,
                nowEpochMs =
                    35L * dayMs
            ).isEmpty()
        )
    }

    @Test
    fun highlyVolatileAmountsAreNotPromotedToCommitments() {
        val observations =
            listOf(
                observation(
                    id = 1,
                    day = 0,
                    amount = 1_000_00L
                ),
                observation(
                    id = 2,
                    day = 30,
                    amount = 4_000_00L
                ),
                observation(
                    id = 3,
                    day = 60,
                    amount = 1_000_00L
                )
            )

        assertTrue(
            RecurringPatternPolicy.detect(
                observations =
                    observations,
                nowEpochMs =
                    70L * dayMs
            ).isEmpty()
        )
    }

    @Test
    fun stalePatternDoesNotKeepNaggingTheUser() {
        val observations =
            listOf(
                observation(
                    id = 1,
                    day = 0,
                    amount = 1_000_00L
                ),
                observation(
                    id = 2,
                    day = 30,
                    amount = 1_000_00L
                ),
                observation(
                    id = 3,
                    day = 60,
                    amount = 1_000_00L
                )
            )

        assertTrue(
            RecurringPatternPolicy.detect(
                observations =
                    observations,
                nowEpochMs =
                    140L * dayMs
            ).isEmpty()
        )
    }

    private fun observation(
        id: Long,
        day: Int,
        amount: Long
    ) =
        RecurringPatternObservation(
            transactionId = id,
            identityKey =
                "counterparty:example",
            label =
                "Example Internet",
            categoryId =
                "INTERNET",
            amountMinor = amount,
            occurredAtEpochMs =
                day.toLong() *
                    dayMs,
            counterparty =
                "Example Internet",
            counterpartyPhone = null,
            accountId =
                "account-mpesa"
        )
}
