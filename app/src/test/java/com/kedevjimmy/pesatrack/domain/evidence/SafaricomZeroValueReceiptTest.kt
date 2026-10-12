package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/** Synthetic receipts and amounts; no identifying statement data is retained. */
class SafaricomZeroValueReceiptTest {
    private val zone=TimeZone.getTimeZone("Africa/Nairobi")
    private val header = """
        M-PESA STATEMENT
        Statement Period: 1 Oct 2026 - 10 Oct 2026
        SUMMARY
        DETAILED STATEMENT
        Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance
    """.trimIndent()+"\n"
    private val newDebit =
        "QAB1234570 2026-10-10 10:30:00 Merchant payment Completed -20.00 490.00"
    private val zero =
        "QAB1234569 2026-10-09 10:30:00 Service activation Completed 0.00 510.00"
    private val olderCredit =
        "QAB1234568 2026-10-08 10:30:00 Received money Completed 100.00 510.00"
    private val oldestDebit =
        "QAB1234567 2026-10-07 10:30:00 Fare payment Completed -50.00 410.00"

    @Test fun verifiedCompactZeroRecordIsKeptForBalanceProofButNotImported() {
        val pages=listOf(header+newDebit+"\n"+zero+
            "\nDisclaimer: local document verification",
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"+
                olderCredit+"\n"+oldestDebit+"\nDisclaimer: synthetic example")
        val review=PdfStatementReview.read(pages,zone)
        assertEquals(review.problems.joinToString(),1,review.informationalRows)
        assertEquals(3,review.rows.size)
        assertEquals(listOf(-2000L,10000L,-5000L),review.rows.map{it.signedMinor})
        assertFalse(review.rows.any{it.reference=="QAB1234569"})
    }

    @Test fun zeroRowWithChangedBalanceFailsClosedWithoutImportingOthers() {
        val review=PdfStatementReview.read(listOf(header+newDebit+"\n"+
            zero.replace("510.00","520.00")+"\n"+olderCredit+"\n"+oldestDebit),zone)
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any{it.contains("balances")})
    }

    @Test fun zeroRowWithNoActualNumericAmountCannotBeDiscarded() {
        val review=PdfStatementReview.read(listOf(header+newDebit+"\n"+
            zero.replace("0.00 510.00","- 510.00")+"\n"+olderCredit+"\n"+oldestDebit),zone)
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any{it.contains("no readable amount")})
    }

    @Test fun duplicateZeroReceiptAlsoRejectsEntireImport() {
        val review=PdfStatementReview.read(listOf(header+newDebit+"\n"+zero+"\n"+
            olderCredit.replace("QAB1234568","QAB1234569")+"\n"+oldestDebit),zone)
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any{it.contains("Repeated receipt")})
    }

    @Test fun tripleColumnZeroNumbersUseBalanceProofNotIncomePosting() {
        val headerWithCurrency="M-PESA STATEMENT\nCurrency: KES\n"+
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"
        val newer="QAB1234568 2026-10-09 10:30:00 Statement verification Completed 0.00 0.00 410.00"
        val older="QAB1234567 2026-10-08 10:30:00 Fare payment Completed - -50.00 410.00"
        val review=PdfStatementReview.read(listOf(headerWithCurrency+newer+"\n"+older),zone)
        assertEquals(review.problems.joinToString(),1,review.informationalRows)
        assertEquals(listOf(-5000L),review.rows.map{it.signedMinor})
    }

    @Test fun dashOnlyTripleColumnsNeverProveZeroMoneyMovement() {
        val headerWithCurrency="M-PESA STATEMENT\nCurrency: KES\n"+
            "Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"
        val newer="QAB1234568 2026-10-09 10:30:00 Statement verification Completed - - 410.00"
        val older="QAB1234567 2026-10-08 10:30:00 Fare payment Completed - -50.00 410.00"
        val review=PdfStatementReview.read(listOf(headerWithCurrency+newer+"\n"+older),zone)
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any{it.contains("missing or ambiguous money cells")})
    }

    @Test fun zeroOnlyStatementCannotCreateEmptyTransactionImport() {
        val body=header+
            "QAB1234569 2026-10-09 10:30:00 Account note Completed 0.00 500.00\n"+
            "QAB1234568 2026-10-08 10:30:00 Account note Completed 0.00 500.00"
        val review=PdfStatementReview.read(listOf(body),zone)
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.isNotEmpty())
    }

    @Test fun singleZeroWithoutAdjacentBalanceProofFailsClosed() {
        val review=PdfStatementReview.read(listOf(header+zero),zone)
        assertTrue(review.rows.isEmpty())
        assertTrue(review.problems.any{it.contains("adjacent balance evidence")})
    }
}
