package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefundMatchingPolicyTest {
    @Test
    fun partialCompletedRefundNeedsReviewWithoutAutomaticMatching() {
        val payment = RefundCandidate(41L, 120_000L, 22_000L, "SAMPLE SHOP")
        assertNull(RefundMatchingPolicy.chooseCandidate(8_000L, 600_000L, "SAMPLE SHOP", listOf(payment)))
        assertEquals(
            RefundReviewReason.PARTIAL,
            RefundMatchingPolicy.reviewReason(8_000L, 600_000L, "SAMPLE SHOP", listOf(payment))
        )
    }

    @Test
    fun ambiguousRefundNeedsReviewRatherThanSelectingOnePurchase() {
        val payments = listOf(
            RefundCandidate(41L, 530_000L, 22_000L, "SAMPLE SHOP"),
            RefundCandidate(42L, 540_000L, 22_000L, "SAMPLE SHOP")
        )
        assertEquals(
            RefundReviewReason.AMBIGUOUS,
            RefundMatchingPolicy.reviewReason(22_000L, 600_000L, "SAMPLE SHOP", payments)
        )
    }

    @Test
    fun unrelatedCreditIsNotARefundCandidate() {
        assertNull(RefundMatchingPolicy.reviewReason(
            8_000L, 600_000L, "OTHER SHOP",
            listOf(RefundCandidate(41L, 120_000L, 22_000L, "SAMPLE SHOP"))
        ))
    }

    @Test
    fun prefixRelatedMerchantNamesAreNotTheSameIdentity() {
        assertNull(RefundMatchingPolicy.chooseCandidate(
            receivedAmountMinor = 22_000L,
            receivedAtEpochMs = 600_000L,
            receivedCounterparty = "SAMPLE TAXI EXPRESS",
            candidates = listOf(RefundCandidate(41L, 120_000L, 22_000L, "SAMPLE TAXI"))
        ))
    }

    @Test
    fun linksUniqueExactAmountFromSameMerchant() {
        val result = RefundMatchingPolicy.chooseCandidate(
            receivedAmountMinor = 22_000L,
            receivedAtEpochMs = 600_000L,
            receivedCounterparty =
                "QUIDEXPLUS KENYA LIMITED 3 3040629",
            candidates = listOf(
                RefundCandidate(
                    transactionId = 41L,
                    occurredAtEpochMs = 120_000L,
                    amountMinor = 22_000L,
                    counterparty =
                        "QUIDEXPLUS KENYA LIMITED"
                )
            )
        )

        assertEquals(41L, result)
    }

    @Test
    fun refusesSameAmountFromDifferentMerchant() {
        val result = RefundMatchingPolicy.chooseCandidate(
            receivedAmountMinor = 22_000L,
            receivedAtEpochMs = 600_000L,
            receivedCounterparty = "QUIDEXPLUS KENYA LIMITED",
            candidates = listOf(
                RefundCandidate(
                    transactionId = 41L,
                    occurredAtEpochMs = 120_000L,
                    amountMinor = 22_000L,
                    counterparty = "OTHER TAXI LIMITED"
                )
            )
        )

        assertNull(result)
    }

    @Test
    fun refusesAmbiguousRepeatedPayments() {
        val result = RefundMatchingPolicy.chooseCandidate(
            receivedAmountMinor = 22_000L,
            receivedAtEpochMs = 600_000L,
            receivedCounterparty = "QUIDEXPLUS KENYA LIMITED",
            candidates = listOf(
                RefundCandidate(
                    41L,
                    540_000L,
                    22_000L,
                    "QUIDEXPLUS KENYA LIMITED"
                ),
                RefundCandidate(
                    42L,
                    545_000L,
                    22_000L,
                    "QUIDEXPLUS KENYA LIMITED"
                )
            )
        )

        assertNull(result)
    }

    @Test
    fun stripsKenyanCompanySuffixesAndNumericReferences() {
        assertEquals(
            "QUIDEXPLUS",
            RefundMatchingPolicy.merchantIdentity(
                "QUIDEXPLUS KENYA LIMITED 3 3040629"
            )
        )
    }
}
