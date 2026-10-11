package com.kedevjimmy.pesatrack.domain.payroll

import org.junit.Assert.*
import org.junit.Test

class PayslipPrivacyReviewTest {
    @Test fun salaryNumbersSurviveWithoutSavingIdentifiersOrOriginalPdfText() {
        val review=PayslipFieldReview.read(listOf("""
            COMPANY NAME: Example Employer
            PAY PERIOD: September 2026
            Staff Number: 008122
            Work Email: employee@example.invalid
            Postal Address: P.O. BOX 123
            GROSS PAY: 80,000.00
            BASIC PAY: 65,000.00
            HOUSE ALLOWANCE: 15,000.00
            GROSS TAX PAY: 80,000.00
            PAYE: 15,000.00
            NSSF: 2,000.00
            SHIF: 2,000.00
            SACCO DEPOSIT: 4,000.00
            NET PAY: 57,000.00
        """.trimIndent()))
        assertEquals("Example Employer",review.employer)
        assertEquals("2026-09",review.period)
        assertEquals(8000000L,review.grossMinor)
        assertEquals(5700000L,review.netMinor)
        assertEquals(4,review.lines.size)
        assertEquals(3,review.earnings.size)
        assertTrue(review.warnings.any{it.contains("Currency is not printed")})
        val savedProjection=listOfNotNull(review.employer,review.period,review.grossMinor.toString(),
            review.netMinor.toString())+review.lines.map{it.label+":"+it.amountMinor}+
            review.earnings.map{it.label+":"+it.amountMinor}
        val summary=savedProjection.joinToString("|")
        assertFalse(summary.contains("008122"));assertFalse(summary.contains("@"))
        assertFalse(summary.contains("BOX"))
    }
    @Test fun YtdAmbiguityDoesNotInventEarnings() {
        val review=PayslipFieldReview.read(listOf("Currency: KES\nBASIC PAY: 65000 780000\nGROSS PAY: 80000\nNET PAY: 56000"))
        assertTrue(review.earnings.isEmpty())
        assertEquals(8000000L,review.grossMinor)
    }
}
