package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment

/**
 * A deliberately bounded intermediary for an eventual native PDF extractor.
 * Caller supplies ALL fragments from a positively identified table region,
 * including unknown columns. It must never silently drop unrecognized text.
 */
data class StatementPositionedPage(
    val pageNumber: Int,
    val tableLeft: Float,
    val tableRight: Float,
    val headerFragments: List<PdfTextFragment>,
    val bodyFragments: List<PdfTextFragment>
)

enum class StatementGeometryProblem {
    INVALID_BOUNDS, INVALID_HEADER, MISSING_DATE_ANCHORS,
    AMBIGUOUS_COLUMNS, UNASSIGNED_FRAGMENT, INVALID_ROWS
}

sealed class StatementGeometryResult {
    data class Reconstructed(val table: StatementTablePage) : StatementGeometryResult()
    /** Only page and reason — never personal text or extracted monetary values. */
    data class Unsupported(val problem: StatementGeometryProblem, val pageNumber: Int) : StatementGeometryResult()
}

/**
 * Reconstructs an in-memory row/cell grid from bounded PDF text rectangles.
 * Unlike flattening native PDF text, preserves inferred column ownership.
 * This is a read-only adapter and is NOT yet connected to production imports.
 */
object UniversalStatementGeometry {
    private const val MAX_FRAGMENTS = 30_000
    private const val MAX_COLUMNS = 40
    private const val MAX_ROWS = 5_000
    private const val Y_EPSILON = 2.5f
    private val dateAnchor = Regex("^(?:[0-9]{4}[-/][0-9]{2}[-/][0-9]{2}|[0-9]{2}[/.-][0-9]{2}[/.-][0-9]{4})(?:[ T].*)?$")

    private fun valid(box: PdfTextFragment): Boolean =
        box.left.isFinite() && box.right.isFinite() && box.top.isFinite() &&
            box.bottom.isFinite() && box.right > box.left && box.bottom > box.top &&
            box.text.isNotBlank() && !box.text.contains('\n') && !box.text.contains('\r')

    fun reconstruct(
        page: StatementPositionedPage,
        profile: StatementFormatProfile = StatementFormatProfile()
    ): StatementGeometryResult {
        fun unsupported(reason: StatementGeometryProblem) =
            StatementGeometryResult.Unsupported(reason, page.pageNumber)
        if (page.pageNumber <= 0 || !page.tableLeft.isFinite() ||
            !page.tableRight.isFinite() || page.tableRight <= page.tableLeft ||
            page.headerFragments.size !in 3..MAX_COLUMNS ||
            page.bodyFragments.isEmpty() ||
            page.headerFragments.size + page.bodyFragments.size > MAX_FRAGMENTS ||
            (page.headerFragments + page.bodyFragments).any { !valid(it) })
            return unsupported(StatementGeometryProblem.INVALID_BOUNDS)

        val headings = page.headerFragments.sortedBy { it.left }
        if (headings.any { it.left < page.tableLeft || it.right > page.tableRight })
            return unsupported(StatementGeometryProblem.INVALID_BOUNDS)
        // This conservative version requires unambiguous text boxes for each
        // complete heading; split-word headings need another proven adapter.
        for (i in 1 until headings.size) {
            if (headings[i - 1].right >= headings[i].left)
                return unsupported(StatementGeometryProblem.AMBIGUOUS_COLUMNS)
        }
        val top = headings.maxOf { it.bottom }
        if (headings.maxOf { it.top } - headings.minOf { it.top } > 6f)
            return unsupported(StatementGeometryProblem.INVALID_HEADER)
        val headers = headings.map { it.text.trim() }
        val dateIndex = headers.indexOfFirst {
            UniversalStatementTableInterpreter.fieldForHeader(it, profile) == StatementField.DATE
        }
        if (dateIndex < 0 || headers.count {
            UniversalStatementTableInterpreter.fieldForHeader(it, profile) == StatementField.DATE
        } != 1)
            return unsupported(StatementGeometryProblem.INVALID_HEADER)

        val boundaries = mutableListOf(page.tableLeft)
        for (i in 1 until headings.size) {
            val separation = (headings[i-1].right + headings[i].left) / 2f
            if (separation <= boundaries.last() || separation >= page.tableRight)
                return unsupported(StatementGeometryProblem.AMBIGUOUS_COLUMNS)
            boundaries.add(separation)
        }
        boundaries.add(page.tableRight)

        fun column(box: PdfTextFragment): Int? {
            if (box.left < page.tableLeft || box.right > page.tableRight || box.top < top - Y_EPSILON)
                return null
            val matches = (0 until headings.size).filter { i ->
                box.left >= boundaries[i] - 0.5f && box.right <= boundaries[i+1] + 0.5f
            }
            return matches.singleOrNull()
        }

        val assigned = page.bodyFragments.map { box -> box to column(box) }
        if (assigned.any { it.second == null })
            return unsupported(StatementGeometryProblem.UNASSIGNED_FRAGMENT)

        val anchors = assigned.filter { (box, col) ->
            col == dateIndex && dateAnchor.matches(box.text.trim())
        }.map { it.first }.sortedBy { it.top }
        if (anchors.isEmpty() || anchors.size > MAX_ROWS)
            return unsupported(StatementGeometryProblem.MISSING_DATE_ANCHORS)
        if (anchors.zipWithNext().any { (a,b) -> b.top - a.top <= Y_EPSILON })
            return unsupported(StatementGeometryProblem.AMBIGUOUS_COLUMNS)

        val rowCells = List(anchors.size) { List(headings.size) { mutableListOf<PdfTextFragment>() } }
        for ((box, columnIndex) in assigned) {
            val centerY = (box.top + box.bottom) / 2f
            val matches = anchors.indices.filter { row ->
                val start = anchors[row].top - Y_EPSILON
                val stop = anchors.getOrNull(row+1)?.top?.minus(Y_EPSILON) ?: Float.POSITIVE_INFINITY
                centerY >= start && centerY < stop
            }
            val row = matches.singleOrNull() ?: return unsupported(StatementGeometryProblem.UNASSIGNED_FRAGMENT)
            rowCells[row][columnIndex!!].add(box)
        }
        val rows = rowCells.map { columns ->
            columns.map { boxes ->
                boxes.sortedWith(compareBy<PdfTextFragment> { it.top }.thenBy { it.left })
                    .joinToString(" ") { it.text.trim() }
            }
        }
        if (rows.size != anchors.size || rows.any { it[dateIndex].isBlank() })
            return unsupported(StatementGeometryProblem.INVALID_ROWS)
        val provenance=rowCells.map {columns->columns.map {boxes->boxes.sortedWith(
            compareBy<PdfTextFragment> {it.top}.thenBy {it.left})}}
        return StatementGeometryResult.Reconstructed(StatementTablePage(page.pageNumber, headers, rows,provenance))
    }
}
