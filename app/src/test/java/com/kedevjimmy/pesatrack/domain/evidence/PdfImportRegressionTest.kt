package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.payroll.*
import com.kedevjimmy.pesatrack.domain.everyday.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class PdfImportRegressionTest {
    @Test fun noTextAndLimitsHaveDistinctSafeFailures() {
        val blank=assertThrows(PdfReadException::class.java) {PdfTextReviewPolicy.preview(listOf(" "))}
        assertEquals(PdfFailure.NO_TEXT,blank.failure)
        assertEquals(PdfFailure.TOO_LARGE,assertThrows(PdfReadException::class.java) {PdfTextReviewPolicy.requireBytes(3145729)}.failure)
        assertEquals(PdfFailure.TOO_MANY_PAGES,assertThrows(PdfReadException::class.java) {PdfTextReviewPolicy.preview(List(41){"x"})}.failure)
        assertEquals(PdfFailure.TOO_MUCH_TEXT,assertThrows(PdfReadException::class.java) {PdfTextReviewPolicy.preview(listOf("x".repeat(100001)))}.failure)
    }
    @Test fun labelledCompanyMonthAndSeparateTextCellsAreReviewed() {
        val s=PayslipFieldReview.read(listOf("Currency: KES\nCompany Name: Example Manufacturing Ltd\nPayroll Month: February 2025\nGross Pay\n70,000.00\nNSSF\n2,000.00\nNHIF\n1,500.00\nNet Pay\n66,500.00"))
        assertEquals("Example Manufacturing Ltd",s.employer);assertEquals("2025-02",s.period)
        assertEquals(7000000L,s.grossMinor);assertEquals(6650000L,s.netMinor)
        assertEquals(listOf(PayrollLineType.NSSF,PayrollLineType.HEALTH),s.lines.map{it.type})
    }
    @Test fun duplicateMetadataAndColumnAmountsCannotBeGuessed() {
        val s=PayslipFieldReview.read(listOf("Currency: KES\nEmployer: Example Ltd\nCompany Name: Other Ltd\nPayroll Month: 2025-02\nPay Period: 2025-03\nGross Pay\n70000 840000\nNSSF\nNHIF\n1500"))
        assertNull(s.employer);assertNull(s.period);assertNull(s.grossMinor)
        assertFalse(s.lines.any{it.type==PayrollLineType.NSSF})
    }
    private val header="M-PESA Statement\nCurrency: KES\nReceipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"
    @Test fun supportedMpesaRowsBecomeCandidatesWithoutCsvRoundTrip() {
        val s=PdfStatementReview.read(listOf(header+"QAB1234567 2025-02-01 10:20:30 Payment received COMPLETED 1,000.00 - 2,000.00\nQAB1234568 2025-02-02 11:00:00 Payment to shop COMPLETED - 200.00 1,800.00"),TimeZone.getTimeZone("Africa/Nairobi"))
        assertEquals(listOf(100000L,-20000L),s.rows.map{it.signedMinor})
        assertEquals(listOf("QAB1234567","QAB1234568"),s.rows.map{it.reference})
        assertTrue(s.problems.isEmpty())
    }
    @Test fun ambiguousOrPartialStatementsNeverOfferPartialImport() {
        for(row in listOf("QAB1234567 2025-02-01 10:20:30 Payment COMPLETED 100.00 100.00 100.00", "QAB1234567 2025-02-01 10:20:30 Payment FAILED 100.00 - 100.00", "QAB1234567 2025-02-31 10:20:30 Payment COMPLETED 100.00 - 100.00", "QAB1234567 2025-02-01 10:20:30 Payment COMPLETED 1,00 - 100.00")) {
            val s=PdfStatementReview.read(listOf(header+row),TimeZone.getTimeZone("Africa/Nairobi"))
            assertTrue(row,s.rows.isEmpty());assertTrue(s.problems.isNotEmpty())
        }
        assertTrue(PdfStatementReview.read(listOf(header,""),TimeZone.getTimeZone("Africa/Nairobi")).rows.isEmpty())
        assertTrue(PdfStatementReview.read(listOf(header.replace("KES","USD")),TimeZone.getTimeZone("Africa/Nairobi")).rows.isEmpty())
    }
    @Test fun correctionsRejectFutureDatesZeroAmountsAndInvalidMoney() {
        val zone=TimeZone.getTimeZone("Africa/Nairobi")
        assertEquals(-12345L,PdfStatementReview.correct("2025-02-01 10:20:30","Corrected","-123.45","QAB1234567",zone).signedMinor)
        for(amount in listOf("0","1,00","NaN","1.001"))assertThrows(IllegalArgumentException::class.java){PdfStatementReview.correct("2025-02-01 10:20:30","Description",amount,"QAB1234567",zone)}
        assertThrows(IllegalArgumentException::class.java){PdfStatementReview.correct("2099-02-01 10:20:30","Description","100","QAB1234567",zone)}
    }
    @Test fun foreignCurrencyAndRepeatedReceiptsStopTheWholePreview() {
        val row="QAB1234567 2025-02-01 10:20:30 Payment COMPLETED 100.00 - 100.00"
        for(pages in listOf(listOf(header+row+"\n"+row),listOf(header.replace("Currency: KES","Currency: USD\nAmounts KES")+row)))
            assertTrue(PdfStatementReview.read(pages,TimeZone.getTimeZone("Africa/Nairobi")).rows.isEmpty())
    }
}
