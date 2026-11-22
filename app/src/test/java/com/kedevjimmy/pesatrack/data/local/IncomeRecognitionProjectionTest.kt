package com.kedevjimmy.pesatrack.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class IncomeRecognitionProjectionTest {
    private val confirmedOwned = setOf("owned-bank")
    @Test fun salaryAndFourOwnedTransferHopsCountIncomeOnce() {
        val salary = row(1, amount = 52_000_00)
        val transfers = (2L..5L).map {
            row(it, amount = 10_000_00).copy(direction = "TRANSFER",
                category = "TRANSFER")
        }
        assertEquals(52_000_00L,
            IncomeRecognitionProjection.confirmedIncomeMinor(listOf(salary) + transfers,
                confirmedOwned) { 0L })
    }

    @Test fun unresolvedCreditsDoNotPolluteConfirmedTotal() {
        val known = row(6, amount = 3_000_00)
        val unknown = row(7, amount = 5_000_00).copy(
            type = "DEPOSIT", category = "UNCATEGORIZED")
        val pending = row(8, amount = 2_000_00).copy(reviewState = "NEEDS_REVIEW")
        val excluded = row(9, amount = 7_000_00).copy(reviewState = "EXCLUDED")
        val rows = listOf(known, unknown, pending, excluded)

        assertEquals(3_000_00L,
            IncomeRecognitionProjection.confirmedIncomeMinor(rows, confirmedOwned) { 0L })
        assertEquals(7_000_00L,
            IncomeRecognitionProjection.unresolvedCreditsMinor(rows, emptySet(), confirmedOwned))
        assertEquals(5_000_00L,
            IncomeRecognitionProjection.unresolvedCreditsMinor(rows, setOf(8L), confirmedOwned))
    }

    @Test fun repaidReceivablePrincipalDoesNotCountAsEarnings() {
        val receipt = row(10, amount = 4_000_00)
        assertEquals(1_000_00L,
            IncomeRecognitionProjection.confirmedIncomeMinor(listOf(receipt), confirmedOwned) {
                if (it == 10L) 3_000_00L else 0L
            })
    }

    @Test(expected = ArithmeticException::class)
    fun totalsRejectOverflowInsteadOfShowingWrappedMoney() {
        IncomeRecognitionProjection.confirmedIncomeMinor(
            listOf(row(11, Long.MAX_VALUE), row(12, 1L)), confirmedOwned) { 0L }
    }

    private fun row(id: Long, amount: Long) = StoredTransaction(
        id = id, transactionCode = "FAKE$id", fingerprint = "fixture-$id",
        type = "RECEIVED", amountMinor = amount, feeMinor = 0L, balanceMinor = null,
        counterparty = "Fictional Employer", counterpartyPhone = null,
        occurredAtText = null, occurredAtEpochMs = 1_700_000_000_000L,
        receivedAtEpochMs = 1_700_000_000_000L, sourcePackage = null,
        sourceKind = "historical_sms", direction = "INCOME", category = "INCOME",
        reviewState = "AUTO_CLASSIFIED", ruleId = null, planningMonth = null,
        reversalOfCode = null, accountId = "owned-bank", linkedAccountId = null,
        payCycleId = null, cycleCandidateReason = null, cycleOverride = null)
}
