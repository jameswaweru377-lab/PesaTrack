package com.kedevjimmy.pesatrack.domain.interpretation

/**
 * Stricter policy for deciding whether an unrecognized message should
 * be retained as an unsupported financial shape.
 *
 * The normal relevance gate intentionally favors recall so PesaTrack
 * does not miss unfamiliar financial wording. Message Lab should be
 * stricter: weak single-word matches such as "credit", "loan",
 * "balance" or "shares" must not become permanent "Not understood"
 * noise unless another independent financial signal is present.
 */
object UnsupportedFinancialCandidatePolicy {
    fun shouldTrack(
        relevance: FinancialMessageRelevance
    ): Boolean {
        if (!relevance.isRelevant) {
            return false
        }

        val signals = relevance.signals

        val hasAmount =
            FinancialRelevanceSignal
                .CURRENCY_AMOUNT in signals
        val hasAction =
            FinancialRelevanceSignal
                .FINANCIAL_ACTION in signals
        val hasAccount =
            FinancialRelevanceSignal
                .ACCOUNT_CONTEXT in signals
        val hasDebt =
            FinancialRelevanceSignal
                .DEBT_CONTEXT in signals
        val hasInvestment =
            FinancialRelevanceSignal
                .INVESTMENT_CONTEXT in signals
        val hasConfirmation =
            FinancialRelevanceSignal
                .FINANCIAL_CONFIRMATION in signals
        val hasPurchase =
            FinancialRelevanceSignal
                .PURCHASE_CONTEXT in signals

        val positiveSignalCount =
            listOf(
                hasAmount,
                hasAction,
                hasAccount,
                hasDebt,
                hasInvestment,
                hasConfirmation,
                hasPurchase
            ).count { it }

        // Known noise classes should never be promoted merely because
        // they also contain a weak financial word.
        if (
            FinancialRelevanceSignal
                .SECURITY_NOISE in signals ||
            FinancialRelevanceSignal
                .PROMOTIONAL_NOISE in signals ||
            FinancialRelevanceSignal
                .FINANCIAL_SOLICITATION in signals ||
            FinancialRelevanceSignal
                .TELECOM_INFORMATION_ONLY in signals
        ) {
            return false
        }

        // Unsupported storage requires corroboration. A single
        // positive signal is enough for interpretation recall, but not
        // enough to tell the user PesaTrack failed to understand a
        // financial message.
        if (positiveSignalCount < 2) {
            return false
        }

        return when {
            hasAmount ->
                hasAction ||
                    hasAccount ||
                    hasDebt ||
                    hasInvestment ||
                    hasPurchase

            hasConfirmation ->
                hasAction ||
                    hasDebt ||
                    hasInvestment

            hasDebt ->
                hasAction ||
                    hasAccount

            hasInvestment ->
                hasAction ||
                    hasAccount

            hasPurchase ->
                hasAction

            else ->
                hasAction && hasAccount
        }
    }
}
