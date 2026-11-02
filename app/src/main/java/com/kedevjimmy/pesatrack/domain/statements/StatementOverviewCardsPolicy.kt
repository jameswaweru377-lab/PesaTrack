package com.kedevjimmy.pesatrack.domain.statements

/** Never mistake gross withdrawals for expenses or tentative matches for posted transfers. */
object StatementOverviewCardsPolicy {
    data class Cards(
        val moneyInMinor: Long?,
        val verifiedExpensesMinor: Long?,
        val reviewedTransfersMinor: Long?,
        val grossMoneyOutMinor: Long?
    )

    fun project(cash: StatementUnifiedOverviewPolicy.CashSummary,
                evidence: SharedAccountingViewPolicy.View?): Cards {
        if (cash.status != StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE)
            return Cards(null, null, null, null)
        val transfer = evidence?.takeIf {
            it.status == SharedAccountingViewPolicy.Status.INDEPENDENT_SCOPES &&
                it.statementGrossInMinor == cash.moneyInMinor &&
                it.statementGrossOutMinor == cash.moneyOutMinor &&
                it.confirmedInternalTransfers > 0
        }?.reviewedInternalTransfersMinor
        // Expense classification for every verified account cash leg has not yet been proved.
        return Cards(cash.moneyInMinor, null, transfer, cash.moneyOutMinor)
    }
}
