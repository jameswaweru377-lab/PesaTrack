package com.kedevjimmy.pesatrack.domain.onboarding

/**
 * M-Shwari savings products are separate optional asset accounts.
 * Confirming an M-Pesa phone does not confirm either savings product.
 */
object LinkedSavingsOnboardingPolicy {
    val providerKeys: Set<String> = setOf(
        "KCB_MPESA_SAVINGS",
        "NCBA_MSHWARI_SAVINGS",
        "NCBA_MSHWARI_LOCK_SAVINGS"
    )

    fun plan(choices: Map<String, OnboardingOwnershipChoice>): OnboardingOwnershipPlan =
        OnboardingReviewPolicy.ownershipPlan(providerKeys, choices)
}
