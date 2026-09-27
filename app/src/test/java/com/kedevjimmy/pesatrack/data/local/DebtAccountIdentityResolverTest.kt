package com.kedevjimmy.pesatrack.data.local

import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtAccountIdentityResolverTest {
    @Test
    fun preservesLegacyFulizaDebtAccountIdentity() {
        val liability =
            FinancialProviderRegistry.resolveAccount(
                "Fuliza M-PESA"
            )

        val identity =
            DebtAccountIdentityResolver.resolve(
                liability
            )

        assertEquals(
            DebtStore.FULIZA_ACCOUNT_ID,
            identity.id
        )
        assertEquals(
            "SAFARICOM_FULIZA",
            identity.providerKey
        )
    }

    @Test
    fun unknownLiabilityGetsStableSyntheticProviderIdentity() {
        val liability =
            FinancialAccountReference(
                entityKey =
                    "discovered:nova-credit",
                displayName = "Nova Credit",
                providerKey = null,
                accountTypeHint =
                    AccountType.OTHER,
                ownershipHint =
                    AccountOwnershipHint.USER_OWNED,
                nature =
                    FinancialAccountNature.LIABILITY
            )

        val first =
            DebtAccountIdentityResolver.resolve(
                liability
            )
        val second =
            DebtAccountIdentityResolver.resolve(
                liability
            )

        assertEquals(first, second)
        assertTrue(
            first.providerKey.startsWith(
                "DISCOVERED:"
            )
        )
        assertEquals(
            "Nova Credit",
            first.name
        )
    }
}
