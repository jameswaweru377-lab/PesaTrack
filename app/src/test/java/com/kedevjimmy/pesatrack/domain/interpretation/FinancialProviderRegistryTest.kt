package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialProviderRegistryTest {
    @Test
    fun resolvesMshwariAsOwnedSavingsAccount() {
        val account =
            FinancialProviderRegistry.resolveAccount(
                "M-Shwari account"
            )

        assertEquals(
            "NCBA_MSHWARI_SAVINGS",
            account.providerKey
        )
        assertEquals(
            AccountType.SAVINGS,
            account.accountTypeHint
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            account.ownershipHint
        )
    }

    @Test
    fun doesNotTreatMshwariLoanAsSavingsAsset() {
        val account =
            FinancialProviderRegistry.resolveAccount(
                "M-Shwari Loan account"
            )

        assertEquals(
            "NCBA_MSHWARI_LOAN",
            account.providerKey
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            account.ownershipHint
        )
        assertEquals(
            AccountType.OTHER,
            account.accountTypeHint
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            account.nature
        )
    }

    @Test
    fun doesNotCollapseFulizaIntoMpesaWallet() {
        val account =
            FinancialProviderRegistry.resolveAccount(
                "Fuliza M-PESA account"
            )

        assertEquals(
            "SAFARICOM_FULIZA",
            account.providerKey
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            account.ownershipHint
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            account.nature
        )
    }

    @Test
    fun providerProfileDescribesFulizaAsTransactionShortfallFunding() {
        val account =
            FinancialProviderRegistry.resolveAccount(
                "Fuliza M-PESA"
            )
        val behavior =
            FinancialProviderRegistry
                .debtBehaviorFor(account)

        assertEquals(
            DebtFundingBehavior.TRANSACTION_SHORTFALL,
            behavior?.fundingBehavior
        )
        assertEquals(
            "SAFARICOM_MPESA",
            behavior?.fundingAccountProviderKey
        )
        assertEquals(
            true,
            behavior?.preserveLegacyTransactionMetadata
        )
        assertEquals(
            "fuliza",
            behavior?.eventNamespace
        )
        assertEquals(
            "debt-fuliza-mpesa",
            behavior?.stableDebtAccountId
        )
    }

    @Test
    fun providerProfileDescribesMshwariLoanAsCashDisbursement() {
        val account =
            FinancialProviderRegistry.resolveAccount(
                "M-Shwari Loan"
            )
        val behavior =
            FinancialProviderRegistry
                .debtBehaviorFor(account)

        assertEquals(
            DebtFundingBehavior.CASH_DISBURSEMENT,
            behavior?.fundingBehavior
        )
        assertEquals(
            "SAFARICOM_MPESA",
            behavior?.fundingAccountProviderKey
        )
    }


    @Test
    fun resolvesKcbMpesaAsOwnedSavingsAccount() {
        val account =
            FinancialProviderRegistry.resolveAccount(
                "KCB M-PESA Account"
            )

        assertEquals(
            "KCB_MPESA_SAVINGS",
            account.providerKey
        )
        assertEquals(
            AccountType.SAVINGS,
            account.accountTypeHint
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            account.ownershipHint
        )
        assertEquals(
            FinancialAccountNature.ASSET,
            account.nature
        )
    }

    @Test
    fun providerProfileKeepsKcbMpesaLoanInsideKcbMpesaDepositAccount() {
        val account =
            FinancialProviderRegistry.resolveAccount(
                "KCB M-PESA Loan"
            )
        val behavior =
            FinancialProviderRegistry
                .debtBehaviorFor(account)

        assertEquals(
            "KCB_MPESA_LOAN",
            account.providerKey
        )
        assertEquals(
            DebtFundingBehavior.CASH_DISBURSEMENT,
            behavior?.fundingBehavior
        )
        assertEquals(
            "KCB_MPESA_SAVINGS",
            behavior?.fundingAccountProviderKey
        )
    }

    @Test
    fun senderScopedKcbMpesaLoanRepaymentUsesLoanIdentity() {
        val account =
            FinancialProviderRegistry
                .resolveLiabilityInContext(
                    rawLabel = "Loan",
                    title = "KCB M-PESA",
                    body =
                        "Your loan repayment of KES 100.00 has been declined due to insufficient funds."
                )

        assertEquals(
            "KCB_MPESA_LOAN",
            account.providerKey
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            account.nature
        )
    }

    @Test
    fun resolvesMshwariLiabilityInsideMpesaMessageThread() {
        val account =
            FinancialProviderRegistry
                .resolveLiabilityInContext(
                    rawLabel = "M-Shwari",
                    title = "M-PESA",
                    body =
                        "Ksh 500.00 from your M-PESA has been used to partially pay your outstanding M-Shwari."
                )

        assertEquals(
            "NCBA_MSHWARI_LOAN",
            account.providerKey
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            account.nature
        )
    }

    @Test
    fun genericLoanInsideMpesaThreadUsesExplicitMshwariProductEvidence() {
        val account =
            FinancialProviderRegistry
                .resolveLiabilityInContext(
                    rawLabel = "Loan",
                    title = "M-PESA",
                    body =
                        "Your M-Shwari loan has been approved and Ksh985.00 has been deposited to your M-PESA account."
                )

        assertEquals(
            "NCBA_MSHWARI_LOAN",
            account.providerKey
        )
    }

    @Test
    fun messageSourceProfileAnchorsMpesaWithoutBalanceText() {
        val account =
            FinancialProviderRegistry
                .resolveObservedAccount(
                    title = "M-PESA",
                    balances = emptyList()
                )

        assertEquals(
            "SAFARICOM_MPESA",
            account?.providerKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            account?.ownershipHint
        )
    }

    @Test
    fun singleKnownOwnedBalanceCanAnchorUnknownMessageSource() {
        val mshwari =
            FinancialProviderRegistry.resolveAccount(
                "M-Shwari"
            )
        val account =
            FinancialProviderRegistry
                .resolveObservedAccount(
                    title = "NCBA",
                    balances = listOf(
                        BalanceObservationFact(
                            account = mshwari,
                            balanceMinor = 500_000L,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                )

        assertEquals(
            "NCBA_MSHWARI_SAVINGS",
            account?.providerKey
        )
    }

    @Test
    fun unknownBalanceDoesNotBecomeOwnedAccountByInference() {
        val unknown =
            FinancialProviderRegistry.resolveAccount(
                "PesaNova"
            )
        val account =
            FinancialProviderRegistry
                .resolveObservedAccount(
                    title = "PesaNova",
                    balances = listOf(
                        BalanceObservationFact(
                            account = unknown,
                            balanceMinor = 500_000L,
                            confidence =
                                InterpretationConfidence.HIGH
                        )
                    )
                )

        assertEquals(null, account)
    }

    @Test
    fun preservesUnknownInstitutionWithoutClaimingOwnership() {
        val account =
            FinancialProviderRegistry.resolveAccount(
                "PesaNova account"
            )

        assertEquals(null, account.providerKey)
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            account.ownershipHint
        )
        assertEquals("PesaNova", account.displayName)
    }

    @Test
    fun canonicalizesBulkBankSenderWithoutProviderSpecificParser() {
        val account =
            FinancialProviderRegistry
                .resolveOwnedMessageSourceAccount(
                    title = "Equity Bulk",
                    bankLike = true
                )

        assertEquals(
            "Equity Bank Kenya",
            account?.displayName
        )
        assertEquals(
            AccountType.BANK,
            account?.accountTypeHint
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            account?.ownershipHint
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            account?.providerKey
        )
    }

    @Test
    fun doesNotForceUnknownWalletSourceIntoBankWhenLanguageIsNotBankLike() {
        val account =
            FinancialProviderRegistry
                .resolveOwnedMessageSourceAccount(
                    title = "Future Wallet Alerts",
                    bankLike = false
                )

        assertEquals(
            "Future Wallet",
            account?.displayName
        )
        assertEquals(
            AccountType.MOBILE_WALLET,
            account?.accountTypeHint
        )
    }

}
