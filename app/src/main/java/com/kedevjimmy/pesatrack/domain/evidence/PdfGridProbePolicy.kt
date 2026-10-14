package com.kedevjimmy.pesatrack.domain.evidence

import kotlin.math.abs
import kotlin.math.roundToInt

/** Local-only drawing evidence. No transaction text, amount, identity or receipt reference. */
data class PdfGridSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

/** Bounded aggregate evidence: not table contents, not proof of a financial grid. */
data class PdfGridPageEvidence(
    val page: Int,
    val horizontalLines: Int,
    val verticalLines: Int,
    val intersections: Int,
    val possibleVectorGrid: Boolean,
    val thinFilledHorizontalRules: Int = 0,
    val thinFilledVerticalRules: Int = 0
) {
    init {
        require(page > 0 &&
            horizontalLines in 0..1200 && verticalLines in 0..1200 &&
            thinFilledHorizontalRules in 0..1200 && thinFilledVerticalRules in 0..1200 &&
            intersections >= 0 &&
            intersections.toLong() <= horizontalLines.toLong() * verticalLines)
    }
}

/** Never describes individual positions, cells, personal details or financial values. */
data class PdfGridProbeReport(val pages: List<PdfGridPageEvidence>) {
    init {
        require(pages.size in 1..PdfTextReviewPolicy.MAX_PAGES)
        require(pages.indices.all { pages[it].page == it + 1 })
    }
    fun safeSummary(): String {
        val gridPages = pages.count { it.possibleVectorGrid }
        val noLines = pages.count { it.horizontalLines == 0 && it.verticalLines == 0 }
        val oneOrientation = pages.count {
            (it.horizontalLines == 0) != (it.verticalLines == 0)
        }
        val bothButFewCrossings = pages.count {
            it.horizontalLines > 0 && it.verticalLines > 0 && !it.possibleVectorGrid
        }
        val filledRules = pages.count {
            it.thinFilledHorizontalRules > 0 || it.thinFilledVerticalRules > 0
        }
        return "Local PDF vector check: ${pages.size} pages examined; " +
            "$gridPages possible grids; $bothButFewCrossings with lines in both directions but " +
            "insufficient grid intersections; $oneOrientation with only one direction of lines; " +
            "$noLines without long straight lines; $filledRules with possible thin filled rules. " +
            "A missing grid does not mean the visible borders are absent (they may be images, " +
            "filled shapes, or disconnected paths). This is diagnostic only; " +
            "no transactions were imported."
    }
}

/** Reject incomplete, malformed, reordered or oversized integer-only IPC. */
object PdfGridProbeWire {
    private const val FIELDS = 7
    fun encode(report: PdfGridProbeReport): IntArray = report.pages.flatMap {
        listOf(it.page,it.horizontalLines,it.verticalLines,it.intersections,
            if(it.possibleVectorGrid)1 else 0,it.thinFilledHorizontalRules,it.thinFilledVerticalRules)
    }.toIntArray()
    fun decode(pages: Int, values: IntArray): PdfGridProbeReport {
        require(pages in 1..PdfTextReviewPolicy.MAX_PAGES && values.size == pages * FIELDS)
        return PdfGridProbeReport((0 until pages).map { index ->
            val i = index * FIELDS
            require(values[i + 4] in 0..1)
            PdfGridPageEvidence(values[i],values[i+1],values[i+2],values[i+3],
                values[i+4] == 1, values[i+5],values[i+6])
        })
    }
}

/**
 * Research-only grid hints from STROKED orthogonal segments and very thin,
 * filled rectangles. Unlike the old classifier, nearby collinear fragments
 * are merged before crossing checks; repeated PDF drawing commands do not
 * manufacture additional rows or intersections.
 *
 * These bounds are geometric heuristics, not a license to import money.
 */
object PdfGridVectorEvidence {
    private const val MAX_SEGMENTS = 10000
    private const val AXIS_TOLERANCE = 1.25f
    private const val JOIN_GAP = 2.5f

    private data class AxisRange(val axis: Float, val start: Float, val end: Float)

    private fun coalesce(ranges: List<AxisRange>): List<AxisRange> {
        if(ranges.isEmpty())return emptyList()
        // Deterministic axis order, merging short adjacent fragments only when
        // they refer to the same observed baseline (within 1.25 PDF points).
        val joined=mutableListOf<AxisRange>()
        for(part in ranges.sortedWith(compareBy<AxisRange> {it.axis}.thenBy {it.start})) {
            val prior=joined.lastOrNull()
            if(prior!=null && abs(prior.axis-part.axis)<=AXIS_TOLERANCE &&
                part.start<=prior.end+JOIN_GAP) {
                joined[joined.lastIndex]=AxisRange(prior.axis,minOf(prior.start,part.start),
                    maxOf(prior.end,part.end))
            } else joined.add(part)
        }
        return joined
    }

    fun analyze(
        page: Int, width: Float, height: Float, segments: List<PdfGridSegment>,
        filledRuleSegments: List<PdfGridSegment> = emptyList()
    ): PdfGridPageEvidence {
        require(page > 0 && width.isFinite() && height.isFinite() && width > 0 && height > 0)
        require(segments.size + filledRuleSegments.size <= MAX_SEGMENTS &&
            (segments + filledRuleSegments).all {
                it.x1.isFinite() && it.y1.isFinite() &&
                it.x2.isFinite() && it.y2.isFinite()
            })

        fun horizontal(source:List<PdfGridSegment>):List<AxisRange> =
            coalesce(source.filter {abs(it.y2-it.y1)<=AXIS_TOLERANCE}.map {
                AxisRange((it.y1+it.y2)/2f,minOf(it.x1,it.x2),maxOf(it.x1,it.x2))
            }).filter {it.end-it.start >= width*.18f}

        fun vertical(source:List<PdfGridSegment>):List<AxisRange> =
            coalesce(source.filter {abs(it.x2-it.x1)<=AXIS_TOLERANCE}.map {
                AxisRange((it.x1+it.x2)/2f,minOf(it.y1,it.y2),maxOf(it.y1,it.y2))
            }).filter {it.end-it.start >= height*.12f}

        val filledH=horizontal(filledRuleSegments)
        val filledV=vertical(filledRuleSegments)
        val hs=horizontal(segments+filledRuleSegments)
        val vs=vertical(segments+filledRuleSegments)
        require(hs.size<=1200 && vs.size<=1200 && filledH.size<=1200 && filledV.size<=1200)
        var crossings=0
        for(h in hs)for(v in vs) {
            if(v.axis>=h.start-1.5f && v.axis<=h.end+1.5f &&
                h.axis>=v.start-1.5f && h.axis<=v.end+1.5f)crossings++
        }
        return PdfGridPageEvidence(page,hs.size,vs.size,crossings,
            hs.size>=3 && vs.size>=3 && crossings>=6,filledH.size,filledV.size)
    }
}
