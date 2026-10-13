package com.kedevjimmy.pesatrack.domain.evidence

import kotlin.math.abs
import kotlin.math.roundToInt

/** Coordinate evidence only. No statement text, amounts, identities or receipt references. */
data class PdfGridSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

data class PdfGridPageEvidence(
    val page: Int, val horizontalLines: Int, val verticalLines: Int,
    val intersections: Int, val possibleVectorGrid: Boolean
) {
    init {
        require(page > 0 && horizontalLines in 0..1200 && verticalLines in 0..1200)
        require(intersections >= 0 && intersections.toLong() <= horizontalLines.toLong() * verticalLines)
    }
}

/** These indicators establish neither an M-Pesa table nor correct financial extraction. */
data class PdfGridProbeReport(val pages: List<PdfGridPageEvidence>) {
    init {
        require(pages.size in 1..PdfTextReviewPolicy.MAX_PAGES)
        require(pages.indices.all { pages[it].page == it + 1 })
    }
    fun safeSummary(): String {
        val likely = pages.count { it.possibleVectorGrid }
        val noLines = pages.count { it.horizontalLines == 0 && it.verticalLines == 0 }
        return "Local vector-line check: ${pages.size} pages examined, " +
            "${likely} pages with possible drawn table grids, ${noLines} pages without long orthogonal vector lines. " +
            "These are geometric hints only: they do not prove the original PDF has an extractable " +
            "financial table, and no transactions were imported."
    }
}

/** Reject malformed or incomplete page-level IPC instead of fabricating partial results. */
object PdfGridProbeWire {
    private const val FIELDS = 5
    fun encode(report: PdfGridProbeReport): IntArray = report.pages.flatMap {
        listOf(it.page,it.horizontalLines,it.verticalLines,it.intersections,if(it.possibleVectorGrid)1 else 0)
    }.toIntArray()
    fun decode(pages: Int, values: IntArray): PdfGridProbeReport {
        require(pages in 1..PdfTextReviewPolicy.MAX_PAGES && values.size == pages * FIELDS)
        return PdfGridProbeReport((0 until pages).map { index ->
            val i = index * FIELDS
            require(values[i + 4] in 0..1)
            PdfGridPageEvidence(values[i],values[i+1],values[i+2],values[i+3],values[i+4] == 1)
        })
    }
}

/** Conservative intersections of long axis-aligned STROKED paths (not pixels/OCR). */
object PdfGridVectorEvidence {
    private const val MAX_SEGMENTS = 10000
    fun analyze(page: Int, width: Float, height: Float, segments: List<PdfGridSegment>): PdfGridPageEvidence {
        require(page > 0 && width.isFinite() && height.isFinite() && width > 0 && height > 0)
        require(segments.size <= MAX_SEGMENTS && segments.all {
            it.x1.isFinite() && it.y1.isFinite() && it.x2.isFinite() && it.y2.isFinite()
        })
        val hs = segments.filter { abs(it.y2-it.y1) <= 1.25f && abs(it.x2-it.x1) >= width * .18f }
            .distinctBy { ((it.y1 + it.y2) * .25f).roundToInt() }
        val vs = segments.filter { abs(it.x2-it.x1) <= 1.25f && abs(it.y2-it.y1) >= height * .12f }
            .distinctBy { ((it.x1 + it.x2) * .25f).roundToInt() }
        // The bound also prevents adversarial PDF drawing commands from causing quadratic work.
        require(hs.size <= 1200 && vs.size <= 1200)
        var crossings = 0
        for (h in hs) for (v in vs) {
            val x = (v.x1 + v.x2) / 2f
            val y = (h.y1 + h.y2) / 2f
            if (x >= minOf(h.x1,h.x2)-1.5f && x <= maxOf(h.x1,h.x2)+1.5f &&
                y >= minOf(v.y1,v.y2)-1.5f && y <= maxOf(v.y1,v.y2)+1.5f) crossings++
        }
        return PdfGridPageEvidence(page,hs.size,vs.size,crossings,
            hs.size >= 3 && vs.size >= 3 && crossings >= 6)
    }
}
