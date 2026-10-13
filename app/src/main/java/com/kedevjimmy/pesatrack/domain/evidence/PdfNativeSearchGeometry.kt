package com.kedevjimmy.pesatrack.domain.evidence

/**
 * Android PdfPageTextContent can carry text with no bounding rectangles.
 * Only try to recover the *exact* bounded text from local native PDF search.
 * A non-unique or incomplete search result is never evidence of a money cell.
 */
data class PdfSearchBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

enum class PdfSearchOutcome { RECOVERED, NO_MATCH, MULTIPLE_MATCHES, INVALID_GEOMETRY }

data class PdfSearchResult(
    val outcome: PdfSearchOutcome,
    val fragment: PdfTextFragment? = null
)

object PdfNativeSearchGeometry {
    const val MAX_QUERIES_PER_PAGE = 8
    const val MAX_QUERY_LENGTH = 180
    const val MAX_MATCH_RECTS = 24

    fun maySearch(text: String): Boolean =
        text.length in 6..MAX_QUERY_LENGTH &&
            text.isNotBlank() &&
            !text.contains('\n') && !text.contains('\r') &&
            text.none {it.isISOControl()}

    /** The native API's exact-query list and page matches are provided in memory only. */
    fun validate(text: String, matches: List<List<PdfSearchBox>>): PdfSearchResult {
        if (!maySearch(text)) return PdfSearchResult(PdfSearchOutcome.INVALID_GEOMETRY)
        if (matches.isEmpty()) return PdfSearchResult(PdfSearchOutcome.NO_MATCH)
        if (matches.size != 1) return PdfSearchResult(PdfSearchOutcome.MULTIPLE_MATCHES)
        val bounds=matches.single()
        if (bounds.size !in 1..MAX_MATCH_RECTS ||
            bounds.any {box ->
                !box.left.isFinite() || !box.top.isFinite() ||
                !box.right.isFinite() || !box.bottom.isFinite() ||
                box.left>=box.right || box.top>=box.bottom
            }
        ) return PdfSearchResult(PdfSearchOutcome.INVALID_GEOMETRY)
        return PdfSearchResult(
            PdfSearchOutcome.RECOVERED,
            PdfTextFragment(
                text,
                bounds.minOf {it.left},bounds.minOf {it.top},
                bounds.maxOf {it.right},bounds.maxOf {it.bottom}
            )
        )
    }
}
