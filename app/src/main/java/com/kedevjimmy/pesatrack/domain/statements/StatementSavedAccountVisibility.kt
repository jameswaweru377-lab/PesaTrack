package com.kedevjimmy.pesatrack.domain.statements

/** Routes imported source accounts without changing verified report membership. */
object StatementSavedAccountVisibility {
    data class Index(val accountIds: List<String>, val bankAccountIds: Set<String>) {
        fun isBank(accountId: String): Boolean = accountId in bankAccountIds
    }

    fun index(
        walletAccountIds: List<String>,
        verifiedBankAccountIds: Set<String>,
        limitedBankAccountIds: Set<String>
    ): Index {
        val banks = verifiedBankAccountIds + limitedBankAccountIds
        return Index((walletAccountIds + banks).distinct(), banks)
    }
}
