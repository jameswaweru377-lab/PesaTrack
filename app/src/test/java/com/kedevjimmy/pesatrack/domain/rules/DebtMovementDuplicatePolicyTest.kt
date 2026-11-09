package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class DebtMovementDuplicatePolicyTest {
    private val first = DebtMovementEvidence(
        eventId = 1L, debtAccountId = "liability-a", type = "DRAWDOWN",
        amountMinor = 10_000L, feeMinor = 100L, linkedTransactionId = 91L,
        occurredAtEpochMs = 100_000L
    )

    @Test fun sameProvenLoanLegFromTwoSourcesIsCountedOnce() {
        val duplicate = first.copy(eventId = 2L, occurredAtEpochMs = 160_000L)
        assertEquals(setOf(1L), DebtMovementDuplicatePolicy.uniqueEventIds(listOf(first, duplicate)))
    }

    @Test fun distinctFeesOrLiabilitiesRemainDistinct() {
        val distinctFee = first.copy(eventId = 2L, feeMinor = 200L)
        val distinctLiability = first.copy(eventId = 3L, debtAccountId = "liability-b")
        assertEquals(setOf(1L, 2L, 3L), DebtMovementDuplicatePolicy.uniqueEventIds(
            listOf(first, distinctFee, distinctLiability)
        ))
    }

    @Test fun unknownLinkAndLegitimateRepeatsRemainVisible() {
        val unknown = first.copy(linkedTransactionId = null)
        val repeated = unknown.copy(eventId = 2L)
        val distant = first.copy(eventId = 3L, occurredAtEpochMs = 1_000_000L)
        assertEquals(setOf(1L, 2L, 3L), DebtMovementDuplicatePolicy.uniqueEventIds(
            listOf(unknown, repeated, distant)
        ))
    }

    @Test fun actualRepaymentAndDrawdownAreNeverCollapsedTogether() {
        val repayment = first.copy(eventId = 2L, type = "REPAYMENT")
        assertEquals(setOf(1L, 2L), DebtMovementDuplicatePolicy.uniqueEventIds(listOf(first, repayment)))
    }

    @Test fun strongerBalanceObservationIsKeptWhenSourcesRepeatALeg() {
        val withObservedBalance = first.copy(eventId = 2L,
            outstandingAfterMinor = 10_100L, occurredAtEpochMs = 160_000L)
        assertEquals(setOf(2L), DebtMovementDuplicatePolicy.uniqueEventIds(
            listOf(first, withObservedBalance)
        ))
    }

    @Test fun contradictoryObservedBalancesDoNotSilentlyDiscardEvidence() {
        val one = first.copy(outstandingAfterMinor = 10_100L)
        val conflicting = first.copy(eventId = 2L, outstandingAfterMinor = 11_100L)
        assertEquals(setOf(1L, 2L), DebtMovementDuplicatePolicy.uniqueEventIds(
            listOf(one, conflicting)
        ))
    }

    @Test fun differentPrincipalProvenanceOrCompletionIsNeverCollapsed() {
        val principal = first.copy(amountIsNetProceeds = false)
        val unknown = principal.copy(eventId = 2L, amountIsNetProceeds = null)
        val netProceeds = principal.copy(eventId = 3L, amountIsNetProceeds = true)
        assertEquals(setOf(1L, 2L, 3L), DebtMovementDuplicatePolicy.uniqueEventIds(
            listOf(principal, unknown, netProceeds)
        ))
        val repayment = first.copy(type = "REPAYMENT", repaymentCompletion = "FULL")
        val partial = repayment.copy(eventId = 2L, repaymentCompletion = "PARTIAL")
        assertEquals(setOf(1L, 2L), DebtMovementDuplicatePolicy.uniqueEventIds(listOf(repayment, partial)))
    }

    @Test fun userReviewedDebtEventIsNotHiddenByAutomaticDeduplication() {
        val reviewed = first.copy(hasUserDecision = true)
        val repeat = reviewed.copy(eventId = 2L, hasUserDecision = false)
        assertEquals(setOf(1L, 2L), DebtMovementDuplicatePolicy.uniqueEventIds(listOf(reviewed, repeat)))
    }
}
