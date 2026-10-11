package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

class CashEvidenceWriteGateTest {
    @Test fun expectedSalaryAndPayslipCannotAuthorizeCash() {
        for (request in listOf(
            FinancialEvidenceRequest(FinancialEvidenceSource.MANUAL, FinancialEvidenceClaim.EXPECTED_RECEIPT, userConfirmed = true, ownAccountResolved = true),
            FinancialEvidenceRequest(FinancialEvidenceSource.PAYSLIP, FinancialEvidenceClaim.CASH_RECEIVED, interpreted = true, userConfirmed = true, ownAccountResolved = true)
        )) assertThrows(IllegalArgumentException::class.java) { FinancialEvidenceContract.requireCashCandidate(request) }
    }
    @Test fun manualReceiptNeedsNoDocumentButNeedsResolvedAccount() {
        val request = FinancialEvidenceRequest(FinancialEvidenceSource.MANUAL, FinancialEvidenceClaim.CASH_RECEIVED, userConfirmed = true, ownAccountResolved = true)
        FinancialEvidenceContract.requireCashCandidate(request)
        assertThrows(IllegalArgumentException::class.java) { FinancialEvidenceContract.requireCashCandidate(request.copy(ownAccountResolved = false)) }
    }
    @Test fun statementSelectionMustBeInterpretedAndExplicit() {
        val request = FinancialEvidenceRequest(FinancialEvidenceSource.STATEMENT, FinancialEvidenceClaim.CASH_SENT, interpreted = true, userConfirmed = true, ownAccountResolved = true)
        FinancialEvidenceContract.requireCashCandidate(request)
        assertThrows(IllegalArgumentException::class.java) { FinancialEvidenceContract.requireCashCandidate(request.copy(userConfirmed = false)) }
        assertThrows(IllegalArgumentException::class.java) { FinancialEvidenceContract.requireCashCandidate(request.copy(interpreted = false)) }
    }
}
