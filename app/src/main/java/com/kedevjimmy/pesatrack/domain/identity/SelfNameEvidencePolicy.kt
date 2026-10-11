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
            candidate == profile -> SelfNameEvidenceLevel.EXACT_THREE_NAMES
            shared == 2 -> SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW
            else -> SelfNameEvidenceLevel.NO_MATCH
        }
    }

    private fun tokens(value: String?): List<String> {
        val normalized = value.orEmpty().trim().uppercase(java.util.Locale.ROOT)
        if (!normalized.matches(Regex("""\p{L}{2,}(?:\s+\p{L}{2,})*"""))) return emptyList()
        return normalized.split(Regex("""\s+"""))
    }
}
