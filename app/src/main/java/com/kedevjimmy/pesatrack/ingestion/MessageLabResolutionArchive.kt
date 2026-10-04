package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseProvenance

enum class MessageLabResolutionKind(
    val displayLabel: String
) {
    PARSER_COVERAGE(
        "Resolved by current parser"
    ),
    NO_LONGER_UNSUPPORTED(
        "No longer unsupported"
    )
}

data class MessageLabResolutionContext(
    val kind:
        MessageLabResolutionKind,
    val parseRoute: String? = null,
    val templateId: String? = null,
    val templateRevision: Int? = null,
    val eventKind: String? = null,
    val productIdentity: String? = null,
    val provenanceReason: String? = null
)

data class ResolvedMessageLabShape(
    val shapeId: String,
    val familyId: String,
    val sender: String,
    val sourcePackage: String?,
    val sourceKind: String?,
    val redactedBody: String,
    val occurrenceCount: Int,
    val familyOccurrenceCount: Int,
    val familyVariantCount: Int,
    val previousUserDecision:
        MessageLabUserDecision?,
    val semanticLabel:
        MessageLabSemanticLabel?,
    val implementationReview:
        MessageLabImplementationReviewState?,
    val resolutionKind:
        MessageLabResolutionKind,
    val parseRoute: String?,
    val templateId: String?,
    val templateRevision: Int?,
    val eventKind: String?,
    val productIdentity: String?,
    val provenanceReason: String?,
    val resolvedAtEpochMs: Long
)

object MessageLabResolutionPolicy {
    fun contextFor(
        recognized: Boolean,
        semanticallyUnderstood: Boolean,
        provenance:
            MessageParseProvenance
    ): MessageLabResolutionContext {
        val parserResolved =
            recognized ||
                semanticallyUnderstood

        return if (parserResolved) {
            MessageLabResolutionContext(
                kind =
                    MessageLabResolutionKind
                        .PARSER_COVERAGE,
                parseRoute =
                    provenance.route.name,
                templateId =
                    provenance.templateId,
                templateRevision =
                    provenance.templateRevision,
                eventKind =
                    provenance.eventKind,
                productIdentity =
                    provenance.productIdentity,
                provenanceReason =
                    provenance.reason
            )
        } else {
            MessageLabResolutionContext(
                kind =
                    MessageLabResolutionKind
                        .NO_LONGER_UNSUPPORTED,
                provenanceReason =
                    "The current relevance policy no longer keeps this shape as unsupported financial evidence."
            )
        }
    }

    fun resolvedRecord(
        shape:
            UnsupportedMessageShape,
        context:
            MessageLabResolutionContext,
        resolvedAtEpochMs: Long
    ): ResolvedMessageLabShape =
        ResolvedMessageLabShape(
            shapeId =
                shape.shapeId,
            familyId =
                shape.familyId,
            sender =
                shape.sender,
            sourcePackage =
                shape.sourcePackage,
            sourceKind =
                shape.sourceKind,
            redactedBody =
                shape.redactedBody,
            occurrenceCount =
                shape.occurrenceCount,
            familyOccurrenceCount =
                maxOf(
                    shape.occurrenceCount,
                    shape.familyOccurrenceCount
                ),
            familyVariantCount =
                shape.familyVariantCount,
            previousUserDecision =
                shape.userDecision,
            semanticLabel =
                shape.semanticLabel,
            implementationReview =
                shape.implementationReview,
            resolutionKind =
                context.kind,
            parseRoute =
                context.parseRoute,
            templateId =
                context.templateId,
            templateRevision =
                context.templateRevision,
            eventKind =
                context.eventKind,
            productIdentity =
                context.productIdentity,
            provenanceReason =
                context.provenanceReason,
            resolvedAtEpochMs =
                resolvedAtEpochMs
        )
}
