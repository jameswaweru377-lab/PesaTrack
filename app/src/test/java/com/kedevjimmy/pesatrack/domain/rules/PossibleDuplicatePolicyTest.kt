package com.kedevjimmy.pesatrack.domain.rules

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

    private fun sample(
        id: Long,
        amount: Long,
        receivedAt: Long,
        source: String,
        code: String? = null,
        direction: String = "NEUTRAL",
        counterparty: String? = null
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
        type = "OTHER"
    )
}
