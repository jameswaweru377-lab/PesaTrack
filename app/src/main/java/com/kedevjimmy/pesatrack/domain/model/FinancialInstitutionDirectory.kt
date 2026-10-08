package com.kedevjimmy.pesatrack.domain.model

import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry

data class FinancialInstitutionOption(
    val reference: FinancialAccountReference,
    val section: String,
    val requiresIdentifier: Boolean
)

object FinancialInstitutionDirectory {
    private val walletProviderKeys =
        listOf(
            "SAFARICOM_MPESA",
            "NCBA_MSHWARI_SAVINGS",
            "NCBA_MSHWARI_LOCK_SAVINGS",
            "KCB_MPESA_SAVINGS",
            "ABSA_TIMIZA_ACCOUNT"
        )

    val options: List<FinancialInstitutionOption> =
        buildList {
            walletProviderKeys.forEach { providerKey ->
                FinancialProviderRegistry
                    .referenceForProviderKey(providerKey)
                    ?.let { reference ->
                        add(
                            FinancialInstitutionOption(
                                reference = reference,
                                section = "Wallets & savings",
                                requiresIdentifier =
                                    providerKey ==
                                        "SAFARICOM_MPESA"
                            )
                        )
                    }
            }

            KenyanBankDirectory.banks.forEach { bank ->
                val providerKey =
                    "KENYAN_BANK_" +
                        bank.id.uppercase()
                            .replace("-", "_")
                add(
                    FinancialInstitutionOption(
                        reference =
                            FinancialAccountReference(
                                entityKey =
                                    "provider:$providerKey",
                                displayName =
                                    bank.name,
                                providerKey =
                                    providerKey,
                                accountTypeHint =
                                    AccountType.BANK,
                                ownershipHint =
                                    AccountOwnershipHint.UNKNOWN,
                                nature =
                                    FinancialAccountNature.ASSET
                            ),
                        section = "Banks",
                        requiresIdentifier = true
                    )
                )
            }
        }

    fun search(query: String): List<FinancialInstitutionOption> {
        val normalized =
            query.trim()
                .lowercase()
        if (normalized.isBlank()) {
            return options
        }

        return options.filter {
            it.reference.displayName
                .lowercase()
                .contains(normalized) ||
                it.reference.providerKey
                    .orEmpty()
                    .lowercase()
                    .contains(normalized)
        }
    }
}
