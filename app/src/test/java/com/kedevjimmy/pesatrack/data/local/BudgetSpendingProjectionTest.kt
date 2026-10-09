package com.kedevjimmy.pesatrack.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetSpendingProjectionTest {
    @Test fun expensesFeesAndRefundsUseOriginalCanonicalCategory() {
        val original = row(1, 100_000, 2_000, "EXPENSE", "OLD_TRANSPORT")
        val refund = row(2, 20_000, 0, "TRANSFER", "OTHER").copy(
            relationType = TransactionStore.RELATION_REFUND, relatedTransactionId = 1
        )
        assertEquals(mapOf("TRANSPORT" to 82_000L), BudgetSpendingProjection.calculate(
            listOf(original, refund), { original }, { if (it == "OLD_TRANSPORT") "TRANSPORT" else it }
        ))
    }

    @Test fun incomeAndTransfersOnlyContributeTheirFees() {
        assertEquals(mapOf("TRANSPORT" to 500L), BudgetSpendingProjection.calculate(
            listOf(row(1, 100_000, 0, "INCOME"), row(2, 100_000, 500, "TRANSFER")),
            { null }, { it }
        ))
    }

    @Test fun refundWithIneligibleOriginalDoesNotReduceSpending() {
        val refund = row(2, 20_000, 0, "TRANSFER").copy(
            relationType = TransactionStore.RELATION_REFUND, relatedTransactionId = 1
        )
        assertEquals(mapOf("TRANSPORT" to 100_000L), BudgetSpendingProjection.calculate(
            listOf(row(3, 100_000, 0, "EXPENSE"), refund), { null }, { it }
        ))
    }

    @Test fun crossMonthRefundCanUseEligibleOriginalOutsideVisibleRows() {
        val original = row(1, 100_000, 0, "EXPENSE")
        val refund = row(2, 20_000, 0, "TRANSFER").copy(
            relationType = TransactionStore.RELATION_REFUND, relatedTransactionId = 1
        )
        assertEquals(mapOf("TRANSPORT" to 30_000L), BudgetSpendingProjection.calculate(
            listOf(row(3, 50_000, 0, "EXPENSE"), refund), { original }, { it }
        ))
    }

    @Test(expected = ArithmeticException::class)
    fun overflowingSpendingFailsInsteadOfDisplayingWrappedMoney() {
        BudgetSpendingProjection.calculate(
            listOf(row(1, Long.MAX_VALUE, 1, "EXPENSE")), { null }, { it }
        )
    }

    private fun row(id: Long, amount: Long, fee: Long, direction: String, category: String = "TRANSPORT") =
        StoredTransaction(
            id = id, transactionCode = "INVENTED$id", fingerprint = "invented$id", type = "SENT_MONEY",
            amountMinor = amount, feeMinor = fee, balanceMinor = null, counterparty = "Invented merchant",
            counterpartyPhone = null, occurredAtText = null, occurredAtEpochMs = 1L,
            receivedAtEpochMs = 1L, sourcePackage = null, sourceKind = "test", direction = direction,
            category = category, reviewState = "USER_CLASSIFIED", ruleId = null, planningMonth = null,
            reversalOfCode = null, accountId = null, linkedAccountId = null, payCycleId = null,
            cycleCandidateReason = null, cycleOverride = null
        )
}
