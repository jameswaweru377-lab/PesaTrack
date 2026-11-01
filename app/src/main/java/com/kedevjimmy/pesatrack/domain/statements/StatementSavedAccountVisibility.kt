package com.kedevjimmy.pesatrack.domain.statements

/** Routes imported source accounts without changing verified report membership. */
object StatementSavedAccountVisibility {
    data class Index(val accountIds: List<String>, val bankAccountIds: Set<String>) {
        fun isBank(accountId: String): Boolean = accountId in bankAccountIds
    }

    /** A display index only; limited sources never enter verified cash reports. */
    data class Overview(
        val accountIds: List<String>,
        val reportCount: Int,
        val limitedOnlyIds: Set<String>
    )

    fun overview(reportAccountIds: List<String>, limitedAccountIds: List<String>): Overview {
        val reports = reportAccountIds.distinct()
        val limitedOnly = limitedAccountIds.toSet() - reports.toSet()
        return Overview((reports + limitedAccountIds).distinct(), reports.size, limitedOnly)
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
