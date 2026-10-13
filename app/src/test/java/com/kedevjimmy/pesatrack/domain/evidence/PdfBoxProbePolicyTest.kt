package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

/** Invented extraction counts; never use a real user's financial statement. */
class PdfBoxProbePolicyTest {
    private fun sample() = PdfBoxProbeReport(
        listOf(
            PdfBoxPageProbe(1, 4000, 190, 1, 0, 0, 0),
            PdfBoxPageProbe(2, 11000, 1030, 0, 0, 20, 20),
            PdfBoxPageProbe(3, 11000, 1020, 0, 2, 21, 21)
        )
    )

    @Test fun canRoundTripThreePagesThroughFixedCountsOnly() {
        val source=sample()
        val encoded=PdfBoxProbeWire.encode(source)
        assertEquals(3*PdfBoxProbeWire.FIELDS_PER_PAGE,encoded.size)
        val read=PdfBoxProbeWire.decode(3,encoded)
        assertEquals(source,read)
        assertEquals(41,read.totalPossibleReceipts)
        assertEquals(41,read.totalCompleted)
        assertEquals(3,read.missingGeometry)
        assertTrue(read.hasUncertainText)
    }

    @Test fun userVisibleMessageDoesNotContainSourceTextOrFinancialValues() {
        val message=sample().safeSummary()
        assertTrue(message.contains("3 pages"))
        assertTrue(message.contains("41 possible receipt rows"))
        assertTrue(message.contains("No transactions were added"))
        assertFalse(message.contains("QAB1234567"))
        assertFalse(message.contains("071***123456"))
        assertFalse(message.contains("KES 10,000"))
    }

    @Test fun refusesShortOrOversizedInterProcessEvidence() {
        val wire=PdfBoxProbeWire.encode(sample())
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxProbeWire.decode(3,wire.dropLast(1).toIntArray())
        }
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxProbeWire.decode(2,wire)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxProbeWire.decode(0,wire)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxProbeWire.decode(161,IntArray(161*PdfBoxProbeWire.FIELDS_PER_PAGE))
        }
    }

    @Test fun sourceCannotForgePageOrderingOrNegativeCounters() {
        val wire=PdfBoxProbeWire.encode(sample())
        val changed=wire.copyOf()
        changed[0]=2
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxProbeWire.decode(3,changed)
        }
        val negative=wire.copyOf()
        negative[PdfBoxProbeWire.FIELDS_PER_PAGE+2]=-1
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxProbeWire.decode(3,negative)
        }
    }

    @Test fun impossibleFinancialRowCountsAreRejectedInsteadOfTrusted() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxPageProbe(1,10,0,0,0,11,0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxPageProbe(1,10,0,0,0,0,11)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PdfBoxProbeReport(listOf(
                PdfBoxPageProbe(1,260000,0,0,0,0,0),
                PdfBoxPageProbe(2,260000,0,0,0,0,0)
            ))
        }
    }

    @Test fun validRectangleUnionKeepsDisplayCoordinatesTopLeft() {
        val a=PdfBoxWordBounds.Rect(36f,90f,80f,102f)
        val b=PdfBoxWordBounds.Rect(80f,90f,128f,102f)
        val union=PdfBoxWordBounds.fromCharacters(listOf(a,b),300f,220f)
        assertEquals(PdfBoxWordBounds.Rect(36f,90f,128f,102f),union)
    }

    @Test fun invalidOutOfPageAndNaNCoordinatesCannotBeCountedAsVerified() {
        val bad=listOf(
            PdfBoxWordBounds.Rect(Float.NaN,1f,10f,20f),
            PdfBoxWordBounds.Rect(45f,5f,44f,20f),
            PdfBoxWordBounds.Rect(1f,5f,10f,Float.POSITIVE_INFINITY),
            PdfBoxWordBounds.Rect(1f,5f,1000f,20f),
            PdfBoxWordBounds.Rect(-20f,5f,30f,20f)
        )
        for (bounds in bad) {
            assertNull(PdfBoxWordBounds.fromCharacters(listOf(bounds),300f,220f))
        }
        assertNull(PdfBoxWordBounds.fromCharacters(emptyList(),300f,220f))
        assertNull(PdfBoxWordBounds.fromCharacters(List(301) {
            PdfBoxWordBounds.Rect(1f,5f,10f,20f)
        },300f,220f))
    }

    @Test fun diagnosticDoesNotClaimVerifiedTransactionsEvenWhenEveryWordIsPositioned() {
        val report=PdfBoxProbeReport(listOf(PdfBoxPageProbe(1,400,20,0,0,2,2)))
        assertFalse(report.hasUncertainText)
        assertTrue(report.safeSummary().contains("not a verified import"))
        assertEquals(2,report.totalPossibleReceipts)
    }
}
