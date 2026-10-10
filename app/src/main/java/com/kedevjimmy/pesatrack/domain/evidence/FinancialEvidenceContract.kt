package com.kedevjimmy.pesatrack.domain.evidence

/**
 * The four independently optional ways PesaTrack can learn about money.
 * SMS and notification capture share a channel but retain distinct provenance.
 *
 * This policy is deliberately pure: it does not write to the ledger, infer
 * account ownership, perform fuzzy matching, or trust a document's net-pay
 * value as proof of a bank deposit.
 */
enum class FinancialEvidenceSource {
    SMS,
    NOTIFICATION,
    STATEMENT,
    PAYSLIP,
    MANUAL
}

enum class FinancialEvidenceClaim {
    CASH_RECEIVED,
    CASH_SENT,
    PAYROLL_BREAKDOWN,
    EXPECTED_RECEIPT
}

enum class EvidenceDisposition {
    /** May enter the existing duplicate/transfer/refund-aware reconciliation pipeline. */
    RECONCILE_CASH_CANDIDATE,
    /** Payroll details, withholding and contributions only: not a cash deposit. */
    PAYROLL_ONLY,
    /** An expectation for planning, never an observed income transaction. */
    PLANNING_ONLY,
    /** Important information is missing or conflicts; do not mutate cash accounting. */
    NEEDS_REVIEW,
    /** This source is not permitted to establish that category of fact. */
    UNSUPPORTED
}

data class FinancialEvidenceRequest(
    val source: FinancialEvidenceSource,
    val claim: FinancialEvidenceClaim,
    /** Source was deterministically interpreted and passed source validation. */
    val interpreted: Boolean = false,
    /** User deliberately recorded a received movement or selected a statement row. */
    val userConfirmed: Boolean = false,
    /** This movement's own account has been safely resolved, not merely named in text. */
    val ownAccountResolved: Boolean = false
)

/**
 * Gate for NEW PesaTrack 2.0 evidence adapters; not wired into legacy ingestion.
 *
 * RECONCILE_CASH_CANDIDATE grants no permission to INSERT directly. The adapter
 * must first use canonical transaction identity, replay, account/amount/date
 * checks and user-correction safeguards. A matching receipt is enriched, not
 * recreated. Ambiguity remains pending.
 */
object FinancialEvidenceContract {
    fun disposition(request: FinancialEvidenceRequest): EvidenceDisposition =
        when (request.claim) {
            FinancialEvidenceClaim.PAYROLL_BREAKDOWN ->
                when (request.source) {
                    FinancialEvidenceSource.PAYSLIP ->
                        if (request.interpreted || request.userConfirmed) {
                            EvidenceDisposition.PAYROLL_ONLY
                        } else {
                            EvidenceDisposition.NEEDS_REVIEW
                        }
                    FinancialEvidenceSource.MANUAL ->
                        if (request.userConfirmed) {
                            EvidenceDisposition.PAYROLL_ONLY
                        } else {
                            EvidenceDisposition.NEEDS_REVIEW
                        }
                    else -> EvidenceDisposition.UNSUPPORTED
                }

            FinancialEvidenceClaim.EXPECTED_RECEIPT ->
                if (request.source == FinancialEvidenceSource.MANUAL && request.userConfirmed) {
                    EvidenceDisposition.PLANNING_ONLY
                } else {
                    EvidenceDisposition.UNSUPPORTED
                }

            FinancialEvidenceClaim.CASH_RECEIVED,
            FinancialEvidenceClaim.CASH_SENT ->
                when (request.source) {
                    FinancialEvidenceSource.PAYSLIP ->
                        EvidenceDisposition.UNSUPPORTED
                    FinancialEvidenceSource.MANUAL ->
                        if (request.userConfirmed && request.ownAccountResolved) {
                            EvidenceDisposition.RECONCILE_CASH_CANDIDATE
                        } else {
                            EvidenceDisposition.NEEDS_REVIEW
                        }
                    FinancialEvidenceSource.STATEMENT ->
                        if (request.interpreted && request.userConfirmed && request.ownAccountResolved) {
                            EvidenceDisposition.RECONCILE_CASH_CANDIDATE
                        } else {
                            EvidenceDisposition.NEEDS_REVIEW
                        }
                    FinancialEvidenceSource.SMS,
                    FinancialEvidenceSource.NOTIFICATION ->
                        if (request.interpreted && request.ownAccountResolved) {
                            EvidenceDisposition.RECONCILE_CASH_CANDIDATE
                        } else {
                            EvidenceDisposition.NEEDS_REVIEW
                        }
                }
        }
}
