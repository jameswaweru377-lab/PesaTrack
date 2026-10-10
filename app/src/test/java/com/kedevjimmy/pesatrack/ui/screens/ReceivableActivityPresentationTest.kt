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
    @Test fun repaymentAndInvalidSourceRemainDistinct() {
        assertEquals("Debts → Repayment received",ReceivableActivityLink("REPAID","Sample",30000,true).label)
        assertTrue(ReceivableActivityLink("LENT","Sample",80000,false).description.contains("Source needs review"))
    }
}
