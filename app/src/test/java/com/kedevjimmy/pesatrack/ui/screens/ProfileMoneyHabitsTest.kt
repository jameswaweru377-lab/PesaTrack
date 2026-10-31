package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileMoneyHabitsTest {
    private fun signal(
        status: String,
        enough: Boolean,
        cash: Int? = null,
        spending: Int? = null
    ) = HomeFinancialSignals(
        score = if (enough) 80 else null,
        status = status,
        explanation = "Synthetic local evidence",
        indicators = listOf(
            HomeSignal("Cash flow", cash, "Observed income and outflow"),
            HomeSignal("Spending", spending, "Observed plan pace")
        ),
        dataSufficient = enough,
        confidence = HomeSignal("Data confidence", if (enough) 100 else 50, "Synthetic"),
        actions = listOf(HomeGuidanceAction("Review my plan", "Inspect monthly plan", "plan"))
    )

    @Test fun missingSnapshotNeverShowsHealthyOrAtRisk() {
        val presentation = presentProfileMoneyHabits(null)
        assertEquals(ProfileMoneyHabitsKind.NEEDS_INFORMATION, presentation.kind)
        assertEquals("Not enough information yet", presentation.label)
    }

    @Test fun completeOnTrackSnapshotIsPositive() {
        val presentation = presentProfileMoneyHabits(signal("On track", true, 90, 90))
        assertEquals(ProfileMoneyHabitsKind.LOOKING_GOOD, presentation.kind)
        assertEquals("Looking good", presentation.label)
    }

    @Test fun observedWeakSpendingIsAttentionNotInventedOverallHealth() {
        val presentation = presentProfileMoneyHabits(signal("Needs attention", false, 30, 20))
        assertEquals(ProfileMoneyHabitsKind.AT_RISK, presentation.kind)
        assertTrue(presentation.subtitle.contains("needs attention"))
        assertTrue(presentation.isProvisional)
    }

    @Test fun oneObservedWarningCanBeFlaggedWithoutFullPicture() {
        val presentation = presentProfileMoneyHabits(signal("Worth reviewing", false, 42))
        assertEquals(ProfileMoneyHabitsKind.AT_RISK, presentation.kind)
        assertTrue(presentation.isProvisional)
    }

    @Test fun partialButUncertainEvidenceStaysUnrated() {
        val presentation = presentProfileMoneyHabits(signal("Partial picture", false))
        assertEquals(ProfileMoneyHabitsKind.NEEDS_INFORMATION, presentation.kind)
    }
}
