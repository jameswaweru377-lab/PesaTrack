package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory

data class FinancialAppSourceProfile(
    val packageName: String,
    val bankId: String? = null,
    val entityKey: String? = null,
    val displayName: String? = null,
    val providerKey: String? = null,
    val accountTypeHint: AccountType =
        AccountType.OTHER,
    val nature: FinancialAccountNature =
        FinancialAccountNature.UNKNOWN
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
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.imbank.digital.retail.app",
                bankId = "i-and-m"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.scb.breezebanking.ke",
                bankId =
                    "standard-chartered"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.mode.familykenya.ui",
                bankId = "family"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.ecobank.mobileapp5",
                bankId = "ecobank"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.mode.chasetest.ui",
                bankId = "sbm"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.mode.stima",
                entityKey =
                    "provider:KENYAN_SACCO_STIMA_DT",
                displayName =
                    "Stima DT Sacco",
                providerKey =
                    "KENYAN_SACCO_STIMA_DT"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "skyworld.mwalimu",
                entityKey =
                    "provider:KENYAN_SACCO_MWALIMU_NATIONAL",
                displayName =
                    "Mwalimu National Sacco",
                providerKey =
                    "KENYAN_SACCO_MWALIMU_NATIONAL"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.branch_international.branch.branch_demo_android",
                entityKey =
                    "provider:KENYAN_LENDER_BRANCH",
                displayName = "Branch",
                providerKey =
                    "KENYAN_LENDER_BRANCH"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.inventureaccess.safarirahisi",
                entityKey =
                    "provider:KENYAN_LENDER_TALA",
                displayName = "Tala",
                providerKey =
                    "KENYAN_LENDER_TALA"
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

        profile.bankId
            ?.let { bankId ->
                val bank =
                    KenyanBankDirectory.banks
                        .firstOrNull {
                            it.id ==
                                bankId
                        }
                        ?: return null

                return FinancialAccountReference(
                    entityKey =
                        "provider:KENYAN_BANK_" +
                            bank.id
                                .uppercase()
                                .replace("-", "_"),
                    displayName =
                        bank.name,
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

        val entityKey =
            profile.entityKey
                ?: return null
        val displayName =
            profile.displayName
                ?: return null

        return FinancialAccountReference(
            entityKey = entityKey,
            displayName = displayName,
            providerKey =
                profile.providerKey,
            accountTypeHint =
                profile.accountTypeHint,
            ownershipHint =
                AccountOwnershipHint.UNKNOWN,
            nature =
                profile.nature
        )
    }

    fun isKnownPackage(
        packageName: String?
    ): Boolean =
        resolve(packageName) != null
}
