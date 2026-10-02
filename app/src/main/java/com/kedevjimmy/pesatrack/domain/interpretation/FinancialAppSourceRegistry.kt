package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory

data class FinancialAppSourceProfile(
    val packageName: String,
    val bankId: String
)

object FinancialAppSourceRegistry {
    private val profiles =
        listOf(
            FinancialAppSourceProfile(
                packageName =
                    "com.kcb.mobilebanking.android.mbp",
                bankId = "kcb"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "ke.co.equitygroup.equitymobile",
                bankId = "equity"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.mcoopcash.retail6",
                bankId = "cooperative"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.nicbank.android",
                bankId = "ncba"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.absa.ke.mobile.android.ui",
                bankId = "absa"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.cfcstanbicbank.app",
                bankId = "stanbic"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.dtb.app",
                bankId = "dtb"
            )
        )

    fun resolve(
        packageName: String?
    ): FinancialAccountReference? {
        val normalizedPackage =
            packageName
                ?.trim()
                ?.lowercase()
                ?.takeIf(
                    String::isNotBlank
                )
                ?: return null

        val profile =
            profiles.firstOrNull {
                it.packageName ==
                    normalizedPackage
            } ?: return null

        val bank =
            KenyanBankDirectory.banks
                .firstOrNull {
                    it.id ==
                        profile.bankId
                } ?: return null

        return FinancialAccountReference(
            entityKey =
                "provider:KENYAN_BANK_" +
                    bank.id
                        .uppercase()
                        .replace("-", "_"),
            displayName = bank.name,
            providerKey =
                "KENYAN_BANK_" +
                    bank.id
                        .uppercase()
                        .replace("-", "_"),
            accountTypeHint =
                AccountType.BANK,
            ownershipHint =
                AccountOwnershipHint.UNKNOWN,
            nature =
                FinancialAccountNature.ASSET
        )
    }

    fun isKnownPackage(
        packageName: String?
    ): Boolean =
        resolve(packageName) != null
}
