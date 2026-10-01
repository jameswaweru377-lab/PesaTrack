package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.messagepack.KenyaMessagePack
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseProvenance

object FinancialMessageInterpreter {
    fun interpret(
        title: String?,
        body: String
    ): FinancialMessageInterpretation {
        val messagePackMatch =
            KenyaMessagePack.route(
                title = title,
                body = body
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

        val candidates =
            FinancialCandidateExtractor.extract(
                normalized
            )
        val resolved =
            FinancialRelationshipResolver.resolve(
                message = normalized,
                candidates = candidates
            )
        val classified =
            FinancialEventClassifier.classify(
                resolved = resolved,
                suppressMovementEvents =
                    semanticAssessment
                        .primaryIntent ==
                    FinancialSemanticIntent
                        .FAILED_ACTION
            )

        return classified.copy(
            semanticAssessment =
                semanticAssessment,
            parseProvenance =
                provenance
        )
    }
}
