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

    /**
     * PDFBox can reconstruct top-of-page display headings from positioned
     * words even when its text-stream prefix omits those headings. Header
     * evidence must come from original PDF glyphs, never a guessed KES string.
     */
    private fun visualStatementHeadings():List<PdfTextFragment> = listOf(
        word("M-PESA",30f,8f),word("STATEMENT",72f,8f),
        word("Statement",30f,20f),word("Period:",89f,20f),
        word("01",135f,20f),word("Oct",149f,20f),word("2026",168f,20f),
        word("-",199f,20f),word("11",213f,20f),word("Oct",227f,20f),
        word("2026",250f,20f),
        word("DETAILED",30f,35f),word("STATEMENT",87f,35f),
        word("Receipt",30f,60f),word("No.",72f,60f),
        word("Completion",97f,60f),word("Time",168f,60f),
        word("Details",201f,60f),word("Transaction",254f,60f),
        word("Status",335f,60f),word("Paid",387f,60f),
        word("In",416f,60f),word("Withdrawn",444f,60f),
        word("Balance",516f,60f)
    )

    private fun recoveredLayout(withTitle:Boolean = true):List<PdfTextFragment> =
        visualStatementHeadings().filter {withTitle || it.top!=8f} +
            geometry().drop(headerGeometry.size)

    @Test fun physicallyPresentHeadersVerifyCurrencyEvenIfPdfBoxTextStreamPrefixIsIncomplete() {
        // The original source prefix is intentionally missing several
        // headings. The PDFBox glyph geometry contains all of them.
        val incompletePrefix="MOBILE MONEY REPORT\n"
        val input=incompletePrefix+first+"\n"+second+"\n"+
            "from - 071***123456\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(recoveredLayout()),listOf(true)
        )
        assertTrue(output.toString(),output is MpesaPositionedRows.Ready)
        val verified=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone
        )
        assertEquals(verified.problems.joinToString(),3,verified.rows.size)
        assertNotNull(verified.currencyReview)
        assertEquals(2,verified.verifiedReceiptTransitions)
    }

    @Test fun missingOriginalTitleStillCannotAuthorizeInferredKes() {
        val input="MOBILE MONEY REPORT\n"+first+"\n"+second+"\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(recoveredLayout(withTitle=false)),listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Ready)
        val verified=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone
        )
        assertTrue(verified.rows.isEmpty())
        assertTrue(verified.problems.any {it.contains("KES could not be verified")})
        assertTrue(verified.problems.any {it.contains("Missing statement headings: M-Pesa statement title")})
        assertFalse(verified.problems.joinToString().contains("QAB1234569"))
    }

    @Test fun explicitForeignCurrencyCannotBeHiddenByRecoveredKenyanHeaders() {
        val input="Currency: USD\n"+first+"\n"+second+"\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(recoveredLayout()),listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Ready)
        val verified=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone
        )
        assertTrue(verified.rows.isEmpty())
        assertTrue(verified.problems.any {
            it.contains("currency other than KES") ||
                it.contains("conflicting currency declarations")
        })
    }

    @Test fun literalStatementBrandInsideTransactionDoesNotInventDocumentTitle() {
        val input="MOBILE MONEY REPORT\n"+
            first.replace("Shop purchase","M-PESA STATEMENT")+"\n"+second+"\n"+oldest
        val output=PdfBoxStatementGeometry.reconstruct(
            listOf(input),listOf(recoveredLayout(withTitle=false).map {
                if(it.text=="Shop purchase") it.copy(text="M-PESA STATEMENT") else it
            }),listOf(true)
        )
        assertTrue(output is MpesaPositionedRows.Ready)
        val verified=PdfStatementReview.read(
            (output as MpesaPositionedRows.Ready).pages,zone
        )
        assertTrue(verified.rows.isEmpty())
    }

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
        val payment="QAB1234569 2026-10-10 10:30:00 Shop purchase Completed -20.00 485.00"
        val page=heading+payment+"\n"+fee+"\n"+second+"\n"+oldest
        val paymentBoxes=geometry().map {
            if(it.text=="490.00" && it.top==100f)word("485.00",522f,100f)
            else it
        }
        val boxes=paymentBoxes+textRow(
            "QAB1234569","2026-10-10","10:30:00","Service fee","-5.00","490.00",118f)
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
