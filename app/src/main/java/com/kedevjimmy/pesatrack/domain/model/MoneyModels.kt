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

enum class MoneyDirection {
    INCOME,
    EXPENSE,
    TRANSFER,
    NEUTRAL
}

enum class ReviewState {
    AUTO_CLASSIFIED,
    NEEDS_REVIEW,
    USER_CLASSIFIED,
    EXCLUDED
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
    val rawMessage: String,
    val reversalOfCode: String? = null
)

data class SmartRule(
    val id: String,
    val counterpartyContains: String,
    val category: TransactionCategory,
    val maximumAmountMinor: Long? = null,
    val name: String = "",
    val notificationTitleContains: String = "",
    val messageContains: String = "",
    val direction: MoneyDirection = MoneyDirection.EXPENSE,
    val planningMonthOffset: Int = 0,
    val displayLabel: String? = null,
    val enabled: Boolean = true
)
