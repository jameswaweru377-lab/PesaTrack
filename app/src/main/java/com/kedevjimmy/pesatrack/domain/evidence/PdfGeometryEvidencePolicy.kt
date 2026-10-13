package com.kedevjimmy.pesatrack.domain.evidence

import java.util.Locale

/**
 * Every native text block must either have sound geometry or be provably
 * irrelevant to receipt reconstruction. Missing receipt/money evidence always
 * makes the page incomplete; missing whitespace must not block a whole PDF.
 */
enum class PdfGeometryDisposition {
    POSITIONED,
    SKIPPED_BLANK,
    SKIPPED_STATIC_HEADING,
    MISSING_BOUNDS,
    INVALID_BOUNDS,
    TOO_MANY_LINE_BOUNDS,
    OVERSIZED_CONTENT,
    UNSAFE_TEXT,
    TOTAL_FRAGMENT_LIMIT
}

/**
 * Never transports native document text, coordinates, identifiers or money.
 * Only fixed-reason counts go from the isolated PDF process to its UI.
 */
data class PdfGeometryDiagnostics(val counts: List<Int>) {
    init {
        require(counts.size == PdfGeometryDisposition.entries.size && counts.all { it >= 0 }) {
            "Invalid PDF geometry diagnostic counters."
        }
    }

    fun count(reason: PdfGeometryDisposition) = counts[reason.ordinal]

    fun safeSummary(): String {
        val reasons = listOf(
            PdfGeometryDisposition.MISSING_BOUNDS to "unbounded text block(s)",
            PdfGeometryDisposition.INVALID_BOUNDS to "invalid text rectangle(s)",
            PdfGeometryDisposition.TOO_MANY_LINE_BOUNDS to "text block(s) with too many line rectangles",
            PdfGeometryDisposition.OVERSIZED_CONTENT to "oversized text block(s)",
            PdfGeometryDisposition.UNSAFE_TEXT to "unsupported text block(s)",
            PdfGeometryDisposition.TOTAL_FRAGMENT_LIMIT to "text block(s) beyond the geometry limit"
        )
        return reasons.mapNotNull { (reason,label) ->
            count(reason).takeIf { it > 0 }?.let { "$it $label" }
        }.joinToString("; ").ifBlank { "Android did not provide usable row coordinates." }
    }
}

object PdfGeometryEvidencePolicy {
    const val MAX_LINE_RECTS = 24
    const val MAX_TEXT = PdfPositionChunkAssembler.MAX_FRAGMENT_TEXT
    const val MAX_TOTAL = PdfPositionChunkAssembler.MAX_TOTAL_FRAGMENTS

    private val fixedTitles = setOf(
        "M-PESA STATEMENT", "M PESA STATEMENT", "SUMMARY",
        "DETAILED STATEMENT", "RECEIPT NO", "RECEIPT NO."
    )
    private val columnHeader = Regex(
        "^RECEIPT NO\\.? COMPLETION TIME DETAILS TRANSACTION STATUS PAID IN" +
            "(?: \\(KES\\))? WITHDRAWN(?: \\(KES\\))? BALANCE(?: \\(KES\\))?$"
    )
    private fun safeHeading(text: String): Boolean {
        if (text.length > 200 || text.any { it.isISOControl() && !it.isWhitespace() }) return false
        val cleaned = text.trim().uppercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        return cleaned in fixedTitles || columnHeader.matches(cleaned)
    }

    /**
     * Independent of provider amounts, account details, or arbitrary words.
     * A fragment resembling a transaction is never treated as decoration.
     */
    fun classify(
        text: String,
        boundCount: Int,
        allBoundsValid: Boolean,
        totalLimitReached: Boolean
    ): PdfGeometryDisposition {
        if (text.isBlank()) return PdfGeometryDisposition.SKIPPED_BLANK
        if (text.length <= MAX_TEXT &&
            text.none { it.isISOControl() && it !in setOf('\n', '\r', '\t') } &&
            boundCount in 1..MAX_LINE_RECTS && allBoundsValid && !totalLimitReached
        ) return PdfGeometryDisposition.POSITIONED
        // Whitelisted headings are already available in the complete text
        // channel; their physical position is not receipt/money evidence.
        if (safeHeading(text)) return PdfGeometryDisposition.SKIPPED_STATIC_HEADING
        if (boundCount == 0) return PdfGeometryDisposition.MISSING_BOUNDS
        if (!allBoundsValid) return PdfGeometryDisposition.INVALID_BOUNDS
        if (boundCount > MAX_LINE_RECTS) return PdfGeometryDisposition.TOO_MANY_LINE_BOUNDS
        if (text.length > MAX_TEXT) return PdfGeometryDisposition.OVERSIZED_CONTENT
        if (text.any {it.isISOControl() && it !in setOf('\n','\r','\t')})
            return PdfGeometryDisposition.UNSAFE_TEXT
        return PdfGeometryDisposition.TOTAL_FRAGMENT_LIMIT
    }

    fun unpack(pageCount: Int, raw: IntArray): List<PdfGeometryDiagnostics> {
        val columns = PdfGeometryDisposition.entries.size
        require(pageCount in 1..PdfTextReviewPolicy.MAX_PAGES && raw.size == pageCount * columns) {
            "PDF geometry reasons were not fully transferred."
        }
        return (0 until pageCount).map {page ->
            PdfGeometryDiagnostics(raw.slice(page * columns until (page + 1) * columns))
        }
    }

    fun pack(rows: List<IntArray>): IntArray {
        val columns = PdfGeometryDisposition.entries.size
        require(rows.size in 1..PdfTextReviewPolicy.MAX_PAGES &&
            rows.all {it.size == columns && it.all {v -> v >= 0}})
        return rows.flatMap {it.toList()}.toIntArray()
    }
}
