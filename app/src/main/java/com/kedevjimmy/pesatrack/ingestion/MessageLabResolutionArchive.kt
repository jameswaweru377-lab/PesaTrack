package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialEventKind
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpretation
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialSemanticIntent
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

enum class MessageLabResolutionAgreement(
    val displayLabel: String
) {
    AGREES(
        "Meaning agrees"
    ),
    DISAGREES(
        "Meaning conflict"
    ),
    UNKNOWN(
        "Meaning not comparable"
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
    val provenanceReason: String? = null,
    val resolvedSemanticLabels:
        Set<MessageLabSemanticLabel> =
        emptySet()
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
    val resolutionAgreement:
        MessageLabResolutionAgreement,
    val resolvedSemanticLabels:
        Set<MessageLabSemanticLabel>,
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
        interpretation:
            FinancialMessageInterpretation
    ): MessageLabResolutionContext =
        contextFor(
            recognized =
                interpretation.recognized,
            semanticallyUnderstood =
                interpretation
                    .semanticallyUnderstood,
            provenance =
                interpretation
                    .parseProvenance,
            resolvedSemanticLabels =
                labelsFor(
                    interpretation
                )
        )

    fun contextFor(
        recognized: Boolean,
        semanticallyUnderstood: Boolean,
        provenance:
            MessageParseProvenance,
        resolvedSemanticLabels:
            Set<MessageLabSemanticLabel> =
            emptySet()
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
                    provenance.reason,
                resolvedSemanticLabels =
                    resolvedSemanticLabels
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

    fun canRetire(
        shape:
            UnsupportedMessageShape,
        context:
            MessageLabResolutionContext
    ): Boolean =
        context.kind !=
            MessageLabResolutionKind
                .PARSER_COVERAGE ||
            agreementFor(
                previous =
                    shape.semanticLabel,
                current =
                    context
                        .resolvedSemanticLabels
            ) !=
            MessageLabResolutionAgreement
                .DISAGREES

    fun agreementFor(
        previous:
            MessageLabSemanticLabel?,
        current:
            Set<MessageLabSemanticLabel>
    ): MessageLabResolutionAgreement =
        when {
            previous == null ||
                previous ==
                MessageLabSemanticLabel
                    .OTHER_FINANCIAL ->
                MessageLabResolutionAgreement
                    .UNKNOWN

            current.isEmpty() ->
                MessageLabResolutionAgreement
                    .UNKNOWN

            previous in current ->
                MessageLabResolutionAgreement
                    .AGREES

            else ->
                MessageLabResolutionAgreement
                    .DISAGREES
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
            resolutionAgreement =
                agreementFor(
                    previous =
                        shape.semanticLabel,
                    current =
                        context
                            .resolvedSemanticLabels
                ),
            resolvedSemanticLabels =
                context
                    .resolvedSemanticLabels,
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

    private fun labelsFor(
        interpretation:
            FinancialMessageInterpretation
    ): Set<MessageLabSemanticLabel> =
        buildSet {
            interpretation.events
                .forEach {
                    event ->
                    eventLabel(
                        event.kind
                    )?.let(::add)
                }

            if (
                interpretation
                    .balances
                    .isNotEmpty()
            ) {
                add(
                    MessageLabSemanticLabel
                        .BALANCE_UPDATE
                )
            }

            interpretation
                .semanticAssessment
                ?.intents
                ?.forEach {
                    intent ->
                    semanticIntentLabel(
                        intent
                    )?.let(::add)
                }
        }

    private fun eventLabel(
        kind:
            FinancialEventKind
    ): MessageLabSemanticLabel? =
        when (kind) {
            FinancialEventKind.PAYMENT,
            FinancialEventKind.EXPENSE,
            FinancialEventKind.FEE ->
                MessageLabSemanticLabel
                    .PAYMENT

            FinancialEventKind.RECEIPT,
            FinancialEventKind.INCOME,
            FinancialEventKind.REFUND ->
                MessageLabSemanticLabel
                    .MONEY_RECEIVED

            FinancialEventKind.TRANSFER ->
                MessageLabSemanticLabel
                    .TRANSFER

            FinancialEventKind.CASH_WITHDRAWAL ->
                MessageLabSemanticLabel
                    .CASH_WITHDRAWAL

            FinancialEventKind.CASH_DEPOSIT ->
                MessageLabSemanticLabel
                    .CASH_DEPOSIT

            FinancialEventKind.DEBT_DRAWDOWN ->
                MessageLabSemanticLabel
                    .LOAN_RECEIVED

            FinancialEventKind.DEBT_REPAYMENT ->
                MessageLabSemanticLabel
                    .LOAN_REPAYMENT

            FinancialEventKind.UNRESOLVED,
            FinancialEventKind.REVERSAL ->
                null
        }

    private fun semanticIntentLabel(
        intent:
            FinancialSemanticIntent
    ): MessageLabSemanticLabel? =
        when (intent) {
            FinancialSemanticIntent.PAYMENT,
            FinancialSemanticIntent.FEE ->
                MessageLabSemanticLabel
                    .PAYMENT

            FinancialSemanticIntent.RECEIPT,
            FinancialSemanticIntent.REFUND ->
                MessageLabSemanticLabel
                    .MONEY_RECEIVED

            FinancialSemanticIntent.TRANSFER ->
                MessageLabSemanticLabel
                    .TRANSFER

            FinancialSemanticIntent.LOAN_DISBURSEMENT ->
                MessageLabSemanticLabel
                    .LOAN_RECEIVED

            FinancialSemanticIntent.LOAN_REPAYMENT ->
                MessageLabSemanticLabel
                    .LOAN_REPAYMENT

            FinancialSemanticIntent.LOAN_BALANCE,
            FinancialSemanticIntent.BALANCE_UPDATE ->
                MessageLabSemanticLabel
                    .BALANCE_UPDATE

            FinancialSemanticIntent.INVESTMENT_ORDER,
            FinancialSemanticIntent.INVESTMENT_EXECUTED ->
                MessageLabSemanticLabel
                    .SAVINGS_INVESTMENT

            FinancialSemanticIntent.REVERSAL,
            FinancialSemanticIntent.CONFIRMATION_ONLY,
            FinancialSemanticIntent.FAILED_ACTION,
            FinancialSemanticIntent.UNKNOWN ->
                null
        }
}
