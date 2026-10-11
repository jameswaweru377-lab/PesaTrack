package com.kedevjimmy.pesatrack.domain.evidence

data class PdfTextPreview(val pages: List<String>, val emptyPageNumbers: List<Int>)

/** Text availability is not a claim that a statement table or payroll was understood. */
object PdfTextReviewPolicy {
    const val MAX_BYTES = 2 * 1024 * 1024L
    const val MAX_PAGES = 40
    const val MAX_CHARACTERS = 100000
    const val TIMEOUT_MS = 45000L
    fun supported(api: Int) = api >= 35
    fun requireBytes(bytes: Long) {
        require(bytes in 1..MAX_BYTES) { "Choose a PDF up to 2 MB." }
    }
    fun preview(pages: List<String>): PdfTextPreview {
        require(pages.size in 1..MAX_PAGES) { "Choose a PDF with 1–40 pages." }
        require(pages.sumOf { it.length.toLong() } <= MAX_CHARACTERS) { "This PDF has too much text. Choose a shorter document." }
        require(pages.any { it.isNotBlank() }) { "No selectable text was found. This may be a scanned PDF; OCR is not available." }
        return PdfTextPreview(pages.toList(), pages.mapIndexedNotNull { i, text -> if (text.isBlank()) i + 1 else null })
    }
}
