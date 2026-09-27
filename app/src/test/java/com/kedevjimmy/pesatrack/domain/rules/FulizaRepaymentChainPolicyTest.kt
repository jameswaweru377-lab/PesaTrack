package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class FulizaRepaymentChainPolicyTest {
    @Test
    fun linksDifferentIncomingAmountUsingBalanceEquation() {
        val match =
            FulizaRepaymentChainPolicy
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

    @Test
    fun linksWhenIncomingBalanceBeforeRepaymentIsKnown() {
        val match =
            FulizaRepaymentChainPolicy
                .chooseFundingTransaction(
                    repaymentAmountMinor = 22_000L,
                    walletBalanceAfterMinor = 0L,
                    repaymentAtEpochMs = 600_000L,
                    candidates = listOf(
                        RepaymentFundingCandidate(
                            transactionId = 41L,
                            amountMinor = 22_000L,
                            occurredAtEpochMs = 590_000L,
                            balanceAfterMinor = 22_000L,
                            priorBalanceMinor = 0L
                        )
                    )
                )

        assertEquals(41L, match?.transactionId)
        assertEquals(
            RepaymentChainConfidence.HIGH,
            match?.confidence
        )
    }

    @Test
    fun usesUniqueVeryCloseIncomingTransactionWhenBalanceHistoryMissing() {
        val match =
            FulizaRepaymentChainPolicy
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
                            priorBalanceMinor = null
                        )
                    )
                )

        assertEquals(91L, match?.transactionId)
        assertEquals(
            RepaymentChainConfidence.MEDIUM,
            match?.confidence
        )
    }

    @Test
    fun refusesAmbiguousNearbyIncomingTransactions() {
        val match =
            FulizaRepaymentChainPolicy
                .chooseFundingTransaction(
                    repaymentAmountMinor = 138_100L,
                    walletBalanceAfterMinor = null,
                    repaymentAtEpochMs = 600_000L,
                    candidates = listOf(
                        RepaymentFundingCandidate(
                            91L,
                            1_438_100L,
                            570_000L,
                            null,
                            null
                        ),
                        RepaymentFundingCandidate(
                            92L,
                            5_000_000L,
                            575_000L,
                            null,
                            null
                        )
                    )
                )

        assertNull(match)
    }
}
