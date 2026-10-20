package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnostic
import java.util.Locale

/** Presentation-only rules for the post-scan institution review. */
internal object OnboardingInstitutionPresentationPolicy {
    const val MAX_VISIBLE_INSTITUTIONS = 5

    private data class ObservationKey(
        val title: String,
        val sourceKind: String,
        val receivedAtEpochMs: Long,
        val messageShapeId: String
    )

    val safaricomProductProviderKeys = setOf(
        "NCBA_MSHWARI_SAVINGS",
        "NCBA_MSHWARI_LOCK_SAVINGS",
        "KCB_MPESA_SAVINGS"
    )

    fun isSafaricomProduct(providerKey: String?): Boolean =
        providerKey in safaricomProductProviderKeys

    /**
     * Historical imports can observe the same SMS again. Diagnostics have no
     * provider row ID, so use the stable redacted shape and source timestamp to
     * avoid counting a retry twice. If that identity is unavailable, preserve
     * the observation rather than guessing that it is a duplicate.
     */
    fun uniqueObservationCount(
        observations: List<FinancialIngestionDiagnostic>
    ): Int {
        val seen = HashSet<ObservationKey>()
        return observations.count { observation ->
            val shapeId = observation.messageShapeId
                ?.takeIf(String::isNotBlank)
                ?: return@count true
            seen.add(
                ObservationKey(
                    title = observation.title.orEmpty().trim().lowercase(Locale.ROOT),
                    sourceKind = observation.sourceKind.trim().lowercase(Locale.ROOT),
                    receivedAtEpochMs = observation.receivedAtEpochMs,
                    messageShapeId = shapeId
                )
            )
        }
    }

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
