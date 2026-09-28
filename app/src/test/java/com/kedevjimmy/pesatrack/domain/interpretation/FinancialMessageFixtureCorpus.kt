package com.kedevjimmy.pesatrack.domain.interpretation

enum class FixtureCoverage {
    GENERIC_COMPLETE,
    GENERIC_SEMANTICS_LEGACY_PERSISTENCE,
    LEGACY_REQUIRED,
    NON_FINANCIAL
}

data class ExpectedBalanceFixture(
    val providerKey: String?,
    val displayName: String,
    val amountMinor: Long
)

data class FinancialMessageFixture(
    val id: String,
    val title: String?,
    val body: String,
    val coverage: FixtureCoverage,
    val transactionCode: String?,
    val expectedKinds: List<CanonicalFinancialRecordKind>,
    val targetKinds: List<CanonicalFinancialRecordKind>,
    val expectedBalances: List<ExpectedBalanceFixture>
)

object FinancialMessageFixtureCorpus {
    val all = listOf(
        FinancialMessageFixture(
            id = "mshwari_to_mpesa",
            title = "MPESA",
            body =
                "TST12ABC01 Confirmed. Ksh 7,000.00 transferred from M-Shwari account on 28/9/26 at 6:00 PM. M-Shwari balance is Ksh 543.00. M-PESA balance is Ksh 7,789.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC01",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.TRANSFER
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.TRANSFER
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "NCBA_MSHWARI_SAVINGS",
                    displayName = "M-Shwari",
                    amountMinor = 54_300L
                ),
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 778_900L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_to_mshwari",
            title = "MPESA",
            body =
                "TST12ABC02 Confirmed. Ksh 2,500.00 transferred to M-Shwari account on 28/9/26 at 6:15 PM. M-Shwari balance is Ksh 3,043.00. New M-PESA balance is Ksh 5,289.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC02",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.TRANSFER
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.TRANSFER
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "NCBA_MSHWARI_SAVINGS",
                    displayName = "M-Shwari",
                    amountMinor = 304_300L
                ),
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 528_900L
                )
            )
        ),
        FinancialMessageFixture(
            id = "unknown_account_to_mpesa",
            title = "MPESA",
            body =
                "TST12ABC03 Confirmed. Ksh 5,000.00 transferred from PesaNova account on 28/9/26 at 7:00 PM. PesaNova balance is Ksh 12,400.00. M-PESA balance is Ksh 8,100.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC03",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.TRANSFER
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.TRANSFER
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = null,
                    displayName = "PesaNova",
                    amountMinor = 1_240_000L
                ),
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 810_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "ordinary_mpesa_payment",
            title = "MPESA",
            body =
                "TST12ABC04 Confirmed. Ksh 450.00 sent to SAMPLE PERSON 0712000000 on 28/9/26 at 7:10 PM. New M-PESA balance is Ksh 7,650.00. Transaction cost, Ksh 7.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC04",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT,
                CanonicalFinancialRecordKind.FEE
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT,
                CanonicalFinancialRecordKind.FEE
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 765_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "ordinary_mpesa_receipt",
            title = "MPESA",
            body =
                "TST12ABC05 Confirmed. You have received Ksh 2,000.00 from SAMPLE PERSON 0712000001 on 28/9/26 at 7:20 PM. New M-PESA balance is Ksh 9,650.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC05",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.RECEIPT
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.RECEIPT
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 965_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_buy_goods_paid_to",
            title = "MPESA",
            body =
                "TST12ABC11 Confirmed. Ksh 850.00 paid to SAMPLE SHOP on 28/9/26 at 7:25 PM. New M-PESA balance is Ksh 8,800.00. Transaction cost, Ksh 0.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC11",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 880_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_paybill_paid_to",
            title = "MPESA",
            body =
                "TST12ABC12 Confirmed. Ksh 1,200.00 paid to SAMPLE UTILITY for account 123456 on 28/9/26 at 7:28 PM. New M-PESA balance is Ksh 7,600.00. Transaction cost, Ksh 20.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC12",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT,
                CanonicalFinancialRecordKind.FEE
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT,
                CanonicalFinancialRecordKind.FEE
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 760_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "fuliza_drawdown",
            title = "MPESA",
            body =
                "TST12ABC06 Confirmed. Fuliza M-PESA amount is KSh 167.00. Access Fee charged KSh 1.67. Total Fuliza M-PESA outstanding amount is KSh 1086.51 due on 18/10/26.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC06",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.DEBT_DRAWDOWN,
                CanonicalFinancialRecordKind.FEE
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.DEBT_DRAWDOWN,
                CanonicalFinancialRecordKind.FEE
            ),
            expectedBalances = emptyList()
        ),
        FinancialMessageFixture(
            id = "fuliza_partial_repayment",
            title = "MPESA",
            body =
                "TST12ABC07 Confirmed. Ksh 220.00 from your M-PESA has been used to partially pay your outstanding Fuliza M-PESA. Your available Fuliza M-PESA limit is Ksh 589.09. Your M-PESA balance is Ksh 0.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC07",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.DEBT_REPAYMENT,
                CanonicalFinancialRecordKind.AVAILABLE_CREDIT_OBSERVATION
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.DEBT_REPAYMENT,
                CanonicalFinancialRecordKind.AVAILABLE_CREDIT_OBSERVATION
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 0L
                )
            )
        ),
        FinancialMessageFixture(
            id = "fuliza_full_repayment",
            title = "MPESA",
            body =
                "TST12ABC08 Confirmed. Ksh 1,381.00 from your M-PESA has been used to fully pay your outstanding Fuliza M-PESA. Available Fuliza balance limit is Ksh 1,440.00. Your M-PESA balance is Ksh 13,000.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC08",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.DEBT_REPAYMENT,
                CanonicalFinancialRecordKind.AVAILABLE_CREDIT_OBSERVATION
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.DEBT_REPAYMENT,
                CanonicalFinancialRecordKind.AVAILABLE_CREDIT_OBSERVATION
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 1_300_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "merchant_refund",
            title = "MPESA",
            body =
                "TST12ABC09 Confirmed. You have received Ksh 220.00 from SAMPLE RIDE LIMITED 3000000 on 28/9/26 at 7:30 PM. New M-PESA balance is Ksh 220.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC09",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.RECEIPT
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.RECEIPT
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 22_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_reversal",
            title = "MPESA",
            body =
                "TST12ABC10 Confirmed. The reversal of transaction TST12ABC04 of Ksh 450.00 has been completed on 28/9/26 at 7:40 PM. New M-PESA balance is Ksh 8,100.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC10",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.REVERSAL
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.REVERSAL
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 810_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_transferred_to_unknown_account",
            title = "MPESA",
            body =
                "TST12ABC13 Confirmed. Ksh 10,000.00 transferred to KCB ACCOUNT on 28/9/26 at 9:54 PM. New M-PESA balance is Ksh 2,500.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC13",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.TRANSFER
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.TRANSFER
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 250_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_sent_to_masked_phone",
            title = "MPESA",
            body =
                "TST12ABC14 Confirmed. Ksh 900.00 sent to SAMPLE PERSON 2547****321 on 28/9/26 at 8:10 AM. New M-PESA balance is Ksh 11,950.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC14",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 1_195_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_cash_withdrawal",
            title = "MPESA",
            body =
                "TST12ABC15 Confirmed. Ksh 1,000.00 withdrawn from M-PESA at SAMPLE AGENT on 28/9/26 at 8:20 AM. New M-PESA balance is Ksh 10,950.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC15",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.CASH_WITHDRAWAL
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.CASH_WITHDRAWAL
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 1_095_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_cash_deposit",
            title = "MPESA",
            body =
                "TST12ABC16 Confirmed. Ksh 2,000.00 deposited into your M-PESA on 28/9/26 at 8:30 AM. New M-PESA balance is Ksh 12,950.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC16",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.CASH_DEPOSIT
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.CASH_DEPOSIT
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 1_295_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "mpesa_airtime_purchase",
            title = "MPESA",
            body =
                "TST12ABC17 Confirmed. Ksh 100.00 airtime purchased on 28/9/26 at 8:40 AM. New M-PESA balance is Ksh 12,850.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC17",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.PAYMENT
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 1_285_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "unknown_wording_single_amount",
            title = "MPESA",
            body =
                "TST12ABC18 Confirmed. Ksh 640.00 processed through M-PESA on 28/9/26 at 9:10 AM. New M-PESA balance is Ksh 12,210.00.",
            coverage = FixtureCoverage.GENERIC_COMPLETE,
            transactionCode = "TST12ABC18",
            expectedKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.UNRESOLVED_MOVEMENT
            ),
            targetKinds = listOf(
                CanonicalFinancialRecordKind.BALANCE_OBSERVATION,
                CanonicalFinancialRecordKind.UNRESOLVED_MOVEMENT
            ),
            expectedBalances = listOf(
                ExpectedBalanceFixture(
                    providerKey = "SAFARICOM_MPESA",
                    displayName = "M-Pesa",
                    amountMinor = 1_221_000L
                )
            )
        ),
        FinancialMessageFixture(
            id = "airtime_balance_only",
            title = "Safaricom",
            body = "Your airtime balance is Ksh 73.00.",
            coverage = FixtureCoverage.NON_FINANCIAL,
            transactionCode = null,
            expectedKinds = emptyList(),
            targetKinds = emptyList(),
            expectedBalances = emptyList()
        )
    )
}
