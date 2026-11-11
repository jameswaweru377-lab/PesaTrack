package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.ActivityReviewOverview
import com.kedevjimmy.pesatrack.data.local.ReviewGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivitySupplementalReaderTest {
    private val pendingReviews = ActivityReviewOverview(
        reviewGroups = listOf(
            ReviewGroup("sample", "Sample shop", 9L, 25, 12_000L, 1_793_000_000_000L)
        ),
        possibleDuplicates = emptyList()
    )

    @Test fun optionalStoreFailureDoesNotHideRecordedReviewQueue() = runBlocking {
        val failures = mutableListOf<String>()
        val loaded = loadActivitySupplementalSafely(
            reviewOverview = { pendingReviews },
            ownershipEvidence = { error("optional ownership read failed") },
            relationships = { emptyList() },
            financialEntities = { emptyList() },
            diagnostics = { emptyList() },
            identityCount = { 0 },
            onOptionalFailure = { source -> failures += source }
        )

        assertEquals(25, loaded.globalOverview.reviewGroups.single().transactionCount)
        assertEquals(listOf("ownership evidence"), failures)
    }

    @Test fun brokenReviewSourceIsReportedInsteadOfClaimingQueueIsEmpty() = runBlocking {
        var failureThrown = false
        try {
            loadActivitySupplementalSafely(
                reviewOverview = { error("main review query failed") },
                ownershipEvidence = { emptyMap() },
                relationships = { emptyList() },
                financialEntities = { emptyList() },
                diagnostics = { emptyList() },
                identityCount = { 0 },
                onOptionalFailure = {}
            )
        } catch (failure: IllegalStateException) {
            failureThrown = failure.message == "main review query failed"
        }
        assertTrue(failureThrown)
    }

    @Test fun cancellationIsNeverSilencedByOptionalReadFallback() = runBlocking {
        var cancelled = false
        try {
            loadActivitySupplementalSafely(
                reviewOverview = { pendingReviews },
                ownershipEvidence = { throw CancellationException("leaving Activity") },
                relationships = { emptyList() },
                financialEntities = { emptyList() },
                diagnostics = { emptyList() },
                identityCount = { 0 },
                onOptionalFailure = {}
            )
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }
}
