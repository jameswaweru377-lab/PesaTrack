package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

/** All PDF fragments are invented; never embed private statement content. */
class PdfGeometryEvidencePolicyTest {
    private fun classify(
        text: String,
        bounds: Int = 0,
        valid: Boolean = bounds > 0,
        limit: Boolean = false
    ) = PdfGeometryEvidencePolicy.classify(text,bounds,valid,limit)

    @Test fun whitespaceOnStatementCoverDoesNotInvalidateTransactionTable() {
        assertEquals(PdfGeometryDisposition.SKIPPED_BLANK, classify(" "))
        assertEquals(PdfGeometryDisposition.SKIPPED_BLANK, classify("\n  \t"))
        assertEquals(PdfGeometryDisposition.SKIPPED_BLANK, classify("   ",4,false))
    }

    @Test fun boundedDocumentLabelsAreSafeWhenAndroidDoesNotExposeTheirGeometry() {
        assertEquals(PdfGeometryDisposition.SKIPPED_STATIC_HEADING,
            classify("M-PESA STATEMENT"))
        assertEquals(PdfGeometryDisposition.SKIPPED_STATIC_HEADING,
            classify("SUMMARY"))
        assertEquals(PdfGeometryDisposition.SKIPPED_STATIC_HEADING,
            classify("DETAILED STATEMENT"))
        assertEquals(PdfGeometryDisposition.SKIPPED_STATIC_HEADING, classify(
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance"))
    }

    @Test fun receiptOrStatusWithoutPositionsMustStillFailClosed() {
        assertEquals(PdfGeometryDisposition.MISSING_BOUNDS,
            classify("QAB1234569 2026-10-10 10:30:00 Synthetic bill Completed -25.00 480.00"))
        assertEquals(PdfGeometryDisposition.MISSING_BOUNDS,
            classify("Completed -25.00 480.00"))
        assertEquals(PdfGeometryDisposition.MISSING_BOUNDS,
            classify("480.00"))
        assertEquals(PdfGeometryDisposition.MISSING_BOUNDS,
            classify("Statement Period: 01 Oct 2026 to 11 Oct 2026"))
    }

    @Test fun headingsWithAccidentalReceiptDataAreNotDroppedAsDecoration() {
        assertEquals(PdfGeometryDisposition.MISSING_BOUNDS,
            classify("M-PESA STATEMENT QAB1234569 Completed 25.00"))
        assertEquals(PdfGeometryDisposition.MISSING_BOUNDS,
            classify("Receipt No. Completion Time Details Completed 25.00"))
        assertEquals(PdfGeometryDisposition.MISSING_BOUNDS,
            classify("M-PESA STATEMENT 490.00"))
    }

    @Test fun realBoundedMultilineDetailKeepsOriginalPhysicalEvidence() {
        val text="Synthetic statement line\nfrom - 07***123456"
        assertEquals(PdfGeometryDisposition.POSITIONED,classify(text,2,true))
        assertEquals(PdfGeometryDisposition.TOO_MANY_LINE_BOUNDS,
            classify(text,25,true))
    }

    @Test fun lengthBoundsAndControlFailuresHaveDistinctSafeReasons() {
        assertEquals(PdfGeometryDisposition.OVERSIZED_CONTENT,
            classify("X".repeat(PdfGeometryEvidencePolicy.MAX_TEXT+1),1,true))
        assertEquals(PdfGeometryDisposition.INVALID_BOUNDS,
            classify("Synthetic purchase",1,false))
        assertEquals(PdfGeometryDisposition.UNSAFE_TEXT,
            classify("Synthetic\u0000purchase",1,true))
        assertEquals(PdfGeometryDisposition.TOTAL_FRAGMENT_LIMIT,
            classify("Synthetic purchase",1,true,true))
    }

    @Test fun diagnosticsTransferMustHaveExactPagesAndNeverIncludeRawText() {
        val types=PdfGeometryDisposition.entries.size
        val first=IntArray(types)
        first[PdfGeometryDisposition.SKIPPED_BLANK.ordinal]=4
        first[PdfGeometryDisposition.MISSING_BOUNDS.ordinal]=2
        first[PdfGeometryDisposition.TOO_MANY_LINE_BOUNDS.ordinal]=1
        val second=IntArray(types)
        second[PdfGeometryDisposition.POSITIONED.ordinal]=75
        val unpacked=PdfGeometryEvidencePolicy.unpack(2,PdfGeometryEvidencePolicy.pack(listOf(first,second)))
        assertEquals(2,unpacked.size)
        assertEquals(4,unpacked.first().count(PdfGeometryDisposition.SKIPPED_BLANK))
        assertEquals(2,unpacked.first().count(PdfGeometryDisposition.MISSING_BOUNDS))
        assertEquals("2 unbounded text block(s); 1 text block(s) with too many line rectangles",
            unpacked.first().safeSummary())
        assertFalse(unpacked.first().safeSummary().contains("Synthetic"))
        assertThrows(IllegalArgumentException::class.java) {
            PdfGeometryEvidencePolicy.unpack(3,PdfGeometryEvidencePolicy.pack(listOf(first,second)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PdfGeometryEvidencePolicy.unpack(1,intArrayOf(-1))
        }
    }

    @Test fun noUnknownGeometryFailuresCannotBeMistakenForMissingMoney() {
        val allGood=IntArray(PdfGeometryDisposition.entries.size)
        allGood[PdfGeometryDisposition.POSITIONED.ordinal]=300
        allGood[PdfGeometryDisposition.SKIPPED_BLANK.ordinal]=4
        val summary=PdfGeometryEvidencePolicy.unpack(1,PdfGeometryEvidencePolicy.pack(listOf(allGood))).single()
        assertEquals(0,summary.count(PdfGeometryDisposition.MISSING_BOUNDS))
        assertEquals("Android did not provide usable row coordinates.",summary.safeSummary())
    }
}
