package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FulizaPairingPolicyTest {
    @Test
    fun pairsExactPaymentPlusMpesaFee() {
        val result = FulizaPairingPolicy.chooseCandidate(
            fulizaFinancedMinor = 16_700L,
            fulizaOccurredAtEpochMs = 100_000L,
            candidates = listOf(
                FulizaPaymentCandidate(
                    transactionId = 7L,
                    occurredAtEpochMs = 90_000L,
                    amountMinor = 16_000L,
                    mpesaFeeMinor = 700L
                )
            )
        )

        assertEquals(7L, result)
    }

    @Test
    fun pairsUniqueShortfallWhenWalletCoveredPartOfPayment() {
        val result = FulizaPairingPolicy.chooseCandidate(
            fulizaFinancedMinor = 4_000L,
            fulizaOccurredAtEpochMs = 100_000L,
            candidates = listOf(
                FulizaPaymentCandidate(
                    transactionId = 8L,
                    occurredAtEpochMs = 95_000L,
                    amountMinor = 12_000L,
                    mpesaFeeMinor = 700L
                )
            )
        )

        assertEquals(8L, result)
    }

    @Test
    fun refusesAmbiguousShortfallPairing() {
        val result = FulizaPairingPolicy.chooseCandidate(
            fulizaFinancedMinor = 4_000L,
            fulizaOccurredAtEpochMs = 100_000L,
            candidates = listOf(
                FulizaPaymentCandidate(8L, 95_000L, 12_000L, 700L),
                FulizaPaymentCandidate(9L, 98_000L, 8_000L, 0L)
            )
        )

        assertNull(result)
    }

    @Test
    fun refusesDistantPayment() {
        val result = FulizaPairingPolicy.chooseCandidate(
            fulizaFinancedMinor = 16_700L,
            fulizaOccurredAtEpochMs = 500_000L,
            candidates = listOf(
                FulizaPaymentCandidate(7L, 100_000L, 16_000L, 700L)
            )
        )

        assertNull(result)
    }
}
