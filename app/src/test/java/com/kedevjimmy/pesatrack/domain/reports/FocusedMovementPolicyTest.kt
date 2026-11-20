package com.kedevjimmy.pesatrack.domain.reports

import org.junit.Assert.*
import org.junit.Test

class FocusedMovementPolicyTest {
    @Test fun `full period ignores pagination and excludes end boundary`() {
        val rows = (1..250).map { FocusedMovement("d:$it", "Lender", "DRAWDOWN", 100, atEpochMs = 10) } +
            FocusedMovement("late", "Lender", "DRAWDOWN", 900, atEpochMs = 20)
        val result = FocusedMovementPolicy.total(rows, "DRAWDOWN", 10, 20)
        assertEquals(250, result.records.size)
        assertEquals(25_000L, result.amountMinor)
    }

    @Test fun `distinct events and loan principal fees remain separate`() {
        val rows = listOf(
            FocusedMovement("a", "Fuliza", "REPAYMENT", 1_000, 80, 11),
            FocusedMovement("a", "Fuliza", "REPAYMENT", 1_000, 80, 11),
            FocusedMovement("b", "Fuliza", "FEE", 80, atEpochMs = 11))
        assertEquals(1_000L, FocusedMovementPolicy.total(rows, "REPAYMENT", 0, 20).amountMinor)
        assertEquals(80L, FocusedMovementPolicy.total(rows, "FEE", 0, 20).amountMinor)
    }

    @Test fun `pending orders and sale proceeds are never investments`() {
        val rows = listOf(FocusedMovement("a", "Shares", "ORDER_SUBMITTED", 500, atEpochMs = 1),
            FocusedMovement("b", "Shares", "SELL_EXECUTED", 300, atEpochMs = 2),
            FocusedMovement("c", "Shares", "INVESTED", 200, atEpochMs = 3))
        assertEquals(200L, FocusedMovementPolicy.total(rows, "INVESTED", 0, 10).amountMinor)
    }

    @Test fun `missing amounts and overflow never become zero`() {
        val missing = FocusedMovementPolicy.total(listOf(FocusedMovement("a", "Shares", "INVESTED", null, atEpochMs = 1)), "INVESTED", 0, 2)
        assertNull(missing.amountMinor)
        assertEquals(1, missing.unknownCount)
        val overflow = listOf(FocusedMovement("a", "S", "SAVED", Long.MAX_VALUE, atEpochMs = 1),
            FocusedMovement("b", "S", "SAVED", 1, atEpochMs = 1))
        assertNull(FocusedMovementPolicy.total(overflow, "SAVED", 0, 2).amountMinor)
    }
}
