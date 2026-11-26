package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityAttentionKindsTest {
    @Test fun noNoticeWhenAllCreditsAndReviewsAreResolved() {
        assertEquals(emptyList<ActivityAttentionKind>(), activityAttentionKinds(0L, 0))
    }

    @Test fun eachNoticeAppearsOnlyWhenItsEvidenceRemains() {
        assertEquals(listOf(ActivityAttentionKind.UNRESOLVED_CREDITS),
            activityAttentionKinds(1_781_555L, 0))
        assertEquals(listOf(ActivityAttentionKind.NEEDS_REVIEW),
            activityAttentionKinds(0L, 2))
    }

    @Test fun bothNoticesShareTheSameSwipeableSlot() {
        assertEquals(listOf(ActivityAttentionKind.UNRESOLVED_CREDITS,
            ActivityAttentionKind.NEEDS_REVIEW), activityAttentionKinds(10_00L, 2))
    }
}
