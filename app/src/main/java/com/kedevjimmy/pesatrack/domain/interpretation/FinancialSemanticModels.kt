package com.kedevjimmy.pesatrack.domain.interpretation

enum class FinancialSemanticIntent {
    PAYMENT,
    RECEIPT,
    TRANSFER,
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    LOAN_DISBURSEMENT,
    LOAN_REPAYMENT,
    LOAN_BALANCE,
    BALANCE_UPDATE,
    REVERSAL,
    REFUND,
    FEE,
    INVESTMENT_ORDER,
    INVESTMENT_EXECUTED,
    CONFIRMATION_ONLY,
    UNKNOWN_FINANCIAL
}

enum class FinancialSemanticStatus {
    LEDGER_EVENT,
    SEMANTIC_EVENT,
    CONFIRMATION,
    INFORMATION,
    NEEDS_REVIEW
}

data class ExternalReferenceFact(
    val value: String,
    val span: MessageSpan?,
    val confidence: InterpretationConfidence
)

data class SemanticAmountFact(
    val amountMinor: Long,
    val span: MessageSpan?,
    val confidence: InterpretationConfidence
)

data class FinancialMessageSemantics(
    val intents: Set<FinancialSemanticIntent> =
        emptySet(),
    val status: FinancialSemanticStatus? = null,
    val externalReferences:
        List<ExternalReferenceFact> = emptyList(),
    val amounts: List<SemanticAmountFact> = emptyList()
) {
    val isMeaningful: Boolean
        get() = intents.isNotEmpty()
}
