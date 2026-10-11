package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
import com.kedevjimmy.pesatrack.domain.identity.SelfNameEvidenceLevel
import com.kedevjimmy.pesatrack.domain.identity.SelfNameEvidencePolicy
import java.util.Locale

data class TransferReviewObservation(
    val id: Long,
    val accountId: String,
    val accountType: String,
    val direction: String,
    val amountMinor: Long,
    val feeMinor: Long,
    val currency: String,
    val epochMs: Long,
    val references: Set<String>,
    val name: String?,
    val phone: String?,
    val snapshotHash: String,
    val protected: Boolean = false,
    val identityRejected: Boolean = false,
    val primaryReference: String? = null
)

data class TransferReviewCandidate(
    val source: TransferReviewObservation,
    val destination: TransferReviewObservation,
    val sourceOwnershipRevision: Long,
    val destinationOwnershipRevision: Long,
    val nameEvidence: SelfNameEvidenceLevel,
    val phoneEvidence: Boolean
) {
    val pairKey: String get() = "${minOf(source.id, destination.id)}:${maxOf(source.id, destination.id)}"
}

enum class TransferPairDecision { CONFIRMED, REJECTED, DEFERRED }
enum class TransferDecisionResult { APPLIED, STALE }

/** Review-only. Identity hints cannot authorize either account ownership or a ledger mutation. */
object OwnedTransferReviewPolicy {
    const val MAX_DELTA_MS = 48L * 60L * 60L * 1000L
    fun candidates(
        observations: List<TransferReviewObservation>,
        confirmedRevisions: Map<String, Long>,
        profileName: String?,
        profilePhone: String?
    ): List<TransferReviewCandidate> {
        if (observations.any { it.id <= 0 } || observations.map { it.id }.distinct().size != observations.size) return emptyList()
        val outgoing = observations.filter { it.direction == "EXPENSE" && it.accountType == "BANK" }
        val incoming = observations.filter { it.direction == "INCOME" && it.accountId == "account-mpesa" && it.accountType == "MOBILE_WALLET" }
        // Include protected and hintless twins in ambiguity checks. Their existence is not
        // permission to choose the convenient unprotected or profile-matching alternative.
        val pairs = outgoing.flatMap { source -> incoming.filter { correlates(source, it) }.map { source to it } }
        val sourceCounts = pairs.groupingBy { it.first.id }.eachCount()
        val destinationCounts = pairs.groupingBy { it.second.id }.eachCount()
        val normalizedPhone = IdentityGraphPolicy.normalizeKenyanPhone(profilePhone)
        return pairs.mapNotNull { (source, destination) ->
            val sourceRevision = confirmedRevisions[source.accountId] ?: return@mapNotNull null
            val destinationRevision = confirmedRevisions[destination.accountId] ?: return@mapNotNull null
            if (sourceRevision <= 0 || destinationRevision <= 0 || source.epochMs != destination.epochMs || source.protected || destination.protected ||
                source.identityRejected || destination.identityRejected || sourceCounts[source.id] != 1 || destinationCounts[destination.id] != 1) return@mapNotNull null
            val levels = listOf(source.name, destination.name).map { SelfNameEvidencePolicy.compare(profileName, it) }
            val level = when {
                SelfNameEvidenceLevel.EXACT_THREE_NAMES in levels -> SelfNameEvidenceLevel.EXACT_THREE_NAMES
                SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW in levels -> SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW
                else -> SelfNameEvidenceLevel.NO_MATCH
            }
            val phoneMatch = normalizedPhone != null && listOf(source.phone, destination.phone)
                .any { IdentityGraphPolicy.normalizeKenyanPhone(it) == normalizedPhone }
            if (level == SelfNameEvidenceLevel.NO_MATCH && !phoneMatch) return@mapNotNull null
            TransferReviewCandidate(source, destination, sourceRevision, destinationRevision, level, phoneMatch)
        }
    }

    private fun correlates(source: TransferReviewObservation, destination: TransferReviewObservation): Boolean {
        if (source.id == destination.id || source.accountId == destination.accountId || source.amountMinor <= 0 ||
            source.amountMinor != destination.amountMinor || source.feeMinor < 0 || destination.feeMinor != 0L ||
            source.amountMinor > Long.MAX_VALUE - source.feeMinor || source.currency != "KES" || destination.currency != "KES" ||
            source.epochMs < 0 || destination.epochMs < 0) return false
        val delta = if (source.epochMs >= destination.epochMs) source.epochMs - destination.epochMs else destination.epochMs - source.epochMs
        // Count all plausible staggered twins before the stricter eligibility
        // gate above: the current canonical transfer has one movement timestamp.
        if (delta > MAX_DELTA_MS) return false
        val sourceCode = source.primaryReference?.let(::reference)
        val destinationCode = destination.primaryReference?.let(::reference)
        // Generic shared customer/account references do not identify a movement.
        // Require a primary transaction code observed on the complementary side.
        return (sourceCode != null && (sourceCode == destinationCode ||
            sourceCode in destination.references.mapNotNull(::reference))) ||
            (destinationCode != null && destinationCode in source.references.mapNotNull(::reference))
    }
    private fun reference(raw: String): String? = raw.trim().uppercase(Locale.ROOT)
        .takeIf { it.length in 5..80 && it.matches(Regex("[A-Z0-9-]+")) }
}
