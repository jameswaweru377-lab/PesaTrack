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
    @Test fun disconnectedCellsAreJoinedOnlyAlongTheirObservedAxis() {
        val split=grid().flatMap {segment->
            val midX=(segment.x1+segment.x2)/2f
            val midY=(segment.y1+segment.y2)/2f
            if(segment.y1==segment.y2)listOf(
                PdfGridSegment(segment.x1,segment.y1,midX-.5f,segment.y2),
                PdfGridSegment(midX+.5f,segment.y1,segment.x2,segment.y2))
            else listOf(
                PdfGridSegment(segment.x1,segment.y1,segment.x2,midY-.5f),
                PdfGridSegment(segment.x1,midY+.5f,segment.x2,segment.y2))
        }
        val report=PdfGridVectorEvidence.analyze(1,600f,600f,split)
        assertEquals(3,report.horizontalLines)
        assertEquals(3,report.verticalLines)
        assertEquals(9,report.intersections)
        assertTrue(report.possibleVectorGrid)
    }
    @Test fun thinFilledRectangleEdgesAreCountedWithoutClaimingFinancialAccuracy() {
        val filled=PdfGridVectorEvidence.analyze(1,600f,600f,emptyList(),grid())
        assertTrue(filled.possibleVectorGrid)
        assertEquals(3,filled.thinFilledHorizontalRules)
        assertEquals(3,filled.thinFilledVerticalRules)
        assertTrue(PdfGridProbeReport(listOf(filled)).safeSummary().contains(
            "with possible thin filled rules"))
    }
    @Test fun missingCrossingsShowDistinctReasonWithoutInventingTable() {
        val horizontal=listOf(grid()[0],grid()[1],grid()[2])
        val onlyHorizontal=PdfGridVectorEvidence.analyze(1,600f,600f,horizontal)
        assertFalse(onlyHorizontal.possibleVectorGrid)
        val disconnected=PdfGridVectorEvidence.analyze(2,600f,600f,
            horizontal+listOf(
                PdfGridSegment(550f,10f,550f,90f),
                PdfGridSegment(560f,10f,560f,90f),
                PdfGridSegment(570f,10f,570f,90f)))
        assertFalse(disconnected.possibleVectorGrid)
        val summary=PdfGridProbeReport(listOf(onlyHorizontal,disconnected)).safeSummary()
        assertTrue(summary.contains("1 with only one direction"))
        assertTrue(summary.contains("1 with lines in both directions"))
        assertFalse(summary.contains("Shop purchase"))
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
