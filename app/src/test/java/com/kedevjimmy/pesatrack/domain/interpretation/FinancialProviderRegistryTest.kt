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
}
