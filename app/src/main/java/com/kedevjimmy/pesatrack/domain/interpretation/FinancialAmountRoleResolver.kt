package com.kedevjimmy.pesatrack.domain.interpretation

/**
 * Resolves monetary roles after extraction so downstream callers never
 * fall back to "the first KES amount in the message".
 */
object FinancialAmountRoleResolver {
    fun unexplainedMoney(
        candidates: FinancialMessageCandidates
    ): List<MoneyValueCandidate> {
        val explained = explainedSpans(candidates)

        return candidates.moneyValues.filter { money ->
            explained.none { span ->
                money.span.overlaps(span)
            }
        }
    }

    fun primaryAmountForConfirmedRule(
        candidates: FinancialMessageCandidates
    ): MoneyValueCandidate? {
        val semanticSpans = buildList {
            candidates.actions.forEach {
                add(it.amountSpan)
            }
            candidates.cashMovements.forEach {
                add(it.amountSpan)
            }
            candidates.reversal?.let {
                add(it.amountSpan)
            }
            candidates.debt.drawdown?.let {
                add(it.amountSpan)
            }
            candidates.debt.repayment?.let {
                add(it.amountSpan)
            }
        }.distinctBy {
            it.startOffset to it.endOffsetExclusive
        }

        if (semanticSpans.size > 1) {
            return null
        }

        if (semanticSpans.size == 1) {
            val span = semanticSpans.single()
            return candidates.moneyValues
                .firstOrNull {
                    it.span.overlaps(span)
                }
        }

        return unexplainedMoney(candidates)
            .singleOrNull()
    }

    private fun explainedSpans(
        candidates: FinancialMessageCandidates
    ): List<MessageSpan> =
        buildList {
            candidates.balances.forEach {
                add(it.amountSpan)
            }
            candidates.fee?.let {
                add(it.amountSpan)
            }
            candidates.reversal?.let {
                add(it.amountSpan)
            }
            candidates.cashMovements.forEach {
                add(it.amountSpan)
            }
            candidates.actions.forEach {
                add(it.amountSpan)
            }
            candidates.debt.drawdown?.let {
                add(it.amountSpan)
                it.outstandingSpan?.let(::add)
                it.availableCreditSpan?.let(::add)
            }
            candidates.debt.repayment?.let {
                add(it.amountSpan)
                it.availableCreditSpan?.let(::add)
            }
        }

    private fun MessageSpan.overlaps(
        other: MessageSpan
    ): Boolean =
        startOffset < other.endOffsetExclusive &&
            other.startOffset < endOffsetExclusive
}
