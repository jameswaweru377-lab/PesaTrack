package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/** Fully invented cash legs and receipt references; never actual user data. */
class StatementReceiptBalanceAuditTest {
    private fun leg(ref: String, day: Long, movement: Long, balance: Long) =
        StatementBalanceLeg(ref,day,movement,balance)

    @Test fun cashLegsWithDifferentInternalBalanceReadingsCanReconcileAsWholeReceipt() {
        val t=1770000000000L
        val legs=listOf(
            leg("QAB1234569",t,-2000L,48000L),
            leg("QAB1234569",t,-500L,60000L),
            leg("QAB1234568",t-86400000L,10000L,50500L),
            leg("QAB1234568",t-86400000L,-1000L,77000L),
            leg("QAB1234567",t-172800000L,-5000L,41500L)
        )
        val result=StatementReceiptBalanceAudit.verify(legs)
        assertEquals(2,result.verifiedTransitions)
        assertEquals(2,result.totalTransitions)
        assertTrue(result.allVerified)
        assertTrue(result.unverifiedReferences.isEmpty())
    }

    @Test fun sameEventMayHaveThreeLegsWithOneConsistentBalanceAnchor() {
        val t=1770000000000L
        val legs=listOf(
            leg("QAB1234569",t,-2000L,46500L),
            leg("QAB1234569",t,500L,90000L),
            leg("QAB1234569",t,-1000L,80000L),
            leg("QAB1234568",t-1000,10000L,49000L),
            leg("QAB1234567",t-2000,-5000L,39000L)
        )
        val result=StatementReceiptBalanceAudit.verify(legs)
        assertEquals(2,result.verifiedTransitions)
        assertTrue(result.credibleLayout)
    }

    @Test fun oneUnreconciledEventInLongOtherwiseValidatedStatementIsFlaggedNotSilentlyIgnored() {
        val newestTime=1770000000000L
        val legs=(0 until 22).map {i ->
            // descending dates; every newer withdrawal should lower balance
            val balance=5000L-(21-i)*100L + (if(i==21) 17L else 0L)
            leg("QAB"+i.toString().padStart(7,'0'),newestTime-i*86400000L,-100L,balance)
        }
        // Fix the synthetic balance direction: newest lower than oldest.
        val corrected=legs.mapIndexed {i,x ->
            x.copy(balanceMinor=5000L-(21-i)*100L+(if(i==21) 17L else 0L))
        }
        val result=StatementReceiptBalanceAudit.verify(corrected)
        assertEquals(21,result.totalTransitions)
        assertEquals(20,result.verifiedTransitions)
        assertEquals(2,result.unverifiedReferences.size)
        assertTrue(result.credibleLayout)
    }

    @Test fun sparseBrokenBalancesFailClosedInsteadOfBecomingAccountFacts() {
        val t=1770000000000L
        val rows=listOf(
            leg("QAB1234569",t,-2000L,49000L),
            leg("QAB1234568",t-1000,10000L,61000L),
            leg("QAB1234567",t-2000,-5000L,41000L)
        )
        assertFalse(StatementReceiptBalanceAudit.verify(rows).credibleLayout)
    }

    @Test fun dateDisorderAndNonContiguousSharedReferenceAreNotAccepted() {
        val t=1770000000000L
        val disorder=listOf(leg("QAB1234569",t,100L,5100L),leg("QAB1234568",t+1000,-100L,5000L),leg("QAB1234567",t-1000,100L,5100L))
        assertThrows(IllegalArgumentException::class.java){StatementReceiptBalanceAudit.verify(disorder)}
        val nonAdjacent=listOf(leg("QAB1234569",t,-100L,4900L),leg("QAB1234568",t-1000,100L,5000L),leg("QAB1234569",t-2000,-100L,4900L))
        assertThrows(IllegalArgumentException::class.java){StatementReceiptBalanceAudit.verify(nonAdjacent)}
    }

    @Test fun highRiskUnverifiedTinyDocumentsCannotPassTheProofRatio() {
        val t=1770000000000L
        val rows=listOf(leg("QAB1234569",t,-100L,3000L),leg("QAB1234568",t-1000,-100L,6000L))
        val result=StatementReceiptBalanceAudit.verify(rows)
        assertFalse(result.credibleLayout)
        assertEquals(2,result.unverifiedReferences.size)
    }

    @Test fun syntheticTextImportPreservesSeparateReceiptLegsEvenWhenIndividualBalancesJump() {
        val meta = """
            M-PESA STATEMENT
            Statement Period: 01 Aug 2026 - 11 Oct 2026
            DETAILED STATEMENT
            Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance
        """.trimIndent()+"\n"
        val rows=listOf(
            "QAB1234569 2026-10-10 10:30:00 Merchant payment Completed -20.00 480.00",
            "QAB1234569 2026-10-10 10:30:00 Fee movement Completed -5.00 600.00",
            "QAB1234568 2026-10-09 10:30:00 Wallet deposit Completed 100.00 505.00",
            "QAB1234568 2026-10-09 10:30:00 Transfer fee Completed -10.00 777.00",
            "QAB1234567 2026-10-08 10:30:00 Transport Completed -50.00 415.00"
        )
        val result=PdfStatementReview.read(listOf(meta+rows.joinToString("\n")),TimeZone.getTimeZone("Africa/Nairobi"))
        assertEquals(result.problems.joinToString(),5,result.rows.size)
        assertTrue(result.balanceReviewReferences.isEmpty())
        assertEquals(2,result.verifiedReceiptTransitions)
    }
}
