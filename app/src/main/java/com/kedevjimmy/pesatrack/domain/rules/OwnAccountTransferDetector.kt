package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

enum class OwnTransferEvidence {
    EXACT_IDENTIFIER,
    ACCOUNT_NAME
}

data class OwnAccountTransferMatch(
    val sourceAccountId: String,
    val sourceName: String,
    val destinationAccountId: String,
    val destinationName: String,
    val evidence: OwnTransferEvidence
) {
    fun asSmartRule(): SmartRule =
        SmartRule(
            id = "auto-own-transfer:$sourceAccountId:$destinationAccountId",
            name = "Own-account transfer",
            counterpartyContains = "",
            category = TransactionCategory.TRANSFER,
            direction = MoneyDirection.TRANSFER,
            displayLabel = "$sourceName → $destinationName",
            preset = RulePreset.OWN_TRANSFER,
            accountId = sourceAccountId,
            linkedAccountId = destinationAccountId
        )
}

object OwnAccountTransferDetector {
    private val phoneRegex = Regex("""(?<!\d)(?:\+?254|0)[17]\d{8}(?!\d)""")
    private val genericAccountWords = setOf(
        "bank", "account", "mobile", "wallet", "savings", "current", "salary", "my"
    )
    private val outgoingMovementRegex = Regex(
        """\b(?:sent|transfer(?:red)?|debit(?:ed)?|paid)\b""", RegexOption.IGNORE_CASE
    )

    fun normalizeKenyanPhone(value: String): String? {
        val digits = value.filter(Char::isDigit)
        return when {
            digits.length == 12 &&
                digits.startsWith("254") &&
                digits.drop(3).firstOrNull() in setOf('1', '7') ->
                "+$digits"

            digits.length == 10 &&
                digits.startsWith("0") &&
                digits.drop(1).firstOrNull() in setOf('1', '7') ->
                "+254" + digits.drop(1)

            digits.length == 9 &&
                digits.firstOrNull() in setOf('1', '7') ->
                "+254$digits"

            else -> null
        }
    }

    fun matchMpesa(
        parsed: ParsedMpesaTransaction,
        accounts: List<MoneyAccount>,
        observedWalletId: String
    ): OwnAccountTransferMatch? {
        val observedWallet = accounts.firstOrNull {
            it.id == observedWalletId && it.enabled
        } ?: return null

        return when (parsed.type) {
            MpesaTransactionType.SENT_MONEY -> {
                val phone = parsed.counterpartyPhone
                    ?.let(::normalizeKenyanPhone)
                    ?: return null
                val destination = accounts.firstOrNull { account ->
                    account.enabled &&
                        account.id != observedWallet.id &&
                        account.identifier
                            ?.let(::normalizeKenyanPhone) == phone
                } ?: return null

                OwnAccountTransferMatch(
                    sourceAccountId = observedWallet.id,
                    sourceName = observedWallet.name,
                    destinationAccountId = destination.id,
                    destinationName = destination.name,
                    evidence = OwnTransferEvidence.EXACT_IDENTIFIER
                )
            }

            MpesaTransactionType.RECEIVED,
            MpesaTransactionType.DEPOSIT -> {
                val phone = parsed.counterpartyPhone?.let(::normalizeKenyanPhone)
                val exactSource = phone?.let { normalized ->
                    accounts.firstOrNull { account ->
                        account.enabled &&
                            account.id != observedWallet.id &&
                            account.identifier
                                ?.let(::normalizeKenyanPhone) == normalized
                    }
                }

                // A receipt from a bank name can be someone else's payment.
                // Exact owned identifiers work here; bank-name receipts are linked
                // only when the complementary own-bank observation arrives.
                val source = exactSource ?: return null

                OwnAccountTransferMatch(
                    sourceAccountId = source.id,
                    sourceName = source.name,
                    destinationAccountId = observedWallet.id,
                    destinationName = observedWallet.name,
                    evidence = if (exactSource != null) {
                        OwnTransferEvidence.EXACT_IDENTIFIER
                    } else {
                        OwnTransferEvidence.ACCOUNT_NAME
                    }
                )
            }

            else -> null
        }
    }

    fun matchConfiguredNotification(
        title: String?,
        body: String,
        accounts: List<MoneyAccount>
    ): OwnAccountTransferMatch? {
        if (!outgoingMovementRegex.containsMatchIn(body)) return null
        val sender = title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val enabled = accounts.filter { it.enabled }
        // A mentioned bank or person is not evidence of the sender's account.
        // Multiple accounts at the same institution require more evidence.
        val source = enabled.filter { accountNameMatches(it.name, sender) }
            .singleOrNull() ?: return null
        val destination = enabled.filter {
            it.id != source.id && identifierAppears(it.identifier, body)
        }.singleOrNull() ?: return null
        return OwnAccountTransferMatch(
            source.id, source.name, destination.id, destination.name,
            OwnTransferEvidence.EXACT_IDENTIFIER
        )
    }

    private fun identifierAppears(
        identifier: String?,
        text: String
    ): Boolean {
        val value =
            identifier
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                } ?: return false

        normalizeKenyanPhone(value)
            ?.let { phone ->
                return phoneRegex
                    .findAll(text)
                    .mapNotNull {
                        normalizeKenyanPhone(
                            it.value
                        )
                    }
                    .any {
                        it == phone
                    }
            }

        val compactIdentifier =
            value.uppercase()
                .replace(
                    Regex("""[^A-Z0-9]+"""),
                    ""
                )
        if (compactIdentifier.length < 4) return false
        // A saved account identifier needs account context and a whole match.
        // Do not recognize a reference, amount or longer account number as owned.
        val identifierPattern = compactIdentifier.map { Regex.escape(it.toString()) }
            .joinToString("""[\s-]*""")
        val accountContext = Regex(
            """(?i)\b(?:a/?c|acct|account)\b(?:\s*(?:number|no\.?))?[^A-Z0-9]{0,24}(?:[*xX#•\s-]*""" +
                identifierPattern + """)\b"""
        )
        return accountContext.containsMatchIn(text)
    }

    private fun accountNameMatches(
        accountName: String,
        text: String
    ): Boolean {
        val compactName =
            accountName.lowercase()
                .replace(
                    Regex("""[^a-z0-9]+"""),
                    ""
                )
        val compactText =
            text.lowercase()
                .replace(
                    Regex("""[^a-z0-9]+"""),
                    ""
                )
        if (
            compactName.length >= 4 &&
            (
                compactName in compactText ||
                    compactText in
                        compactName
                )
        ) {
            return true
        }

        val textTokens =
            tokenize(text).toSet()
        val accountTokens =
            tokenize(accountName)
                .filterNot(
                    genericAccountWords::
                        contains
                )
        return accountTokens.any {
                token ->
            token.length >= 3 &&
                token in textTokens
        }
    }

    private fun tokenize(value: String): List<String> =
        value.lowercase()
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
            .split(Regex("""\s+"""))
            .filter(String::isNotBlank)
}

