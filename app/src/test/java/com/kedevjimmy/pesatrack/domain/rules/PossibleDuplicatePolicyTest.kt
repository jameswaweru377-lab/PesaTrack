package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PossibleDuplicatePolicyTest {
    @Test
    fun codeLessCopiesFromDifferentSourcesWithinSecondsAreFlagged() {
        assertTrue(
            PossibleDuplicatePolicy.isCandidate(
                sample(
                    id = 1,
                    amount = 98_500L,
                    receivedAt = 1_000_000L,
                    source = "com.messages"
                ),
                sample(
                    id = 2,
                    amount = 98_500L,
                    receivedAt = 1_012_000L,
                    source = "com.truecaller"
                )
            )
        )
    }

    @Test
    fun codedAndCodeLessPairCanBeReviewedWithinNinetySeconds() {
        assertTrue(
            PossibleDuplicatePolicy.isCandidate(
                sample(
                    id = 1,
                    amount = 12_500L,
                    receivedAt = 1_000_000L,
                    source = "com.safaricom.mpesa",
                    code = "TQK12ABC34"
                ),
                sample(
                    id = 2,
                    amount = 12_500L,
                    receivedAt = 1_070_000L,
                    source = "com.bank"
                )
            )
        )
    }

    @Test
    fun complementaryDirectionsCanBeReviewedWithinOneMinute() {
        assertTrue(
            PossibleDuplicatePolicy.isCandidate(
                sample(
                    id = 1,
                    amount = 98_500L,
                    receivedAt = 1_000_000L,
                    source = "com.mpesa",
                    direction = "EXPENSE"
                ),
                sample(
                    id = 2,
                    amount = 98_500L,
                    receivedAt = 1_050_000L,
                    source = "com.bank",
                    direction = "INCOME"
                )
            )
        )
    }

    @Test
    fun distinctRealMpesaCodesAreNeverFlagged() {
        assertFalse(
            PossibleDuplicatePolicy.isCandidate(
                sample(
                    id = 1,
                    amount = 12_500L,
                    receivedAt = 1_000_000L,
                    source = "com.mpesa",
                    code = "AAA1111111"
                ),
                sample(
                    id = 2,
                    amount = 12_500L,
                    receivedAt = 1_003_000L,
                    source = "com.bank",
                    code = "BBB2222222"
                )
            )
        )
    }

    @Test
    fun sameSourceTimeAloneDoesNotCreateDuplicateReview() {
        assertFalse(
            PossibleDuplicatePolicy.isCandidate(
                sample(
                    id = 1,
                    amount = 12_500L,
                    receivedAt = 1_000_000L,
                    source = "com.bank"
                ),
                sample(
                    id = 2,
                    amount = 12_500L,
                    receivedAt = 1_004_000L,
                    source = "com.bank"
                )
            )
        )
    }

    @Test
    fun differentKnownIdentitiesAreNotFlagged() {
        assertFalse(
            PossibleDuplicatePolicy.isCandidate(
                sample(
                    id = 1,
                    amount = 12_500L,
                    receivedAt = 1_000_000L,
                    source = "com.mpesa",
                    counterparty = "Alice"
                ),
                sample(
                    id = 2,
                    amount = 12_500L,
                    receivedAt = 1_004_000L,
                    source = "com.bank",
                    counterparty = "Bob"
                )
            )
        )
    }

    @Test
    fun reversalIsNotAPossibleDuplicateEvenWhenAmountTimeAndSourcesMatch() {
        val payment = sample(
            id = 70, amount = 44_000L, receivedAt = 1_000_000L,
            source = "com.messages", type = "SEND_MONEY"
        )
        val reversal = sample(
            id = 71, amount = 44_000L, receivedAt = 1_003_000L,
            source = "com.safaricom.mpesa", type = "REVERSAL"
        )
        assertFalse(PossibleDuplicatePolicy.isCandidate(payment, reversal))
        assertFalse(PossibleDuplicatePolicy.isCandidate(reversal, payment))
        assertFalse(PossibleDuplicatePolicy.isCandidate(reversal,
            reversal.copy(id = 72, sourcePackage = "com.bank")))
    }

    @Test
    fun parsePairIdsRejectsMalformedSelfPairsAndNoncanonicalTokens() {
        assertEquals(1L to 2L, PossibleDuplicatePolicy.parsePairIds("1:2"))
        assertEquals(2L to 1L, PossibleDuplicatePolicy.parsePairIds("2:1"))
        for (key in listOf("1:bad:2", "1:2:3", "1:01", "0:2", "-1:2", "2:2", "1:", ":2", " 1:2", "1:2 ", "")) {
            assertEquals("Must reject malformed pair: $key", null, PossibleDuplicatePolicy.parsePairIds(key))
        }
    }

    @Test
    fun currentConfirmationRequiresTheExactDisplayedObservations() {
        val first = sample(id = 1, amount = 12_500L, receivedAt = 1_000_000L, source = "com.mpesa",
            code = "ABC12345")
        val second = sample(id = 2, amount = 12_500L, receivedAt = 1_002_000L, source = "com.bank")
        assertTrue(PossibleDuplicatePolicy.canConfirm("1:2", first, second, first, second))
        assertTrue(PossibleDuplicatePolicy.canConfirm("2:1", second, first, second, first))

        val changed = listOf(
            second.copy(amountMinor = 12_600L),
            second.copy(reviewState = "USER_CLASSIFIED"),
            second.copy(reviewState = "EXCLUDED"),
            second.copy(type = "REVERSAL"),
            second.copy(receivedAtEpochMs = 1_110_000L),
            second.copy(sourcePackage = "com.mpesa"),
            second.copy(direction = "INCOME"),
            second.copy(transactionCode = "NEWCODE1"),
            second.copy(counterparty = "Different merchant")
        )
        changed.forEach { current ->
            assertFalse("Changed observations must be stale: $current",
                PossibleDuplicatePolicy.canConfirm("1:2", first, second, first, current))
        }
        assertFalse(PossibleDuplicatePolicy.canConfirm("2:1", first, second, first, second))
        assertFalse(PossibleDuplicatePolicy.canConfirm("1:bad:2", first, second, first, second))
    }

    @Test
    fun changedOrProtectedPairCannotBeConfirmedEvenIfItsChangedVersionIsShown() {
        val first = sample(id = 10, amount = 30_000L, receivedAt = 1_000_000L,
            source = "com.messages")
        val second = sample(id = 11, amount = 30_000L, receivedAt = 1_004_000L,
            source = "com.bank")
        val invalidCurrentPairs = listOf(
            first.copy(type = "REVERSAL") to second,
            first.copy(reviewState = "USER_CLASSIFIED") to second,
            first.copy(amountMinor = 10_000L) to second,
            first to second.copy(sourcePackage = "com.messages"),
            first to second.copy(receivedAtEpochMs = 1_200_000L)
        )
        invalidCurrentPairs.forEach { (a, b) ->
            assertFalse(PossibleDuplicatePolicy.canConfirm("10:11", a, b, a, b))
        }
    }

    private fun sample(
        id: Long,
        amount: Long,
        receivedAt: Long,
        source: String,
        code: String? = null,
        direction: String = "NEUTRAL",
        counterparty: String? = null,
        type: String = "OTHER"
    ) = PossibleDuplicateObservation(
        id = id,
        amountMinor = amount,
        receivedAtEpochMs = receivedAt,
        transactionCode = code,
        sourcePackage = source,
        sourceKind = "notification",
        direction = direction,
        reviewState = "NEEDS_REVIEW",
        category = "UNCATEGORIZED",
        counterparty = counterparty,
        evidenceLabel = null,
        type = type
    )
}
