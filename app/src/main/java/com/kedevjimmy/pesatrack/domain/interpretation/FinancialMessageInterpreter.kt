package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.messagepack.KenyaMessagePack
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseProvenance

object FinancialMessageInterpreter {
    private val
        PROVENANCE_ONLY_DEBT_EVENT_KINDS =
        setOf(
            "DEBT_DUE_NOTICE"
        )

    fun interpret(
        title: String?,
        body: String,
        sourcePackage: String? = null,
        sourceKind: String? = null
    ): FinancialMessageInterpretation {
        val messagePackMatch =
            KenyaMessagePack.route(
                title = title,
                body = body,
                sourcePackage =
                    sourcePackage,
                sourceKind =
                    sourceKind
            )
        val provenance =
            messagePackMatch
                ?.let(
                    MessageParseProvenance::template
                )
                ?: MessageParseProvenance
                    .genericFallback()

        val normalized =
            FinancialMessageNormalizer.normalize(
                title = title,
                body = body
            )

        val relevance =
            FinancialMessageRelevanceGate.assess(
                normalized
            )

        if (!relevance.isRelevant) {
            return FinancialMessageInterpretation(
                transactionCode = null,
                occurredAtText = null,
                balances = emptyList(),
                events = emptyList(),
                parseProvenance =
                    provenance
            )
        }

        val semanticAssessment =
            FinancialSemanticAnalyzer.analyze(
                normalized
            )

        val extractedCandidates =
            FinancialCandidateExtractor.extract(
                normalized
            )
        val candidates =
            MessagePackCandidateBridge.enrich(
                candidates =
                    extractedCandidates,
                match =
                    messagePackMatch,
                message =
                    normalized
            )
        val resolved =
            FinancialRelationshipResolver.resolve(
                message = normalized,
                candidates = candidates
            )
        val provenanceOnlyDebtShape =
            messagePackMatch
                ?.eventKind
                ?.uppercase() in
                PROVENANCE_ONLY_DEBT_EVENT_KINDS

        val classified =
            FinancialEventClassifier.classify(
                resolved = resolved,
                suppressMovementEvents =
                    semanticAssessment
                        .primaryIntent ==
                    FinancialSemanticIntent
                        .FAILED_ACTION ||
                        provenanceOnlyDebtShape
            )

        return classified.copy(
            semanticAssessment =
                semanticAssessment,
            parseProvenance =
                provenance
        )
    }
}
