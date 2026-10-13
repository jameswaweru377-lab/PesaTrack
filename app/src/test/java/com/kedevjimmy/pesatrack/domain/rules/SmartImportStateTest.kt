package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.*
import org.junit.Test

class SmartImportStateTest {
    @Test fun oneCompletedImportCannotArmAlertsWhileAnotherIsRunning() {
        val both = SmartImportState().begin().begin()
        assertEquals(2,both.active)
        val oneComplete = both.finish(true)
        assertFalse(oneComplete.ready)
        assertFalse(oneComplete.recover().ready)
        assertTrue(both.finish(true).finish(true).ready)
    }
    @Test fun anyFailedImportKeepsOverlappingBatchSuppressedRegardlessOfCompletionOrder() {
        val both = SmartImportState().begin().begin()
        assertFalse(both.finish(false).finish(true).ready)
        assertFalse(both.finish(true).finish(false).ready)
        assertTrue(both.finish(false).finish(true).begin().finish(true).ready)
    }
    @Test fun completionWithoutActiveOperationCannotArmAlerts() {
        assertFalse(SmartImportState().finish(true).ready)
    }
}
