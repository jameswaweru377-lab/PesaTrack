package com.kedevjimmy.pesatrack.domain.rules
import org.junit.Assert.*
import org.junit.Test
class PlanningReminderPolicyTest {
    @Test fun budgetThresholdAndOverLimitRemainDistinct() {
        assertNull(PlanningReminderPolicy.budgetLevel(18999, 20000))
        assertEquals("near", PlanningReminderPolicy.budgetLevel(19000, 20000))
        assertEquals("over", PlanningReminderPolicy.budgetLevel(20000, 20000))
        assertNull(PlanningReminderPolicy.budgetLevel(100, 0))
        assertEquals("over", PlanningReminderPolicy.budgetLevel(Long.MAX_VALUE, Long.MAX_VALUE))
    }
    @Test fun goalsNeedEvidenceAnElapsedScheduleAndAnUnfinishedTarget() {
        assertFalse(PlanningReminderPolicy.goalDue(99,100, 10,100))
        assertTrue(PlanningReminderPolicy.goalDue(100,100, 10,100))
        assertFalse(PlanningReminderPolicy.goalDue(100,100, 100,100))
        assertFalse(PlanningReminderPolicy.goalDue(100,100, null,100))
        assertFalse(PlanningReminderPolicy.goalDue(100,100, 0,0))
    }
    @Test fun spendingShareIsRoundedAgainstFullPeriodTotal() {
        assertEquals("40%", PlanningReminderPolicy.share(4000,10000))
        assertEquals("0%", PlanningReminderPolicy.share(4000,0))
        assertEquals("33%", PlanningReminderPolicy.share(1,3))
    }
}
