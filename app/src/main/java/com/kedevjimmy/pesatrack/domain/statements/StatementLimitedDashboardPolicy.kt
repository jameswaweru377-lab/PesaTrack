package com.kedevjimmy.pesatrack.domain.statements

/** Presentation evidence for one reconciled document; never classifies or posts money. */
object StatementLimitedDashboardPolicy {
    data class Dashboard(
        val report: StatementGrandTotalReconciledPolicy.Report,
        val largestOutflowRowNumbers: List<Int>
    )

    fun project(
        sourceId: String,
        expectedRows: Int,
        expectedMoneyInMinor: Long,
        expectedMoneyOutMinor: Long,
        rows: List<StatementGrandTotalReconciledPolicy.Row>
    ): Dashboard? {
        val report = StatementGrandTotalReconciledPolicy.project(
            sourceId, expectedRows, expectedMoneyInMinor, expectedMoneyOutMinor, rows
        ) ?: return null
        val largest = rows.asSequence().filter { it.movementMinor < 0L }
            .sortedWith(compareBy<StatementGrandTotalReconciledPolicy.Row> { it.movementMinor }
                .thenBy { it.number })
            .take(5).map { it.number }.toList()
        return Dashboard(report, largest)
    }
}
