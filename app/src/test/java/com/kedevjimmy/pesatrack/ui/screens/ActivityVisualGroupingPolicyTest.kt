package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.*
import org.junit.Test

class ActivityVisualGroupingPolicyTest {
    @Test fun ringRemainsVisibleWhenExpensesExceedIncome() {
        assertEquals(1f, netFlowRingProgress(10_000L, 4_925_972L)!!, 0f)
        assertEquals(1f, netFlowRingProgress(0L, 100L)!!, 0f)
        assertEquals(.4f, netFlowRingProgress(1_000L, 400L)!!, .0001f)
        assertEquals(0f, netFlowRingProgress(1_000L, 0L)!!, 0f)
        assertNull(netFlowRingProgress(0L, 0L))
        assertNull(netFlowRingProgress(-1L, 1L))
        assertNull(netFlowRingProgress(1L, -1L))
    }

    @Test fun firstAndLastTransactionAreCorrectlyGrouped() {
        assertEquals(ActivityGroupPosition.ONLY, activityGroupPosition(0, 1))
        assertEquals(ActivityGroupPosition.FIRST, activityGroupPosition(0, 3))
        assertEquals(ActivityGroupPosition.MIDDLE, activityGroupPosition(1, 3))
        assertEquals(ActivityGroupPosition.LAST, activityGroupPosition(2, 3))
        assertEquals(ActivityGroupPosition.FIRST, activityGroupPosition(0, 2))
        assertEquals(ActivityGroupPosition.LAST, activityGroupPosition(1, 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidGroupIndexDoesNotSilentlyAssignAnEdge() {
        activityGroupPosition(3, 3)
    }
}
