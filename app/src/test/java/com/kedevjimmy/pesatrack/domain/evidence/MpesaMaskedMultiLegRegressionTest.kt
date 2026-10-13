package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import com.kedevjimmy.pesatrack.domain.everyday.StatementCsv
import com.kedevjimmy.pesatrack.domain.everyday.StatementLegDraft
import com.kedevjimmy.pesatrack.domain.everyday.StatementRow
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionFingerprint
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

/**
 * Inspired by general *shapes* of multi-line financial statements, not any
 * person's PDF. Every date, code, description and amount below is invented.
 */
class MpesaMaskedMultiLegRegressionTest {
    private val zone=TimeZone.getTimeZone("Africa/Nairobi")
    private val meta = """
        M-PESA STATEMENT
        Statement Period: 01 Aug 2026 - 11 Oct 2026
        DETAILED STATEMENT
        Receipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance
    """.trimIndent()+"\n"
    private val newest = "QAB1234569 2026-10-10 10:30:00 Paid at store Completed -20.00 490.00"
    private val oldest = "QAB1234567 2026-10-08 10:30:00 Paid for transport Completed -50.00 410.00"

    @Test fun pageThreeStyleMaskedReversalAfterStatusAndMoneyRemainsDescription() {
        val middle =
            "QAB1234568 2026-10-09 10:30:00 Reversal via API\n" +
                "Completed 100.00 510.00\n" +
                "from - 071***123456\n" +
                "reference processed"
        val result=PdfStatementReview.read(listOf(meta+newest+"\n"+middle+"\n"+oldest),zone)
        assertTrue(result.problems.joinToString(),result.problems.isEmpty())
        assertEquals(3,result.rows.size)
        assertEquals(10000L,result.rows[1].signedMinor)
        assertTrue(result.rows[1].description.contains("from - 071***123456"))
    }

    @Test fun maskedIdentifiersCanWrapWithoutKnownInstitutionVerbs() {
        val middle =
            "QAB1234568 2026-10-09 10:30:00 Credited by contact\n" +
                "Completed 100.00 510.00\n" +
                "customer - 071***123456\n" +
                "information"
        val result=PdfStatementReview.read(listOf(meta+newest+"\n"+middle+"\n"+oldest),zone)
        assertEquals(result.problems.joinToString(),3,result.rows.size)
        assertTrue(result.rows[1].description.contains("071***123456"))
    }

    @Test fun numericContinuationBeyondValidMoneyCellsStillAbortsWholeStatement() {
        val middle =
            "QAB1234568 2026-10-09 10:30:00 Reversal via API\n" +
                "Completed 100.00 510.00\n" +
                "from - 071***123456\n" +
                "70.00"
        val result=PdfStatementReview.read(listOf(meta+newest+"\n"+middle+"\n"+oldest),zone)
        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.isNotEmpty())
    }

    @Test fun sameReferenceSupportsPaymentAndSeparateFeeWithoutLosingEither() {
        val debitAndFee = listOf(
            "QAB1234569 2026-10-10 10:30:00 Paid merchant Completed -200.00 390.00",
            "QAB1234569 2026-10-10 10:30:00 Fee charged Completed -10.00 590.00",
            "QAB1234568 2026-10-09 10:30:00 Money received Completed 100.00 600.00"
        )
        val result=PdfStatementReview.read(listOf(meta+debitAndFee.joinToString("\n")),zone)
        assertEquals(result.problems.joinToString(),3,result.rows.size)
        assertEquals(listOf(-20000L,-1000L,10000L),result.rows.map {it.signedMinor})
        PdfStatementReview.requireDistinctStatementLegs(result.rows)
        assertEquals(2,result.rows.count {it.reference=="QAB1234569"})
    }

    @Test fun twoIdenticalLegsUnderOneReferenceFailRatherThanSilentlyDeduplicate() {
        val text="QAB1234569 2026-10-10 10:30:00 Payment Completed -20.00 490.00"
        val result=PdfStatementReview.read(listOf(meta+text+"\n"+text+"\n"+oldest),zone)
        assertTrue(result.rows.isEmpty())
    }

    @Test fun eachStatementLegHasItsOwnReproducibleFingerprintWithoutChangingSmsCodeIdentity() {
        val base=StatementRow(1770000000000L,"Synthetic payment",-2000L,"QAB1234569")
        val fee=base.copy(description="Service fee",signedMinor=-100L)
        val keyOne="a".repeat(64)
        val keyTwo="b".repeat(64)
        val p1=StatementLegDraft.create(base,keyOne)
        val p2=StatementLegDraft.create(fee,keyTwo)
        assertNull(p1.transactionCode)
        assertNull(p2.transactionCode)
        assertEquals(TransactionFingerprint.of(p1,base.date),TransactionFingerprint.of(StatementLegDraft.create(base,keyOne),base.date))
        assertNotEquals(TransactionFingerprint.of(p1,base.date),TransactionFingerprint.of(p2,fee.date))
    }

    @Test fun malformedDraftIdentityNeverCreatesMoneyEvidence() {
        val r=StatementRow(1770000000000L,"Synthetic",-100L,"QAB1234569")
        assertThrows(IllegalArgumentException::class.java) {StatementLegDraft.create(r,"not-an-import-hash")}
        assertThrows(IllegalArgumentException::class.java) {StatementLegDraft.create(r.copy(signedMinor=0L),"a".repeat(64))}
    }
}
