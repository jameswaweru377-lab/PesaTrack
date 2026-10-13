package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

class PdfBoxVerificationReportTest {
    @Test fun completeCountsRoundTripWithoutFinancialAuthority() {
        val report=PdfBoxVerificationReport.decode(intArrayOf(881,879,850,1,0,1))
        assertEquals(881,report.receiptRows)
        assertEquals(879,report.cashRows)
        assertTrue(report.safeSummary().contains("not independently verified"))
        assertTrue(report.safeSummary().contains("Nothing has been imported"))
        assertArrayEquals(intArrayOf(881,879,850,1,0,1),report.encode())
    }
    @Test fun missingInvalidOrImpossibleProofCannotBecomeReviewAuthority() {
        for(values in listOf(intArrayOf(),intArrayOf(1,1,0,1,1),intArrayOf(881,882,850,1,1,1),
            intArrayOf(1,1,1,1,1,1),intArrayOf(881,880,850,2,1,1),intArrayOf(5001,1,0,0,0,0))) {
            assertThrows(IllegalArgumentException::class.java){PdfBoxVerificationReport.decode(values)}
        }
    }
}
