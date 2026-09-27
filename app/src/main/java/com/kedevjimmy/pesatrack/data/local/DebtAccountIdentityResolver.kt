package com.kedevjimmy.pesatrack.data.local

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import java.security.MessageDigest

data class DebtAccountIdentity(
    val id: String,
    val providerKey: String,
    val name: String
)

object DebtAccountIdentityResolver {
    private val legacyIds = mapOf(
        "SAFARICOM_FULIZA" to
            DebtStore.FULIZA_ACCOUNT_ID
    )

    fun resolve(
        liability: FinancialAccountReference
    ): DebtAccountIdentity {
        val providerKey =
            liability.providerKey
                ?: "DISCOVERED:" +
                    liability.entityKey

        val id = legacyIds[providerKey]
            ?: "debt-" + shortHash(providerKey)

        return DebtAccountIdentity(
            id = id,
            providerKey = providerKey,
            name = liability.displayName
        )
    }

    private fun shortHash(
        value: String
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .take(10)
            .joinToString("") { byte ->
                "%02x".format(byte)
            }
}
