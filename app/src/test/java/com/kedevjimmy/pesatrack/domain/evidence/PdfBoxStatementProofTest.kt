package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.*
import org.junit.Assert.*
import org.junit.Test

class PdfBoxStatementProofTest {
    private fun leg(ref:String,day:String,description:String,paid:Long,debit:Long,balance:Long)=
        PdfBoxReceiptEvidence(1,1,ref,"2026-10-$day 10:30:00",description,emptyMap(),paid,debit,balance)

    @Test fun openingBalanceMustConstrainTheSameReceiptPathAsRunningBalances() {
        val rows=listOf(
            leg("QAB1234569","10","Payment",0,2000,49000),
            leg("QAB1234568","09","Credit",10000,0,51000),
            leg("QAB1234568","09","Information",0,0,50000)
        )
        // 500 + 100 = 600 is not involved: the opening is 400, so the
        // oldest event must close at 500, not the 510 anchor needed by the
        // newer -20 leg. Independently choosing endpoints would be false proof.
        assertNull(PdfBoxStatementFinancialAudit.verify(rows,listOf("Opening Balance: 400.00")))
    }

    @Test fun malformedOrConflictingDeclaredSummaryCannotBeTreatedAsAbsent() {
        val rows=listOf(
            leg("QAB1234569","10","Payment",0,2000,49000),
            leg("QAB1234568","09","Credit",10000,0,51000)
        )
        assertNull(PdfBoxStatementFinancialAudit.verify(rows,listOf("Total Paid In: unreadable")))
        assertNull(PdfBoxStatementFinancialAudit.verify(rows,
            listOf("Total Paid In: 100.00\nTotal Paid In: 101.00")))
    }

    @Test fun noSummaryDoesNotBecomeVerifiedSummaryOrOpeningProof() {
        val rows=listOf(
            leg("QAB1234569","10","Payment",0,2000,49000),
            leg("QAB1234568","09","Credit",10000,0,51000)
        )
        val proof=PdfBoxStatementFinancialAudit.verify(rows,emptyList())!!
        assertFalse(proof.totalsVerified || proof.openingVerified || proof.closingVerified)
        assertEquals(1,proof.verifiedTransitions)
    }

    @Test fun oneEventNeedsOpeningProofAndCheckedMoneyArithmetic() {
        val row=leg("QAB1234569","10","Credit",10000,0,51000)
        assertNull(PdfBoxStatementFinancialAudit.verify(listOf(row),emptyList()))
        assertNotNull(PdfBoxStatementFinancialAudit.verify(listOf(row),listOf("Opening Balance: 410.00")))
        assertNull(PdfBoxStatementFinancialAudit.verify(listOf(row),listOf("Opening Balance: 411.00")))
        assertNull(PdfBoxStatementFinancialAudit.verify(
            listOf(row.copy(paidInMinor=Long.MAX_VALUE)),listOf("Opening Balance: 410.00")))
    }

    @Test fun duplicateLegOrNonContiguousReceiptCannotSilentlyDisappear() {
        val first=leg("QAB1234569","10","Payment",0,2000,49000)
        val second=leg("QAB1234568","09","Credit",10000,0,51000)
        assertNull(PdfBoxStatementFinancialAudit.verify(listOf(first,first,second),emptyList()))
        assertNull(PdfBoxStatementFinancialAudit.verify(listOf(first,second,first.copy(description="Fee")),emptyList()))
    }
}
