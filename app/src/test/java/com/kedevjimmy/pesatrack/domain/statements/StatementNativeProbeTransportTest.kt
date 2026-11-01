package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextPreview
import com.kedevjimmy.pesatrack.domain.evidence.PdfGeometryDiagnostics
import com.kedevjimmy.pesatrack.domain.evidence.PdfGeometryDisposition
import org.junit.Assert.*
import org.junit.Test

class StatementNativeProbeTransportTest {
    private val header = PdfTextFragment("Date", 8f, 12f, 40f, 22f)
    private val amount = PdfTextFragment("10.00", 120f, 42f, 150f, 53f)

    private fun preview(
        pages: List<List<PdfTextFragment>>,
        flags: List<Boolean> = List(pages.size) { true },
        texts: List<String> = List(pages.size) { "Date and amounts" }
    ) = PdfTextPreview(texts, emptyList(), pages, flags)

    @Test fun api34RefusesNativeProbe() {
        val result = StatementNativeProbeTransport.prepare(34, preview(listOf(listOf(header))))
        assertEquals(NativeProbePreparation.Blocked(NativeProbeBlockReason.UNSUPPORTED_DEVICE), result)
    }

    @Test fun missingPositionedPageOrFlagRefusesAllPages() {
        val missingPage = PdfTextPreview(listOf("first", "second"), emptyList(),
            listOf(listOf(header)), listOf(true, true))
        assertEquals(NativeProbePreparation.Blocked(NativeProbeBlockReason.INCOMPLETE_GEOMETRY),
            StatementNativeProbeTransport.prepare(35, missingPage))

        val missingFlag = PdfTextPreview(listOf("first", "second"), emptyList(),
            listOf(listOf(header), listOf(amount)), listOf(true))
        assertEquals(NativeProbePreparation.Blocked(NativeProbeBlockReason.INCOMPLETE_GEOMETRY),
            StatementNativeProbeTransport.prepare(35, missingFlag))
    }

    @Test fun oneIncompleteCoordinatePageRefusesAllPages() {
        val candidate = preview(listOf(listOf(header), listOf(amount)), listOf(true, false))
        val blocked = StatementNativeProbeTransport.prepare(35, candidate) as NativeProbePreparation.Blocked
        assertEquals(NativeProbeBlockReason.INCOMPLETE_GEOMETRY, blocked.reason)
        assertEquals(2, blocked.pageNumber)
    }

    @Test fun incompleteNativeGeometryReportsOnlyFixedSafeCounts() {
        val reasons = IntArray(PdfGeometryDisposition.entries.size)
        reasons[PdfGeometryDisposition.MISSING_BOUNDS.ordinal] = 3
        val syntheticPrivateLine = "Invented account 000011112222 and amount 999.99"
        val candidate = PdfTextPreview(
            pages = listOf("Date and amounts", syntheticPrivateLine),
            emptyPageNumbers = emptyList(),
            positionedPages = listOf(listOf(header), listOf(amount)),
            positionedComplete = listOf(true, false),
            geometryDiagnostics = listOf(
                PdfGeometryDiagnostics(List(reasons.size) { 0 }),
                PdfGeometryDiagnostics(reasons.toList())
            )
        )
        val blocked = StatementNativeProbeTransport.prepare(35, candidate) as NativeProbePreparation.Blocked
        assertEquals(NativeProbeBlockReason.INCOMPLETE_GEOMETRY, blocked.reason)
        assertEquals(2, blocked.pageNumber)
        assertEquals("3 unbounded text block(s)", blocked.safeDetail)
        assertFalse(blocked.safeDetail.orEmpty().contains("000011112222"))
    }

    @Test fun completeTwoPagePreviewPreservesOriginalPageOrder() {
        val candidate = preview(listOf(listOf(header), listOf(amount)))
        val result = StatementNativeProbeTransport.prepare(35, candidate)
        assertTrue(result is NativeProbePreparation.Ready)
        val pages = (result as NativeProbePreparation.Ready).pages
        assertEquals(listOf(1, 2), pages.map { it.pageNumber })
        assertEquals(listOf(header), pages[0].words)
        assertEquals(listOf(amount), pages[1].words)
    }

    @Test fun emptyOrScannedPageRefusesAllPages() {
        val candidate = preview(listOf(listOf(header), emptyList()))
        assertEquals(NativeProbePreparation.Blocked(NativeProbeBlockReason.EMPTY_OR_SCANNED_DOCUMENT),
            StatementNativeProbeTransport.prepare(35, candidate))
    }
}
