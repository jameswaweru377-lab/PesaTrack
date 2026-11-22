package com.kedevjimmy.pesatrack.data.local

/**
 * The source row stays in the canonical SQLite ledger. Recognition is a
 * replayable read-only projection, not an extra transaction or a bank import.
 */
enum class IncomeRecognitionStatus { CONFIRMED, UNRESOLVED, NOT_INCOME }

enum class IncomeRecognitionReason {
    EXPLICIT_USER_DECISION,
    OBSERVED_EXTERNAL_RECEIPT,
    OWNED_ACCOUNT_MOVEMENT,
    REFUND_OR_REVERSAL,
    LOAN_PRINCIPAL,
    ASSET_REALLOCATION,
    CASH_DEPOSIT_UNATTRIBUTED,
    PENDING_REVIEW,
    ORIGIN_UNVERIFIED,
    NOT_A_CREDIT
}

data class IncomeRecognitionDecision(
    val status: IncomeRecognitionStatus,
    val reason: IncomeRecognitionReason
)

/** Existing persisted evidence; never infer provenance from a display name. */
data class IncomeRecognitionEvidence(
    val direction: String,
    val type: String,
    val category: String,
    val reviewState: String,
    val sourceKind: String,
    val transactionCode: String?,
    val counterparty: String?,
    val accountId: String?,
    val linkedAccountId: String?,
    val relationType: String?,
    val amountMinor: Long = 1L,
    val cycleCandidateReason: String? = null
)

object IncomeRecognitionPolicy {
    fun isConfirmedIncome(row: StoredTransaction, confirmedOwnedAccountIds: Set<String>): Boolean =
        classify(row, confirmedOwnedAccountIds).status == IncomeRecognitionStatus.CONFIRMED

    fun classify(row: StoredTransaction, confirmedOwnedAccountIds: Set<String>): IncomeRecognitionDecision = classify(
        IncomeRecognitionEvidence(
            direction = row.direction, type = row.type, category = row.category,
            reviewState = row.reviewState, sourceKind = row.sourceKind,
            transactionCode = row.transactionCode, counterparty = row.counterparty,
            accountId = row.accountId, linkedAccountId = row.linkedAccountId,
            relationType = row.relationType, amountMinor = row.amountMinor,
            cycleCandidateReason = row.cycleCandidateReason
        ), confirmedOwnedAccountIds
    )

    fun classify(
        evidence: IncomeRecognitionEvidence,
        confirmedOwnedAccountIds: Set<String>
    ): IncomeRecognitionDecision {
        fun decided(status: IncomeRecognitionStatus, reason: IncomeRecognitionReason) =
            IncomeRecognitionDecision(status, reason)

        if (evidence.direction != "INCOME" || evidence.amountMinor <= 0L) {
            return decided(IncomeRecognitionStatus.NOT_INCOME, IncomeRecognitionReason.NOT_A_CREDIT)
        }
        if (evidence.relationType == "REFUND" || evidence.type == "REVERSAL") {
            return decided(IncomeRecognitionStatus.NOT_INCOME, IncomeRecognitionReason.REFUND_OR_REVERSAL)
        }
        if (evidence.category == "DEBT_LOAN") {
            return decided(IncomeRecognitionStatus.NOT_INCOME, IncomeRecognitionReason.LOAN_PRINCIPAL)
        }
        if (evidence.relationType in setOf("INVESTMENT_BUY", "INVESTMENT_SELL")) {
            return decided(IncomeRecognitionStatus.NOT_INCOME, IncomeRecognitionReason.ASSET_REALLOCATION)
        }
        if (!evidence.linkedAccountId.isNullOrBlank() &&
            evidence.linkedAccountId != evidence.accountId) {
            return decided(IncomeRecognitionStatus.NOT_INCOME, IncomeRecognitionReason.OWNED_ACCOUNT_MOVEMENT)
        }
        if (evidence.reviewState !in setOf("AUTO_CLASSIFIED", "USER_CLASSIFIED") ||
            evidence.cycleCandidateReason != null) {
            return decided(IncomeRecognitionStatus.UNRESOLVED, IncomeRecognitionReason.PENDING_REVIEW)
        }
        if (evidence.reviewState == "USER_CLASSIFIED" || evidence.sourceKind == "manual_setup") {
            return decided(IncomeRecognitionStatus.CONFIRMED, IncomeRecognitionReason.EXPLICIT_USER_DECISION)
        }
        if (evidence.type == "DEPOSIT") {
            return decided(IncomeRecognitionStatus.UNRESOLVED, IncomeRecognitionReason.CASH_DEPOSIT_UNATTRIBUTED)
        }
        // A sender/reference proves an observed receipt, not its economic nature.
        // The category must independently identify earnings (e.g. an approved
        // salary rule). Generic incoming credits stay unresolved.
        val observedCashReceipt = evidence.type == "RECEIVED" &&
            evidence.category == "INCOME" &&
            evidence.accountId in confirmedOwnedAccountIds &&
            !evidence.transactionCode.isNullOrBlank() &&
            !evidence.counterparty.isNullOrBlank() &&
            isTrustedLocalMessageSource(evidence.sourceKind)
        return if (observedCashReceipt) {
            decided(IncomeRecognitionStatus.CONFIRMED, IncomeRecognitionReason.OBSERVED_EXTERNAL_RECEIPT)
        } else {
            decided(IncomeRecognitionStatus.UNRESOLVED, IncomeRecognitionReason.ORIGIN_UNVERIFIED)
        }
    }

    private fun isTrustedLocalMessageSource(sourceKind: String): Boolean =
        sourceKind in setOf(
            "historical_sms", "historical_financial_message",
            "mpesa_notification", "configured_notification", "financial_notification",
            "sms", "live_sms"
        ) || sourceKind.startsWith("interpreted_historical_") ||
            sourceKind.startsWith("interpreted_mpesa_") ||
            sourceKind.startsWith("interpreted_configured_") ||
            sourceKind.startsWith("interpreted_financial_")
}
