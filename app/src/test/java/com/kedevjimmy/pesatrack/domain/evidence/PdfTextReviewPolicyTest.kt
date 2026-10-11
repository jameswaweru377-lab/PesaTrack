package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

class PdfTextReviewPolicyTest {
    @Test fun olderAndroidCannotPromiseTextExtraction() {
        assertFalse(PdfTextReviewPolicy.supported(24))
        assertFalse(PdfTextReviewPolicy.supported(34))
        assertTrue(PdfTextReviewPolicy.supported(35))
    }
    @Test fun mixedPagesRemainExplicitlyIncomplete() {
        val preview = PdfTextReviewPolicy.preview(listOf("Credit 100.00", "  ", "Payslip"))
        assertEquals(3, preview.pages.size)
        assertEquals(listOf(2), preview.emptyPageNumbers)
        assertEquals("Credit 100.00", preview.pages.first())
    }
    @Test fun everyEmptyPageCannotClaimReadableDocument() {
        assertThrows(IllegalArgumentException::class.java) { PdfTextReviewPolicy.preview(listOf("", " ")) }
    }
    @Test fun limitsFailInsteadOfTruncatingEvidence() {
        assertThrows(IllegalArgumentException::class.java) { PdfTextReviewPolicy.requireBytes(3 * 1024 * 1024 + 1L) }
        assertThrows(IllegalArgumentException::class.java) { PdfTextReviewPolicy.preview(List(41) { "x" }) }
        assertThrows(IllegalArgumentException::class.java) { PdfTextReviewPolicy.preview(listOf("x".repeat(100001))) }
        assertThrows(IllegalArgumentException::class.java) { PdfTextReviewPolicy.preview(emptyList()) }
        PdfTextReviewPolicy.requireBytes(3 * 1024 * 1024L)
        assertEquals(100000, PdfTextReviewPolicy.preview(listOf("x".repeat(100000))).pages.single().length)
    }
}
