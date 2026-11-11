package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.ActivityReviewOverview
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewItem
import com.kedevjimmy.pesatrack.domain.identity.OwnershipReviewEvidence
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnostic
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticCode
import kotlinx.coroutines.CancellationException

/** Review queue is essential; unrelated supporting readers must not make it disappear. */
internal suspend fun loadActivitySupplementalSafely(
    reviewOverview: suspend () -> ActivityReviewOverview,
    ownershipEvidence: suspend () -> Map<String, OwnershipReviewEvidence>,
    relationships: suspend () -> List<RelationshipReviewItem>,
    financialEntities: suspend () -> List<DiscoveredFinancialEntity>,
    diagnostics: suspend () -> List<FinancialIngestionDiagnostic>,
    identityCount: suspend () -> Int,
    onOptionalFailure: (String) -> Unit
): ActivitySupplementalData {
    suspend fun <T> optional(source: String, fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            onOptionalFailure(source)
            fallback
        }

    // Never turn a broken authoritative review query into a fake empty review list.
    val overview = reviewOverview()
    val evidence = optional("ownership evidence", emptyMap()) { ownershipEvidence() }
    val relationshipItems = optional("relationship reviews", emptyList()) { relationships() }
    val entityItems = optional("financial entities", emptyList()) { financialEntities() }
    val diagnosticItems = optional("financial diagnostics", emptyList()) { diagnostics() }
    val pendingIdentityCount = optional("identity reviews", 0) { identityCount() }

    return ActivitySupplementalData(
        globalOverview = overview,
        ownershipEvidence = evidence,
        relationshipReviews = relationshipItems,
        pendingFinancialEntities = entityItems
            .filter { evidence[it.entityKey]?.canAsk == true }
            .distinctBy { it.entityKey },
        financialDiagnostics = diagnosticItems
            .filter { it.code != FinancialIngestionDiagnosticCode.IGNORED_NON_FINANCIAL },
        identityReviewCount = pendingIdentityCount
    )
}
