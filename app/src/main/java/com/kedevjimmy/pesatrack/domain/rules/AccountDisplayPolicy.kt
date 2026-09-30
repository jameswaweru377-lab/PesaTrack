package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount

data class TrackedAccountGroup(
    val key: String,
    val displayName: String,
    val type: AccountType,
    val accounts: List<MoneyAccount>
) {
    val accountIds: Set<String> =
        accounts.map { it.id }.toSet()

    val memberNames: Set<String> =
        accounts.map { it.name }.toSet()
}

object AccountDisplayPolicy {
    private val supportedTypes =
        setOf(
            AccountType.MOBILE_WALLET,
            AccountType.BANK,
            AccountType.SAVINGS
        )

    private val genericLabels =
        setOf(
            "account",
            "available",
            "balance",
            "closing",
            "current",
            "new",
            "unknown",
            "unknown account",
            "your"
        )

    fun isConfirmedOwned(
        account: MoneyAccount,
        confirmedAccountIds: Set<String>,
        includeCash: Boolean = false
    ): Boolean {
        if (!account.enabled) {
            return false
        }

        if (
            includeCash &&
            account.type ==
                AccountType.CASH
        ) {
            return true
        }

        return account.id in
            confirmedAccountIds ||
            !account.identifier
                .isNullOrBlank()
    }

    fun groups(
        accounts: List<MoneyAccount>
    ): List<TrackedAccountGroup> =
        accounts
            .asSequence()
            .filter { it.enabled }
            .filter { it.type in supportedTypes }
            .filter {
                isMeaningfulAccountName(
                    it.name
                )
            }
            .groupBy(::groupKey)
            .values
            .map { members ->
                val first =
                    members.first()
                val canonical =
                    canonicalDisplayName(
                        first
                    )
                TrackedAccountGroup(
                    key =
                        normalize(canonical),
                    displayName = canonical,
                    type =
                        groupType(members),
                    accounts =
                        members.sortedBy {
                            it.name.lowercase()
                        }
                )
            }
            .sortedWith(
                compareBy<
                    TrackedAccountGroup
                > {
                    typeOrder(it.type)
                }.thenBy {
                    it.displayName.lowercase()
                }
            )

    fun isMeaningfulAccountName(
        raw: String
    ): Boolean {
        val normalized =
            normalize(raw)
        return normalized.isNotBlank() &&
            normalized !in genericLabels
    }

    fun matchesSource(
        group: TrackedAccountGroup,
        sourceName: String
    ): Boolean {
        val source =
            canonicalName(
                sourceName,
                group.type
            )
        val sourceKey =
            normalize(source)

        if (
            sourceKey ==
            normalize(group.displayName)
        ) {
            return true
        }

        return group.memberNames.any {
            normalize(
                canonicalName(
                    it,
                    group.type
                )
            ) == sourceKey
        }
    }

    private fun groupKey(
        account: MoneyAccount
    ): String {
        val normalized =
            normalize(account.name)
        if (
            account.type ==
                AccountType.SAVINGS &&
            normalized.startsWith(
                "m shwari"
            )
        ) {
            return "m-shwari"
        }

        return normalize(
            canonicalDisplayName(
                account
            )
        )
    }

    private fun canonicalDisplayName(
        account: MoneyAccount
    ): String =
        if (
            account.type ==
                AccountType.SAVINGS &&
            normalize(account.name)
                .startsWith(
                    "m shwari"
                )
        ) {
            "M-Shwari"
        } else {
            canonicalName(
                account.name,
                account.type
            )
        }

    private fun canonicalName(
        raw: String,
        type: AccountType
    ): String =
        if (type == AccountType.BANK) {
            KenyanBankDirectory
                .matchSender(raw)
                ?.name
                ?: raw.trim()
        } else {
            raw.trim()
        }

    private fun groupType(
        accounts: List<MoneyAccount>
    ): AccountType =
        when {
            accounts.any {
                it.type ==
                    AccountType.MOBILE_WALLET
            } ->
                AccountType.MOBILE_WALLET

            accounts.any {
                it.type ==
                    AccountType.BANK
            } ->
                AccountType.BANK

            else ->
                AccountType.SAVINGS
        }

    private fun typeOrder(
        type: AccountType
    ): Int =
        when (type) {
            AccountType.MOBILE_WALLET -> 0
            AccountType.BANK -> 1
            AccountType.SAVINGS -> 2
            else -> 3
        }

    private fun normalize(
        value: String
    ): String =
        value.lowercase()
            .replace(
                Regex("""[^a-z0-9]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
}
