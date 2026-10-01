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
            id = "generic_credit_to_phone_and_beneficiary",
            title = "EQUITY",
            body =
                "Your transaction of Kshs. 20000.0 has been credited to 254700000001 SAMPLE BENEFICIARY. Ref. EQA94DF3A2EB374. MPESA Ref. UITFT8N12T on 29/09/2026 at 20:54:36.",
            expectedIntent =
                FinancialSemanticIntent.TRANSFER,
            expectedRole =
                FinancialSemanticRole.NEW_EVENT,
            expectedReferences =
                setOf(
                    "EQA94DF3A2EB374",
                    "UITFT8N12T"
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
            id = "declined_loan_repayment",
            title = "KCB M-PESA",
            body =
                "Dear SAMPLE CUSTOMER, your loan repayment of KES 100.00 has been declined due to insufficient funds. Your KCB M-PESA Account balance is KES 0.00.",
            expectedIntent =
                FinancialSemanticIntent.FAILED_ACTION,
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
