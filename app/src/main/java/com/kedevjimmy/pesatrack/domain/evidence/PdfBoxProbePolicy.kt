package com.kedevjimmy.pesatrack.domain.evidence

/**
 * Count-only results from a second PDF reader. These counts are hints about
 * extraction coverage, NOT permission to insert financial transactions.
 * Neither original text, coordinates, passwords nor identifiers cross IPC.
 */
data class PdfBoxPageProbe(
    val number: Int,
    val textCharacters: Int,
    val positionedWords: Int,
    val unpositionedWords: Int,
    val invalidRectangles: Int,
    val possibleReceiptRows: Int,
    val completedMarkers: Int
) {
    init {
        require(number > 0 && textCharacters >= 0 && positionedWords >= 0 &&
            unpositionedWords >= 0 && invalidRectangles >= 0 &&
            possibleReceiptRows >= 0 && completedMarkers >= 0)
        require(possibleReceiptRows <= textCharacters &&
            completedMarkers <= textCharacters) { "Impossible PDF evidence counts." }
    }
}

data class PdfBoxProbeReport(val pages: List<PdfBoxPageProbe>) {
    init {
        require(pages.isNotEmpty() && pages.size <= PdfTextReviewPolicy.MAX_PAGES &&
            pages.indices.all { pages[it].number == it + 1 })
        require(pages.sumOf { it.textCharacters.toLong() } <= PdfTextReviewPolicy.MAX_CHARACTERS)
    }

    val totalPossibleReceipts get() = pages.sumOf { it.possibleReceiptRows }
    val totalCompleted get() = pages.sumOf { it.completedMarkers }
    val missingGeometry get() = pages.sumOf { it.unpositionedWords + it.invalidRectangles }
    val hasUncertainText get() = pages.any { it.unpositionedWords != 0 || it.invalidRectangles != 0 }

    fun safeSummary(): String =
        "PDFBox examined ${pages.size} pages locally: " +
            "$totalPossibleReceipts possible receipt rows, $totalCompleted Completed labels, " +
            "$missingGeometry word-position issues. " +
            "This is a read-only diagnostic, not a verified import. No transactions were added."
}

/** Strict bounded Binder wire format with no strings from the document. */
object PdfBoxProbeWire {
    const val FIELDS_PER_PAGE = 7

    fun encode(report: PdfBoxProbeReport): IntArray =
        report.pages.flatMap { page ->
            listOf(
                page.number, page.textCharacters, page.positionedWords,
                page.unpositionedWords, page.invalidRectangles,
                page.possibleReceiptRows, page.completedMarkers
            )
        }.toIntArray()

    fun decode(pageCount: Int, payload: IntArray): PdfBoxProbeReport {
        require(pageCount in 1..PdfTextReviewPolicy.MAX_PAGES &&
            payload.size == pageCount * FIELDS_PER_PAGE) { "Incomplete PDFBox probe transfer." }
        return PdfBoxProbeReport((0 until pageCount).map { page ->
            val offset = page * FIELDS_PER_PAGE
            PdfBoxPageProbe(
                payload[offset], payload[offset+1], payload[offset+2], payload[offset+3],
                payload[offset+4], payload[offset+5], payload[offset+6]
            )
        })
    }
}

/** Conservative non-financial test of text/position quality for isolated probe. */
object PdfBoxWordBounds {
    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    fun fromCharacters(characters: List<Rect>, maxHeight: Float, maxWidth: Float): Rect? {
        if (characters.isEmpty() || characters.size > 300 ||
            !maxHeight.isFinite() || !maxWidth.isFinite() ||
            maxHeight <= 0f || maxWidth <= 0f ||
            characters.any { c ->
                !c.left.isFinite() || !c.top.isFinite() ||
                !c.right.isFinite() || !c.bottom.isFinite() ||
                c.left < -8f || c.top < -8f ||
                c.right > maxWidth+8f || c.bottom > maxHeight+8f ||
                c.right <= c.left || c.bottom <= c.top
            }) return null
        return Rect(
            characters.minOf { it.left },characters.minOf { it.top },
            characters.maxOf { it.right },characters.maxOf { it.bottom }
        )
    }
}
