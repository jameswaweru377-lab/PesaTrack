package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.*
import org.junit.Test

class DebtReceiptCorrelationPolicyTest {
    private val at = 1_780_000_000_000L
    private val row = DebtReceiptCorrelationPolicy.Candidate(
        transactionId = 41L, transactionCode = "QAZ1234567",
        accountId = "owned-mpesa", amountMinor = 12_345L,
        occurredAtEpochMs = at, direction = "EXPENSE",
        category = "OTHER", reviewState = "NEEDS_REVIEW",
        ruleId = null, relationType = null, reversalCode = null,
        balanceAfterMinor = 987_654L
    )

    private fun select(vararg entries: DebtReceiptCorrelationPolicy.Candidate) =
        DebtReceiptCorrelationPolicy.choose(
            receipt = "QAZ1234567", accountId = "owned-mpesa",
            amountMinor = 12_345L, occurredAtEpochMs = at,
            walletBalanceAfterMinor = 987_654L, candidates = entries.toList()
        )

    @Test fun acceptsOnlyUniqueUnreviewedSameReceiptDebit() {
        assertEquals(41L, select(row)?.transactionId)
        assertNull(select())
        assertNull(select(row, row.copy(transactionId = 42L)))
    }

    @Test fun rejectsAmountAccountTimeAndBalanceConflicts() {
        assertNull(select(row.copy(accountId = "other-bank")))
        assertNull(select(row.copy(amountMinor = 10_801L)))
        assertNull(select(row.copy(occurredAtEpochMs = at - 16L * 60_000)))
        assertNull(select(row.copy(balanceAfterMinor = 12L)))
        assertNull(select(row.copy(transactionCode = "OTHER12345")))
    }

    @Test fun preservesUserChoicesReversalsAndExistingRelations() {
        assertNull(select(row.copy(reviewState = "USER_CLASSIFIED")))
        assertNull(select(row.copy(reviewState = "EXCLUDED")))
        assertNull(select(row.copy(category = "EATING_OUT")))
        assertNull(select(row.copy(relationType = "REFUND")))
        assertNull(select(row.copy(reversalCode = "QAB1234567")))
        assertNull(select(row.copy(ruleId = "user_rule")))
        assertNull(select(row.copy(direction = "INCOME")))
    }
}
