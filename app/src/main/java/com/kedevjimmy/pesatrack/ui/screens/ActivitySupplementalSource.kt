package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import android.util.Log
import com.kedevjimmy.pesatrack.data.local.ActivityReviewOverview
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.IdentityGraphStore
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Home's prompt and Activity's queue use the same local-only review evidence. */
internal suspend fun readActivitySupplementalData(context: Context): ActivitySupplementalData =
    withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val store = TransactionStore.get(app)
        val financial = FinancialUnderstandingStore.get(app)
        val diagnostics = FinancialIngestionDiagnosticsStore.get(app)
        val identity = IdentityGraphStore.get(app)
        val relationships = RelationshipReviewStore.get(app)
        loadActivitySupplementalSafely(
            reviewOverview = { store.activityReviewOverview(performMaintenance = false) },
            ownershipEvidence = { financial.ownershipReviewEvidence() },
            relationships = { relationships.pendingReviews() },
            financialEntities = { financial.pendingEntities() },
            diagnostics = { diagnostics.recent(500) },
            identityCount = {
                identity.pendingReviewPrompts(limit = 100, includeOptional = false).size
            },
            onOptionalFailure = { source ->
                Log.w("PesaTrackActivity", "Optional Activity reader unavailable: $source")
            }
        )
    }

/** Match the count above the existing global Activity review queue exactly. */
internal fun activityPendingReviewCount(
    overview: ActivityReviewOverview,
    relationshipCount: Int,
    ownershipCount: Int,
    identityCount: Int
): Int = overview.reviewGroups.sumOf { it.transactionCount } +
    overview.possibleDuplicates.size + relationshipCount + ownershipCount + identityCount

internal fun activityPendingReviewCount(data: ActivitySupplementalData): Int =
    activityPendingReviewCount(
        data.globalOverview,
        data.relationshipReviews.size,
        data.pendingFinancialEntities.count {
            shouldOfferOwnershipReview(it, data.financialDiagnostics)
        },
        data.identityReviewCount
    )
