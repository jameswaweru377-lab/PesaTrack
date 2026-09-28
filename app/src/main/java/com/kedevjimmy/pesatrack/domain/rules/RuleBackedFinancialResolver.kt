package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAmountRoleResolver
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialCandidateExtractor
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.SmartRule

/**
 * Applies a user-confirmed Smart Rule only after generic extraction has
 * assigned monetary roles. The rule may enrich direction/category/account
 * meaning, but it never chooses "the first KES amount".
 */
object RuleBackedFinancialResolver {
    fun resolve(
        title: String?,
        body: String,
        rule: SmartRule
    ): ParsedMpesaTransaction? {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = title,
                body = body
            )
        if (!normalized.hasCurrencyHint) {
            return null
        }

        val candidates =
            FinancialCandidateExtractor.extract(
                normalized
            )
        val amount =
            FinancialAmountRoleResolver
                .primaryAmountForConfirmedRule(
                    candidates
                )
                ?: return null

        val balance =
            candidates.balances
                .distinctBy {
                    it.amountSpan.startOffset to
                        it.amountSpan.endOffsetExclusive
                }
                .singleOrNull()
                ?.amountMinor

        val label = rule.displayLabel
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: rule.name
                .trim()
                .takeIf { it.isNotBlank() }
            ?: title
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        return ParsedMpesaTransaction(
            transactionCode =
                candidates.transactionCode
                    ?.value
                    ?.uppercase(),
            type = MpesaTransactionType.OTHER,
            amountMinor = amount.amountMinor,
            counterparty = label,
            feeMinor =
                candidates.fee?.amountMinor ?: 0L,
            balanceMinor = balance,
            occurredAtText =
                candidates.occurredAt?.value,
            rawMessage = body
        )
    }
}
