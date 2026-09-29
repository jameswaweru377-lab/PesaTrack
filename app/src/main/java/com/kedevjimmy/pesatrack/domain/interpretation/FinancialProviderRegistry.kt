package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType

enum class DebtFundingBehavior {
    TRANSACTION_SHORTFALL,
    CASH_DISBURSEMENT,
    UNKNOWN
}

data class DebtProductBehavior(
    val fundingBehavior: DebtFundingBehavior,
    val fundingAccountProviderKey: String? = null,
    val preserveLegacyTransactionMetadata: Boolean = false,
    val eventNamespace: String? = null,
    val stableDebtAccountId: String? = null
)

data class FinancialProviderAccountKnowledge(
    val providerKey: String,
    val canonicalName: String,
    val accountType: AccountType,
    val ownershipHint: AccountOwnershipHint,
    val nature: FinancialAccountNature,
    val aliases: Set<String>,
    val debtBehavior: DebtProductBehavior? = null
)

data class FinancialMessageSourceProfile(
    val profileKey: String,
    val titleAliases: Set<String>,
    val defaultObservedAccountProviderKey: String
)

object FinancialProviderRegistry {
    private val known = listOf(
        // Specific products must come before broader family names.
        FinancialProviderAccountKnowledge(
            providerKey = "NCBA_MSHWARI_LOCK_SAVINGS",
            canonicalName = "M-Shwari Lock Savings",
            accountType = AccountType.SAVINGS,
            ownershipHint = AccountOwnershipHint.USER_OWNED,
            nature = FinancialAccountNature.ASSET,
            aliases = setOf(
                "M SHWARI LOCK SAVINGS",
                "MSHWARI LOCK SAVINGS"
            )
        ),
        FinancialProviderAccountKnowledge(
            providerKey = "NCBA_MSHWARI_LOAN",
            canonicalName = "M-Shwari Loan",
            accountType = AccountType.OTHER,
            ownershipHint = AccountOwnershipHint.UNKNOWN,
            nature = FinancialAccountNature.LIABILITY,
            aliases = setOf(
                "M SHWARI LOAN",
                "MSHWARI LOAN"
            ),
            debtBehavior = DebtProductBehavior(
                fundingBehavior =
                    DebtFundingBehavior.CASH_DISBURSEMENT,
                fundingAccountProviderKey =
                    "SAFARICOM_MPESA"
            )
        ),
        FinancialProviderAccountKnowledge(
            providerKey = "SAFARICOM_FULIZA",
            canonicalName = "Fuliza M-PESA",
            accountType = AccountType.OTHER,
            ownershipHint = AccountOwnershipHint.UNKNOWN,
            nature = FinancialAccountNature.LIABILITY,
            aliases = setOf(
                "FULIZA M PESA",
                "FULIZA MPESA",
                "FULIZA"
            ),
            debtBehavior = DebtProductBehavior(
                fundingBehavior =
                    DebtFundingBehavior.TRANSACTION_SHORTFALL,
                fundingAccountProviderKey =
                    "SAFARICOM_MPESA",
                preserveLegacyTransactionMetadata = true,
                eventNamespace = "fuliza",
                stableDebtAccountId = "debt-fuliza-mpesa"
            )
        ),
        FinancialProviderAccountKnowledge(
            providerKey = "SAFARICOM_MPESA",
            canonicalName = "M-Pesa",
            accountType = AccountType.MOBILE_WALLET,
            ownershipHint = AccountOwnershipHint.USER_OWNED,
            nature = FinancialAccountNature.ASSET,
            aliases = setOf(
                "M PESA",
                "MPESA"
            )
        ),
        FinancialProviderAccountKnowledge(
            providerKey = "NCBA_MSHWARI_SAVINGS",
            canonicalName = "M-Shwari",
            accountType = AccountType.SAVINGS,
            ownershipHint = AccountOwnershipHint.USER_OWNED,
            nature = FinancialAccountNature.ASSET,
            aliases = setOf(
                "M SHWARI",
                "MSHWARI"
            )
        )
    )

    private val messageSources = listOf(
        FinancialMessageSourceProfile(
            profileKey = "SAFARICOM_MPESA_MESSAGES",
            titleAliases = setOf(
                "M PESA",
                "MPESA"
            ),
            defaultObservedAccountProviderKey =
                "SAFARICOM_MPESA"
        )
    )

    fun resolveAccount(rawLabel: String): FinancialAccountReference {
        val cleaned = cleanAccountLabel(rawLabel)
        val normalized = normalize(cleaned)

        val match = known.firstOrNull { provider ->
            provider.aliases.any { alias ->
                normalized == alias ||
                    normalized.startsWith("$alias ")
            }
        }

        if (match != null) {
            return FinancialAccountReference(
                entityKey = "provider:" + match.providerKey,
                displayName = match.canonicalName,
                providerKey = match.providerKey,
                accountTypeHint = match.accountType,
                ownershipHint = match.ownershipHint,
                nature = match.nature
            )
        }

        return FinancialAccountReference(
            entityKey = "discovered:" + slug(normalized),
            displayName = cleaned,
            providerKey = null,
            accountTypeHint =
                inferAccountType(
                    normalize(rawLabel)
                ),
            ownershipHint = AccountOwnershipHint.UNKNOWN,
            nature = inferAccountNature(normalized)
        )
    }

    fun resolveObservedAccount(
        title: String?,
        balances: List<BalanceObservationFact>
    ): FinancialAccountReference? {
        val distinctAccounts =
            balances.map { it.account }
                .distinctBy { it.entityKey }

        val sourceProfile =
            messageSourceProfileFor(title)
        if (sourceProfile != null) {
            val preferred =
                referenceForProviderKey(
                    sourceProfile
                        .defaultObservedAccountProviderKey
                )
            if (preferred != null) {
                return distinctAccounts
                    .firstOrNull {
                        it.providerKey ==
                            preferred.providerKey
                    }
                    ?: preferred
            }
        }

        val sourceAccount =
            resolveMessageSourceAccount(title)
        val sourceBalance =
            sourceAccount?.let { source ->
                distinctAccounts.singleOrNull {
                    it.entityKey == source.entityKey &&
                        it.nature !=
                            FinancialAccountNature.LIABILITY
                }
            }
        if (sourceBalance != null && sourceAccount != null) {
            return sourceAccount.copy(
                ownershipHint =
                    AccountOwnershipHint.USER_OWNED,
                nature = FinancialAccountNature.ASSET
            )
        }

        val ownedAssets =
            distinctAccounts.filter {
                it.ownershipHint ==
                    AccountOwnershipHint.USER_OWNED &&
                    it.nature ==
                        FinancialAccountNature.ASSET
            }

        return ownedAssets.singleOrNull()
    }

    fun resolveMessageSourceAccount(
        title: String?
    ): FinancialAccountReference? {
        val sourceTitle = title
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val profile =
            messageSourceProfileFor(sourceTitle)
        if (profile != null) {
            return referenceForProviderKey(
                profile.defaultObservedAccountProviderKey
            )
        }

        val resolved = resolveAccount(sourceTitle)
        return if (
            resolved.nature ==
                FinancialAccountNature.UNKNOWN
        ) {
            resolved.copy(
                nature = FinancialAccountNature.ASSET
            )
        } else {
            resolved
        }
    }

    fun resolveOwnedMessageSourceAccount(
        title: String?
    ): FinancialAccountReference? =
        resolveMessageSourceAccount(title)
            ?.takeIf {
                it.nature != FinancialAccountNature.LIABILITY
            }
            ?.copy(
                ownershipHint =
                    AccountOwnershipHint.USER_OWNED,
                nature = FinancialAccountNature.ASSET
            )

    private fun messageSourceProfileFor(
        title: String?
    ): FinancialMessageSourceProfile? {
        val normalizedTitle =
            title?.let(::normalize)
                ?.takeIf { it.isNotBlank() }
                ?: return null

        return messageSources.firstOrNull { profile ->
            profile.titleAliases.any { alias ->
                normalizedTitle == alias ||
                    normalizedTitle.startsWith(
                        "$alias "
                    )
            }
        }
    }

    fun mpesaAccount(): FinancialAccountReference =
        resolveAccount("M-Pesa")

    fun cashAccount(): FinancialAccountReference =
        FinancialAccountReference(
            entityKey = "system:cash",
            displayName = "Cash",
            providerKey = null,
            accountTypeHint = AccountType.CASH,
            ownershipHint = AccountOwnershipHint.USER_OWNED,
            nature = FinancialAccountNature.ASSET
        )


    fun debtBehaviorFor(
        reference: FinancialAccountReference
    ): DebtProductBehavior? =
        reference.providerKey?.let { providerKey ->
            known.firstOrNull {
                it.providerKey == providerKey
            }?.debtBehavior
        }

    fun referenceForProviderKey(
        providerKey: String
    ): FinancialAccountReference? =
        known.firstOrNull {
            it.providerKey == providerKey
        }?.let { provider ->
            FinancialAccountReference(
                entityKey =
                    "provider:" +
                        provider.providerKey,
                displayName =
                    provider.canonicalName,
                providerKey =
                    provider.providerKey,
                accountTypeHint =
                    provider.accountType,
                ownershipHint =
                    provider.ownershipHint,
                nature =
                    provider.nature
            )
        }


    fun looksLikeNonFinancialBalanceLabel(rawLabel: String): Boolean {
        val normalized = normalize(rawLabel)
        return NON_FINANCIAL_BALANCE_WORDS.any { word ->
            normalized.contains(word)
        }
    }

    private fun cleanAccountLabel(raw: String): String =
        raw.trim()
            .replace(
                Regex(
                    """^(?:NEW|YOUR|CURRENT)\s+""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(
                Regex(
                    """\s+(?:ACCOUNT|WALLET)\s*$""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { "Unknown account" }

    private fun inferAccountType(normalized: String): AccountType =
        when {
            "BANK" in normalized ||
                "CURRENT" in normalized ||
                "SALARY" in normalized ->
                AccountType.BANK
            "SAVING" in normalized ->
                AccountType.SAVINGS
            "WALLET" in normalized ||
                "MOBILE" in normalized ->
                AccountType.MOBILE_WALLET
            else -> AccountType.OTHER
        }

    private fun inferAccountNature(
        normalized: String
    ): FinancialAccountNature =
        when {
            normalized == "LOAN" ||
                normalized.endsWith(" LOAN") ||
                " LOAN " in normalized ||
                normalized == "OVERDRAFT" ||
                normalized.endsWith(" OVERDRAFT") ||
                " OVERDRAFT " in normalized ->
                FinancialAccountNature.LIABILITY
            else -> FinancialAccountNature.UNKNOWN
        }

    private fun normalize(value: String): String =
        value.uppercase()
            .replace(Regex("""[^A-Z0-9]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun slug(value: String): String =
        value.lowercase()
            .replace(Regex("""[^a-z0-9]+"""), "-")
            .trim('-')
            .ifBlank { "unknown" }
            .take(64)

    private val NON_FINANCIAL_BALANCE_WORDS = setOf(
        "AIRTIME",
        "DATA",
        "BUNDLE",
        "MINUTE",
        "SMS BALANCE",
        "POINT"
    )
}
