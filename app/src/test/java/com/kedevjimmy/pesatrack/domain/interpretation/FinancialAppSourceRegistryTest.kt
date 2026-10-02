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
                    "Diamond Trust Bank Kenya",
                "com.scb.breezebanking.ke" to
                    "Standard Chartered Bank Kenya",
                "com.mode.familykenya.ui" to
                    "Family Bank"
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
    fun resolvesVerifiedDigitalLenderPackagesWithoutInventingLoanMeaning() {
        val branch =
            FinancialAppSourceRegistry
                .resolve(
                    "com.branch_international.branch.branch_demo_android"
                )
        val tala =
            FinancialAppSourceRegistry
                .resolve(
                    " COM.INVENTUREACCESS.SAFARIRAHISI "
                )

        assertEquals(
            "Branch",
            branch?.displayName
        )
        assertEquals(
            "KENYAN_LENDER_BRANCH",
            branch?.providerKey
        )
        assertEquals(
            AccountType.OTHER,
            branch?.accountTypeHint
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            branch?.ownershipHint
        )
        assertEquals(
            FinancialAccountNature.UNKNOWN,
            branch?.nature
        )

        assertEquals(
            "Tala",
            tala?.displayName
        )
        assertEquals(
            "KENYAN_LENDER_TALA",
            tala?.providerKey
        )
        assertEquals(
            AccountType.OTHER,
            tala?.accountTypeHint
        )
        assertEquals(
            FinancialAccountNature.UNKNOWN,
            tala?.nature
        )
    }

    @Test
    fun resolvesVerifiedIMAndSaccoPackagesConservatively() {
        val im =
            FinancialAppSourceRegistry
                .resolve(
                    "com.imbank.digital.retail.app"
                )
        val stima =
            FinancialAppSourceRegistry
                .resolve(
                    "com.mode.stima"
                )
        val mwalimu =
            FinancialAppSourceRegistry
                .resolve(
                    " SKYWORLD.MWALIMU "
                )

        assertEquals(
            "I&M Bank Kenya",
            im?.displayName
        )
        assertEquals(
            AccountType.BANK,
            im?.accountTypeHint
        )
        assertEquals(
            FinancialAccountNature.ASSET,
            im?.nature
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            im?.ownershipHint
        )

        assertEquals(
            "Stima DT Sacco",
            stima?.displayName
        )
        assertEquals(
            "KENYAN_SACCO_STIMA_DT",
            stima?.providerKey
        )
        assertEquals(
            AccountType.OTHER,
            stima?.accountTypeHint
        )
        assertEquals(
            FinancialAccountNature.UNKNOWN,
            stima?.nature
        )
        assertEquals(
            AccountOwnershipHint.UNKNOWN,
            stima?.ownershipHint
        )

        assertEquals(
            "Mwalimu National Sacco",
            mwalimu?.displayName
        )
        assertEquals(
            "KENYAN_SACCO_MWALIMU_NATIONAL",
            mwalimu?.providerKey
        )
        assertEquals(
            AccountType.OTHER,
            mwalimu?.accountTypeHint
        )
        assertEquals(
            FinancialAccountNature.UNKNOWN,
            mwalimu?.nature
        )
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
        assertTrue(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "com.branch_international.branch.branch_demo_android"
                )
        )
        assertTrue(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "com.inventureaccess.safarirahisi"
                )
        )
        assertTrue(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "com.imbank.digital.retail.app"
                )
        )
        assertTrue(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "com.mode.stima"
                )
        )
        assertTrue(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "skyworld.mwalimu"
                )
        )
        assertTrue(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "com.scb.breezebanking.ke"
                )
        )
        assertTrue(
            FinancialAppSourceRegistry
                .isKnownPackage(
                    "com.mode.familykenya.ui"
                )
        )
    }
}
