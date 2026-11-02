package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.*
import org.junit.Test

class PhoneAlertPolicyTest {
    @Test fun everyPreviewOpensItsRelevantScreen() {
        assertEquals(8, PhoneAlertType.entries.size)
        assertEquals("bills", PhoneAlertType.BILL.route)
        assertEquals("salary", PhoneAlertType.SALARY.route)
        assertEquals("budgets", PhoneAlertType.BUDGET.route)
        assertEquals("activity-attention", PhoneAlertType.REVIEW.route)
        assertEquals("saving-goals", PhoneAlertType.GOAL.route)
        assertEquals("debts", PhoneAlertType.DEBT.route)
        assertEquals("data-health", PhoneAlertType.DATA.route)
        assertEquals("backup-reminder", PhoneAlertType.BACKUP.route)
    }

    @Test fun previewsHaveClearTestLabelsAndNoFinancialAmounts() {
        PhoneAlertType.entries.forEach { alert ->
            assertTrue(alert.previewTitle.startsWith("Test: "))
            assertTrue(alert.previewBody.isNotBlank())
            assertFalse(alert.previewBody.contains("KES"))
        }
    }

    @Test fun newHeadsUpChannelsCannotBeSilentlyReusedAsLegacyDefaultChannels() {
        val channels = PhoneAlertPolicy.highVisibilityChannels
        assertEquals(4, channels.size)
        assertFalse(channels.contains("bills"))
        assertFalse(channels.contains("planning"))
        assertFalse(channels.contains("salary_check"))
        assertEquals(channels.size, channels.toSet().size)
    }

    @Test fun testDelaysAreExplicitAndBounded() {
        assertEquals(setOf(0, 10, 20, 30), PhoneAlertPolicy.previewDelaysMinutes)
        assertNull(PhoneAlertPolicy.previewType("not-a-real-notice"))
        assertEquals(PhoneAlertType.REVIEW, PhoneAlertPolicy.previewType("REVIEW"))
    }
}
