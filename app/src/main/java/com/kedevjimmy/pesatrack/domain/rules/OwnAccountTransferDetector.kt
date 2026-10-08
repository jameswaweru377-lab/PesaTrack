package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
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
    private val phoneRegex = Regex("""(?:\+?254|0)[17]\d{8}""")
    private val genericAccountWords = setOf(
        "bank", "account", "mobile", "wallet", "savings", "current", "salary", "my"
    )
    private val outgoingWords = listOf(
        "sent", "transfer", "transferred", "debit", "debited", "paid", "m-pesa", "mpesa"
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
        val enabledAccounts =
            accounts.filter {
                it.enabled
            }
        val lowerBody =
            body.lowercase()
        if (
            outgoingWords.none(
                lowerBody::contains
            )
        ) {
            return null
        }

        // The message sender identifies the source institution, but never the
        // destination. The destination must match an identifier that the user
        // explicitly saved for one of their accounts (phone/account number).
        val source =
            enabledAccounts
                .firstOrNull { account ->
                    title?.let {
                        accountNameMatches(
                            account.name,
                            it
                        )
                    } == true
                }
                ?: enabledAccounts
                    .firstOrNull { account ->
                        accountNameMatches(
                            account.name,
                            body
                        )
                    }

        if (source != null) {
            val destination =
                enabledAccounts
                    .firstOrNull { account ->
                        account.id !=
                            source.id &&
                            identifierAppears(
                                account.identifier,
                                body
                            )
                    }

            if (destination != null) {
                return OwnAccountTransferMatch(
                    sourceAccountId =
                        source.id,
                    sourceName =
                        source.name,
                    destinationAccountId =
                        destination.id,
                    destinationName =
                        destination.name,
                    evidence =
                        OwnTransferEvidence
                            .EXACT_IDENTIFIER
                )
            }
        }

        // Backward-compatible bank/savings -> mobile-wallet route. This still
        // requires the user's exact wallet phone number in the message.
        val normalizedPhones =
            phoneRegex.findAll(body)
                .mapNotNull {
                    normalizeKenyanPhone(
                        it.value
                    )
                }
                .toSet()
        if (normalizedPhones.isEmpty()) {
            return null
        }

        val destination =
            enabledAccounts
                .firstOrNull { account ->
                    account.type ==
                        AccountType
                            .MOBILE_WALLET &&
                        account.identifier
                            ?.let(
                                ::normalizeKenyanPhone
                            )
                            ?.let(
                                normalizedPhones::
                                    contains
                            ) == true
                } ?: return null

        val bankSource =
            enabledAccounts
                .firstOrNull { account ->
                    account.id !=
                        destination.id &&
                        account.type in
                            setOf(
                                AccountType.BANK,
                                AccountType.SAVINGS
                            ) &&
                        (
                            title?.let {
                                accountNameMatches(
                                    account.name,
                                    it
                                )
                            } == true ||
                                accountNameMatches(
                                    account.name,
                                    body
                                )
                            )
                } ?: return null

        return OwnAccountTransferMatch(
            sourceAccountId =
                bankSource.id,
            sourceName =
                bankSource.name,
            destinationAccountId =
                destination.id,
            destinationName =
                destination.name,
            evidence =
                OwnTransferEvidence
                    .EXACT_IDENTIFIER
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
        if (
            compactIdentifier.length <
            4
        ) {
            return false
        }

        if (
            compactIdentifier.length <=
            6
        ) {
            // A short last-4/last-6 value is useful only when the message
            // presents it as an account identifier. Do not match the same
            // digits when they happen to occur in an amount/reference/date.
            val accountContext =
                Regex(
                    """(?i)(?:a/?c|acct|account)[^A-Z0-9]{0,24}(?:[*xX#•\s-]*""" +
                        Regex.escape(
                            compactIdentifier
                        ) +
                        """)\b"""
                )
            return accountContext
                .containsMatchIn(text)
        }

        val compactText =
            text.uppercase()
                .replace(
                    Regex("""[^A-Z0-9]+"""),
                    ""
                )
        return compactIdentifier in
            compactText
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

