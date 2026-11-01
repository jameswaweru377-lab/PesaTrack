package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatementLimitedDashboardPolicyTest {
    private val source = "invented-source"
    private val rows = listOf(
        StatementGrandTotalReconciledPolicy.Row(source, 0, 1_780_000_000_000, 10_000),
        StatementGrandTotalReconciledPolicy.Row(source, 1, 1_780_000_000_001, -300),
        StatementGrandTotalReconciledPolicy.Row(source, 2, 1_780_000_000_002, -700)
    )

    @Test fun largestOutflowsAreSelectedOnlyAfterFullSourceReconciliation() {
        val dashboard = StatementLimitedDashboardPolicy.project(source, 3, 10_000, 1_000, rows)!!
        assertEquals(listOf(2, 1), dashboard.largestOutflowRowNumbers)
        assertEquals(9_000L, dashboard.report.netMinor)
    }

    @Test fun missingRowsAndMixedSourcesNeverProduceDashboardFigures() {
        assertNull(StatementLimitedDashboardPolicy.project(source, 3, 10_000, 1_000, rows.dropLast(1)))
        assertNull(StatementLimitedDashboardPolicy.project(source, 3, 10_000, 1_000,
            rows.mapIndexed { index, row -> if (index == 2) row.copy(sourceId = "other") else row }))
    }

    @Test fun equalOutflowsAreStableAndTheListIsBounded() {
        val outflows = (0 until 7).map { number ->
            StatementGrandTotalReconciledPolicy.Row(source, number, 1_780_000_000_000L + number, -100)
        }
        val dashboard = StatementLimitedDashboardPolicy.project(source, 7, 0, 700, outflows)!!
        assertEquals(listOf(0, 1, 2, 3, 4), dashboard.largestOutflowRowNumbers)
    }

    @Test fun invalidMagnitudeCannotBecomeAChartOrRanking() {
        val invalid = listOf(StatementGrandTotalReconciledPolicy.Row(source, 0, 1_780_000_000_000L, Long.MIN_VALUE))
        assertNull(StatementLimitedDashboardPolicy.project(source, 1, 0, Long.MAX_VALUE, invalid))
    }
}
