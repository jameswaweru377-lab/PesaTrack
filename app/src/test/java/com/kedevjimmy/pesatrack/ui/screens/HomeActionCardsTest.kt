package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeActionCardsTest {
    @Test fun `review card never treats unknown counts as caught up`() {
        assertEquals("Review status unavailable", homeQuickReviewTitle(null))
        assertEquals("You're all caught up", homeQuickReviewTitle(0))
        assertEquals("2 items need review", homeQuickReviewTitle(2))
    }

    @Test fun `insight card uses verified guidance instead of invented numbers`() {
        val guidance = HomeGuidanceAction("Review financial activity", "Check unclear transactions.", "activity")
        assertEquals("Review financial activity", homeInsightCardText(listOf(guidance), 4).first)
        assertEquals("Check unclear transactions.", homeInsightCardText(listOf(guidance), 4).second)
        assertEquals("Insights need more records", homeInsightCardText(emptyList(), 0).first)
        assertEquals("No financial guidance needs attention", homeInsightCardText(emptyList(), 4).first)
    }
}
