package com.kedevjimmy.pesatrack.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedSavingsOnboardingPolicyTest {
    @Test fun mpesaPhoneConfirmationDoesNotImplicitlyOwnSavingsProducts() {
        val plan = LinkedSavingsOnboardingPolicy.plan(emptyMap())
        assertTrue(plan.mineKeys.isEmpty())
        assertTrue(plan.notMineKeys.isEmpty())
    }

    @Test fun lockSavingsAndOrdinarySavingsHaveIndependentChoices() {
        val choices = mapOf(
            "NCBA_MSHWARI_LOCK_SAVINGS" to OnboardingOwnershipChoice.MINE,
            "NCBA_MSHWARI_SAVINGS" to OnboardingOwnershipChoice.NOT_MINE,
            "SAFARICOM_MPESA" to OnboardingOwnershipChoice.MINE,
            "SOME_OTHER_ACCOUNT" to OnboardingOwnershipChoice.NOT_MINE
        )
        val plan = LinkedSavingsOnboardingPolicy.plan(choices)
        assertEquals(setOf("NCBA_MSHWARI_LOCK_SAVINGS"), plan.mineKeys)
        assertEquals(setOf("NCBA_MSHWARI_SAVINGS"), plan.notMineKeys)
    }

    @Test fun unsureKeepsProductUnconfirmed() {
        val plan = LinkedSavingsOnboardingPolicy.plan(mapOf(
            "NCBA_MSHWARI_LOCK_SAVINGS" to OnboardingOwnershipChoice.UNSURE,
            "NCBA_MSHWARI_SAVINGS" to OnboardingOwnershipChoice.UNSURE
        ))
        assertTrue(plan.mineKeys.isEmpty())
        assertTrue(plan.notMineKeys.isEmpty())
    }
}
