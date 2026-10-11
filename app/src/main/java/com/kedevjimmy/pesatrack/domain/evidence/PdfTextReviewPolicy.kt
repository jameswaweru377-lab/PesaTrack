package com.kedevjimmy.pesatrack.domain.evidence

data class PdfTextPreview(val pages: List<String>, val emptyPageNumbers: List<Int>)

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
