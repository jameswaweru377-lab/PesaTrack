package com.kedevjimmy.pesatrack.domain.identity

/**
 * Local profile-name similarity is review evidence, never a cash-ledger mutation.
 * Exact three-name matching is strong identity evidence ONLY when corroborated
 * independently by known owned-account transfer observations.
 */
enum class SelfNameEvidenceLevel { EXACT_THREE_NAMES, TWO_OF_THREE_REVIEW, NO_MATCH }

object SelfNameEvidencePolicy {
    fun compare(configuredFullName: String?, observedCounterpartyName: String?): SelfNameEvidenceLevel {
        val profile = tokens(configuredFullName)
        val candidate = tokens(observedCounterpartyName)
        if (profile.size != 3 || profile.toSet().size != 3 ||
            candidate.size !in 2..3 || candidate.toSet().size != candidate.size) {
            return SelfNameEvidenceLevel.NO_MATCH
        }
        val shared = candidate.toSet().intersect(profile.toSet()).size
        return when {
            candidate.size == 3 && shared == 3 -> SelfNameEvidenceLevel.EXACT_THREE_NAMES
            shared == 2 -> SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW
            else -> SelfNameEvidenceLevel.NO_MATCH
        }
    }

    private fun tokens(value: String?): List<String> =
        IdentityGraphPolicy.normalizeName(value)?.split(" ")
            ?.filter { it.length >= 2 && it.all(Char::isLetter) }.orEmpty()
}
