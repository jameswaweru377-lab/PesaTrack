package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingOwnershipReplayPolicyTest {
    private val cleanImport = HistoricalImportResult(
        scanned = 10, matched = 8, inserted = 8, duplicates = 0, ignored = 2
    )

    @Test fun unownedBankReceiptsAndDeferredPaymentsRequireReplayAfterOwnership() {
        // Unowned receipts are inserted without an account; deferred debits are
        // not inserted. Both are counted in pendingReview by the coordinator.
        assertTrue(shouldReplaySmsAfterOwnership(
            cleanImport.copy(inserted = 1, pendingReview = 1), 1, true, 100L
        ))
        assertTrue(shouldReplaySmsAfterOwnership(
            cleanImport.copy(inserted = 0, pendingReview = 1), 1, true, 100L
        ))
    }

    @Test fun verifiedEventsDoNotCauseAnUnnecessarySecondScan() {
        assertFalse(shouldReplaySmsAfterOwnership(cleanImport, 1, true, 100L))
        // After process recreation the result is lost while the REVIEW stage remains.
        assertTrue(shouldReplaySmsAfterOwnership(null, 1, true, 100L))
        // A partial scan cannot prove it saw all outstanding bank movements.
        assertTrue(shouldReplaySmsAfterOwnership(cleanImport.copy(cancelled = true), 1, true, 100L))
        assertFalse(shouldReplaySmsAfterOwnership(cleanImport.copy(pendingReview = 1), 0, true, 100L))
        // Revoking permission cannot erase already-observed unowned bank debits.
        assertTrue(shouldReplaySmsAfterOwnership(cleanImport.copy(pendingReview = 1), 1, false, 100L))
        assertTrue(shouldReplaySmsAfterOwnership(cleanImport.copy(pendingReview = 1), 1, true, null))
        assertFalse(shouldReplaySmsAfterOwnership(null, 1, false, 100L))
    }

    @Test fun partialOrFailedOwnershipReplayCannotBeReportedAsComplete() {
        assertFalse(ownershipReplayCompleted(null))
        assertFalse(ownershipReplayCompleted(cleanImport.copy(cancelled = true)))
        assertTrue(ownershipReplayCompleted(cleanImport))
    }

    @Test fun interruptedReplayIsRequiredEvenAfterPermissionChanges() {
        assertTrue(ownershipReplayRequired(true, cleanImport, 1, false, 100L))
        assertTrue(ownershipReplayRequired(true, null, 1, true, 100L))
        assertFalse(ownershipReplayRequired(false, cleanImport, 1, true, 100L))
        // An initial pending scan may have been lost on process recreation.
        assertTrue(ownershipReplayRequired(false, null, 1, false, 100L,
            priorScanUnverified = true))
        assertFalse(ownershipReplayRequired(false, null, 1, false, 100L,
            priorScanUnverified = false))
    }
}
