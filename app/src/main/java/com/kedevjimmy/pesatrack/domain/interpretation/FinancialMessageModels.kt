package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType

enum class InterpretationConfidence {
    HIGH,
    MEDIUM,
    LOW
}

enum class AccountOwnershipHint {
    USER_OWNED,
    EXTERNAL,
    UNKNOWN
}

enum class FinancialAccountNature {
    ASSET,
    LIABILITY,
    UNKNOWN
}

enum class FinancialEventKind {
    PAYMENT,
    RECEIPT,
    TRANSFER,
    INCOME,
    EXPENSE,
    LOAN_DRAWDOWN,
    DEBT_DRAWDOWN,
    DEBT_REPAYMENT,
    AVAILABLE_CREDIT_OBSERVATION,
    REFUND,
    FEE
}

data class FinancialAccountReference(
    val entityKey: String,
    val displayName: String,
    val providerKey: String?,
    val accountTypeHint: AccountType,
    val ownershipHint: AccountOwnershipHint,
    val nature: FinancialAccountNature
)

data class BalanceObservationFact(
    val account: FinancialAccountReference,
    val balanceMinor: Long,
    val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
)

sealed interface FinancialEventFact {
    val kind: FinancialEventKind
    val confidence: InterpretationConfidence
}

data class TransferEventFact(
    val amountMinor: Long,
    val source: FinancialAccountReference,
    val destination: FinancialAccountReference,
    val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val kind: FinancialEventKind =
        FinancialEventKind.TRANSFER
}

enum class PaymentRouteHint {
    PERSON_TO_PERSON,
    MERCHANT,
    BILL
}

data class PaymentEventFact(
    val amountMinor: Long,
    val source: FinancialAccountReference,
    val counterparty: String,
    val counterpartyPhone: String?,
    val routeHint: PaymentRouteHint,
    val accountReference: String? = null,
    val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val kind: FinancialEventKind =
        FinancialEventKind.PAYMENT
}

data class ReceiptEventFact(
    val amountMinor: Long,
    val destination: FinancialAccountReference,
    val counterparty: String,
    val counterpartyPhone: String?,
    val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val kind: FinancialEventKind =
        FinancialEventKind.RECEIPT
}

data class DebtDrawdownEventFact(
    val amountMinor: Long,
    val liability: FinancialAccountReference,
    val feeMinor: Long,
    val outstandingAfterMinor: Long?,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val kind: FinancialEventKind =
        FinancialEventKind.DEBT_DRAWDOWN
}

data class DebtRepaymentEventFact(
    val amountMinor: Long,
    val liability: FinancialAccountReference,
    val fundingAccount: FinancialAccountReference?,
    val completion: DebtCompletion,
    val outstandingAfterMinor: Long?,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val kind: FinancialEventKind =
        FinancialEventKind.DEBT_REPAYMENT
}

data class AvailableCreditObservationFact(
    val liability: FinancialAccountReference,
    val availableCreditMinor: Long,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val kind: FinancialEventKind =
        FinancialEventKind.AVAILABLE_CREDIT_OBSERVATION
}

data class FinancialMessageInterpretation(
    val transactionCode: String?,
    val occurredAtText: String?,
    val balances: List<BalanceObservationFact>,
    val events: List<FinancialEventFact>,
    val metadataEvidence: List<FieldEvidence> = emptyList(),
    val legacyFallbackRequired: Boolean = true
) {
    val recognized: Boolean
        get() = balances.isNotEmpty() || events.isNotEmpty()

    val consumesLegacyTransactionParsing: Boolean
        get() =
            events.isNotEmpty() &&
                !legacyFallbackRequired
}
