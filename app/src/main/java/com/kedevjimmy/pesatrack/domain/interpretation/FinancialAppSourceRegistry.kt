package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory

enum class FinancialAppInstitutionKind {
    BANK,
    LENDER,
    FINANCIAL_PLATFORM
}

data class FinancialAppSourceProfile(
    val packageName: String,
    val displayName: String,
    val institutionKind:
        FinancialAppInstitutionKind,
    val bankId: String? = null
)

object FinancialAppSourceRegistry {
    private val profiles =
        listOf(
            FinancialAppSourceProfile(
                packageName =
                    "com.kcb.mobilebanking.android.mbp",
                displayName =
                    "KCB Bank Kenya",
                institutionKind =
                    FinancialAppInstitutionKind.BANK,
                bankId = "kcb"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "ke.co.equitygroup.equitymobile",
                displayName =
                    "Equity Bank Kenya",
                institutionKind =
                    FinancialAppInstitutionKind.BANK,
                bankId = "equity"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.mcoopcash.retail6",
                displayName =
                    "Co-operative Bank of Kenya",
                institutionKind =
                    FinancialAppInstitutionKind.BANK,
                bankId = "cooperative"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.nicbank.android",
                displayName =
                    "NCBA Bank Kenya",
                institutionKind =
                    FinancialAppInstitutionKind.BANK,
                bankId = "ncba"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.absa.ke.mobile.android.ui",
                displayName =
                    "Absa Bank Kenya",
                institutionKind =
                    FinancialAppInstitutionKind.BANK,
                bankId = "absa"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.cfcstanbicbank.app",
                displayName =
                    "Stanbic Bank Kenya",
                institutionKind =
                    FinancialAppInstitutionKind.BANK,
                bankId = "stanbic"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.dtb.app",
                displayName =
                    "Diamond Trust Bank Kenya",
                institutionKind =
                    FinancialAppInstitutionKind.BANK,
                bankId = "dtb"
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.branch_international.branch.branch_demo_android",
                displayName = "Branch",
                institutionKind =
                    FinancialAppInstitutionKind
                        .FINANCIAL_PLATFORM
            ),
            FinancialAppSourceProfile(
                packageName =
                    "com.inventureaccess.safarirahisi",
                displayName = "Tala",
                institutionKind =
                    FinancialAppInstitutionKind.LENDER
            ),
            FinancialAppSourceProfile(
                packageName =
                    "ke.co.absa.digitalwallet",
                displayName = "Timiza",
                institutionKind =
                    FinancialAppInstitutionKind
                        .FINANCIAL_PLATFORM
            )
        )

    fun identify(
        packageName: String?
    ): FinancialAppSourceProfile? {
        val normalizedPackage =
            normalizePackage(
                packageName
            ) ?: return null

        return profiles.firstOrNull {
            it.packageName ==
                normalizedPackage
        }
    }

    /**
     * A package can establish the institution that emitted
     * a notification, but it must not automatically become
     * a user-owned account. Only verified bank packages
     * produce an account-shaped source reference here, and
     * even those keep ownership UNKNOWN.
     *
     * Multi-product financial apps such as Branch/Timiza,
     * and lender apps such as Tala, stay source identity
     * only until the message itself supplies account/debt
     * semantics.
     */
    fun resolve(
        packageName: String?
    ): FinancialAccountReference? {
        val profile =
            identify(packageName)
                ?: return null
        val bankId =
            profile.bankId
                ?: return null

        val bank =
            KenyanBankDirectory.banks
                .firstOrNull {
                    it.id == bankId
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
        identify(packageName) != null

    private fun normalizePackage(
        packageName: String?
    ): String? =
        packageName
            ?.trim()
            ?.lowercase()
            ?.takeIf(
                String::isNotBlank
            )
}
