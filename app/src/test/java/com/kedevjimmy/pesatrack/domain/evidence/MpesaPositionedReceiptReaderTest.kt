package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.MpesaPositionedReceiptReader
import com.kedevjimmy.pesatrack.domain.everyday.MpesaPositionedRows
import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/**
 * All details/phone-like tokens and receipts here are invented. Blocks model
 * native PDF regions, not a copy of a user's statement or transaction data.
 */
class MpesaPositionedReceiptReaderTest {
    private val heading="""
        M-PESA STATEMENT
        Statement Period: 01 Aug 2026 - 11 Oct 2026
        DETAILED STATEMENT
        Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance
    """.trimIndent()+"\n"
    private val sourceRows=listOf(
        "QAB1234569 2026-10-10 10:30:00 Shop purchase\n" +
            "numeric account 1234567890\nCompleted -20.00 490.00",
        "QAB1234568 2026-10-09 10:30:00 Reversal via API\n" +
            "from - 071***123456\nCompleted 100.00 510.00",
        "QAB1234567 2026-10-08 10:30:00 Transport\nCompleted -50.00 410.00"
    )
    private fun fragment(text:String,x:Float,top:Float,right:Float,bottom:Float)=
        PdfTextFragment(text,x,top,right,bottom)
    private fun fragments()=listOf(
        fragment("M-PESA STATEMENT",30f,10f,200f,20f),
        fragment("Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance",45f,60f,556f,69f),
        fragment("QAB1234569 2026-10-10 10:30:00 Shop purchase\nnumeric account 1234567890",35f,100f,244f,113f),
        fragment("Completed -20.00 490.00",300f,100f,556f,108f),
        fragment("QAB1234568 2026-10-09 10:30:00 Reversal via API\nfrom - 071***123456",35f,130f,244f,143f),
        fragment("Completed 100.00 510.00",300f,130f,556f,138f),
        fragment("QAB1234567 2026-10-08 10:30:00 Transport",35f,160f,244f,168f),
        fragment("Completed -50.00 410.00",300f,160f,556f,168f),
        fragment("Disclaimer: Synthetic footer",35f,195f,250f,201f)
    )
    private fun reconstructed(
        document:String=heading+sourceRows.joinToString("\n"),
        boxes:List<PdfTextFragment> = fragments(),
        complete:Boolean=true
    ) = MpesaPositionedReceiptReader.reconstruct(listOf(document),listOf(boxes),listOf(complete))

    @Test fun physicalDetailsColumnsKeepMaskedAndNumericReferencesOutOfMoneyCells() {
        val result=reconstructed()
        assertTrue(result.toString(),result is MpesaPositionedRows.Ready)
        val candidates=PdfStatementReview.read(
            (result as MpesaPositionedRows.Ready).pages,TimeZone.getTimeZone("Africa/Nairobi")
        )
        assertTrue(candidates.problems.joinToString(),candidates.problems.isEmpty())
        assertEquals(3,candidates.rows.size)
        assertTrue(candidates.rows.first().description.contains("numeric account 1234567890"))
        assertTrue(candidates.rows[1].description.contains("071***123456"))
        assertEquals(listOf(-2000L,10000L,-5000L),candidates.rows.map {it.signedMinor})
    }

    @Test fun nativeMultilineFragmentsMayBeCompleteEvenThoughLegacyReflowIsSingleLineOnly() {
        val boxes=fragments()
        assertTrue(boxes.any {it.text.contains("\n")})
        assertTrue(reconstructed(boxes=boxes) is MpesaPositionedRows.Ready)
    }

    @Test fun rightSideExtraNumbersCannotBeMistakenForDescriptionOrIgnored() {
        val mutated=fragments().map {
            if(it.text=="Completed 100.00 510.00")
                it.copy(text="Completed 100.00 510.00 70.00")
            else it
        }
        assertTrue(reconstructed(boxes=mutated) is MpesaPositionedRows.Unsupported)
    }

    @Test fun unboundedOrMissingNativeFragmentsFailClosed() {
        assertEquals(MpesaPositionedRows.Reason.INCOMPLETE_GEOMETRY,
            (reconstructed(complete=false) as MpesaPositionedRows.Unsupported).reason)
        val dropped=fragments().filterNot {it.text.startsWith("Completed 100")}
        assertEquals(MpesaPositionedRows.Reason.AMBIGUOUS_RECEIPT,
            (reconstructed(boxes=dropped) as MpesaPositionedRows.Unsupported).reason)
    }

    @Test fun crossingStatusColumnAndMixedFinancialDetailsCannotBeReassigned() {
        val moved=fragments().map {
            if(it.text.startsWith("QAB1234568"))
                it.copy(right=360f)
            else it
        }
        assertTrue(reconstructed(boxes=moved) is MpesaPositionedRows.Unsupported)
    }

    @Test fun wholeDocumentMissingReceiptCountMustNotImportVisibleSubset() {
        val original=heading+sourceRows.joinToString("\n")+"\nQAB1234500 2026-10-07 10:30:00 Missing Completed -10.00 400.00"
        val result=reconstructed(document=original)
        assertEquals(MpesaPositionedRows.Reason.UNVERIFIED_ROW_COUNT,
            (result as MpesaPositionedRows.Unsupported).reason)
    }

    @Test fun unknownOrConflictingColumnFragmentsAreNeverSilentlyDeleted() {
        val boxes=fragments()+fragment("70.00",401f,130f,433f,138f)
        assertTrue(reconstructed(boxes=boxes) is MpesaPositionedRows.Unsupported)
    }
}
