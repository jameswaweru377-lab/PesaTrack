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
        assertFalse(value.needsReview)
    }

    @Test fun `unknown liquid account prevents misleading spendable total`() {
        val value = PlanSpendableBalancePolicy.calculate(
            listOf(
                PlanSpendableAccount("equity", null, isProtected = false),
                PlanSpendableAccount("mpesa", 500_000L, isProtected = false)
            ),
            upcomingBillsMinor = 50_000L
        )
        assertNull(value.spendableMinor)
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
