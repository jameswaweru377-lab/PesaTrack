package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialEvidenceMatchPolicyTest {
    private fun observation(
        source: FinancialEvidenceSource = FinancialEvidenceSource.SMS,
        scope: String = "sample-bank-sender",
        nativeKey: String? = "sms-001",
        claim: FinancialEvidenceClaim = FinancialEvidenceClaim.CASH_RECEIVED,
        account: String? = "confirmed-owned-bank",
        amount: Long? = 52_000_00L,
        occurredAt: Long? = 1_700_000_000_000L,
        reference: String? = "TESTREF01"
    ) = CashEvidenceObservation(
        source = source,
        sourceScope = scope,
        sourceNativeKey = nativeKey,
        claim = claim,
        ownedAccountId = account,
        amountMinor = amount,
        occurredAtEpochMs = occurredAt,
        transactionReference = reference
    )

    @Test fun sameNativeIdentityAndMatchingFactsIsReplayNotNewIncome() {
        val first = observation()
        assertEquals(CashEvidenceMatchResult.SAME_OBSERVATION_REPLAY,
            FinancialEvidenceMatchPolicy.compare(first, first.copy()))
    }

    @Test fun reusedNativeIdentityWithDifferentAmountIsConflictNotOverwrite() {
        assertEquals(CashEvidenceMatchResult.SOURCE_IDENTITY_CONFLICT,
            FinancialEvidenceMatchPolicy.compare(observation(), observation(amount = 53_000_00L)))
    }

    @Test fun correctedTimeOnSameIdentityCannotBeSilentlyOverwritten() {
        val first = observation()
        assertEquals(CashEvidenceMatchResult.SOURCE_IDENTITY_CONFLICT,
            FinancialEvidenceMatchPolicy.compare(first,
                first.copy(occurredAtEpochMs = first.occurredAtEpochMs!! + 60_000L)))
    }

    @Test fun sameReferenceDifferentSourcesOffersStrongCandidateOnly() {
        val sms = observation()
        val csv = observation(source = FinancialEvidenceSource.STATEMENT,
            scope = "example-statement", nativeKey = "row-1", reference = "testref01")
        assertEquals(CashEvidenceMatchResult.STRONG_REFERENCE_CANDIDATE,
            FinancialEvidenceMatchPolicy.compare(sms, csv))
    }

    @Test fun similarAmountAndTimeWithoutSharedReferenceIsReviewNotAutoMerge() {
        val sms = observation(reference = null)
        val csv = observation(source = FinancialEvidenceSource.STATEMENT,
            scope = "file", nativeKey = "row-7", reference = null)
        assertEquals(CashEvidenceMatchResult.POSSIBLE_MATCH_REVIEW,
            FinancialEvidenceMatchPolicy.compare(sms, csv))
    }

    @Test fun sameAmountTwinsWithDifferentReferencesStayDistinct() {
        assertEquals(CashEvidenceMatchResult.DISTINCT_MOVEMENTS,
            FinancialEvidenceMatchPolicy.compare(
                observation(source = FinancialEvidenceSource.STATEMENT, nativeKey = "row-1"),
                observation(source = FinancialEvidenceSource.STATEMENT, nativeKey = "row-2",
                    reference = "DIFFERENT_REFERENCE")
            ))
    }

    @Test fun oppositeDirectionsAreNeverCashDuplicates() {
        assertEquals(CashEvidenceMatchResult.DISTINCT_MOVEMENTS,
            FinancialEvidenceMatchPolicy.compare(observation(),
                observation(source = FinancialEvidenceSource.STATEMENT, claim = FinancialEvidenceClaim.CASH_SENT)))
    }

    @Test fun differentOwnedAccountsRequireTransferReconciliationInstead() {
        assertEquals(CashEvidenceMatchResult.DISTINCT_MOVEMENTS,
            FinancialEvidenceMatchPolicy.compare(observation(),
                observation(source = FinancialEvidenceSource.STATEMENT, account = "another-owned-account")))
    }

    @Test fun missingOwnedAccountCannotInferOwnershipFromSenderOrReference() {
        assertEquals(CashEvidenceMatchResult.INSUFFICIENT_EVIDENCE,
            FinancialEvidenceMatchPolicy.compare(observation(account = null),
                observation(source = FinancialEvidenceSource.STATEMENT)))
    }

    @Test fun payslipNetPayableNeverProvesReceivedBankCash() {
        assertEquals(CashEvidenceMatchResult.INSUFFICIENT_EVIDENCE,
            FinancialEvidenceMatchPolicy.compare(observation(source = FinancialEvidenceSource.PAYSLIP),
                observation(source = FinancialEvidenceSource.STATEMENT)))
    }

    @Test fun expectedSalaryIsNotCashEvidence() {
        assertEquals(CashEvidenceMatchResult.INSUFFICIENT_EVIDENCE,
            FinancialEvidenceMatchPolicy.compare(observation(claim = FinancialEvidenceClaim.EXPECTED_RECEIPT),
                observation(source = FinancialEvidenceSource.STATEMENT)))
    }

    @Test fun timeGapBeyondReferenceWindowRejectsLink() {
        assertEquals(CashEvidenceMatchResult.DISTINCT_MOVEMENTS,
            FinancialEvidenceMatchPolicy.compare(observation(),
                observation(source = FinancialEvidenceSource.STATEMENT,
                    occurredAt = 1_700_000_000_000L + 4L * 24L * 60L * 60L * 1000L)))
    }

    @Test fun sameNativeIdAcrossDifferentSourceScopesIsNotReplay() {
        val first = observation(scope = "bank-a", reference = null)
        val second = observation(scope = "bank-b", reference = null)
        assertEquals(CashEvidenceMatchResult.POSSIBLE_MATCH_REVIEW,
            FinancialEvidenceMatchPolicy.compare(first, second))
    }

    @Test fun unknownAmountsDoNotBecomeIncome() {
        assertEquals(CashEvidenceMatchResult.INSUFFICIENT_EVIDENCE,
            FinancialEvidenceMatchPolicy.compare(observation(amount = null),
                observation(source = FinancialEvidenceSource.STATEMENT)))
    }
}
