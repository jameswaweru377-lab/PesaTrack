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

        if (!normalized.hasCurrencyHint) {
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
