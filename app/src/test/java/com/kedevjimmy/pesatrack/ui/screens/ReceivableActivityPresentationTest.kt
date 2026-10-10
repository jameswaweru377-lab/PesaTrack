package com.kedevjimmy.pesatrack.ui.screens
import org.junit.Assert.*
import org.junit.Test
class ReceivableActivityPresentationTest {
    @Test fun loanLabelIncludesConfirmedPersonAndPrincipal() {
        val link = ReceivableActivityLink("LENT","Sample Borrower",80000,true)
        assertEquals("Debts → Owed to me",link.label)
        assertTrue(link.description.contains("Sample Borrower"))
        assertTrue(link.description.contains("800.00"))
        assertFalse(link.description.contains("1,600.00"))
    }
    @Test fun directLoanDescriptionDistinguishesItFromSharedBills() {
        val link = ReceivableActivityLink("LENT","Norman",20000,true)
        assertTrue(link.description.contains("Money lent"))
    }
    @Test fun sharedBillShowsPersonOriginalCategoryAndOwnShare() {
        val link = ReceivableActivityLink("LENT","Norman",12000,true,true,"Eating out",20000)
        assertEquals("Shared bill",link.title)
        assertTrue(link.description.contains("Shared bill with Norman"))
        assertTrue(link.description.contains("Eating out"))
        assertTrue(link.description.contains("Owed to me"))
        assertTrue(link.billSummary!!.contains("120.00 of KES 200.00"))
        assertTrue(link.billSummary!!.contains("Your share: KES 80.00"))
    }
    @Test fun oldPartialLoanDoesNotBecomeAnAssumedSharedBill() {
        val link = ReceivableActivityLink("LENT","Norman",12000,true,false,"Eating out",20000)
        assertEquals("Money lent",link.title)
        assertNull(link.billSummary)
    }
    @Test fun changedSharedSourceDoesNotPresentAnOwnShareAsTrusted() {
        val link = ReceivableActivityLink("LENT","Norman",12000,false,true,"Eating out",12000)
        assertTrue(link.description.contains("Source needs review"))
        assertNull(link.billSummary)
    }
    @Test fun repaymentAndInvalidSourceRemainDistinct() {
        assertEquals("Debts → Repayment received",ReceivableActivityLink("REPAID","Sample",30000,true).label)
        assertTrue(ReceivableActivityLink("LENT","Sample",80000,false).description.contains("Source needs review"))
    }
}
