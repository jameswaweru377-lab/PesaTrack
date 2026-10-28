package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.model.AccountType

/** Declarative issuer vocabulary. A layout profile is not permission to import an untested PDF. */
data class StatementSourceProfile(
    val providerKey: String,
    val label: String,
    val accountType: AccountType,
    val currency: String,
    val table: StatementFormatProfile,
    val formatsProvenOnDevice: Set<String> = emptySet()
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
                aliases = mapOf("Value" to StatementField.VALUE_DATE),
                allowCreditDebitBalanceSuffix = true
            )
        )
    ).associateBy { it.providerKey }

    fun forProvider(providerKey: String): StatementSourceProfile? = profiles[providerKey]
}
