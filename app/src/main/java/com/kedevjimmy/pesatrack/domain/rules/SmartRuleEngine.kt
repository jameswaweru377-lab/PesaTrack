package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

object SmartRuleEngine {
    fun categorize(
        transaction: ParsedMpesaTransaction,
        rules: List<SmartRule>
    ): TransactionCategory =
        matchTransaction(transaction, rules)?.category
            ?: TransactionCategory.UNCATEGORIZED

    fun matchTransaction(
        transaction: ParsedMpesaTransaction,
        rules: List<SmartRule>
    ): SmartRule? {
        val counterparty = transaction.counterparty?.normalize()

        return rules
            .asSequence()
            .filter(SmartRule::enabled)
            .filter { rule -> rule.counterpartyContains.isNotBlank() }
            .filter { rule ->
                counterparty?.contains(rule.counterpartyContains.normalize()) == true
            }
            .filter { rule ->
                rule.maximumAmountMinor == null || transaction.amountMinor <= rule.maximumAmountMinor
            }
            .firstOrNull()
    }

    fun matchNotification(
        title: String?,
        body: String,
        rules: List<SmartRule>
    ): SmartRule? {
        val normalizedTitle = title.orEmpty().normalize()
        val normalizedBody = body.normalize()

        return rules
            .asSequence()
            .filter(SmartRule::enabled)
            .filter { rule ->
                rule.notificationTitleContains.isNotBlank() ||
                    rule.messageContains.isNotBlank()
            }
            .filter { rule ->
                rule.notificationTitleContains.isBlank() ||
                    normalizedTitle.contains(rule.notificationTitleContains.normalize())
            }
            .filter { rule ->
                rule.messageContains.isBlank() ||
                    normalizedBody.contains(rule.messageContains.normalize())
            }
            .firstOrNull()
    }

    private fun String.normalize(): String =
        trim().lowercase().replace(Regex("""\s+"""), " ")
}
