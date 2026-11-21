package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.FinancialSmsSenderApprovalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FinancialSmsApprovalCheckpointTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("pesatrack_financial_sms_senders", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("historical_import_checkpoint", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun newSenderApprovalInvalidatesEarlierPartialScan() {
        val approvals = FinancialSmsSenderApprovalStore(context)
        val checkpoints = HistoricalImportCheckpointStore(context)
        val originalRevision = approvals.revision()
        checkpoints.save("recent", 100, 1000, 9, nowEpochMs = 1200,
            approvalRevision = originalRevision)
        assertEquals(9L, checkpoints.load("recent", nowEpochMs = 1300,
            expectedSinceEpochMs = 100, expectedApprovalRevision = originalRevision)?.lastSmsId)

        assertTrue(approvals.approve("ZENFIN"))
        assertTrue(approvals.revision() > originalRevision)
        assertNull(checkpoints.load("recent", nowEpochMs = 1300,
            expectedSinceEpochMs = 100, expectedApprovalRevision = approvals.revision()))
    }

    @Test fun previousScanCannotWriteAStaleCheckpointAfterPolicyChanges() {
        val approvals = FinancialSmsSenderApprovalStore(context)
        val checkpoints = HistoricalImportCheckpointStore(context)
        val staleRevision = approvals.revision()
        assertTrue(approvals.approve("ZENFIN"))
        checkpoints.save("recent", 100, 1000, 9, nowEpochMs = 1200,
            approvalRevision = staleRevision)
        assertNull(checkpoints.load("recent", nowEpochMs = 1300,
            expectedSinceEpochMs = 100, expectedApprovalRevision = approvals.revision()))
    }

    @Test fun approvalCanBeRevokedButBuiltInSendersCannotBeRemoved() {
        val approvals = FinancialSmsSenderApprovalStore(context)
        assertFalse(approvals.approve("EQUITY"))
        assertTrue(approvals.approve("ZENFIN"))
        assertTrue("ZENFIN" in approvals.listApproved())
        assertTrue(approvals.remove("ZENFIN"))
        assertFalse("ZENFIN" in approvals.listApproved())
        assertTrue(approvals.listApproved().isEmpty())
    }
}
