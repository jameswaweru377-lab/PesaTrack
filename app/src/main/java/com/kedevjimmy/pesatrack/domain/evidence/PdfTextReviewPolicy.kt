package com.kedevjimmy.pesatrack.domain.evidence

data class PdfTextPreview(
    val pages: List<String>,
    val emptyPageNumbers: List<Int>,
    /** Optional in-memory API35+ geometry; empty for existing production callers. */
    val positionedPages: List<List<PdfTextFragment>> = emptyList(),
    /** False only when text that may affect a receipt lacks trustworthy geometry. */
    val positionedComplete: List<Boolean> = emptyList(),
    /** Fixed count-based reasons; no document text or financial values included. */
    val geometryDiagnostics: List<PdfGeometryDiagnostics> = emptyList(),

)

/** Text availability is not a claim that a statement table or payroll was understood. */
object PdfTextReviewPolicy {
    const val MAX_BYTES = 8 * 1024 * 1024L
    const val MAX_PAGES = 160
    const val MAX_CHARACTERS = 500000
    const val TIMEOUT_MS = 90000L
    fun supported(api: Int) = api >= 35
    fun requireBytes(bytes: Long) {
        if(bytes<=0) throw PdfReadException(PdfFailure.EMPTY)
        if(bytes>MAX_BYTES) throw PdfReadException(PdfFailure.TOO_LARGE)
    }
    fun preview(pages: List<String>): PdfTextPreview {
        if(pages.isEmpty()) throw PdfReadException(PdfFailure.NO_TEXT)
        if(pages.size>MAX_PAGES) throw PdfReadException(PdfFailure.TOO_MANY_PAGES)
        if(pages.sumOf { it.length.toLong() }>MAX_CHARACTERS) throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
        if(pages.none {it.isNotBlank()}) throw PdfReadException(PdfFailure.NO_TEXT)
        return PdfTextPreview(pages.toList(), pages.mapIndexedNotNull { i, text -> if (text.isBlank()) i + 1 else null })
    }
}


/** Small ordered IPC chunks avoid Android Binder's single-transaction size limit. */
class PdfTextChunkAssembler {
    companion object { const val CHUNK_CHARACTERS = 12_000 }
    private val pages = mutableListOf<StringBuilder>()
    private var characters = 0L

    fun add(pageIndex: Int, chunk: String) {
        require(chunk.length <= CHUNK_CHARACTERS) { "A PDF text chunk is oversized." }
        require(pageIndex >= 0 && pageIndex < PdfTextReviewPolicy.MAX_PAGES) { "Invalid PDF page index." }
        require(pageIndex == pages.size || pageIndex == pages.size - 1) { "PDF pages arrived out of order." }
        val next = characters + chunk.length
        if(next > PdfTextReviewPolicy.MAX_CHARACTERS)
            throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
        if(pageIndex == pages.size) pages.add(StringBuilder())
        pages[pageIndex].append(chunk)
        characters = next
    }

    fun finish(expectedPages: Int): PdfTextPreview {
        require(pages.size == expectedPages) { "PDF text transfer was incomplete." }
        return PdfTextReviewPolicy.preview(pages.map { it.toString() })
    }
}

/**
 * Bound, ordered text-box IPC assembler. Completeness is claimed only after
 * the service reports exact per-page fragment counts and coverage flags.
 * No text, coordinates or identifiers are logged or persisted.
 */
class PdfPositionChunkAssembler {
    companion object {
        const val MAX_CHUNK = 32
        const val MAX_FRAGMENT_TEXT = 1_200
        const val MAX_TOTAL_FRAGMENTS = 32_000
    }
    private val pages = mutableListOf<MutableList<PdfTextFragment>>()
    private var total = 0
    fun startPage(page: Int) {
        require(page == pages.size && page < PdfTextReviewPolicy.MAX_PAGES) {
            "PDF geometry pages must arrive consecutively."
        }
        pages.add(mutableListOf())
    }
    fun add(
        page: Int, texts: List<String>,
        left: FloatArray, top: FloatArray, right: FloatArray, bottom: FloatArray
    ) {
        require(texts.size in 1..MAX_CHUNK && left.size == texts.size &&
            top.size == texts.size && right.size == texts.size &&
            bottom.size == texts.size) { "Invalid PDF geometry IPC chunk." }
        require(page == pages.size - 1) { "PDF geometry pages arrived out of order." }
        require(total + texts.size <= MAX_TOTAL_FRAGMENTS) { "Too many positioned PDF text blocks." }
        val verified = texts.indices.map { i ->
            val text = texts[i]
            val l = left[i]; val t = top[i]; val r = right[i]; val b = bottom[i]
            // A complete Details block can have wrapped text. Keep its
            // original bounding rectangle, not a flattened money-cell guess.
            require(text.isNotBlank() && text.length <= MAX_FRAGMENT_TEXT &&
                text.none {it.isISOControl() && it !in setOf('\n','\r','\t')} &&
                l.isFinite() && t.isFinite() && r.isFinite() && b.isFinite() &&
                l < r && t < b) { "Invalid PDF geometry fragment." }
            PdfTextFragment(text, l, t, r, b)
        }
        pages[page].addAll(verified)
        total += verified.size
    }

    fun finish(expectedPages: Int, expectedCounts: IntArray, complete: BooleanArray):
        Pair<List<List<PdfTextFragment>>, List<Boolean>> {
        require(expectedPages > 0 && expectedPages <= PdfTextReviewPolicy.MAX_PAGES &&
            pages.size == expectedPages &&
            expectedCounts.size == expectedPages && complete.size == expectedPages &&
            expectedCounts.all { it >= 0 } &&
            expectedCounts.sumOf { it.toLong() } == total.toLong()) { "PDF geometry transfer incomplete." }
        val data = List(expectedPages) { index -> pages.getOrNull(index)?.toList().orEmpty() }
        require(data.indices.all { data[it].size == expectedCounts[it] }) {
            "Missing or out-of-order PDF geometry fragments."
        }
        return data to complete.toList()
    }
}
