package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

class PdfTextPositionReflowTest {
    @Test fun scrambledColumnFragmentsRecoverDocumentRowOrder() {
        val fragments = listOf(
            PdfTextFragment("2026-09-02 11:00:00",75f,73f,182f,81f),
            PdfTextFragment("Payment Completed",210f,73f,309f,81f),
            PdfTextFragment("QAB1234568",10f,73f,70f,81f),
            PdfTextFragment("-20.00 480.00",331f,73f,415f,81f),
            PdfTextFragment("100.00 500.00",331f,57f,414f,65f),
            PdfTextFragment("Payment Completed",210f,57f,310f,65f),
            PdfTextFragment("QAB1234567",10f,57f,70f,65f),
            PdfTextFragment("2026-09-03 11:00:00",75f,57f,185f,65f)
        )
        val input=fragments.joinToString("\n") {it.text}
        val result=PdfTextPositionReflow.reflow(fragments,input)
        val rows=result.lines()
        assertEquals(2,rows.size)
        assertTrue(rows[0].startsWith("QAB1234567 2026-09-03"))
        assertTrue(rows[0].endsWith("100.00 500.00"))
        assertTrue(rows[1].startsWith("QAB1234568 2026-09-02"))
        assertTrue(rows[1].endsWith("-20.00 480.00"))
    }

    @Test fun incompleteGeometryCannotQuietlyDropOriginalText() {
        val input="Statement period May 2026\nReceipt No. Completion Time\nQAB1234567 2026-09-03"
        val bad=listOf(PdfTextFragment("Receipt No",0f,10f,30f,20f))
        assertEquals(input,PdfTextPositionReflow.reflow(bad,input))
    }

    @Test fun overlappingAmbiguousTextFallsBackToOriginal() {
        val fragments=listOf(
            PdfTextFragment("PAID IN",100f,10f,145f,20f),
            PdfTextFragment("WITHDRAWN",128f,10f,189f,20f)
        )
        val original="PAID IN\nWITHDRAWN"
        assertEquals(original,PdfTextPositionReflow.reflow(fragments,original))
    }

    @Test fun invalidAndMultiLineBoundingBoxesUseOriginal() {
        val fragments=listOf(
            PdfTextFragment("receipt\ncode",0f,0f,100f,20f),
            PdfTextFragment("amount",Float.NaN,0f,100f,20f)
        )
        val original="receipt\ncode\namount"
        assertEquals(original,PdfTextPositionReflow.reflow(fragments,original))
    }
}
