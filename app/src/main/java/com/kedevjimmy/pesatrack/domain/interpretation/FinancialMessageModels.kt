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
    TRANSFER,
    INCOME,
    EXPENSE,
    LOAN_DRAWDOWN,
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

data class FinancialMessageInterpretation(
    val transactionCode: String?,
    val occurredAtText: String?,
    val balances: List<BalanceObservationFact>,
    val events: List<FinancialEventFact>,
    val metadataEvidence: List<FieldEvidence> = emptyList()
) {
    val recognized: Boolean
        get() = balances.isNotEmpty() || events.isNotEmpty()

    val consumesLegacyTransactionParsing: Boolean
        get() = events.isNotEmpty()
}
