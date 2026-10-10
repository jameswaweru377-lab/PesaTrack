package com.kedevjimmy.pesatrack.domain.onboarding

/** Four-digit bank account suffixes remain local display hints, never match authority. */
data class LastFourHintValidation(
    val entries: Map<String, String> = emptyMap(),
    val error: String? = null
)

object OnboardingLastFourHintPolicy {
    fun validate(
        eligibleAccountIds: Set<String>,
        proposed: Map<String, String>
    ): LastFourHintValidation {
        val accepted = mutableMapOf<String, String>()
        proposed.forEach { (accountId, raw) ->
            if (accountId !in eligibleAccountIds) {
                return LastFourHintValidation(error = "The account list changed. Review the accounts again.")
            }
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return@forEach
            if (trimmed.length != 4 || !trimmed.all(Char::isDigit)) {
                return LastFourHintValidation(error = "Enter exactly four digits, or leave this optional field blank.")
            }
            accepted[accountId] = trimmed
        }
        return LastFourHintValidation(entries = accepted)
    }
}
