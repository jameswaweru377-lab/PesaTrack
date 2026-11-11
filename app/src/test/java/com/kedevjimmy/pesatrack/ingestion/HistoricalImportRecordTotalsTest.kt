package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoricalImportRecordTotalsTest {
    @Test fun evidenceEventsAreNotReportedAsActivityTransactions() {
        val totals = HistoricalImportRecordTotals()
            .plus(ledgerTransactions = 2, otherFinancialRecords = 5)
            .plus(ledgerTransactions = 1, otherFinancialRecords = 0)

        assertEquals(3, totals.ledgerTransactions)
        assertEquals(5, totals.otherFinancialRecords)
        assertEquals(8, totals.allRecords)
    }

    @Test fun emptyAndDuplicateOnlyMessagesDoNotIncreaseAnyCount() {
        val totals = HistoricalImportRecordTotals().plus(0, 0)
        assertEquals(0, totals.ledgerTransactions)
        assertEquals(0, totals.otherFinancialRecords)
        assertEquals(0, totals.allRecords)
    }
}
