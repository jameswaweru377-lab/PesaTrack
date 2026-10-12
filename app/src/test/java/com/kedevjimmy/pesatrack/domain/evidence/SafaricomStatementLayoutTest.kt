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

    @Test fun wrappedDetailsBelowMoneyCellsAreAttachedToCorrectReceipt() {
        val text=official+
            "QAB1234569 2026-10-10 10:30:00 Payment Completed -20.00 490.00\n"+
            "to a synthetic merchant\n"+
            "QAB1234568 2026-10-09 10:30:00 Funds received Completed 100.00 510.00\n"+
            "QAB1234567 2026-10-08 10:30:00 Payment Completed -50.00 410.00"
        val result=PdfStatementReview.read(listOf(text),zone)
        assertEquals(result.problems.joinToString(),3,result.rows.size)
        assertTrue(result.rows.first().description.contains("synthetic merchant"))
    }
    @Test fun ambiguousPostRowMoneyAndStatusAreNeverAppendedAsDescription() {
        val text=official+
            "QAB1234569 2026-10-10 10:30:00 Payment Completed -20.00 490.00\n"+
            "100.00\n"+
            "QAB1234568 2026-10-09 10:30:00 Funds received Completed 100.00 510.00\n"+
            "QAB1234567 2026-10-08 10:30:00 Payment Completed -50.00 410.00"
        val result=PdfStatementReview.read(listOf(text),zone)
        assertTrue(result.rows.isEmpty())
    }
    @Test fun printedCurrencyUnitsInTableCanAuthorizeExistingLayout() {
        val text="M-PESA STATEMENT (KES)\n"+
            "Receipt No. Completion Time Details Transaction Status Paid In (KES) Withdrawn (KES) Balance (KES)\n"+
            "QAB1234567 2026-10-08 10:30:00 Taxi Completed - -50.00 410.00"
        val result=PdfStatementReview.read(listOf(text),zone)
        assertEquals(result.problems.joinToString(),1,result.rows.size)
        assertNull(result.currencyReview)
    }

    @Test fun completedStatusAndMoneyCellsOnSeparatePdfLinesRemainReviewable() {
        val page = official+
            "QAB1234569 2026-10-10 10:30:00 Payment at shop\n"+
            "Completed\n-20.00\n490.00\n"+
            "QAB1234568 2026-10-09 10:30:00 Funds received\n"+
            "Completed\n100.00\n510.00\n"+
            "QAB1234567 2026-10-08 10:30:00 Payment of fare\n"+
            "Completed\n-50.00\n410.00"
        val review=PdfStatementReview.read(listOf(page),zone)
        assertEquals(review.problems.joinToString(),3,review.rows.size)
        assertEquals(listOf(-2000L,10000L,-5000L),review.rows.map{it.signedMinor})
    }

    @Test fun threeColumnAmountsFollowingStatusAreKeptInTheirOriginalOrder() {
        val page="M-PESA STATEMENT\nCurrency: KES\n"+
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"+
            "QAB1234567 2026-10-08 10:30:00 Fare payment\n"+
            "Completed\n-\n-50.00\n410.00"
        val review=PdfStatementReview.read(listOf(page),zone)
        assertEquals(review.problems.joinToString(),listOf(-5000L),review.rows.map{it.signedMinor})
    }

    @Test fun repeatedColumnHeaderAfterLastReceiptIsNotItsDescription() {
        val page=official+newer+"\n"+middle+"\n"+older+"\n"+
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance"
        val review=PdfStatementReview.read(listOf(page),zone)
        assertEquals(review.problems.joinToString(),3,review.rows.size)
        assertFalse(review.rows.last().description.contains("Receipt No"))
    }

    @Test fun wrappedMemoAfterSplitMoneyColumnsCanBeReattached() {
        val page=official+
            "QAB1234569 2026-10-10 10:30:00 Payment\n"+
            "Completed\n-20.00\n490.00\n"+
            "at synthetic merchant\n"+
            middle+"\n"+older
        val review=PdfStatementReview.read(listOf(page),zone)
        assertEquals(review.problems.joinToString(),3,review.rows.size)
        assertTrue(review.rows.first().description.contains("synthetic merchant"))
    }

    @Test fun ambiguouslyInterleavedMoneyAndTextStillRejectsWholeStatement() {
        val page=official+
            "QAB1234569 2026-10-10 10:30:00 Payment\n"+
            "Completed\n-20.00\ncontinued memo\n490.00\n"+
            middle+"\n"+older
        val review=PdfStatementReview.read(listOf(page),zone)
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any{it.contains("Page 1, receipt row 1")})
    }

    @Test fun looseNumericContinuationIsNotMistakenForAnotherMoneyCell() {
        val page=official+newer+"\n100.00\n"+middle+"\n"+older
        val review=PdfStatementReview.read(listOf(page),zone)
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any{it.contains("Page 1, receipt row 1")})
    }
}
