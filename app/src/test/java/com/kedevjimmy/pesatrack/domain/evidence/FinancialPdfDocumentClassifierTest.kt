package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialPdfDocumentClassifierTest {
    @Test fun employeePayslipWithNoKesMetadataIsRoutedToPayroll() {
        val pages=listOf("""
            EMPLOYER PAYSLIP
            Staff Number: 000111
            Email: employee@example.invalid
            BASIC PAY 65,000.00
            HOUSE ALLOWANCE 15,000.00
            PAYE 12,000.00
            NET PAY 60,000.00
        """.trimIndent())
        assertEquals(FinancialPdfDocumentKind.PAYSLIP,FinancialPdfDocumentClassifier.classify(pages))
    }
    @Test fun statementMetadataDoesNotBecomePayroll() {
        val pages=listOf("M-PESA Statement\nCurrency: KES\nReceipt No. Completion Time Paid In Withdrawn Balance")
        assertEquals(FinancialPdfDocumentKind.STATEMENT,FinancialPdfDocumentClassifier.classify(pages))
    }
    @Test fun unrelatedEmploymentLetterIsNotClassifiedAsPayslip() {
        val pages=listOf("Employer: Example Company\nStaff employee 000111\nP.O. Box 1234 Nairobi")
        assertEquals(FinancialPdfDocumentKind.UNKNOWN,FinancialPdfDocumentClassifier.classify(pages))
    }
}
