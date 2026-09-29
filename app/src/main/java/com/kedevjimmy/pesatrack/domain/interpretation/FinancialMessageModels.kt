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
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    UNRESOLVED,
    REVERSAL,
    INCOME,
    EXPENSE,
    DEBT_DRAWDOWN,
    DEBT_REPAYMENT,
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

    // Semantic understanding can arrive before the generic
    // persistence path is migrated. Legacy adapters remain a
    // temporary safety net for those event families.
    val genericIngestionReady: Boolean
        get() = false
}

data class TransferEventFact(
    val amountMinor: Long,
    val source: FinancialAccountReference,
    val destination: FinancialAccountReference,
    val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val genericIngestionReady: Boolean =
        true
    override val kind: FinancialEventKind =
        FinancialEventKind.TRANSFER
}

enum class PaymentRouteHint {
    PERSON_TO_PERSON,
    MERCHANT,
    BILL,
    AIRTIME
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
    override val genericIngestionReady: Boolean =
        true
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
    override val genericIngestionReady: Boolean =
        true
    override val kind: FinancialEventKind =
        FinancialEventKind.RECEIPT
}

data class CashWithdrawalEventFact(
    val amountMinor: Long,
    val source: FinancialAccountReference,
    val cashDestination: FinancialAccountReference,
    val counterparty: String?,
    val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val genericIngestionReady: Boolean =
        true
    override val kind: FinancialEventKind =
        FinancialEventKind.CASH_WITHDRAWAL
}

data class CashDepositEventFact(
    val amountMinor: Long,
    val cashSource: FinancialAccountReference,
    val destination: FinancialAccountReference,
    val counterparty: String?,
    val feeMinor: Long,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val genericIngestionReady: Boolean =
        true
    override val kind: FinancialEventKind =
        FinancialEventKind.CASH_DEPOSIT
}

data class UnresolvedFinancialEventFact(
    val amountMinor: Long,
    val observedAccount: FinancialAccountReference?,
    val feeMinor: Long,
    override val confidence: InterpretationConfidence =
        InterpretationConfidence.LOW,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val genericIngestionReady: Boolean =
        true
    override val kind: FinancialEventKind =
        FinancialEventKind.UNRESOLVED
}

data class ReversalEventFact(
    val amountMinor: Long,
    val referencedTransactionCode: String?,
    val observedAccount: FinancialAccountReference?,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val genericIngestionReady: Boolean =
        true
    override val kind: FinancialEventKind =
        FinancialEventKind.REVERSAL
}

data class DebtDrawdownEventFact(
    val amountMinor: Long,
    val liability: FinancialAccountReference,
    val proceedsAccount: FinancialAccountReference? = null,
    val amountIsNetProceeds: Boolean = false,
    val feeMinor: Long,
    val outstandingAfterMinor: Long?,
    val availableCreditAfterMinor: Long?,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val genericIngestionReady: Boolean =
        true
    override val kind: FinancialEventKind =
        FinancialEventKind.DEBT_DRAWDOWN
}

data class DebtRepaymentEventFact(
    val amountMinor: Long,
    val liability: FinancialAccountReference,
    val fundingAccount: FinancialAccountReference?,
    val completion: DebtCompletion,
    val outstandingAfterMinor: Long?,
    val availableCreditAfterMinor: Long?,
    val walletBalanceAfterMinor: Long?,
    override val confidence: InterpretationConfidence,
    val evidence: List<FieldEvidence> = emptyList()
) : FinancialEventFact {
    override val genericIngestionReady: Boolean =
        true
    override val kind: FinancialEventKind =
        FinancialEventKind.DEBT_REPAYMENT
}

data class FinancialMessageInterpretation(
    val transactionCode: String?,
    val occurredAtText: String?,
    val balances: List<BalanceObservationFact>,
    val events: List<FinancialEventFact>,
    val metadataEvidence: List<FieldEvidence> = emptyList(),
    val semantics: FinancialSemanticAssessment? = null
) {
    val recognized: Boolean
        get() =
            balances.isNotEmpty() ||
                events.isNotEmpty() ||
                semantics?.understood == true

    val consumesLegacyTransactionParsing: Boolean
        get() =
            events.isNotEmpty() &&
                events.all {
                    it.genericIngestionReady
                }
}
