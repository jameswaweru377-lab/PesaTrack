package com.kedevjimmy.pesatrack.domain.statements

/** Two independent money scopes. A matching suggestion never makes them additive. */
object CrossSourceTotalsPreviewPolicy {
    data class ActivityTotals(val incomeMinor: Long, val spendingMinor: Long)
    data class Preview(
        val activity: ActivityTotals?,
        val statementMoneyInMinor: Long?,
        val statementMoneyOutMinor: Long?,
        val statementStatus: StatementUnifiedOverviewPolicy.CashStatus,
        val strongMatchCount: Int,
        val unresolvedCount: Int,
        val comparable: Boolean = false
    )

    fun project(activity: ActivityTotals?, statement: StatementUnifiedOverviewPolicy.CashSummary,
                strongMatchCount: Int, unresolvedCount: Int): Preview {
        require(strongMatchCount >= 0 && unresolvedCount >= 0)
        require(activity == null || (activity.incomeMinor >= 0 && activity.spendingMinor >= 0))
        require(statement.moneyInMinor == null || statement.moneyInMinor >= 0)
        require(statement.moneyOutMinor == null || statement.moneyOutMinor >= 0)
        val available = statement.status == StatementUnifiedOverviewPolicy.CashStatus.AVAILABLE
        return Preview(activity, if (available) statement.moneyInMinor else null,
            if (available) statement.moneyOutMinor else null, statement.status,
            strongMatchCount, unresolvedCount)
    }
}
