package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanSpendableBalancePolicyTest {
    @Test fun `liquid account balances exclude protected savings and unpaid bills`() {
        val value = PlanSpendableBalancePolicy.calculate(
            listOf(
                PlanSpendableAccount("equity", 3_000_000L, isProtected = false),
                PlanSpendableAccount("mpesa", 500_000L, isProtected = false),
                PlanSpendableAccount("locked", 1_200_000L, isProtected = true)
            ),
            upcomingBillsMinor = 200_000L
        )
        assertEquals(3_300_000L, value.spendableMinor)
        assertEquals(3_500_000L, value.liquidMinor)
        assertEquals(1_200_000L, value.protectedMinor)
        assertEquals(4_700_000L, value.totalAccountsMinor)
        assertFalse(value.needsReview)
    }

    @Test fun `known spendable balance survives an unknown account`() {
        val value = PlanSpendableBalancePolicy.calculate(
            listOf(
                PlanSpendableAccount("equity", null, isProtected = false),
                PlanSpendableAccount("mpesa", 500_000L, isProtected = false)
            ),
            upcomingBillsMinor = 50_000L
        )
        assertEquals(450_000L, value.spendableMinor)
        assertNull(value.totalAccountsMinor)
        assertTrue(value.needsReview)
    }

    @Test fun `stale evidence does not hide a fresh recorded zero`() {
        val accounts = listOf(
            PlanSpendableAccount("stale", 500_000L, isProtected = false, provisional = true),
            PlanSpendableAccount("wallet", 0L, isProtected = false)
        )
        assertEquals(0L, PlanSpendableBalancePolicy.calculate(accounts, 0L).spendableMinor)
        assertNull(PlanSpendableBalancePolicy.calculate(accounts.take(1), 0L).spendableMinor)
        assertNull(PlanSpendableBalancePolicy.calculate(accounts, null).spendableMinor)
    }

    @Test fun `uncertain protected savings stay unavailable without reducing spendable cash`() {
        val filteredSavings = BalancePresentationPolicy.savingsBalance(-2_995L, true, true)
        val value = PlanSpendableBalancePolicy.calculate(
            listOf(
                PlanSpendableAccount("mpesa", 120_000L, isProtected = false),
                PlanSpendableAccount("locked", filteredSavings, isProtected = true)
            ),
            upcomingBillsMinor = 20_000L
        )
        assertNull(value.protectedMinor)
        assertNull(value.totalAccountsMinor)
        assertEquals(100_000L, value.spendableMinor)
        assertTrue(value.needsReview)
    }

    @Test fun `unpaid bills cannot turn spendable into negative bank money`() {
        val value = PlanSpendableBalancePolicy.calculate(
            listOf(PlanSpendableAccount("wallet", 20_000L, isProtected = false)),
            upcomingBillsMinor = 30_000L
        )
        assertEquals(0L, value.spendableMinor)
        assertTrue(value.needsReview)
    }

    @Test fun `duplicated account evidence must never double count`() {
        val same = PlanSpendableAccount("wallet", 20_000L, isProtected = false)
        val value = PlanSpendableBalancePolicy.calculate(listOf(same, same), 0L)
        assertEquals(20_000L, value.spendableMinor)
    }

    @Test fun `provisional account evidence cannot be called currently spendable money`() {
        val value = PlanSpendableBalancePolicy.calculate(
            listOf(PlanSpendableAccount("wallet", 150_000L,
                isProtected = false, provisional = true)), 20_000L
        )
        assertNull(value.spendableMinor)
        assertEquals(150_000L, value.liquidMinor)
        assertTrue(value.needsReview)
    }

    @Test fun `unknown or overflowing source is unavailable rather than fabricated`() {
        assertNull(PlanSpendableBalancePolicy.calculate(emptyList(), 0L).spendableMinor)
        val value = PlanSpendableBalancePolicy.calculate(
            listOf(PlanSpendableAccount("one", Long.MAX_VALUE, false),
                PlanSpendableAccount("two", 1L, false)), 0L
        )
        assertNull(value.spendableMinor)
        assertTrue(value.needsReview)
    }
}
