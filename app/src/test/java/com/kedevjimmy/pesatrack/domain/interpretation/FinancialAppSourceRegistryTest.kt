package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAppSourceRegistryTest {
    @Test
    fun resolvesVerifiedKenyanBankPackagesWithoutClaimingOwnership() {
        val equity =
            FinancialAppSourceRegistry.resolve(
                "ke.co.equitygroup.equitymobile"
            )
        val kcb =
            FinancialAppSourceRegistry.resolve(
                "COM.KCB.MOBILEBANKING.ANDROID.MBP"
            )
        val coop =
            FinancialAppSourceRegistry.resolve(
                " com.mcoopcash.retail6 "
            )

        assertEquals(
            "Equity Bank Kenya",
            equity?.displayName
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            equity?.providerKey
        )
        assertEquals(
            AccountType.BANK,
            equity?.accountTypeHint
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            equity?.ownershipHint
        )

        assertEquals(
            "KCB Bank Kenya",
            kcb?.displayName
        )
        assertEquals(
            "Co-operative Bank of Kenya",
            coop?.displayName
        )
    }

    @Test
    fun resolvesAdditionalVerifiedBankAppPackages() {
        val expected =
            mapOf(
                "com.nicbank.android" to
                    "NCBA Bank Kenya",
                "com.absa.ke.mobile.android.ui" to
                    "Absa Bank Kenya",
                "com.cfcstanbicbank.app" to
                    "Stanbic Bank Kenya",
                "com.dtb.app" to
                    "Diamond Trust Bank Kenya"
            )

        expected.forEach {
                (packageName, name) ->
            assertEquals(
                name,
                FinancialAppSourceRegistry
                    .resolve(
                        packageName
                    )
                    ?.displayName
            )
        }
    }

    @Test
    fun unknownPackageDoesNotInventFinancialIdentity() {
        assertNull(
            FinancialAppSourceRegistry.resolve(
                "com.example.futurebank"
            )
        )
        assertFalse(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "com.example.futurebank"
                )
        )
        assertTrue(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "com.dtb.app"
                )
        )
    }
}
