package com.kedevjimmy.pesatrack.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerPresentationRefreshTest {
    @Test fun presentationRefreshReloadsScreensWithoutPretendingMoneyChanged() {
        val before = LedgerUpdateBus.state.value

        LedgerUpdateBus.requestPresentationRefresh()

        val after = LedgerUpdateBus.state.value
        assertEquals(before.revision + 1L, after.revision)
        assertEquals(before.dataRevision, after.dataRevision)
        assertEquals(before.processingCount, after.processingCount)
    }

    @Test fun financialChangesStillAdvanceBothRevisions() {
        val before = LedgerUpdateBus.state.value

        LedgerUpdateBus.notifyChanged()

        val after = LedgerUpdateBus.state.value
        assertEquals(before.revision + 1L, after.revision)
        assertEquals(before.dataRevision + 1L, after.dataRevision)
    }

    @Test fun finishedImportWithNoChangesDoesNotSignalMoneyChanged() {
        LedgerUpdateBus.processingStarted()
        val before = LedgerUpdateBus.state.value

        LedgerUpdateBus.processingFinished(changed = false)

        val after = LedgerUpdateBus.state.value
        assertEquals(before.revision, after.revision)
        assertEquals(before.dataRevision, after.dataRevision)
        assertEquals(before.processingCount - 1, after.processingCount)
    }
}
