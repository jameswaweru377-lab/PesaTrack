package com.kedevjimmy.pesatrack.domain.everyday

import org.junit.Assert.*
import org.junit.Test

class StatementWriteValidationTest {
    private val row = StatementRow(1000, "Synthetic salary", 5000000, "FAKE-REF")
    @Test fun validPaymentAndReceiptRemainIndependentCandidates() {
        StatementCsv.validateRows(listOf(row, row.copy(signedMinor = -1000)), 2000)
    }
    @Test fun invalidDirectAdapterRowsFailBeforeLedgerPreview() {
        for (bad in listOf(row.copy(date = Long.MAX_VALUE), row.copy(date = 0), row.copy(signedMinor = 0),
            row.copy(signedMinor = Long.MIN_VALUE), row.copy(description = " "), row.copy(description = "x".repeat(201)),
            row.copy(reference = "x".repeat(101)))) {
            assertThrows(IllegalArgumentException::class.java) { StatementCsv.validateRows(listOf(bad), 2000) }
        }
    }
    @Test fun largeBatchesFailWithoutPartialValidation() {
        assertThrows(IllegalArgumentException::class.java) { StatementCsv.validateRows(List(5001) { row }, 2000) }
        StatementCsv.validateRows(List(5000) { row }, 2000)
        StatementCsv.validateRows(emptyList(), 2000)
    }
}
