package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

/** All coordinates and counts are invented. No private source statement is used. */
class PdfGridProbePolicyTest {
    private fun grid() = listOf(
        PdfGridSegment(10f,10f,510f,10f),
        PdfGridSegment(10f,45f,510f,45f),
        PdfGridSegment(10f,90f,510f,90f),
        PdfGridSegment(30f,10f,30f,90f),
        PdfGridSegment(220f,10f,220f,90f),
        PdfGridSegment(470f,10f,470f,90f)
    )
    @Test fun syntheticRuledCellsProduceGridHint() {
        val report=PdfGridVectorEvidence.analyze(1,600f,600f,grid())
        assertEquals(3,report.horizontalLines)
        assertEquals(3,report.verticalLines)
        assertEquals(9,report.intersections)
        assertTrue(report.possibleVectorGrid)
    }
    @Test fun noLinesAndDecorativeRulesCannotClaimTable() {
        assertFalse(PdfGridVectorEvidence.analyze(1,600f,600f,emptyList()).possibleVectorGrid)
        assertFalse(PdfGridVectorEvidence.analyze(1,600f,600f,grid().take(2)).possibleVectorGrid)
    }
    @Test fun sameLinePaintedRepeatedlyDoesNotInventGrid() {
        val lines=listOf(grid().first(),grid()[3])
        assertFalse(PdfGridVectorEvidence.analyze(1,600f,600f,lines+lines+lines).possibleVectorGrid)
    }
    @Test fun missingOrMalformedPageEvidenceIsRejected() {
        val report=PdfGridProbeReport(listOf(
            PdfGridVectorEvidence.analyze(1,600f,600f,grid()),
            PdfGridVectorEvidence.analyze(2,600f,600f,emptyList())))
        assertEquals(report,PdfGridProbeWire.decode(2,PdfGridProbeWire.encode(report)))
        assertTrue(report.safeSummary().contains("no transactions were imported"))
        assertThrows(IllegalArgumentException::class.java) {
            PdfGridProbeWire.decode(2,PdfGridProbeWire.encode(report).dropLast(1).toIntArray())
        }
        val malicious=PdfGridProbeWire.encode(report).also {it[4]=2}
        assertThrows(IllegalArgumentException::class.java){PdfGridProbeWire.decode(2,malicious)}
    }
    @Test fun nonfiniteAndUnboundedGeometryRefuses() {
        assertThrows(IllegalArgumentException::class.java){
            PdfGridVectorEvidence.analyze(1,600f,600f,listOf(PdfGridSegment(Float.NaN,1f,2f,2f)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PdfGridVectorEvidence.analyze(1,600f,600f,List(10001){grid()[0]})
        }
    }
}
