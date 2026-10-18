package com.kedevjimmy.pesatrack.ui.screens

import java.util.Locale

/** Presentation-only rules for the post-scan institution review. */
internal object OnboardingInstitutionPresentationPolicy {
    const val MAX_VISIBLE_INSTITUTIONS = 5

    val safaricomProductProviderKeys = setOf(
        "NCBA_MSHWARI_SAVINGS",
        "NCBA_MSHWARI_LOCK_SAVINGS",
        "KCB_MPESA_SAVINGS"
    )

    fun isSafaricomProduct(providerKey: String?): Boolean =
        providerKey in safaricomProductProviderKeys

    fun topFive(sources: List<DetectedSetupSource>): List<DetectedSetupSource> =
        sources
            .filterNot { isSafaricomProduct(it.reference.providerKey) }
            .sortedWith(
                compareByDescending<DetectedSetupSource> { it.messageCount }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
                    .thenBy { it.reference.entityKey }
            )
            .take(MAX_VISIBLE_INSTITUTIONS)

    /** Returns no identifier when fewer than three digits are known. */
    fun maskIdentifier(identifier: String?): String? {
        val digits = identifier.orEmpty().filter(Char::isDigit)
        return digits.takeIf { it.length >= 3 }
            ?.takeLast(3)
            ?.let { "••• $it" }
    }
}
