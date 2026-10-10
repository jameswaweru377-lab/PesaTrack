package com.kedevjimmy.pesatrack.domain.receivables
import org.junit.Assert.*
import org.junit.Test

class SharedBillPolicyTest {
    @Test fun restaurantShareLeavesOwnExpenseWithinTheOriginalBill() {
        assertTrue(SharedBillPolicy.validAmount(20000,12000))
        assertEquals(8000L,SharedBillPolicy.ownShare(20000,12000))
    }
    @Test fun fullBillZeroNegativeAndExcessAmountsCannotBecomeSharedDebt() {
        listOf(-1L,0L,20000L,30000L,Long.MAX_VALUE).forEach {
            assertFalse(SharedBillPolicy.validAmount(20000,it))
        }
        assertFalse(SharedBillPolicy.validAmount(0,1))
        assertFalse(SharedBillPolicy.validAmount(-20000,100))
    }
    @Test(expected = IllegalArgumentException::class)
    fun fullAmountCannotBePresentedAsHavingAnOwnShare() {
        SharedBillPolicy.ownShare(20000,20000)
    }
    @Test fun extremeValidAmountsDoNotOverflow() {
        assertEquals(Long.MAX_VALUE - 1,SharedBillPolicy.ownShare(Long.MAX_VALUE,1))
        assertEquals(1L,SharedBillPolicy.ownShare(Long.MAX_VALUE,Long.MAX_VALUE - 1))
    }
    @Test fun sharedIntentRequiresExplicitJournalProvenance() {
        assertTrue(SharedBillPolicy.isSharedEvent("shared:invented-uuid"))
        assertFalse(SharedBillPolicy.isSharedEvent("user:invented-uuid"))
        assertFalse(SharedBillPolicy.isSharedEvent("provider:shared:invented"))
    }
}
