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

    @Test
    fun pairsExactTransactionFunding() {
        val result =
            DebtFundingPairingPolicy
                .chooseTransactionShortfallCandidate(
                    financedMinor = 16_700L,
                    financedAtEpochMs = 100_000L,
                    candidates = listOf(
                        DebtFundingCandidate(
                            transactionId = 7L,
                            occurredAtEpochMs = 90_000L,
                            amountMinor = 16_000L,
                            transactionFeeMinor = 700L
                        )
                    )
                )

        assertEquals(7L, result)
    }

    @Test
    fun refusesDistantTransactionFunding() {
        val result =
            DebtFundingPairingPolicy
                .chooseTransactionShortfallCandidate(
                    financedMinor = 16_700L,
                    financedAtEpochMs = 500_000L,
                    candidates = listOf(
                        DebtFundingCandidate(
                            transactionId = 7L,
                            occurredAtEpochMs = 100_000L,
                            amountMinor = 16_000L,
                            transactionFeeMinor = 700L
                        )
                    )
                )

        assertNull(result)
    }

    @Test
    fun linksRepaymentWhenIncomingBalanceBeforeRepaymentIsKnown() {
        val match =
            DebtRepaymentChainPolicy
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
    fun refusesAmbiguousNearbyRepaymentFunding() {
        val match =
            DebtRepaymentChainPolicy
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
    @Test
    fun linksDebtRepaymentToUniqueMatchingLoanProceeds() {
        val match =
            DebtProceedsRepaymentLinkPolicy.choose(
                repaymentAmountMinor = 98_500L,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    DebtProceedsEventCandidate(
                        debtEventId = 17L,
                        amountMinor = 98_500L,
                        occurredAtEpochMs = 570_000L
                    )
                )
            )

        assertEquals(17L, match?.debtEventId)
        assertEquals(
            RepaymentChainConfidence.HIGH,
            match?.confidence
        )
    }

    @Test
    fun refusesAmbiguousLoanProceedsForDebtRepayment() {
        val match =
            DebtProceedsRepaymentLinkPolicy.choose(
                repaymentAmountMinor = 98_500L,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    DebtProceedsEventCandidate(
                        debtEventId = 17L,
                        amountMinor = 98_500L,
                        occurredAtEpochMs = 570_000L
                    ),
                    DebtProceedsEventCandidate(
                        debtEventId = 18L,
                        amountMinor = 98_500L,
                        occurredAtEpochMs = 580_000L
                    )
                )
            )

        assertNull(match)
    }

    @Test
    fun ignoresLoanProceedsWithDifferentAmountOrDistantTime() {
        val match =
            DebtProceedsRepaymentLinkPolicy.choose(
                repaymentAmountMinor = 98_500L,
                repaymentAtEpochMs = 900_000L,
                candidates = listOf(
                    DebtProceedsEventCandidate(
                        debtEventId = 17L,
                        amountMinor = 90_000L,
                        occurredAtEpochMs = 899_000L
                    ),
                    DebtProceedsEventCandidate(
                        debtEventId = 18L,
                        amountMinor = 98_500L,
                        occurredAtEpochMs = 100_000L
                    )
                )
            )

        assertNull(match)
    }


    @Test
    fun doesNotLinkRepaymentToMoneyThatArrivedAfterIt() {
        val candidateAfterRepayment = RepaymentFundingCandidate(
            transactionId = 93L,
            amountMinor = 22_000L,
            occurredAtEpochMs = 601_000L,
            balanceAfterMinor = 22_000L,
            priorBalanceMinor = 0L
        )
        assertNull(
            DebtRepaymentChainPolicy.chooseFundingTransaction(
                repaymentAmountMinor = 22_000L,
                walletBalanceAfterMinor = 0L,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(candidateAfterRepayment)
            )
        )
    }

    @Test
    fun laterMoneyCannotObscureRealEarlierRepaymentFunding() {
        val earlier = RepaymentFundingCandidate(
            transactionId = 94L,
            amountMinor = 22_000L,
            occurredAtEpochMs = 590_000L,
            balanceAfterMinor = null,
            priorBalanceMinor = null
        )
        val later = earlier.copy(
            transactionId = 95L,
            occurredAtEpochMs = 610_000L
        )
        val match = DebtRepaymentChainPolicy.chooseFundingTransaction(
            repaymentAmountMinor = 22_000L,
            walletBalanceAfterMinor = null,
            repaymentAtEpochMs = 600_000L,
            candidates = listOf(later, earlier)
        )
        assertEquals(94L, match?.transactionId)
        assertEquals(RepaymentChainConfidence.MEDIUM, match?.confidence)
    }

    @Test
    fun laterLoanDisbursementCannotFundPastRepayment() {
        val later = DebtProceedsEventCandidate(
            debtEventId = 21L,
            amountMinor = 98_500L,
            occurredAtEpochMs = 601_000L
        )
        assertNull(
            DebtProceedsRepaymentLinkPolicy.choose(
                repaymentAmountMinor = 98_500L,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(later)
            )
        )
        assertEquals(
            22L,
            DebtProceedsRepaymentLinkPolicy.choose(
                repaymentAmountMinor = 98_500L,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    later,
                    later.copy(debtEventId = 22L, occurredAtEpochMs = 590_000L)
                )
            )?.debtEventId
        )
    }

    @Test
    fun refusesBalanceObservationThatPredatesRepayment() {
        val match =
            DebtRepaymentBalancePolicy.choose(
                explicitBalanceAfterMinor = 1_300_000L,
                computedBalanceAfterMinor = null,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    RepaymentBalanceCandidate(
                        observationId = 60L,
                        balanceMinor = 1_300_000L,
                        observedAtEpochMs = 590_000L
                    )
                )
            )

        assertNull(match)
    }

    @Test
    fun earlierBalanceCannotObscureUniqueResultingBalance() {
        val match =
            DebtRepaymentBalancePolicy.choose(
                explicitBalanceAfterMinor = null,
                computedBalanceAfterMinor = 1_300_000L,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    RepaymentBalanceCandidate(
                        observationId = 61L,
                        balanceMinor = 1_300_000L,
                        observedAtEpochMs = 590_000L
                    ),
                    RepaymentBalanceCandidate(
                        observationId = 62L,
                        balanceMinor = 1_300_000L,
                        observedAtEpochMs = 605_000L
                    )
                )
            )

        assertEquals(62L, match?.observationId)
        assertEquals(
            RepaymentChainConfidence.HIGH,
            match?.confidence
        )
    }

    @Test
    fun linksExplicitResultingBalanceObservation() {
        val match =
            DebtRepaymentBalancePolicy.choose(
                explicitBalanceAfterMinor =
                    1_300_000L,
                computedBalanceAfterMinor = null,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    RepaymentBalanceCandidate(
                        observationId = 51L,
                        balanceMinor = 1_300_000L,
                        observedAtEpochMs = 605_000L
                    )
                )
            )

        assertEquals(51L, match?.observationId)
        assertEquals(
            RepaymentChainConfidence.HIGH,
            match?.confidence
        )
    }

    @Test
    fun linksBalanceFromDeterministicFundingEquation() {
        val match =
            DebtRepaymentBalancePolicy.choose(
                explicitBalanceAfterMinor = null,
                computedBalanceAfterMinor =
                    1_300_000L,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    RepaymentBalanceCandidate(
                        observationId = 52L,
                        balanceMinor = 1_300_000L,
                        observedAtEpochMs = 650_000L
                    )
                )
            )

        assertEquals(52L, match?.observationId)
        assertEquals(
            RepaymentChainConfidence.HIGH,
            match?.confidence
        )
    }

    @Test
    fun refusesAmbiguousResultingBalances() {
        val match =
            DebtRepaymentBalancePolicy.choose(
                explicitBalanceAfterMinor =
                    1_300_000L,
                computedBalanceAfterMinor = null,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    RepaymentBalanceCandidate(
                        observationId = 52L,
                        balanceMinor = 1_300_000L,
                        observedAtEpochMs = 605_000L
                    ),
                    RepaymentBalanceCandidate(
                        observationId = 53L,
                        balanceMinor = 1_300_000L,
                        observedAtEpochMs = 610_000L
                    )
                )
            )

        assertNull(match)
    }

    @Test
    fun negativeRepaymentBalanceMustNotBecomeFabricatedZero() {
        val derived =
            DebtRepaymentBalancePolicy.deriveBalanceAfterRepayment(
                priorBalanceMinor = 1_000L,
                incomingAmountMinor = 200L,
                repaymentAmountMinor = 1_500L
            )

        assertNull(derived)
        assertNull(
            DebtRepaymentBalancePolicy.choose(
                explicitBalanceAfterMinor = null,
                computedBalanceAfterMinor = derived,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    RepaymentBalanceCandidate(
                        observationId = 70L,
                        balanceMinor = 0L,
                        observedAtEpochMs = 605_000L
                    )
                )
            )
        )
    }

    @Test
    fun validRepaymentEquationPreservesExactMinorUnits() {
        assertEquals(
            1_050L,
            DebtRepaymentBalancePolicy.deriveBalanceAfterRepayment(
                priorBalanceMinor = 2_000L,
                incomingAmountMinor = 50L,
                repaymentAmountMinor = 1_000L
            )
        )
        assertEquals(
            0L,
            DebtRepaymentBalancePolicy.deriveBalanceAfterRepayment(
                priorBalanceMinor = 1_000L,
                incomingAmountMinor = 100L,
                repaymentAmountMinor = 1_100L
            )
        )
    }

    @Test
    fun rejectsArithmeticOverflowAndInvalidBalanceEvidence() {
        assertNull(
            DebtRepaymentBalancePolicy.deriveBalanceAfterRepayment(
                priorBalanceMinor = Long.MAX_VALUE,
                incomingAmountMinor = 100L,
                repaymentAmountMinor = 50L
            )
        )
        assertNull(
            DebtRepaymentBalancePolicy.deriveBalanceAfterRepayment(
                priorBalanceMinor = null,
                incomingAmountMinor = 1_000L,
                repaymentAmountMinor = 500L
            )
        )
        assertNull(
            DebtRepaymentBalancePolicy.deriveBalanceAfterRepayment(
                priorBalanceMinor = -1L,
                incomingAmountMinor = 1_000L,
                repaymentAmountMinor = 500L
            )
        )
        assertNull(
            DebtRepaymentBalancePolicy.deriveBalanceAfterRepayment(
                priorBalanceMinor = 1_000L,
                incomingAmountMinor = 500L,
                repaymentAmountMinor = 0L
            )
        )
    }

    @Test
    fun doesNotTreatAvailableCreditAsCashBalance() {
        val match =
            DebtRepaymentBalancePolicy.choose(
                explicitBalanceAfterMinor =
                    13_000_00L,
                computedBalanceAfterMinor = null,
                repaymentAtEpochMs = 600_000L,
                candidates = listOf(
                    RepaymentBalanceCandidate(
                        observationId = 54L,
                        balanceMinor = 1_440_00L,
                        observedAtEpochMs = 605_000L
                    )
                )
            )

        assertNull(match)
    }

}
