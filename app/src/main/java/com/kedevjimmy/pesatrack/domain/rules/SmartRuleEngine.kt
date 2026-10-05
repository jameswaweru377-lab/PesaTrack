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
    ): SmartRule? =
        matchTransaction(
            counterparty = transaction.counterparty,
            counterpartyPhone = transaction.counterpartyPhone,
            amountMinor = transaction.amountMinor,
            rules = rules.filter { !SalaryEvidencePolicy.isSalary(it) || SalaryEvidencePolicy.accepts(transaction) }
        )

    fun matchTransaction(
        counterparty: String?,
        counterpartyPhone: String?,
        amountMinor: Long,
        rules: List<SmartRule>
    ): SmartRule? {
        val normalizedCounterparty =
            counterparty?.normalizeName()
        val phone =
            normalizePhone(counterpartyPhone.orEmpty())

        return rules
            .asSequence()
            .filter(SmartRule::enabled)
            .filter { rule ->
                when {
                    rule.counterpartyPhoneEquals.isNotBlank() -> {
                        val expectedPhone =
                            normalizePhone(
                                rule.counterpartyPhoneEquals
                            )
                        if (phone.isNotBlank()) {
                            phone == expectedPhone
                        } else {
                            rule.counterpartyExact.isNotBlank() &&
                                normalizedCounterparty ==
                                rule.counterpartyExact
                                    .normalizeName()
                        }
                    }

                    rule.counterpartyExact.isNotBlank() ->
                        normalizedCounterparty != null &&
                            normalizedCounterparty ==
                            rule.counterpartyExact
                                .normalizeName()

                    rule.counterpartyContains.isNotBlank() ->
                        normalizedCounterparty?.contains(
                            rule.counterpartyContains
                                .normalizeName()
                        ) == true

                    else -> false
                }
            }
            .filter { rule ->
                rule.maximumAmountMinor == null ||
                    amountMinor <=
                    rule.maximumAmountMinor
            }
            .firstOrNull()
    }

    fun matchNotification(
        title: String?,
        body: String,
        rules: List<SmartRule>
    ): SmartRule? {
        val normalizedTitle = title.orEmpty().normalizeText()
        val normalizedBody = body.normalizeText()

        return rules
            .asSequence()
            .filter(SmartRule::enabled)
            .filter { rule ->
                rule.notificationTitleContains.isNotBlank() ||
                    rule.messageContains.isNotBlank()
            }
            .filter { rule ->
                rule.notificationTitleContains.isBlank() ||
                    normalizedTitle.contains(rule.notificationTitleContains.normalizeText())
            }
            .filter { rule ->
                rule.messageContains.isBlank() ||
                    normalizedBody.contains(rule.messageContains.normalizeText())
            }
            .firstOrNull()
    }

    fun sameStrongIdentity(
        transaction: ParsedMpesaTransaction,
        rule: SmartRule
    ): Boolean {
        val phone = normalizePhone(transaction.counterpartyPhone.orEmpty())
        if (rule.counterpartyPhoneEquals.isNotBlank()) {
            return phone.isNotBlank() &&
                phone == normalizePhone(rule.counterpartyPhoneEquals)
        }

        if (rule.counterpartyExact.isNotBlank()) {
            return transaction.counterparty?.normalizeName() ==
                rule.counterpartyExact.normalizeName()
        }

        return false
    }

    private fun String.normalizeName(): String =
        trim()
            .lowercase()
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun String.normalizeText(): String =
        trim().lowercase().replace(Regex("""\s+"""), " ")

    private fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return when {
            digits.startsWith("254") && digits.length == 12 -> "+" + digits
            digits.startsWith("0") && digits.length == 10 -> "+254" + digits.drop(1)
            else -> value.trim()
        }
    }
}

