package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.*
import org.junit.Test

class AutomaticHistoryRepairPolicyTest {
    private val day = 86_400_000L
    private val now = 200L * day

    @Test fun noPreviousImportDoesNotAuthorizeAnInboxScan() {
        assertNull(AutomaticHistoryRepairPolicy.startEpochMs(null, null, now))
    }
    @Test fun selectedHistoryWinsOverOlderCapturedRecords() {
        assertEquals(190L * day, AutomaticHistoryRepairPolicy.startEpochMs(190L * day, 110L * day, now))
    }
    @Test fun olderInstallOnlyReprocessesItsCapturedSmsRange() {
        assertEquals(180L * day, AutomaticHistoryRepairPolicy.startEpochMs(null, 180L * day, now))
    }
    @Test fun workIsBoundedAndInvalidDatesDoNotBecomeFullInboxScans() {
        assertEquals(100L * day, AutomaticHistoryRepairPolicy.startEpochMs(day, null, now))
        assertNull(AutomaticHistoryRepairPolicy.startEpochMs(0L, null, now))
        assertNull(AutomaticHistoryRepairPolicy.startEpochMs(now + day, null, now))
    }
}
