package com.kedevjimmy.pesatrack.domain.onboarding

/** An account found in a message is not necessarily the user's own account. */
enum class OnboardingOwnershipChoice { MINE, NOT_MINE, UNSURE }

data class OnboardingOwnershipPlan(
    val mineKeys: Set<String>,
    val notMineKeys: Set<String>
)

/** Only the user's explicit choice for a currently pending entity may change ownership. */
object OnboardingReviewPolicy {
    fun ownershipPlan(
        candidateKeys: Collection<String>,
        choices: Map<String, OnboardingOwnershipChoice>
    ): OnboardingOwnershipPlan {
        val candidates = candidateKeys.toSet()
        val mine = mutableSetOf<String>()
        val notMine = mutableSetOf<String>()
        choices.forEach { (key, choice) ->
            if (key !in candidates) return@forEach
            when (choice) {
                OnboardingOwnershipChoice.MINE -> mine += key
                OnboardingOwnershipChoice.NOT_MINE -> notMine += key
                OnboardingOwnershipChoice.UNSURE -> Unit
            }
        }
        return OnboardingOwnershipPlan(mine, notMine)
    }

    /** A refreshed suggestion must not override the user's salary confirmation. */
    fun retainSalaryAcceptance(
        previous: OnboardingSalarySuggestion?,
        refreshed: OnboardingSalarySuggestion?,
        explicitlyAccepted: Boolean
    ): Boolean = explicitlyAccepted && previous != null && previous == refreshed
}
