package com.kedevjimmy.pesatrack.domain.statements

/** Annual evidence is not comparable to the week/month periods used by Reports. */
object SharedAccountingReportScopePolicy {
    fun forPeriod(period: String, year: Int, evidence: SharedAccountingViewPolicy.View?):
        SharedAccountingViewPolicy.View? =
        evidence?.takeIf { period == "Year" && it.year == year }
}
