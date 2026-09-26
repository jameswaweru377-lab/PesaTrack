package com.kedevjimmy.pesatrack.domain.model

enum class TransactionCategory {
    RENT,
    LUNCH,
    ELECTRICITY,
    INTERNET,
    TRANSPORT,
    SHOPPING,
    FAMILY,
    FEES,
    PHONE_AND_DATA,
    INCOME,
    TRANSFER,
    CASH,
    SAVINGS,
    OTHER,
    UNCATEGORIZED
}

enum class MpesaTransactionType {
    SENT_MONEY,
    PAY_BILL,
    BUY_GOODS,
    RECEIVED,
    AIRTIME,
    WITHDRAWAL,
    DEPOSIT,
    REVERSAL,
    OTHER
}

data class ParsedMpesaTransaction(
    val transactionCode: String?,
    val type: MpesaTransactionType,
    val amountMinor: Long,
    val counterparty: String?,
    val feeMinor: Long,
    val balanceMinor: Long?,
    val occurredAtText: String?,
    val rawMessage: String
)

data class SmartRule(
    val id: String,
    val counterpartyContains: String,
    val category: TransactionCategory,
    val maximumAmountMinor: Long? = null
)
