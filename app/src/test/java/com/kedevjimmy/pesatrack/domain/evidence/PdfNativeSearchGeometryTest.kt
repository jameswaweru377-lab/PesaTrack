package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

/**
 * Invented text and coordinates. No real financial source strings or values.
 */
class PdfNativeSearchGeometryTest {
    private fun box(l:Float=40f,t:Float=50f,r:Float=300f,b:Float=65f) =
        PdfSearchBox(l,t,r,b)

    @Test fun exactlyOneNativeMatchWithValidRectangleCanRecoverCoordinates() {
        val text="Synthetic statement description"
        val result=PdfNativeSearchGeometry.validate(text,listOf(listOf(box())))
        assertEquals(PdfSearchOutcome.RECOVERED,result.outcome)
        assertNotNull(result.fragment)
        assertEquals(text,result.fragment!!.text)
        assertEquals(40f,result.fragment!!.left,0f)
        assertEquals(65f,result.fragment!!.bottom,0f)
    }

    @Test fun splitLineNativeMatchKeepsFullTextWithoutDroppingCoordinates() {
        val result=PdfNativeSearchGeometry.validate(
            "Synthetic transaction completed with special label",
            listOf(listOf(box(35f,120f,300f,128f),box(35f,132f,210f,142f)))
        )
        assertEquals(PdfSearchOutcome.RECOVERED,result.outcome)
        assertEquals(120f,result.fragment!!.top,0f)
        assertEquals(142f,result.fragment!!.bottom,0f)
    }

    @Test fun noMatchNeverTurnsDocumentNarrativeIntoFinancialEvidence() {
        val result=PdfNativeSearchGeometry.validate(
            "Synthetic amount 42.00",emptyList()
        )
        assertEquals(PdfSearchOutcome.NO_MATCH,result.outcome)
        assertNull(result.fragment)
    }

    @Test fun nonUniqueMatchesAreNeverArbitrarilyAssignedToASingleRow() {
        val result=PdfNativeSearchGeometry.validate(
            "Completed 12.00 900.00",listOf(listOf(box()),listOf(box(40f,100f,300f,116f)))
        )
        assertEquals(PdfSearchOutcome.MULTIPLE_MATCHES,result.outcome)
        assertNull(result.fragment)
    }

    @Test fun rectangleCompletenessAndArithmeticAreRequired() {
        val text="Synthetic transaction 12.00"
        val cases=listOf(
            emptyList<PdfSearchBox>(),
            listOf(box(Float.NaN,5f,30f,20f)),
            listOf(box(100f,5f,30f,20f)),
            listOf(box(0f,0f,30f,Float.POSITIVE_INFINITY)),
            List(PdfNativeSearchGeometry.MAX_MATCH_RECTS+1) {box()}
        )
        cases.forEach {candidate ->
            val result=PdfNativeSearchGeometry.validate(text,listOf(candidate))
            assertEquals(PdfSearchOutcome.INVALID_GEOMETRY,result.outcome)
            assertNull(result.fragment)
        }
    }

    @Test fun searchOnlyUsesShortSingleLineTextFromIsolatedDocumentMemory() {
        assertFalse(PdfNativeSearchGeometry.maySearch(""))
        assertFalse(PdfNativeSearchGeometry.maySearch("123"))
        assertFalse(PdfNativeSearchGeometry.maySearch("A\nB content"))
        assertFalse(PdfNativeSearchGeometry.maySearch("XYZ\tstatus"))
        assertFalse(PdfNativeSearchGeometry.maySearch("x".repeat(PdfNativeSearchGeometry.MAX_QUERY_LENGTH+1)))
        assertTrue(PdfNativeSearchGeometry.maySearch("Completed 12.00 480.00"))
        assertTrue(PdfNativeSearchGeometry.MAX_QUERIES_PER_PAGE<=8)
    }

    @Test fun diagnosticMessageKeepsNoTransactionStringsOrValues() {
        val counts=IntArray(PdfGeometryDisposition.entries.size)
        counts[PdfGeometryDisposition.MISSING_BOUNDS.ordinal]=1
        counts[PdfGeometryDisposition.NATIVE_SEARCH_AMBIGUOUS.ordinal]=1
        val diagnostic=PdfGeometryDiagnostics(counts.toList())
        val message=diagnostic.safeSummary()
        assertTrue(message.contains("1 unbounded text block"))
        assertTrue(message.contains("multiple matches"))
        assertFalse(message.contains("12.00"))
    }
}
