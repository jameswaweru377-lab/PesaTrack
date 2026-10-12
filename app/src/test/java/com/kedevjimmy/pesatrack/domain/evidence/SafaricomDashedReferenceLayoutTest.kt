package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/** All references and amounts are invented; no private PDF text is retained. */
class SafaricomDashedReferenceLayoutTest {
    private val zone=TimeZone.getTimeZone("Africa/Nairobi")
    private val header="""
        M-PESA STATEMENT
        Statement Period: 1 Oct 2026 - 11 Oct 2026
        SUMMARY
        DETAILED STATEMENT
        Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance
    """.trimIndent()+"\n"
    private val newer="QAB1234569 2026-10-10 10:30:00 Shop purchase Completed -20.00 490.00"
    private val older="QAB1234567 2026-10-08 10:30:00 Taxi payment Completed -50.00 410.00"
    private fun review(middle: String) =
        PdfStatementReview.read(listOf(header+newer+"\n"+middle+"\n"+older),zone)

    @Test fun reversalReferenceBeforeMoneyCanWrapAfterCompletedStatus() {
        val middle="QAB1234568 2026-10-09 10:30:00 Money reversal via API\n"+
            "Completed\nfrom -\n1234567890\n100.00\n510.00"
        val result=review(middle)
        assertEquals(result.problems.joinToString(),listOf(-2000L,10000L,-5000L),
            result.rows.map {it.signedMinor})
        assertTrue(result.rows[1].description.contains("from - 1234567890"))
    }

    @Test fun reversalReferenceAfterMoneyCanWrapWithoutBecomingExtraColumn() {
        val middle="QAB1234568 2026-10-09 10:30:00 Money reversal via API\n"+
            "Completed 100.00 510.00\nfrom -\n1234567890"
        val result=review(middle)
        assertEquals(result.problems.joinToString(),3,result.rows.size)
        assertEquals(10000L,result.rows[1].signedMinor)
        assertTrue(result.rows[1].description.contains("from - 1234567890"))
    }

    @Test fun attachedDashReferenceIsNotMisreadAsNegativeAmount() {
        val middle="QAB1234568 2026-10-09 10:30:00 Money reversal via API\n"+
            "Completed 100.00 510.00\nfrom -1234567890"
        val result=review(middle)
        assertEquals(result.problems.joinToString(),3,result.rows.size)
        assertTrue(result.rows[1].description.contains("from -1234567890"))
    }

    @Test fun unlabelledExtraNumericTokensStillRejectAllRows() {
        val middle="QAB1234568 2026-10-09 10:30:00 Money reversal\n"+
            "Completed 100.00 510.00\n- 1234567890"
        val result=review(middle)
        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.any {it.contains("Page 1, receipt row 2")})
    }

    @Test fun numericMoneyAfterWrappedReferenceRemainsAmbiguous() {
        val middle="QAB1234568 2026-10-09 10:30:00 Money reversal\n"+
            "Completed 100.00 510.00\nfrom - 1234567890\n70.00"
        val result=review(middle)
        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.any {it.contains("Page 1, receipt row 2")})
    }

    @Test fun recoveringReferenceNeverBypassesRunningBalanceProof() {
        val middle="QAB1234568 2026-10-09 10:30:00 Money reversal\n"+
            "Completed 100.00 520.00\nfrom - 1234567890"
        val result=review(middle)
        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.any {it.contains("balances")})
    }
}
