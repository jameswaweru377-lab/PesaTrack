package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/** Invented receipts and amounts; never store screenshots or private account metadata as fixtures. */
class SafaricomStatementLayoutTest {
    private val zone=TimeZone.getTimeZone("Africa/Nairobi")
    private val official= """
        M-PESA STATEMENT
        Statement Period: 11 May 2026 - 11 Oct 2026
        SUMMARY
        Transaction Type PAID IN PAID OUT
        DETAILED STATEMENT
        Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance
    """.trimIndent()+"\n"
    private val newer="QAB1234569 2026-10-10 10:30:00 Payment at shop Completed -20.00 490.00"
    private val middle="QAB1234568 2026-10-09 10:30:00 Funds received Completed 100.00 510.00"
    private val older="QAB1234567 2026-10-08 10:30:00 Payment of fare Completed -50.00 410.00"

    @Test fun kenyanStatementWithoutPrintedCurrencyCanBeReviewedAfterBalanceProof() {
        val pages=listOf(official+newer+"\n"+middle+"\n"+
            "Disclaimer: Verify your statement\nStatement Verification Code\n",
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"+
                older+"\nDisclaimer: verification code")
        val result=PdfStatementReview.read(pages,zone)
        assertEquals(result.problems.joinToString(),3,result.rows.size)
        assertEquals(listOf(-2000L,10000L,-5000L),result.rows.map{it.signedMinor})
        assertNotNull(result.currencyReview)
    }

    @Test fun statementTitleAloneDoesNotAuthorizeCurrencyGuess() {
        val text="M-PESA STATEMENT\nReceipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"+newer
        val result=PdfStatementReview.read(listOf(text),zone)
        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.isNotEmpty())
    }

    @Test fun explicitForeignCurrencyOverridesBrandedStructure() {
        val result=PdfStatementReview.read(listOf(official+"Currency: USD\n"+newer+"\n"+middle+"\n"+older),zone)
        assertTrue(result.rows.isEmpty())
    }

    @Test fun incorrectBalancesNeverImportAnyPartialRows() {
        val text=official+newer+"\n"+middle.replace("510.00","610.00")+"\n"+older
        val result=PdfStatementReview.read(listOf(text),zone)
        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.any{it.contains("balances")})
    }

    @Test fun noSignedDebitsMeansCompactMoneyColumnsAreUnproven() {
        val text=official+middle+"\nQAB1234567 2026-10-08 10:30:00 Deposit Completed 100.00 410.00"
        val result=PdfStatementReview.read(listOf(text),zone)
        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.isNotEmpty())
    }

    @Test fun explicitThreeCellLayoutStillWorksWithSignedWithdrawnAmount() {
        val withCurrency="M-PESA Statement\nCurrency: KES\n" +
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"+
            "QAB1234567 2026-10-08 10:30:00 Transport Completed - -50.00 410.00"
        val result=PdfStatementReview.read(listOf(withCurrency),zone)
        assertEquals(result.problems.joinToString(),listOf(-5000L),result.rows.map{it.signedMinor})
        assertNull(result.currencyReview)
    }

    @Test fun footerAndSummaryMustNotTurnIntoTransactionOrMoney() {
        val text=official+newer+"\n"+middle+"\n"+older+"\n"+
            "Disclaimer: Personal information handled privately\n"+
            "Statement Verification Code ABCDEFG\n"+
            "PAID IN 1000.00 PAID OUT 1000.00"
        val result=PdfStatementReview.read(listOf(text),zone)
        assertEquals(result.problems.joinToString(),3,result.rows.size)
    }
}
