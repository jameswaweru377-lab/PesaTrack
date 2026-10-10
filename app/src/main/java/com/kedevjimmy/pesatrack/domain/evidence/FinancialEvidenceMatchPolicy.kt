package com.kedevjimmy.pesatrack.domain.evidence

import java.util.Locale

/**
 * A local evidence observation is NOT a new canonical cash transaction.
 * sourceScope identifies the sender/statement/import scope without carrying
 * raw SMS, a personal identifier, or a document into this policy.
 */
data class CashEvidenceObservation(
    val source: FinancialEvidenceSource,
    val sourceScope: String,
    val sourceNativeKey: String?,
    val claim: FinancialEvidenceClaim,
    val ownedAccountId: String?,
    val amountMinor: Long?,
    val currency: String = "KES",
    val occurredAtEpochMs: Long?,
    val transactionReference: String? = null
)

/** These are link-review candidates only. None authorizes a ledger write or automatic merge. */
enum class CashEvidenceMatchResult {
    /** Same source identity and fully consistent facts: replay the observation idempotently. */
    SAME_OBSERVATION_REPLAY,
    /** Same source identity but contradictory facts; hold for audit, never overwrite. */
    SOURCE_IDENTITY_CONFLICT,
    /** Strong shared reference, but repository must still prove unique canonical match. */
    STRONG_REFERENCE_CANDIDATE,
    /** Same cash shape without a shared reference: needs user or authoritative evidence. */
    POSSIBLE_MATCH_REVIEW,
    /** Different movements or otherwise incompatible, do not link as one cash movement. */
    DISTINCT_MOVEMENTS,
    /** Missing or unsupported cash authority; do not infer ownership or income. */
    INSUFFICIENT_EVIDENCE
}

/**
 * Pure match policy for future evidence persistence and import adapters.
 *
 * This is intentionally NOT wired into TransactionStore, which already owns
 * reconciliation, references, mirror detection and user corrections. A later
 * adapter may use this policy to OFFER a candidate, never directly insert,
 * merge, transfer, or change a verified user correction.
 */
object FinancialEvidenceMatchPolicy {
    private const val MAX_REFERENCE_DELTA_MS = 72L * 60L * 60L * 1000L
    private const val MAX_POSSIBLE_DELTA_MS = 48L * 60L * 60L * 1000L

    fun compare(a: CashEvidenceObservation, b: CashEvidenceObservation): CashEvidenceMatchResult {
        if (a.source == FinancialEvidenceSource.PAYSLIP || b.source == FinancialEvidenceSource.PAYSLIP ||
            a.claim !in cashClaims || b.claim !in cashClaims) {
            return CashEvidenceMatchResult.INSUFFICIENT_EVIDENCE
        }

        val sameSourceIdentity = a.source == b.source &&
            a.sourceScope.isNotBlank() && a.sourceScope == b.sourceScope &&
            !a.sourceNativeKey.isNullOrBlank() &&
            a.sourceNativeKey == b.sourceNativeKey

        val referenceA = normalizeReference(a.transactionReference)
        val referenceB = normalizeReference(b.transactionReference)
        val accountA = a.ownedAccountId?.takeIf { it.isNotBlank() }
        val accountB = b.ownedAccountId?.takeIf { it.isNotBlank() }
        val validMoney = a.amountMinor != null && b.amountMinor != null &&
            a.amountMinor > 0L && b.amountMinor > 0L &&
            a.currency.isNotBlank() && b.currency.isNotBlank()
        val validDates = a.occurredAtEpochMs != null && b.occurredAtEpochMs != null &&
            a.occurredAtEpochMs >= 0 && b.occurredAtEpochMs >= 0
        val basicEqual = validMoney && a.amountMinor == b.amountMinor &&
            a.currency.uppercase(Locale.ROOT) == b.currency.uppercase(Locale.ROOT) &&
            a.claim == b.claim && accountA != null && accountA == accountB
        val closeMs = if (validDates) {
            val at = a.occurredAtEpochMs!!
            val bt = b.occurredAtEpochMs!!
            if (at >= bt) at - bt else bt - at
        } else null

        if (sameSourceIdentity) {
            // A native identity is never a license to overwrite a contradictory record.
            return if (basicEqual && closeMs == 0L && referenceA == referenceB) {
                CashEvidenceMatchResult.SAME_OBSERVATION_REPLAY
            } else {
                CashEvidenceMatchResult.SOURCE_IDENTITY_CONFLICT
            }
        }
        if (!validMoney || !validDates || accountA == null || accountB == null) {
            return CashEvidenceMatchResult.INSUFFICIENT_EVIDENCE
        }
        if (!basicEqual) return CashEvidenceMatchResult.DISTINCT_MOVEMENTS
        if (referenceA != null && referenceB != null) {
            return if (referenceA == referenceB && closeMs!! <= MAX_REFERENCE_DELTA_MS) {
                CashEvidenceMatchResult.STRONG_REFERENCE_CANDIDATE
            } else {
                CashEvidenceMatchResult.DISTINCT_MOVEMENTS
            }
        }
        return if (closeMs!! <= MAX_POSSIBLE_DELTA_MS) {
            CashEvidenceMatchResult.POSSIBLE_MATCH_REVIEW
        } else CashEvidenceMatchResult.DISTINCT_MOVEMENTS
    }

    private val cashClaims = setOf(
        FinancialEvidenceClaim.CASH_RECEIVED,
        FinancialEvidenceClaim.CASH_SENT
    )

    private fun normalizeReference(raw: String?): String? =
        raw?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() }
}
