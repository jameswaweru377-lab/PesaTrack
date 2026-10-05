package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.SmartRule

/**
 * Bridges generic financial interpretation into the existing
 * user-learning/rule layer. Financial meaning is established
 * before this runs; rules enrich categorization and planning,
 * they do not parse provider message syntax.
 */
object InterpretedRuleResolver {
    fun match(
        transaction: ParsedMpesaTransaction,
        title: String?,
        body: String,
        rules: List<SmartRule>
    ): SmartRule? =
        match(
            counterparty = transaction.counterparty,
            counterpartyPhone =
                transaction.counterpartyPhone,
            amountMinor = transaction.amountMinor,
            title = title,
            body = body,
            rules = rules.filter { !SalaryEvidencePolicy.isSalary(it) || SalaryEvidencePolicy.accepts(transaction) }
        )

    fun match(
        counterparty: String?,
        counterpartyPhone: String?,
        amountMinor: Long,
        title: String?,
        body: String,
        rules: List<SmartRule>
    ): SmartRule? =
        SmartRuleEngine.matchTransaction(
            counterparty = counterparty,
            counterpartyPhone =
                counterpartyPhone,
            amountMinor = amountMinor,
            rules = rules
        ) ?: SmartRuleEngine.matchNotification(
            title = title,
            body = body,
            rules = rules
        )
}

