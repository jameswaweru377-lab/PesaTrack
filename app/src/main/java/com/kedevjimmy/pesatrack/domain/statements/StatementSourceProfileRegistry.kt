package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.model.AccountType

/** Declarative issuer vocabulary. A layout profile never bypasses full-document admission. */
data class StatementSourceProfile(
    val providerKey: String,
    val label: String,
    val accountType: AccountType,
    val currency: String,
    val table: StatementFormatProfile,
    val sampleValidatedFormats: Set<String> = emptySet()
)

object StatementSourceProfileRegistry {
    private val profiles = listOf(
        StatementSourceProfile(
            providerKey = "KENYAN_BANK_EQUITY",
            label = "Equity Bank Kenya",
            accountType = AccountType.BANK,
            currency = "KES",
            table = StatementFormatProfile(
                // Value is still unconfirmed; never admit a populated value as an amount.
                aliases = mapOf(
                    "Value" to StatementField.VALUE_DATE,
                    "Payment reference" to StatementField.REFERENCE
                ),
                allowCreditDebitBalanceSuffix = true
            ),
            // Verified against the private selectable-text issuer PDF in a
            // memory-only local trial; Android device acceptance is still pending.
            sampleValidatedFormats = setOf("PDF")
        )
    ).associateBy { it.providerKey }

    fun forProvider(providerKey: String): StatementSourceProfile? = profiles[providerKey]

    fun importableBankProfiles(): List<StatementSourceProfile> =
        profiles.values.filter { it.accountType == AccountType.BANK && "PDF" in it.sampleValidatedFormats }
            .sortedBy { it.label }
}
