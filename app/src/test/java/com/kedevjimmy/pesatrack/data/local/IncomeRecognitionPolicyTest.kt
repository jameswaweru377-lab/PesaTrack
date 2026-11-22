package com.kedevjimmy.pesatrack.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** All identities, amounts and sources here are invented. */
class IncomeRecognitionPolicyTest {
    private val confirmedOwned = setOf("owned-wallet")
    @Test fun referencedThirdPartyReceiptIsGenuineIncomeWithoutSalaryRule() {
        val receipt = row(1).copy(counterparty = "Fictional Client",
            transactionCode = "FAKEEXT001", sourceKind = "historical_sms")
        assertEquals(IncomeRecognitionStatus.CONFIRMED,
            IncomeRecognitionPolicy.classify(receipt, confirmedOwned).status)
    }

    @Test fun firstObservedOwnBankToWalletReceiptNeverManufacturesSalary() {
        val onlyVisibleLeg = row(2).copy(
            accountId = "owned-wallet", linkedAccountId = "owned-bank",
            counterparty = "Fictional Bank", transactionCode = "FAKETFR001")
        assertEquals(IncomeRecognitionStatus.NOT_INCOME,
            IncomeRecognitionPolicy.classify(onlyVisibleLeg, confirmedOwned).status)
        assertFalse(IncomeRecognitionPolicy.isConfirmedIncome(onlyVisibleLeg, confirmedOwned))
    }

    @Test fun cashDepositAndUnattributedBankCreditStayUnresolved() {
        val deposit = row(3).copy(type = "DEPOSIT", counterparty = null)
        val bankCredit = row(4).copy(type = "RECEIVED",
            transactionCode = null, counterparty = null,
            sourceKind = "historical_financial_message")
        assertEquals(IncomeRecognitionStatus.UNRESOLVED,
            IncomeRecognitionPolicy.classify(deposit, confirmedOwned).status)
        assertEquals(IncomeRecognitionStatus.UNRESOLVED,
            IncomeRecognitionPolicy.classify(bankCredit, confirmedOwned).status)
    }

    @Test fun anUnclassifiedReceiptIsNotAutomaticallyEarnings() {
        val receipt = row(14).copy(category = "UNCATEGORIZED")
        assertEquals(IncomeRecognitionStatus.UNRESOLVED,
            IncomeRecognitionPolicy.classify(receipt, confirmedOwned).status)
    }

    @Test fun aReceiptWithNoConfirmedReceivingAccountCannotBecomeIncome() {
        val receipt = row(15).copy(accountId = null)
        assertFalse(IncomeRecognitionPolicy.isConfirmedIncome(receipt, confirmedOwned))
    }

    @Test fun explicitlyConfirmedIncomeWorksWithoutSmsOrBankBalance() {
        val manualSalary = row(5).copy(type = "OTHER", accountId = null,
            sourceKind = "manual_setup", reviewState = "USER_CLASSIFIED",
            transactionCode = null, counterparty = null)
        assertTrue(IncomeRecognitionPolicy.isConfirmedIncome(manualSalary, confirmedOwned))
    }

    @Test fun refundLoanAndInvestmentSalePrincipalAreNeverEarnings() {
        val refund = row(6).copy(relationType = "REFUND",
            reviewState = "USER_CLASSIFIED")
        val loan = row(7).copy(category = "DEBT_LOAN",
            reviewState = "USER_CLASSIFIED")
        val soldOwnedAsset = row(8).copy(relationType = "INVESTMENT_SELL",
            reviewState = "USER_CLASSIFIED")
        for (item in listOf(refund, loan, soldOwnedAsset)) {
            assertEquals(IncomeRecognitionStatus.NOT_INCOME,
                IncomeRecognitionPolicy.classify(item, confirmedOwned).status)
        }
    }

    @Test fun pendingExcludedAndSourceOnlyCreditsNeverBecomeConfirmed() {
        val pending = row(9).copy(reviewState = "NEEDS_REVIEW")
        val excluded = row(10).copy(reviewState = "EXCLUDED")
        val statementOnly = row(11).copy(sourceKind = "statement",
            counterparty = "Fictional Payroll", transactionCode = "FAKEPAY001")
        assertFalse(IncomeRecognitionPolicy.isConfirmedIncome(pending, confirmedOwned))
        assertFalse(IncomeRecognitionPolicy.isConfirmedIncome(excluded, confirmedOwned))
        assertFalse(IncomeRecognitionPolicy.isConfirmedIncome(statementOnly, confirmedOwned))
    }

    @Test fun lateExternalEvidenceChangesProjectionWithoutInsertingAnotherRow() {
        val credit = row(12).copy(counterparty = null, transactionCode = null)
        val laterSource = credit.copy(counterparty = "Fictional Employer",
            transactionCode = "FAKESAL001")
        assertEquals(IncomeRecognitionStatus.UNRESOLVED,
            IncomeRecognitionPolicy.classify(credit, confirmedOwned).status)
        assertTrue(IncomeRecognitionPolicy.isConfirmedIncome(laterSource, confirmedOwned))
        assertEquals(credit.id, laterSource.id)
    }

    @Test fun queryFactsAndActivityRowUseSameDecision() {
        val row = row(13).copy(type = "RECEIVED", counterparty = "Fictional Client")
        val fact = IncomeRecognitionEvidence(
            direction = row.direction, type = row.type, category = row.category,
            reviewState = row.reviewState, sourceKind = row.sourceKind,
            transactionCode = row.transactionCode, counterparty = row.counterparty,
            accountId = row.accountId, linkedAccountId = row.linkedAccountId,
            relationType = row.relationType, amountMinor = row.amountMinor)
        assertEquals(IncomeRecognitionPolicy.classify(row, confirmedOwned),
            IncomeRecognitionPolicy.classify(fact, confirmedOwned))
    }

    private fun row(id: Long) = StoredTransaction(
        id = id, transactionCode = "FAKE$id", fingerprint = "synthetic-$id",
        type = "RECEIVED", amountMinor = 7_000_00, feeMinor = 0,
        balanceMinor = null, counterparty = "Fictional Payer", counterpartyPhone = null,
        occurredAtText = null, occurredAtEpochMs = 1_700_000_000_000,
        receivedAtEpochMs = 1_700_000_000_000, sourcePackage = null,
        sourceKind = "historical_sms", direction = "INCOME", category = "INCOME",
        reviewState = "AUTO_CLASSIFIED", ruleId = null, planningMonth = null,
        reversalOfCode = null, accountId = "owned-wallet", linkedAccountId = null,
        payCycleId = null, cycleCandidateReason = null, cycleOverride = null)
}
