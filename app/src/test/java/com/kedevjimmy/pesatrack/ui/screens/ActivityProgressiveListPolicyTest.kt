package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.*
import org.junit.Test

class ActivityProgressiveListPolicyTest {
    @Test fun firstBatchPreservesNewestRowsAndOrder() {
        val rows = (100 downTo 1).toList()
        val visible = activityVisibleRows(rows, ACTIVITY_FIRST_BATCH)
        assertEquals(24,visible.size)
        assertEquals((100 downTo 77).toList(),visible)
    }

    @Test fun addingPagesDoesNotDuplicateOrLoseRows() {
        val original=(0 until 73).toList()
        var count=ACTIVITY_FIRST_BATCH
        val sizes=mutableListOf<Int>()
        while(count<original.size) {
            count=activityVisibleCount(count,original.size)
            sizes+=count
        }
        assertEquals(listOf(48,72,73),sizes)
        assertEquals(original,activityVisibleRows(original,count))
        assertEquals(73,activityVisibleCount(73,73))
    }

    @Test fun shortListsNeverShowNonexistentData() {
        assertEquals(0,activityVisibleRows(emptyList<Int>(),24).size)
        assertEquals(5,activityVisibleRows((1..5).toList(),24).size)
        assertEquals(5,activityVisibleCount(24,5))
    }

    @Test fun changedSearchOrPeriodCanResetToFirstBatch() {
        val matches=(1..70).toList()
        val visible=activityVisibleRows(matches,ACTIVITY_FIRST_BATCH)
        assertEquals(24,visible.size)
        assertEquals(24,activityVisibleRows(matches.reversed(),ACTIVITY_FIRST_BATCH).size)
        assertEquals(70,matches.size)
    }

    @Test(expected=IllegalArgumentException::class)
    fun invalidBatchFailsFast() { activityVisibleCount(3,10,0) }
}
