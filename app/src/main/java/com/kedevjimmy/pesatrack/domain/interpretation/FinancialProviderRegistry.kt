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
    val preserveLegacyTransactionMetadata: Boolean = false
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
                preserveLegacyTransactionMetadata = true
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
            accountTypeHint = inferAccountType(normalized),
            ownershipHint = AccountOwnershipHint.UNKNOWN,
            nature = FinancialAccountNature.UNKNOWN
        )
    }

    fun mpesaAccount(): FinancialAccountReference =
        resolveAccount("M-Pesa")

    fun debtBehaviorFor(
        reference: FinancialAccountReference
    ): DebtProductBehavior? =
        reference.providerKey?.let { providerKey ->
            known.firstOrNull {
                it.providerKey == providerKey
            }?.debtBehavior
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
