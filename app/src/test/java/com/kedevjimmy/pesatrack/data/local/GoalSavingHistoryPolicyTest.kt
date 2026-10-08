package com.kedevjimmy.pesatrack.data.local

import com.kedevjimmy.pesatrack.domain.intelligence.SavingObservation
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.*
import org.junit.Test

class GoalSavingHistoryPolicyTest {
    private val types = mapOf("wallet" to AccountType.MOBILE_WALLET, "savings" to AccountType.SAVINGS,
        "otherSavings" to AccountType.SAVINGS)
    private fun deposit() = StoredTransaction(1, "TST9000001", "synthetic", "TRANSFER", 1000L, 0L,
        null, "SAMPLE", null, null, 1L, 1L, null, "TEST", "TRANSFER", "SAVINGS", "USER_CONFIRMED",
        "goal", null, null, "wallet", "savings", null, null, null)

    @Test fun transferCategoryWithdrawalReducesTheSameGoalsSavedAmount() {
        val rows = listOf(deposit(), deposit().copy(id = 2, amountMinor = 300L, category = "TRANSFER",
            accountId = "savings", linkedAccountId = "wallet"))
        val observations = GoalSavingHistoryPolicy.observations(rows, "goal", types)
        assertEquals(listOf(1000L, -300L), observations.map { it.deltaMinor })
        assertEquals(700L, GoalSavingHistoryPolicy.netSaved(observations))
        assertEquals(1000L, rows.first().amountMinor) // selector has no mutation authority
    }
    @Test fun unrelatedUnreviewedAndStructuralMovesDoNotInflateSavings() {
        val rows = listOf(deposit().copy(ruleId = "other"), deposit().copy(reviewState = "NEEDS_REVIEW"),
            deposit().copy(reviewState = "EXCLUDED"), deposit().copy(direction = "INCOME"),
            deposit().copy(type = "REVERSAL"), deposit().copy(cycleCandidateReason = "uncertain"),
            deposit().copy(accountId = "savings", linkedAccountId = "otherSavings"),
            deposit().copy(relationType = "REFUND"))
        assertEquals(0L, GoalSavingHistoryPolicy.netSaved(GoalSavingHistoryPolicy.observations(rows, "goal", types)))
    }
    @Test fun overflowIsUnavailableAndNetWithdrawalCannotBecomePositiveProgress() {
        assertNull(GoalSavingHistoryPolicy.netSaved(listOf(SavingObservation(1, Long.MAX_VALUE), SavingObservation(2, 1))))
        assertEquals(0L, GoalSavingHistoryPolicy.netSaved(listOf(SavingObservation(1, -500))))
        assertEquals(Long.MAX_VALUE, GoalSavingHistoryPolicy.netSaved(listOf(
            SavingObservation(1, Long.MAX_VALUE), SavingObservation(2, 1), SavingObservation(3, -1))))
    }
}
