package com.kedevjimmy.pesatrack.domain.interpretation

object FinancialMessageInterpreter {
    fun interpret(
        title: String?,
        body: String
    ): FinancialMessageInterpretation {
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
                events = emptyList()
            )
        }

        val candidates =
            FinancialCandidateExtractor.extract(
                normalized
            )
        val resolved =
            FinancialRelationshipResolver.resolve(
                message = normalized,
                candidates = candidates
            )
        return FinancialEventClassifier.classify(
            resolved
        )
    }
}
