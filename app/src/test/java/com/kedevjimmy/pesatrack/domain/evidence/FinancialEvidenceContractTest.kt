package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialEvidenceContractTest {
    private fun check(
        source: FinancialEvidenceSource,
        claim: FinancialEvidenceClaim,
        interpreted: Boolean = false,
        userConfirmed: Boolean = false,
        ownAccountResolved: Boolean = false
    ): EvidenceDisposition = FinancialEvidenceContract.disposition(
        FinancialEvidenceRequest(
            source = source,
            claim = claim,
            interpreted = interpreted,
            userConfirmed = userConfirmed,
            ownAccountResolved = ownAccountResolved
        )
    )

    @Test fun manualReceivedSalaryNeedsNoSmsStatementOrPayslip() {
        assertEquals(
            EvidenceDisposition.RECONCILE_CASH_CANDIDATE,
            check(
                FinancialEvidenceSource.MANUAL,
                FinancialEvidenceClaim.CASH_RECEIVED,
                userConfirmed = true,
                ownAccountResolved = true
            )
        )
    }

    @Test fun plannedManualSalaryDoesNotBecomeIncome() {
        assertEquals(
            EvidenceDisposition.PLANNING_ONLY,
            check(
                FinancialEvidenceSource.MANUAL,
                FinancialEvidenceClaim.EXPECTED_RECEIPT,
                userConfirmed = true,
                ownAccountResolved = true
            )
        )
    }

    @Test fun anUnconfirmedManualAmountCannotEnterCashLedger() {
        assertEquals(
            EvidenceDisposition.NEEDS_REVIEW,
            check(
                FinancialEvidenceSource.MANUAL,
                FinancialEvidenceClaim.CASH_RECEIVED,
                ownAccountResolved = true
            )
        )
    }

    @Test fun aManualReceiptWithoutResolvedOwnAccountRemainsUncertain() {
        assertEquals(
            EvidenceDisposition.NEEDS_REVIEW,
            check(
                FinancialEvidenceSource.MANUAL,
                FinancialEvidenceClaim.CASH_RECEIVED,
                userConfirmed = true
            )
        )
    }

    @Test fun trustedSmsCanProceedWithoutAnyDocument() {
        assertEquals(
            EvidenceDisposition.RECONCILE_CASH_CANDIDATE,
            check(
                FinancialEvidenceSource.SMS,
                FinancialEvidenceClaim.CASH_RECEIVED,
                interpreted = true,
                ownAccountResolved = true
            )
        )
    }

    @Test fun trustedNotificationCanProceedWithoutSmsHistoryImport() {
        assertEquals(
            EvidenceDisposition.RECONCILE_CASH_CANDIDATE,
            check(
                FinancialEvidenceSource.NOTIFICATION,
                FinancialEvidenceClaim.CASH_SENT,
                interpreted = true,
                ownAccountResolved = true
            )
        )
    }

    @Test fun unknownSmsMustNotCreateNewAccountByProviderNameAlone() {
        assertEquals(
            EvidenceDisposition.NEEDS_REVIEW,
            check(
                FinancialEvidenceSource.SMS,
                FinancialEvidenceClaim.CASH_RECEIVED,
                interpreted = true
            )
        )
    }

    @Test fun statementRowNeedsInterpretationConfirmationAndOwnedAccount() {
        val source = FinancialEvidenceSource.STATEMENT
        val claim = FinancialEvidenceClaim.CASH_RECEIVED
        assertEquals(EvidenceDisposition.NEEDS_REVIEW, check(source, claim, interpreted = true))
        assertEquals(EvidenceDisposition.NEEDS_REVIEW, check(source, claim, interpreted = true, userConfirmed = true))
        assertEquals(EvidenceDisposition.NEEDS_REVIEW, check(source, claim, userConfirmed = true, ownAccountResolved = true))
        assertEquals(
            EvidenceDisposition.RECONCILE_CASH_CANDIDATE,
            check(source, claim, interpreted = true, userConfirmed = true, ownAccountResolved = true)
        )
    }

    @Test fun payslipNetPayableCannotCreateBankDeposit() {
        for (claim in listOf(FinancialEvidenceClaim.CASH_RECEIVED, FinancialEvidenceClaim.CASH_SENT)) {
            assertEquals(
                EvidenceDisposition.UNSUPPORTED,
                check(
                    FinancialEvidenceSource.PAYSLIP,
                    claim,
                    interpreted = true,
                    userConfirmed = true,
                    ownAccountResolved = true
                )
            )
        }
    }

    @Test fun parsedPayslipOrConfirmedManualDeductionsStayPayrollOnly() {
        assertEquals(
            EvidenceDisposition.PAYROLL_ONLY,
            check(
                FinancialEvidenceSource.PAYSLIP,
                FinancialEvidenceClaim.PAYROLL_BREAKDOWN,
                interpreted = true
            )
        )
        assertEquals(
            EvidenceDisposition.PAYROLL_ONLY,
            check(
                FinancialEvidenceSource.MANUAL,
                FinancialEvidenceClaim.PAYROLL_BREAKDOWN,
                userConfirmed = true
            )
        )
    }

    @Test fun unverifiedPayslipExtractionNeedsReviewBeforePayrollFacts() {
        assertEquals(
            EvidenceDisposition.NEEDS_REVIEW,
            check(FinancialEvidenceSource.PAYSLIP, FinancialEvidenceClaim.PAYROLL_BREAKDOWN)
        )
    }

    @Test fun aPayrollBreakdownCannotBeInventedFromSmsOrStatement() {
        for (source in listOf(
            FinancialEvidenceSource.SMS,
            FinancialEvidenceSource.NOTIFICATION,
            FinancialEvidenceSource.STATEMENT
        )) {
            assertEquals(
                EvidenceDisposition.UNSUPPORTED,
                check(source, FinancialEvidenceClaim.PAYROLL_BREAKDOWN, interpreted = true)
            )
        }
    }

    @Test fun documentOrSmsCannotClaimManualPlannedSalary() {
        for (source in FinancialEvidenceSource.values()) {
            if (source == FinancialEvidenceSource.MANUAL) continue
            assertEquals(
                EvidenceDisposition.UNSUPPORTED,
                check(
                    source,
                    FinancialEvidenceClaim.EXPECTED_RECEIPT,
                    interpreted = true,
                    userConfirmed = true,
                    ownAccountResolved = true
                )
            )
        }
    }

    @Test fun eachCashSourceRequiresIndependentProof() {
        for (source in FinancialEvidenceSource.values()) {
            assertEquals(
                if (source == FinancialEvidenceSource.PAYSLIP) EvidenceDisposition.UNSUPPORTED
                else EvidenceDisposition.NEEDS_REVIEW,
                check(source, FinancialEvidenceClaim.CASH_RECEIVED)
            )
        }
    }
}
