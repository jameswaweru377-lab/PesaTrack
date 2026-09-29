package com.kedevjimmy.pesatrack.domain.interpretation

/**
 * Sanitized language fixtures for Financial Intelligence V2.
 *
 * Provider names are test context only. Production relevance and
 * normalization logic must not branch on any provider in this corpus.
 */
data class FinancialIntelligenceV2FoundationFixture(
    val id: String,
    val title: String?,
    val body: String,
    val expectedRelevant: Boolean,
    val expectedNormalizedFragments: List<String> =
        emptyList()
)

object FinancialIntelligenceV2FoundationCorpus {
    val all = listOf(
        FinancialIntelligenceV2FoundationFixture(
            id = "equity_kenya_shillings_reference",
            title = "EQUITY",
            body =
                "Your transaction of Kenya Shillings 20,000.00 has been credited to SAMPLE BENEFICIARY. Equity Ref EQA12345. M-Pesa Ref UFT12345.",
            expectedRelevant = true,
            expectedNormalizedFragments =
                listOf("KES 20,000.00")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "mshwari_transfer_kshs",
            title = "M-Shwari",
            body =
                "Kshs 10,000.00 transferred from M-Pesa to your M-Shwari account. M-Shwari balance is Kshs 16,000.00.",
            expectedRelevant = true,
            expectedNormalizedFragments =
                listOf(
                    "KES 10,000.00",
                    "KES 16,000.00"
                )
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "mshwari_loan_balance_sh",
            title = "M-Shwari",
            body =
                "Your total loan balance is Sh 1,075.00.",
            expectedRelevant = true,
            expectedNormalizedFragments =
                listOf("KES 1,075.00")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "mshwari_locked_savings_move",
            title = "M-Shwari",
            body =
                "Dear customer, 4,000 has been moved from your locked savings account to your M-Shwari account.",
            expectedRelevant = true
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "branch_loan_disbursed_long_currency",
            title = "Branch",
            body =
                "Your loan of Kenya Shilling 4,607 has been disbursed to your M-Pesa account.",
            expectedRelevant = true,
            expectedNormalizedFragments =
                listOf("KES 4,607")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "branch_transfer_processed_confirmation",
            title = "Branch",
            body =
                "Your transfer of funds has been processed.",
            expectedRelevant = true
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "generic_bank_credit_kes",
            title = "Future Finance Alerts",
            body =
                "KES 3,000 credited to A/C ****1234 from SAMPLE PERSON. Ref ABC12345.",
            expectedRelevant = true,
            expectedNormalizedFragments =
                listOf("KES 3,000")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "generic_bank_debit_suffix_currency",
            title = "Future Finance Alerts",
            body =
                "Your account was debited 2,500 Kenya Shillings for transfer to SAMPLE PERSON. Ref XYZ12345.",
            expectedRelevant = true,
            expectedNormalizedFragments =
                listOf("KES 2,500")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "merchant_payment_confirmation",
            title = "Planet Net",
            body =
                "Payment of Kshs. 2,000 has been received for your internet account.",
            expectedRelevant = true,
            expectedNormalizedFragments =
                listOf("KES 2,000")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "investment_execution",
            title = "Zidi",
            body =
                "You have successfully bought 3 EQA shares valued at Kenya Shillings 3,021 via code UFT12345.",
            expectedRelevant = true,
            expectedNormalizedFragments =
                listOf("KES 3,021")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "investment_order_submitted",
            title = "Zidi",
            body =
                "Your buy order of 10 SAMPLE shares has been submitted.",
            expectedRelevant = true
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "safaricom_airtime_balance_only",
            title = "Safaricom",
            body =
                "Your airtime balance is Sh73.00.",
            expectedRelevant = false,
            expectedNormalizedFragments =
                listOf("KES 73.00")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "safaricom_bundle_promotion",
            title = "Safaricom",
            body =
                "Flash offer! Get 500MB today for Kshs 20. Dial *544# to buy.",
            expectedRelevant = false,
            expectedNormalizedFragments =
                listOf("KES 20")
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "google_verification_code",
            title = "Google",
            body =
                "Your Google verification code is 449211. Do not share this code.",
            expectedRelevant = false
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "discord_login",
            title = "Discord",
            body =
                "New login detected. Use code 123456 to verify your account.",
            expectedRelevant = false
        ),
        FinancialIntelligenceV2FoundationFixture(
            id = "personal_message_with_number",
            title = "0712000000",
            body =
                "Meet me at 5:30 and remember to bring the charger.",
            expectedRelevant = false
        )
    )
}
