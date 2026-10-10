package com.kedevjimmy.pesatrack.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingReviewPolicyTest {
    @Test fun onlyExplicitChoicesForPendingCandidatesTakeEffect() {
        val plan = OnboardingReviewPolicy.ownershipPlan(
            candidateKeys = listOf("wallet", "recipient", "ambiguous"),
            choices = mapOf(
                "wallet" to OnboardingOwnershipChoice.MINE,
                "recipient" to OnboardingOwnershipChoice.NOT_MINE,
                "ambiguous" to OnboardingOwnershipChoice.UNSURE,
                "old-reviewed" to OnboardingOwnershipChoice.NOT_MINE
            )
        )
        assertEquals(setOf("wallet"), plan.mineKeys)
        assertEquals(setOf("recipient"), plan.notMineKeys)
    }

    @Test fun skippedAccountsCauseNoOwnershipMutation() {
        val plan = OnboardingReviewPolicy.ownershipPlan(listOf("bank", "wallet"), emptyMap())
        assertTrue(plan.mineKeys.isEmpty())
        assertTrue(plan.notMineKeys.isEmpty())
    }

    @Test fun explicitNotMineNeverBecomesMine() {
        val plan = OnboardingReviewPolicy.ownershipPlan(
            listOf("synthetic:recipient"),
            mapOf("synthetic:recipient" to OnboardingOwnershipChoice.NOT_MINE)
        )
        assertTrue(plan.mineKeys.isEmpty())
        assertEquals(setOf("synthetic:recipient"), plan.notMineKeys)
    }

    private fun salary(label: String, accountId: String = "owned-bank") =
        OnboardingSalarySuggestion(
            label = label, accountId = accountId,
            occurrenceCount = 3, monthCount = 3, representativeAmountMinor = 5_200_000L
        )

    @Test fun salaryIsNeverConfirmedByDefaultOrAfterBeingDeclined() {
        assertFalse(OnboardingReviewPolicy.retainSalaryAcceptance(salary("EMPLOYER"), salary("EMPLOYER"), false))
        assertFalse(OnboardingReviewPolicy.retainSalaryAcceptance(null, salary("EMPLOYER"), true))
    }

    @Test fun salaryConsentPersistsOnlyForAnUnchangedSuggestion() {
        val original = salary("EMPLOYER")
        assertTrue(OnboardingReviewPolicy.retainSalaryAcceptance(original, original.copy(), true))
        assertFalse(OnboardingReviewPolicy.retainSalaryAcceptance(original, salary("OTHER"), true))
        assertFalse(OnboardingReviewPolicy.retainSalaryAcceptance(original, salary("EMPLOYER", "other-bank"), true))
        assertFalse(OnboardingReviewPolicy.retainSalaryAcceptance(original, null, true))
    }
}
