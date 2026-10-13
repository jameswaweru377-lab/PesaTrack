package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

/** All synthetic fragments; no private document text or account details. */
class PdfPositionChunkAssemblerTest {
    private fun add(
        assembler: PdfPositionChunkAssembler, page: Int,
        text: String = "Debit", x: Float = 100f
    ) {
        assembler.add(
            page,listOf(text),floatArrayOf(x),floatArrayOf(30f),
            floatArrayOf(x+25f),floatArrayOf(39f)
        )
    }

    @Test fun optionalGeometryDoesNotChangeLegacyPlainTextPreview() {
        val preview=PdfTextReviewPolicy.preview(listOf("Local synthetic statement"))
        assertEquals(1,preview.pages.size)
        assertTrue(preview.positionedPages.isEmpty())
        assertTrue(preview.positionedComplete.isEmpty())
    }

    @Test fun geometryFragmentsStayOrderedAndRequireExactPageProof() {
        val assembler=PdfPositionChunkAssembler()
        assembler.startPage(0)
        add(assembler,0,"Date",40f)
        add(assembler,0,"Paid In",200f)
        assembler.startPage(1)
        add(assembler,1,"Balance",300f)
        val (pages,complete)=assembler.finish(2,intArrayOf(2,1),booleanArrayOf(true,true))
        assertEquals(listOf(2,1),pages.map {it.size})
        assertEquals("Paid In",pages[0][1].text)
        assertEquals(listOf(true,true),complete)
    }

    @Test fun emptyGeometryPageMustBeExplicitNotSilentlySkipped() {
        val assembler=PdfPositionChunkAssembler()
        assembler.startPage(0)
        assembler.startPage(1)
        add(assembler,1)
        val (pages,complete)=assembler.finish(2,intArrayOf(0,1),booleanArrayOf(false,true))
        assertTrue(pages[0].isEmpty())
        assertFalse(complete[0])
    }

    @Test fun wrongCountsOrMissingPagesRejectTheEntireResult() {
        val assembler=PdfPositionChunkAssembler()
        assembler.startPage(0)
        add(assembler,0)
        assertThrows(IllegalArgumentException::class.java) {
            assembler.finish(2,intArrayOf(1,0),booleanArrayOf(true,true))
        }
        assertThrows(IllegalArgumentException::class.java) {
            assembler.finish(1,intArrayOf(2),booleanArrayOf(true))
        }
    }

    @Test fun duplicateAndOutOfOrderGeometryPageMarkersReject() {
        val assembler=PdfPositionChunkAssembler()
        assembler.startPage(0)
        assertThrows(IllegalArgumentException::class.java) { assembler.startPage(0) }
        assertThrows(IllegalArgumentException::class.java) { assembler.startPage(2) }
        assembler.startPage(1)
        assertThrows(IllegalArgumentException::class.java) { add(assembler,0) }
    }

    @Test fun multilineDetailsStayBoundedButInvalidCoordinatesAreRejected() {
        val assembler=PdfPositionChunkAssembler()
        assembler.startPage(0)
        add(assembler,0,"Synthetic detail\nwrapped account 07***123")
        assertEquals("Synthetic detail\nwrapped account 07***123",
            assembler.finish(1,intArrayOf(1),booleanArrayOf(true)).first[0][0].text)
        assertThrows(IllegalArgumentException::class.java) { add(assembler,0,"Amount",Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) {
            assembler.add(0,listOf("Amount"),floatArrayOf(10f),
                floatArrayOf(5f),floatArrayOf(8f),floatArrayOf(15f))
        }
    }

    @Test fun extraChunkLengthOrOversizedTextCannotOverflowBinder() {
        val assembler=PdfPositionChunkAssembler()
        assembler.startPage(0)
        assertThrows(IllegalArgumentException::class.java) {
            assembler.add(0,listOf("A","B"),floatArrayOf(1f),
                floatArrayOf(1f),floatArrayOf(2f),floatArrayOf(3f))
        }
        assertThrows(IllegalArgumentException::class.java) {
            add(assembler,0,"X".repeat(PdfPositionChunkAssembler.MAX_FRAGMENT_TEXT+1))
        }
    }
}
