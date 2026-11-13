package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanningNotificationCopyTest {
    @Test fun nearAndReachedUseExistingBudgetName() {
        assertEquals("Food budget almost full", PlanningNotificationCopy.budgetHeadline("Food", "near"))
        assertEquals("Transport budget limit reached", PlanningNotificationCopy.budgetHeadline("Transport budget", "over"))
    }

    @Test fun amountsAreDerivedFromMinorUnitsWithoutRoundingAwayCents() {
        assertEquals("KES 9,500 of KES 10,000 used", PlanningNotificationCopy.budgetUsage(950_000, 1_000_000))
        assertEquals("KES 95.25 of KES 100 used", PlanningNotificationCopy.budgetUsage(9_525, 10_000))
    }
}
