package com.kedevjimmy.pesatrack.domain.evidence
import com.kedevjimmy.pesatrack.domain.everyday.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class PdfBoundaryRegressionTest {
    private val zone=TimeZone.getTimeZone("Africa/Nairobi")
    private val header="M-PESA Statement\nCurrency: KES\nReceipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"
    private val valid="QAB1234567 2025-02-01 10:20:30 Payment COMPLETED 100.00 - 100.00"
    private val bad="BADREF 2025-02-02 11:00:00 Payment COMPLETED - 50.00 50.00"
    @Test fun malformedRowsBeforeOrAfterValidRowsNeverMergeOrDisappear() {
        for(text in listOf("$valid\n$bad","$bad\n$valid","$valid $bad","$valid\nBADREF invalid-date Payment COMPLETED - 50.00 50.00")) {
            val result=PdfStatementReview.read(listOf(header+text),zone)
            assertTrue(text,result.rows.isEmpty());assertTrue(result.problems.isNotEmpty())
        }
    }
    @Test fun currencyMustComeFromStatementMetadataNotTransactionDescription() {
        for(currency in listOf("Currency USD","Currency\nUSD","Currency: USD","")) {
            val result=PdfStatementReview.read(listOf(header.replace("Currency: KES",currency)+valid.replace("Payment","KES conversion")),zone)
            assertTrue(currency,result.rows.isEmpty())
        }
    }
    @Test fun splitKesMetadataAndWrappedDescriptionsAreSupported() {
        val result=PdfStatementReview.read(listOf(header.replace("Currency: KES","Currency\nKES")+valid.replace("Payment","Payment to\nsynthetic merchant")),zone)
        assertEquals(1,result.rows.size);assertEquals(10000L,result.rows.single().signedMinor)
    }
    @Test fun missingStatusBeforeSplitReceiptNeverStealsLaterAmount() {
        val text="QAB1234567 2025-02-01 10:20:30 Payment 100.00 - 100.00\nQAB1234568\n2025-02-02 11:00:00 Payment COMPLETED - 50.00 50.00"
        assertTrue(PdfStatementReview.read(listOf(header+text),zone).rows.isEmpty())
        assertTrue(PdfStatementReview.read(listOf(header+text.replace("\n"," ")),zone).rows.isEmpty())
    }
    @Test fun correctedReceiptUsesCanonicalUppercaseForSmsReplayDedup() {
        val row=PdfStatementReview.correct("2025-02-01 10:20:30","Payment","100"," qab1234567 ",zone)
        assertEquals("QAB1234567",row.reference)
        for(reference in listOf("BAD","QAB 1234567","QAB1234567\nQAB1234568"))
            assertThrows(IllegalArgumentException::class.java){PdfStatementReview.correct("2025-02-01 10:20:30","Payment","100",reference,zone)}
    }
    @Test fun editedReferencesCannotCollideAcrossDifferentRows() {
        val first=PdfStatementReview.correct("2025-02-01 10:20:30","Payment","100","QAB1234567",zone)
        val second=first.copy(date=first.date+86400000,description="Other payment",signedMinor=-5000,reference="QAB1234568")
        PdfStatementReview.requireUniqueReferences(listOf(first,second))
        assertThrows(IllegalArgumentException::class.java){PdfStatementReview.requireUniqueReferences(listOf(first,second.copy(reference="QAB1234567")))}
        assertThrows(IllegalArgumentException::class.java){PdfStatementReview.requireUniqueReferences(listOf(first,second.copy(reference="qab1234567")))}
    }
    @Test fun recognizedKesColumnUnitsCanEstablishStatementCurrency() {
        val columnHeader="M-PESA Statement\nReceipt No. Completion Time Details Transaction Status Paid In (KES) Withdrawn (KES) Balance (KES)\n"
        val result=PdfStatementReview.read(listOf(columnHeader+valid),zone)
        assertEquals(result.problems.toString(),1,result.rows.size)
        assertEquals(10000L,result.rows.single().signedMinor)
    }
    @Test fun labeledStatementCurrencyAndTitleUnitsAreSupported() {
        val labeled=header.replace("Currency: KES","Statement Currency: KES")
        val title=header.replace("M-PESA Statement\nCurrency: KES","M-PESA Statement (KES)")
        for(text in listOf(labeled,title)) {
            val result=PdfStatementReview.read(listOf(text+valid),zone)
            assertEquals(text,1,result.rows.size)
        }
    }
    @Test fun absentOrForeignOrConflictingHeaderCurrenciesStillFailClosed() {
        val noEvidence="M-PESA Statement\nReceipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"
        val conflicting="M-PESA Statement\nCurrency: USD\nReceipt No. Completion Time Details Transaction Status Paid In (KES) Withdrawn (KES) Balance (KES)\n"
        val foreign="M-PESA Statement (USD)\nReceipt No. Completion Time Details Transaction Status Paid In Withdrawn Balance\n"
        for(text in listOf(noEvidence,conflicting,foreign)) {
            val result=PdfStatementReview.read(listOf(text+valid.replace("Payment","Payment KES")),zone)
            assertTrue(text,result.rows.isEmpty())
            assertTrue(text,result.problems.isNotEmpty())
        }
    }

}
