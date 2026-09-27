package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DebtLinkingPoliciesTest {
    @Test
    fun pairsTransactionShortfallWithoutProviderKnowledge() {
        val result =
            DebtFundingPairingPolicy
                .chooseTransactionShortfallCandidate(
                    financedMinor = 4_000L,
                    financedAtEpochMs = 100_000L,
                    candidates = listOf(
                        DebtFundingCandidate(
                            transactionId = 8L,
                            occurredAtEpochMs = 95_000L,
                            amountMinor = 12_000L,
                            transactionFeeMinor = 700L
                        )
                    )
                )

        assertEquals(8L, result)
    }

    @Test
    fun refusesAmbiguousTransactionShortfall() {
        val result =
            DebtFundingPairingPolicy
                .chooseTransactionShortfallCandidate(
                    financedMinor = 4_000L,
                    financedAtEpochMs = 100_000L,
                    candidates = listOf(
                        DebtFundingCandidate(
                            8L,
                            95_000L,
                            12_000L,
                            700L
                        ),
                        DebtFundingCandidate(
                            9L,
                            98_000L,
                            8_000L,
                            0L
                        )
                    )
                )

        assertNull(result)
    }

    @Test
    fun linksRepaymentFromBalanceEquationWithoutProviderName() {
        val match =
            DebtRepaymentChainPolicy
                .chooseFundingTransaction(
                    repaymentAmountMinor = 138_100L,
                    walletBalanceAfterMinor =
                        1_300_000L,
                    repaymentAtEpochMs = 600_000L,
                    candidates = listOf(
                        RepaymentFundingCandidate(
                            transactionId = 91L,
                            amountMinor = 1_438_100L,
                            occurredAtEpochMs = 570_000L,
                            balanceAfterMinor = null,
                            priorBalanceMinor = 0L
                        )
                    )
                )

        assertNotNull(match)
        assertEquals(91L, match?.transactionId)
        assertEquals(
            RepaymentChainConfidence.HIGH,
            match?.confidence
        )
    }
}
