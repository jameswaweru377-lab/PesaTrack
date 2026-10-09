package com.kedevjimmy.pesatrack.domain.rules
import org.junit.Assert.*
import org.junit.Test
class BalancePresentationPolicyTest {
    @Test fun unknownTotalsStayUnknownAndOverflowDoesNotBecomeMoney() {
        assertNull(BalancePresentationPolicy.totalKnown(emptyList()))
        assertNull(BalancePresentationPolicy.totalKnown(listOf(null, null)))
        assertEquals(0L, BalancePresentationPolicy.totalKnown(listOf(null, 0L)))
        assertEquals(300L, BalancePresentationPolicy.totalKnown(listOf(400L, null, -100L)))
        assertNull(BalancePresentationPolicy.totalKnown(listOf(Long.MAX_VALUE, 1L)))
    }

    @Test fun incompleteNegativeSavingsEstimateIsNotPresentedAsAvailableMoney() {
        assertNull(BalancePresentationPolicy.savingsBalance(-2995,true,true))
        assertEquals(38L,BalancePresentationPolicy.savingsBalance(38,true,true))
        assertEquals(0L,BalancePresentationPolicy.savingsBalance(0,true,true))
    }
    @Test fun observedOverdraftAndUnknownArePreserved() {
        assertEquals(-2995L,BalancePresentationPolicy.savingsBalance(-2995,false,true))
        assertEquals(-2995L,BalancePresentationPolicy.savingsBalance(-2995,true,false))
        assertNull(BalancePresentationPolicy.savingsBalance(null,true,true))
    }
}
