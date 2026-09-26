package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

object SmartRuleEngine {
    fun categorize(
        transaction: ParsedMpesaTransaction,
        rules: List<SmartRule>
    ): TransactionCategory {
        val counterparty = transaction.counterparty?.normalize()
            ?: return TransactionCategory.UNCATEGORIZED

        return rules
            .asSequence()
            .filter { rule -> counterparty.contains(rule.counterpartyContains.normalize()) }
            .filter { rule ->
                rule.maximumAmountMinor == null || transaction.amountMinor <= rule.maximumAmountMinor
            }
            .firstOrNull()
            ?.category
            ?: TransactionCategory.UNCATEGORIZED
    }

    private fun String.normalize(): String =
        trim().lowercase().replace(Regex("""\s+"""), " ")
}
