package com.kedevjimmy.pesatrack.domain.rules
import org.junit.Assert.*
import org.junit.Test
class BalancePresentationPolicyTest {
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
