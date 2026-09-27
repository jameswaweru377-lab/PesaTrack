package com.kedevjimmy.pesatrack.domain.interpretation

enum class FinancialField {
    TRANSACTION_CODE,
    OCCURRED_AT,
    AMOUNT,
    FEE,
    ACCOUNT,
    SOURCE_ACCOUNT,
    DESTINATION_ACCOUNT,
    COUNTERPARTY,
    ACCOUNT_REFERENCE,
    BALANCE,
    LIABILITY,
    OUTSTANDING_BALANCE,
    AVAILABLE_CREDIT,
    COMPLETION,
    RELATIONSHIP
}

enum class EvidenceOrigin {
    MESSAGE_TEXT,
    PROVIDER_PROFILE,
    USER_CONFIRMED,
    RECONCILIATION,
    LEGACY_ADAPTER
}

data class MessageSpan(
    val startOffset: Int,
    val endOffsetExclusive: Int
) {
    init {
        require(startOffset >= 0)
        require(endOffsetExclusive >= startOffset)
    }
}

data class FieldEvidence(
    val field: FinancialField,
    val origin: EvidenceOrigin,
    val ruleId: String,
    val span: MessageSpan? = null,
    val confidence: InterpretationConfidence
)

enum class CanonicalFinancialRecordKind {
    PAYMENT,
    RECEIPT,
    TRANSFER,
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    REFUND,
    REVERSAL,
    DEBT_DRAWDOWN,
    DEBT_REPAYMENT,
    FEE,
    BALANCE_OBSERVATION,
    AVAILABLE_CREDIT_OBSERVATION
}

sealed interface CanonicalFinancialRecord {
    val kind: CanonicalFinancialRecordKind
    val confidence: InterpretationConfidence
    val evidence: List<FieldEvidence>
}

data class MoneyMovementDraft(
    override val kind: CanonicalFinancialRecordKind,
    val amountMinor: Long,
    val source: FinancialAccountReference?,
    val destination: FinancialAccountReference?,
    val counterparty: String? = null,
    val relatedTransactionCode: String? = null,
    val feeMinor: Long = 0L,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence> = emptyList()
) : CanonicalFinancialRecord {
    init {
        require(
            kind in setOf(
                CanonicalFinancialRecordKind.PAYMENT,
                CanonicalFinancialRecordKind.RECEIPT,
                CanonicalFinancialRecordKind.TRANSFER,
                CanonicalFinancialRecordKind.CASH_WITHDRAWAL,
                CanonicalFinancialRecordKind.CASH_DEPOSIT,
                CanonicalFinancialRecordKind.REFUND,
                CanonicalFinancialRecordKind.REVERSAL
            )
        )
        require(amountMinor >= 0L)
        require(feeMinor >= 0L)
    }
}

enum class DebtCompletion {
    PARTIAL,
    FULL,
    UNKNOWN
}

data class DebtMovementDraft(
    override val kind: CanonicalFinancialRecordKind,
    val amountMinor: Long,
    val liability: FinancialAccountReference,
    val fundingAccount: FinancialAccountReference?,
    val completion: DebtCompletion = DebtCompletion.UNKNOWN,
    val outstandingAfterMinor: Long? = null,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence> = emptyList()
) : CanonicalFinancialRecord {
    init {
        require(
            kind == CanonicalFinancialRecordKind.DEBT_DRAWDOWN ||
                kind == CanonicalFinancialRecordKind.DEBT_REPAYMENT
        )
        require(amountMinor >= 0L)
        require(
            liability.nature ==
                FinancialAccountNature.LIABILITY ||
                liability.nature ==
                    FinancialAccountNature.UNKNOWN
        )
    }
}

data class FeeDraft(
    val amountMinor: Long,
    val chargedTo: FinancialAccountReference?,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence> = emptyList()
) : CanonicalFinancialRecord {
    override val kind: CanonicalFinancialRecordKind =
        CanonicalFinancialRecordKind.FEE

    init {
        require(amountMinor >= 0L)
    }
}

data class BalanceObservationDraft(
    val account: FinancialAccountReference,
    val balanceMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence> = emptyList()
) : CanonicalFinancialRecord {
    override val kind: CanonicalFinancialRecordKind =
        CanonicalFinancialRecordKind.BALANCE_OBSERVATION
}

data class AvailableCreditObservationDraft(
    val liability: FinancialAccountReference,
    val availableCreditMinor: Long,
    override val confidence: InterpretationConfidence,
    override val evidence: List<FieldEvidence> = emptyList()
) : CanonicalFinancialRecord {
    override val kind: CanonicalFinancialRecordKind =
        CanonicalFinancialRecordKind.AVAILABLE_CREDIT_OBSERVATION

    init {
        require(availableCreditMinor >= 0L)
    }
}

data class CanonicalFinancialMessageDraft(
    val transactionCode: String?,
    val occurredAtText: String?,
    val records: List<CanonicalFinancialRecord>,
    val metadataEvidence: List<FieldEvidence>,
    val requiresLegacyFallback: Boolean
) {
    val recognized: Boolean
        get() = records.isNotEmpty()

    fun recordsOf(
        kind: CanonicalFinancialRecordKind
    ): List<CanonicalFinancialRecord> =
        records.filter { it.kind == kind }
}
