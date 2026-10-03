package com.kedevjimmy.pesatrack.ingestion

enum class MessageLabSemanticLabel(
    val displayLabel: String
) {
    PAYMENT(
        "Payment / spending"
    ),
    MONEY_RECEIVED(
        "Money received"
    ),
    TRANSFER(
        "Transfer"
    ),
    CASH_WITHDRAWAL(
        "Cash withdrawal"
    ),
    CASH_DEPOSIT(
        "Cash deposit"
    ),
    LOAN_RECEIVED(
        "Loan received"
    ),
    LOAN_REPAYMENT(
        "Loan repayment"
    ),
    BALANCE_UPDATE(
        "Balance update"
    ),
    SAVINGS_INVESTMENT(
        "Savings / investment"
    ),
    OTHER_FINANCIAL(
        "Other financial"
    )
}
