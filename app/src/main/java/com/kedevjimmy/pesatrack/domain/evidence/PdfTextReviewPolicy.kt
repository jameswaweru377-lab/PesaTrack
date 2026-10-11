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
