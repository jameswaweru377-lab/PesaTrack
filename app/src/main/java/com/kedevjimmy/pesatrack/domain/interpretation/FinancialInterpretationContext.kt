package com.kedevjimmy.pesatrack.domain.interpretation

data class FinancialInterpretationContext(
    val learnedAccounts: List<FinancialAccountReference> =
        emptyList()
) {
    fun resolveLearnedAccount(
        rawLabel: String
    ): FinancialAccountReference? {
        val normalized = normalize(rawLabel)
        if (normalized.isBlank()) return null

        return learnedAccounts.firstOrNull { account ->
            normalize(account.displayName) == normalized
        }
    }

    fun observedAccountForTitle(
        title: String?
    ): FinancialAccountReference? {
        val normalized =
            title?.let(::normalize)
                ?.takeIf { it.isNotBlank() }
                ?: return null

        return learnedAccounts.firstOrNull { account ->
            account.ownershipHint ==
                AccountOwnershipHint.USER_OWNED &&
                account.nature ==
                    FinancialAccountNature.ASSET &&
                normalize(account.displayName) == normalized
        }
    }

    private fun normalize(
        value: String
    ): String =
        value.uppercase()
            .replace(
                Regex("""[^A-Z0-9]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()

    companion object {
        val EMPTY = FinancialInterpretationContext()
    }
}
