package com.kedevjimmy.pesatrack.domain.interpretation

data class FinancialIntelligenceV2SemanticFixture(
    val id: String,
    val title: String?,
    val body: String,
    val expectedIntent: FinancialSemanticIntent,
    val expectedRole: FinancialSemanticRole,
    val expectedReferences: Set<String> =
        emptySet()
)

object FinancialIntelligenceV2SemanticCorpus {
    val all = listOf(
        FinancialIntelligenceV2SemanticFixture(
            id = "equity_credit_to_beneficiary",
            title = "EQUITY",
            body =
                "Your transaction of Kenya Shillings 20,000.00 has been credited to SAMPLE BENEFICIARY. Equity Ref EQA12345. M-Pesa Ref UFT12345.",
            expectedIntent =
                FinancialSemanticIntent.TRANSFER,
            expectedRole =
                FinancialSemanticRole.NEW_EVENT,
            expectedReferences =
                setOf(
                    "EQA12345",
                    "UFT12345"
                )
        ),
        FinancialIntelligenceV2SemanticFixture(
            id = "mshwari_loan_balance",
            title = "M-Shwari",
            body =
                "Your total loan balance is Kshs 1,075.00.",
            expectedIntent =
                FinancialSemanticIntent.LOAN_BALANCE,
            expectedRole =
                FinancialSemanticRole.INFORMATION
        ),
        FinancialIntelligenceV2SemanticFixture(
            id = "branch_loan_disbursed",
            title = "Branch",
            body =
                "Your loan of Kenya Shillings 4,607 has been disbursed to your M-Pesa account.",
            expectedIntent =
                FinancialSemanticIntent
                    .LOAN_DISBURSEMENT,
            expectedRole =
                FinancialSemanticRole.NEW_EVENT
        ),
        FinancialIntelligenceV2SemanticFixture(
            id = "branch_transfer_processed",
            title = "Branch",
            body =
                "Your transfer of funds has been processed.",
            expectedIntent =
                FinancialSemanticIntent
                    .CONFIRMATION_ONLY,
            expectedRole =
                FinancialSemanticRole.CONFIRMATION
        ),
        FinancialIntelligenceV2SemanticFixture(
            id = "generic_loan_repayment",
            title = "Future Finance",
            body =
                "Loan repayment of KES 1,000.00 received. Loan balance is KES 4,000.00.",
            expectedIntent =
                FinancialSemanticIntent.LOAN_REPAYMENT,
            expectedRole =
                FinancialSemanticRole.NEW_EVENT
        ),
        FinancialIntelligenceV2SemanticFixture(
            id = "investment_order",
            title = "Zidi",
            body =
                "Your buy order of 10 SAMPLE shares valued at KES 367 has been submitted.",
            expectedIntent =
                FinancialSemanticIntent.INVESTMENT_ORDER,
            expectedRole =
                FinancialSemanticRole.PENDING_ACTION
        ),
        FinancialIntelligenceV2SemanticFixture(
            id = "investment_executed",
            title = "Zidi",
            body =
                "You have successfully bought 3 EQA shares valued at KES 3,021 via code UFT12345.",
            expectedIntent =
                FinancialSemanticIntent
                    .INVESTMENT_EXECUTED,
            expectedRole =
                FinancialSemanticRole.NEW_EVENT,
            expectedReferences =
                setOf("UFT12345")
        ),
        FinancialIntelligenceV2SemanticFixture(
            id = "loan_due_reminder",
            title = "Future Finance",
            body =
                "Your total loan balance is KES 2,500. Repayment due on 5 Oct 2026.",
            expectedIntent =
                FinancialSemanticIntent.LOAN_BALANCE,
            expectedRole =
                FinancialSemanticRole.REMINDER
        ),
        FinancialIntelligenceV2SemanticFixture(
            id = "unknown_financial_wording",
            title = "Future Finance",
            body =
                "KES 640.00 processed through your account.",
            expectedIntent =
                FinancialSemanticIntent
                    .CONFIRMATION_ONLY,
            expectedRole =
                FinancialSemanticRole.CONFIRMATION
        )
    )
}
