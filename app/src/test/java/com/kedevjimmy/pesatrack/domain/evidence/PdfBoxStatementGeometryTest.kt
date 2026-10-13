package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.MpesaPositionedRows
import com.kedevjimmy.pesatrack.domain.everyday.PdfBoxStatementGeometry
import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/**
 * Entirely invented PDFBox word coordinates, dates and transaction values.
 * These tests cannot replace a private-device PDF acceptance run.
 */
class PdfBoxStatementGeometryTest {
    private val zone=TimeZone.getTimeZone("Africa/Nairobi")
    private val heading="""
        M-PESA STATEMENT
        Statement Period: 01 Oct 2026 - 11 Oct 2026
        DETAILED STATEMENT
        Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance
    """.trimIndent()+"\n"
    private val first="QAB1234569 2026-10-10 10:30:00 Shop purchase Completed -20.00 490.00"
    private val second="QAB1234568 2026-10-09 10:30:00 Reversal via API Completed 100.00 510.00"
    private val oldest="QAB1234567 2026-10-08 10:30:00 Transport Completed -50.00 410.00"

    private fun word(value:String,x:Float,y:Float):PdfTextFragment =
        PdfTextFragment(value,x,y,x+(value.length*3.0f),y+8f)
    private fun textRow(code:String,day:String,time:String,description:String,
                        movement:String,balance:String,y:Float):List<PdfTextFragment> = listOf(
        word(code,35f,y),word(day,99f,y),word(time,166f,y),
        word(description,221f,y),word("Completed",330f,y),
        word(movement,406f,y),word(balance,522f,y)
    )
    private val headerGeometry=listOf(word("M-PESA",30f,9f),word("STATEMENT",72f,9f))
    private fun geometry():List<PdfTextFragment> = headerGeometry+
        textRow("QAB1234569","2026-10-10","10:30:00","Shop purchase","-20.00","490.00",100f)+
        textRow("QAB1234568","2026-10-09","10:30:00","Reversal via API","100.00","510.00",140f)+
        listOf(word("from",221f,152f),word("-",241f,152f),word("071***123456",255f,152f))+
        textRow("QAB1234567","2026-10-08","10:30:00","Transport","-50.00","410.00",190f)

    @Test fun wrappedMaskedAccountInDetailsCannotBecomeAPaidInColumn() {
        val original=heading+first+"\n"+second+"\nfrom - 071***123456\n"+oldest
        val normalized=PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(geometry()),listOf(true)
        )
        assertTrue(normalized.toString(),normalized is MpesaPositionedRows.Ready)
        val candidates=PdfStatementReview.read(
            (normalized as MpesaPositionedRows.Ready).pages,zone
        )
        assertTrue(candidates.problems.joinToString(),candidates.problems.isEmpty())
        assertEquals(3,candidates.rows.size)
        assertEquals(listOf(-2000L,10000L,-5000L),candidates.rows.map {it.signedMinor})
        assertTrue(candidates.rows[1].description.contains("071***123456"))
        assertEquals(2,candidates.verifiedReceiptTransitions)
    }

    @Test fun unknownMoneyToTheRightOfCompletedRejectsEntireDocument() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val moved=geometry()+word("70.00",467f,140f)
        assertTrue(
            PdfBoxStatementGeometry.reconstruct(listOf(original),listOf(moved),listOf(true))
                is MpesaPositionedRows.Unsupported
        )
    }

    @Test fun missingStatusOrUnknownNumericFinancialFragmentCannotCreateCash() {
        val original=heading+first+"\n"+second+"\n"+oldest
        val dropped=geometry().filterNot {it.text=="Completed" && it.top==140f}
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(dropped),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
        val bad=geometry()+word("45.00",365f,152f)
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(bad),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun missingOrInvalidGeometryAndMissingReceiptRejectAllPages() {
        val original=heading+first+"\n"+second+"\n"+oldest
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(geometry()),listOf(false)
        ) is MpesaPositionedRows.Unsupported)
        val countMismatch=original+"\nQAB1234500 2026-10-07 10:30:00 Unknown Completed -10.00 400.00"
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(countMismatch),listOf(geometry()),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
        val invalid=geometry()+PdfTextFragment("Unsupported",Float.NaN,24f,120f,32f)
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(original),listOf(invalid),listOf(true)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun secondPageFailurePreventsFirstPageFinancialReview() {
        val page1=heading+first
        val page2=heading+second
        val boxes1=headerGeometry+textRow(
            "QAB1234569","2026-10-10","10:30:00","Shop purchase","-20.00","490.00",100f)
        assertTrue(PdfBoxStatementGeometry.reconstruct(
            listOf(page1,page2),listOf(boxes1,emptyList()),listOf(true,false)
        ) is MpesaPositionedRows.Unsupported)
    }

    @Test fun repeatedReceiptMayContainTwoDistinctStatementMovements() {
        val fee="QAB1234569 2026-10-10 10:30:00 Service fee Completed -5.00 495.00"
        val page=heading+first+"\n"+fee+"\n"+second+"\n"+oldest
        val boxes=geometry()+textRow(
            "QAB1234569","2026-10-10","10:30:00","Service fee","-5.00","495.00",118f)
        val reconstructed=PdfBoxStatementGeometry.reconstruct(
            listOf(page),listOf(boxes),listOf(true)
        )
        assertTrue(reconstructed.toString(),reconstructed is MpesaPositionedRows.Ready)
        val candidates=PdfStatementReview.read(
            (reconstructed as MpesaPositionedRows.Ready).pages,zone
        )
        assertEquals(candidates.problems.joinToString(),4,candidates.rows.size)
        assertEquals(2,candidates.rows.count {it.reference=="QAB1234569"})
    }
}
