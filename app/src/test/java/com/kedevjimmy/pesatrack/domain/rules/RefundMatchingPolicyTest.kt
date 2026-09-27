package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefundMatchingPolicyTest {
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
