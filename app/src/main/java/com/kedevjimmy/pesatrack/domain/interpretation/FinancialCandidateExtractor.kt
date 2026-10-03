package com.kedevjimmy.pesatrack.domain.interpretation

import java.math.RoundingMode

enum class FinancialActionCandidateKind {
    TRANSFER_FROM_ACCOUNT,
    TRANSFER_TO_ACCOUNT,
    PAYMENT_SENT_TO,
    PAYMENT_PAID_TO,
    ACCOUNT_DEBIT_TO,
    TRANSACTION_CREDITED_TO,
    AIRTIME_PURCHASE,
    RECEIPT_FROM,
    RECEIPT_CREDITED_FROM
}

data class TextValueCandidate(
    val value: String,
    val span: MessageSpan,
    val ruleId: String
)

data class MoneyValueCandidate(
    val amountMinor: Long,
    val span: MessageSpan,
    val ruleId: String
)

data class BalanceCandidate(
    val rawAccountLabel: String,
    val accountSpan: MessageSpan?,
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val ruleId: String
)

data class FeeCandidate(
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val ruleId: String
)

data class AccountDebitCandidate(
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val accountReference: String?,
    val accountReferenceSpan: MessageSpan?,
    val ruleId: String
)

data class FinancialActionCandidate(
    val kind: FinancialActionCandidateKind,
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val partyLabel: String,
    val partySpan: MessageSpan,
    val partyPhone: String?,
    val accountReference: String? = null,
    val accountReferenceSpan: MessageSpan? = null,
    val ruleId: String
)

enum class CashMovementCandidateKind {
    WITHDRAWAL,
    DEPOSIT
}

data class CashMovementCandidate(
    val kind: CashMovementCandidateKind,
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val accountLabel: String,
    val accountSpan: MessageSpan,
    val counterpartyLabel: String?,
    val counterpartySpan: MessageSpan?,
    val ruleId: String
)

data class ReversalCandidate(
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val referencedTransactionCode: String,
    val referencedCodeSpan: MessageSpan,
    val ruleId: String
)

data class FinancialMessageCandidates(
    val transactionCode: TextValueCandidate?,
    val occurredAt: TextValueCandidate?,
    val moneyValues: List<MoneyValueCandidate>,
    val balances: List<BalanceCandidate>,
    val fee: FeeCandidate?,
    val accountDebit: AccountDebitCandidate? = null,
    val reversal: ReversalCandidate?,
    val cashMovements: List<CashMovementCandidate>,
    val actions: List<FinancialActionCandidate>,
    val debt: DebtMessageCandidates
)

object FinancialCandidateExtractor {
    private val moneyRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val codeRegex = Regex(
        """\b([A-Z0-9]{10})\s+Confirmed\b""",
        RegexOption.IGNORE_CASE
    )
    private val occurredAtRegex = Regex(
        """\bon\s+(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\s+(?:at\s+)?\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM)?)""",
        RegexOption.IGNORE_CASE
    )
    private val GENERIC_BALANCE_ACCOUNT_LABELS =
        setOf(
            "ACCOUNT",
            "AVAILABLE",
            "BALANCE",
            "CLOSING",
            "CURRENT",
            "NEW",
            "YOUR"
        )

    private val balanceRegex = Regex(
        """(?:^|[.!?]\s*)(?:New\s+|Your\s+)?([A-Za-z][A-Za-z0-9&'’.-]*(?:\s+[A-Za-z0-9&'’.-]+){0,4})\s+balance\s+(?:is|of)\s*(?:(?:Ksh|KES)\s*)?([0-9,]+(?:\.\d{1,2})?)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )
    private val availableBalanceRegex = Regex(
        """(?:^|[.!?]\s*)(?:your\s+)?(?:(?:available|current|closing)\s+)?balance\s*(?:is\s*)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )
    private val genericLoanBalanceRegex = Regex(
        """\b(?:(?:total|outstanding)\s+)?loan\s+(?:outstanding\s+)?balance\s*(?:is|of|:)?\s*(?:(?:Ksh|KES)\s*)?([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val feeRegex = Regex(
        """(?:transaction\s+(?:cost|fee)|access\s+fee(?:\s+(?:(?:is\s+)?charged|charge(?:d)?(?:\s+of)?|of))?|charge)\s*[,.:]?\s*(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val transferFromRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:transferred|withdrawn|moved)\s+from\s+(.+?)\s+account\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val transferFromNamedSourceRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:transferred|moved)\s+from\s+(.+?)(?=\s+on\s+\d{1,2}[/-]|\.\s*(?:New\s+|Your\s+)?[A-Za-z]|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val receivedFromAccountRegex = Regex(
        """(?:received\s+)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+from\s+(.+?)\s+account\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val transferToRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:transferred|deposited|moved)\s+to\s+(.+?)\s+account\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val transferToNamedDestinationRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:transferred|moved)\s+to\s+(.+?)(?=\s+on\s+\d{1,2}[/-]|\.\s*(?:New\s+|Your\s+)?[A-Za-z]|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val sentAmountRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+sent\s+to\b""",
        RegexOption.IGNORE_CASE
    )
    private val sentPartyRegex = Regex(
        """sent\s+to\s+(.+?)(?=\s+(?:\+?254|0)\d{9}\b|\s+on\s+\d|\.\s*(?:New\s+)?M-PESA|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val sentPhoneRegex = Regex(
        """sent\s+to\s+.+?\s+((?:\+?254|0)\d{9})\b""",
        RegexOption.IGNORE_CASE
    )
    private val trailingMaskedPhoneRegex = Regex(
        """\s+(?:(?:\+?254|0)?[0-9xX*\-\s]{7,})$"""
    )
    private val paidAmountRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+paid\s+to\b""",
        RegexOption.IGNORE_CASE
    )
    private val paidPartyRegex = Regex(
        """paid\s+to\s+(.+?)(?=\s+for\s+account\b|\s+on\s+\d|\.\s*(?:New\s+)?M-PESA|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val paidAccountReferenceRegex = Regex(
        """\bfor\s+account\s+(.+?)(?=\s+on\s+\d|\.\s*(?:New\s+)?M-PESA|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val accountDebitToRegexes = listOf(
        Regex(
            """(?:your\s+)?(?:[A-Za-z][A-Za-z0-9&'’.-]{1,30}\s+){0,2}(?:a/?c|acct|account)(?:\s+[A-Za-z0-9*Xx-]+)?\s+(?:has\s+been\s+)?debited(?:\s+(?:with|by))?\s+(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?).{0,180}?(?:transfer\s+to|sent\s+to|paid\s+to|beneficiary\s*[:\-]?|to)\s+([A-Za-z][A-Za-z0-9&'’()./+\- ]{1,80}?)(?=\s+(?:on|ref|reference|available|current|closing|balance|successful|successfully|completed)\b|[.;]|$)""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        ),
        Regex(
            """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?debited\s+from\s+(?:your\s+)?(?:[A-Za-z][A-Za-z0-9&'’.-]{1,30}\s+){0,2}(?:a/?c|acct|account)(?:\s+[A-Za-z0-9*Xx-]+)?.{0,180}?(?:transfer\s+to|sent\s+to|paid\s+to|beneficiary\s*[:\-]?|to)\s+([A-Za-z][A-Za-z0-9&'’()./+\- ]{1,80}?)(?=\s+(?:on|ref|reference|available|current|closing|balance|successful|successfully|completed)\b|[.;]|$)""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        )
    )
    private val accountDebitWithoutCounterpartyRegexes =
        listOf(
            Regex(
                """(?:your\s+)?(?:[A-Za-z][A-Za-z0-9&'’.-]{1,30}\s+){0,2}(?:a/?c|acct|account)(?:\s+([A-Za-z0-9*Xx-]+))?\s+(?:has\s+been\s+)?debited(?:\s+(?:with|by))?\s+(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?debited\s+from\s+(?:your\s+)?(?:[A-Za-z][A-Za-z0-9&'’.-]{1,30}\s+){0,2}(?:a/?c|acct|account)(?:\s+([A-Za-z0-9*Xx-]+))?""",
                RegexOption.IGNORE_CASE
            )
        )

    private val transactionCreditedToRegex = Regex(
        """\b(?:your\s+)?transaction\s+of\s+(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?credited\s+to\s+(?!your\s+(?:a/?c|account|wallet)\b)(?:(?:\+?254|0)\d{9}\s+)?([A-Za-z][A-Za-z0-9&'’()./+\- ]{1,100}?)(?=\s+(?:on|ref|reference|available|current|closing|balance|m-?pesa\s+ref)\b|[.;]|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val transactionCreditedToPhoneRegex = Regex(
        """\bcredited\s+to\s+((?:\+?254|0)\d{9})\b""",
        RegexOption.IGNORE_CASE
    )

    private val receiptRegex = Regex(
        """(?:you\s+have\s+)?received\s+(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+from\s+(.+?)(?=\s+(?:\+?254|0)\d{9}\b|\s+on\s+\d|\.\s*(?:New\s+)?M-PESA|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val creditedFromRegexes = listOf(
        Regex(
            """\bcredited\s+(?:with\s+)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:from|by)\s+(.+?)(?=\s+(?:on|ref|reference|available|current|closing|a/c)\b|[.;]|$)""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        ),
        Regex(
            """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?credited(?:\s+to\s+(?:your\s+)?account)?\s+(?:from|by)\s+(.+?)(?=\s+(?:on|ref|reference|available|current|closing|a/c)\b|[.;]|$)""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        ),
        Regex(
            """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?received\s+from\s+(.+?)(?=\s+(?:on|ref|reference|available|current|closing|a/c)\b|[.;]|$)""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        )
    )
    private val receivedPhoneRegex = Regex(
        """received\s+(?:Ksh|KES)\s*[0-9,]+(?:\.\d{1,2})?\s+from\s+.+?\s+((?:\+?254|0)\d{9})\b""",
        RegexOption.IGNORE_CASE
    )
    private val cashWithdrawalRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+withdrawn\s+from\s+(.+?)(?=\s+at\s+|\s+on\s+\d|\.\s*(?:New\s+)?[A-Za-z]|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val withdrawalCounterpartyRegex = Regex(
        """withdrawn\s+from\s+.+?\s+at\s+(.+?)(?=\s+on\s+\d|\.\s*(?:New\s+)?[A-Za-z]|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val cashDepositRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+deposited\s+(?:into|to)\s+(?:your\s+)?(.+?)(?=\s+on\s+\d|\.\s*(?:New\s+)?[A-Za-z]|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val depositCounterpartyRegex = Regex(
        """deposited\s+(?:into|to)\s+(?:your\s+)?[A-Za-z0-9&'’.-]+(?:\s+[A-Za-z0-9&'’.-]+){0,4}\s+at\s+(.+?)(?=\s+on\s+\d|\.\s*(?:New\s+)?[A-Za-z]|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val airtimePurchaseRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(airtime)\s+(?:purchased|bought)\b""",
        RegexOption.IGNORE_CASE
    )

    private val reversalOfTransactionRegex = Regex(
        """\breversal\s+of\s+(?:transaction\s+)?([A-Z0-9]{10})\s+(?:of|for)\s+(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val transactionReversedRegex = Regex(
        """\b(?:transaction\s+)?([A-Z0-9]{10})\s+(?:of\s+)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?).{0,80}?\b(?:has\s+been\s+)?reversed\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val reversalReferenceRegex = Regex(
        """\breversal\s+of\s+(?:transaction\s+)?([A-Z0-9]{10})\b""",
        RegexOption.IGNORE_CASE
    )
    private val reversedTransactionReferenceRegex = Regex(
        """\b(?:transaction\s+)?([A-Z0-9]{10})\s+has\s+been\s+(?:successfully\s+)?reversed\b""",
        RegexOption.IGNORE_CASE
    )
    private val reversalCreditedAmountRegex = Regex(
        """\breversed\b.*?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:is|has\s+been)\s+credited\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    fun extract(
        message: NormalizedFinancialMessage
    ): FinancialMessageCandidates {
        val body = message.body
        val actions = buildList {
            transferFromCandidate(body)?.let(::add)
            transferToCandidate(body)?.let(::add)
            accountDebitCandidate(body)?.let(::add)
            transactionCreditedToCandidate(body)
                ?.let(::add)
            paymentCandidate(body)?.let(::add)
            paidToCandidate(body)?.let(::add)
            airtimePurchaseCandidate(body)?.let(::add)
            receiptCandidate(body)?.let(::add)
            creditedReceiptCandidate(body)?.let(::add)
        }

        return FinancialMessageCandidates(
            transactionCode = codeRegex
                .find(body)
                ?.groupCandidate(
                    groupIndex = 1,
                    ruleId = "transaction_code.confirmed"
                ),
            occurredAt = occurredAtRegex
                .find(body)
                ?.groupCandidate(
                    groupIndex = 1,
                    ruleId = "transaction_time.on_at"
                ),
            moneyValues = moneyRegex
                .findAll(body)
                .mapNotNull {
                    it.moneyCandidate(
                        groupIndex = 1,
                        ruleId = "money.amount"
                    )
                }
                .toList(),
            balances = buildList {
                addAll(
                    balanceRegex
                        .findAll(body)
                        .mapNotNull { match ->
                            balanceCandidate(
                                match = match,
                                message = message
                            )
                        }
                )
                availableBalanceCandidate(
                    message = message
                )?.let(::add)
                genericLoanBalanceCandidate(
                    message = message
                )?.let(::add)
            },
            fee = feeRegex
                .find(body)
                ?.moneyCandidate(
                    groupIndex = 1,
                    ruleId = "fee.amount"
                )
                ?.let {
                    FeeCandidate(
                        amountMinor = it.amountMinor,
                        amountSpan = it.span,
                        ruleId = it.ruleId
                    )
                },
            accountDebit =
                accountDebitWithoutCounterpartyCandidate(
                    body
                ),
            reversal = reversalCandidate(body),
            cashMovements = listOfNotNull(
                cashWithdrawalCandidate(body),
                cashDepositCandidate(body)
            ),
            actions = actions,
            debt = DebtCandidateExtractor.extract(
                message
            )
        )
    }

    private fun cashWithdrawalCandidate(
        body: String
    ): CashMovementCandidate? {
        val match = cashWithdrawalRegex.find(body)
            ?: return null
        val amount = match.groups[1] ?: return null
        val account = match.groups[2] ?: return null
        val counterparty =
            withdrawalCounterpartyRegex.find(body)
                ?.groups
                ?.get(1)

        return CashMovementCandidate(
            kind = CashMovementCandidateKind.WITHDRAWAL,
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            accountLabel = account.value.trim(),
            accountSpan = account.range.toSpan(),
            counterpartyLabel =
                counterparty?.value?.trim(),
            counterpartySpan =
                counterparty?.range?.toSpan(),
            ruleId = "cash.withdrawal"
        )
    }

    private fun cashDepositCandidate(
        body: String
    ): CashMovementCandidate? {
        val match = cashDepositRegex.find(body)
            ?: return null
        val amount = match.groups[1] ?: return null
        val account = match.groups[2] ?: return null
        val counterparty =
            depositCounterpartyRegex.find(body)
                ?.groups
                ?.get(1)

        return CashMovementCandidate(
            kind = CashMovementCandidateKind.DEPOSIT,
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            accountLabel = account.value.trim(),
            accountSpan = account.range.toSpan(),
            counterpartyLabel =
                counterparty?.value?.trim(),
            counterpartySpan =
                counterparty?.range?.toSpan(),
            ruleId = "cash.deposit"
        )
    }

    private fun reversalCandidate(
        body: String
    ): ReversalCandidate? {
        val directMatch =
            reversalOfTransactionRegex.find(body)
                ?: transactionReversedRegex.find(body)

        if (directMatch != null) {
            val referenced =
                directMatch.groups[1] ?: return null
            val amount =
                directMatch.groups[2] ?: return null

            return ReversalCandidate(
                amountMinor = amountToMinor(amount.value),
                amountSpan = amount.range.toSpan(),
                referencedTransactionCode =
                    referenced.value.uppercase(),
                referencedCodeSpan =
                    referenced.range.toSpan(),
                ruleId = "reversal.transaction"
            )
        }

        val referenced =
            reversalReferenceRegex.find(body)
                ?.groups?.get(1)
                ?: reversedTransactionReferenceRegex
                    .find(body)
                    ?.groups?.get(1)
                ?: return null
        val creditedAmount =
            reversalCreditedAmountRegex.find(body)
                ?.groups?.get(1)
                ?: return null

        return ReversalCandidate(
            amountMinor =
                amountToMinor(creditedAmount.value),
            amountSpan =
                creditedAmount.range.toSpan(),
            referencedTransactionCode =
                referenced.value.uppercase(),
            referencedCodeSpan =
                referenced.range.toSpan(),
            ruleId =
                "reversal.transaction.credited_after_reversal"
        )
    }

    private fun balanceCandidate(
        match: MatchResult,
        message: NormalizedFinancialMessage
    ): BalanceCandidate? {
        val account = match.groups[1] ?: return null
        val amount = match.groups[2] ?: return null
        val rawAccount = account.value.trim()
        val normalizedAccount =
            rawAccount.uppercase()
                .replace(
                    Regex("""\s+"""),
                    " "
                )
        if (
            normalizedAccount in
                GENERIC_BALANCE_ACCOUNT_LABELS
        ) {
            return null
        }

        val sourceScopedLoan =
            normalizedAccount == "LOAN" ||
                normalizedAccount.endsWith(
                    " LOAN"
                )

        val sourceLooksMshwari =
            message.title
                ?.uppercase()
                ?.replace(
                    Regex("""[^A-Z0-9]+"""),
                    " "
                )
                ?.contains("M SHWARI") ==
                true ||
                message.body
                    .uppercase()
                    .replace(
                        Regex("""[^A-Z0-9]+"""),
                        " "
                    )
                    .contains("M SHWARI")

        val sourceScopedLockSavings =
            sourceLooksMshwari &&
                normalizedAccount in
                    setOf(
                        "LOCK SAVING",
                        "LOCK SAVINGS",
                        "LOCK SAVINGS ACCOUNT"
                    )

        val contextualLiability =
            if (sourceScopedLoan) {
                FinancialProviderRegistry
                    .knownLiabilityInContext(
                        rawLabel = rawAccount,
                        title = message.title,
                        body = message.body
                    )
            } else {
                null
            }

        val accountLabel =
            when {
                sourceScopedLoan &&
                    contextualLiability != null ->
                    contextualLiability.displayName

                sourceScopedLoan ->
                    message.title
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank() &&
                                FinancialProviderRegistry
                                    .resolveMessageSourceAccount(
                                        it
                                    )
                                    ?.providerKey !=
                                "SAFARICOM_MPESA"
                        }
                        ?.let { title ->
                            if (
                                title.endsWith(
                                    "loan",
                                    ignoreCase = true
                                )
                            ) {
                                title
                            } else {
                                "$title Loan"
                            }
                        }
                        ?: "Loan"

                sourceScopedLockSavings ->
                    "M-Shwari Lock Savings"

                else -> rawAccount
            }

        return BalanceCandidate(
            rawAccountLabel = accountLabel,
            accountSpan = account.range.toSpan(),
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            ruleId =
                if (sourceScopedLoan) {
                    "balance.sender_scoped_loan"
                } else {
                    "balance.account_amount"
                }
        )
    }

    private fun genericLoanBalanceCandidate(
        message: NormalizedFinancialMessage
    ): BalanceCandidate? {
        val match =
            genericLoanBalanceRegex
                .find(message.body)
                ?: return null
        val amount =
            match.groups[1]
                ?: return null
        val knownLiability =
            FinancialProviderRegistry
                .knownLiabilityInContext(
                    rawLabel = "Loan",
                    title = message.title,
                    body = message.body
                )
        val sourceTitle =
            message.title
                ?.trim()
                ?.takeIf {
                    it.isNotBlank() &&
                        FinancialProviderRegistry
                            .resolveMessageSourceAccount(
                                it
                            )
                            ?.providerKey !=
                        "SAFARICOM_MPESA"
                }
        val accountLabel =
            knownLiability
                ?.displayName
                ?: sourceTitle
                    ?.let { title ->
                        if (
                            title.endsWith(
                                "loan",
                                ignoreCase = true
                            )
                        ) {
                            title
                        } else {
                            "$title Loan"
                        }
                    }
                ?: "Loan"

        return BalanceCandidate(
            rawAccountLabel =
                accountLabel,
            accountSpan = null,
            amountMinor =
                amountToMinor(
                    amount.value
                ),
            amountSpan =
                amount.range.toSpan(),
            ruleId =
                "balance.generic_loan"
        )
    }

    private fun availableBalanceCandidate(
        message: NormalizedFinancialMessage
    ): BalanceCandidate? {
        val match =
            availableBalanceRegex.find(message.body)
                ?: return null
        val amount = match.groups[1]
            ?: return null
        val title = message.title
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Account"

        return BalanceCandidate(
            rawAccountLabel = title,
            accountSpan = null,
            amountMinor = amountToMinor(
                amount.value
            ),
            amountSpan = amount.range.toSpan(),
            ruleId =
                "balance.message_source_available"
        )
    }

    private fun transferFromCandidate(
        body: String
    ): FinancialActionCandidate? {
        val explicitAccountMatch =
            transferFromRegex.find(body)
                ?: receivedFromAccountRegex.find(body)
        if (explicitAccountMatch != null) {
            return actionCandidate(
                match = explicitAccountMatch,
                kind =
                    FinancialActionCandidateKind
                        .TRANSFER_FROM_ACCOUNT,
                ruleId =
                    "transfer_from.explicit_account"
            )
        }

        val namedMatch =
            transferFromNamedSourceRegex.find(body)
                ?: return null
        return actionCandidate(
            match = namedMatch,
            kind =
                FinancialActionCandidateKind
                    .TRANSFER_FROM_ACCOUNT,
            ruleId = "transfer_from.named_source"
        )
    }

    private fun transferToCandidate(
        body: String
    ): FinancialActionCandidate? {
        val explicitAccountMatch =
            transferToRegex.find(body)
        if (explicitAccountMatch != null) {
            return actionCandidate(
                match = explicitAccountMatch,
                kind =
                    FinancialActionCandidateKind
                        .TRANSFER_TO_ACCOUNT,
                ruleId =
                    "transfer_to.explicit_account"
            )
        }

        val namedMatch =
            transferToNamedDestinationRegex.find(body)
                ?: return null
        return actionCandidate(
            match = namedMatch,
            kind =
                FinancialActionCandidateKind
                    .TRANSFER_TO_ACCOUNT,
            ruleId =
                "transfer_to.named_destination"
        )
    }

    private fun paymentCandidate(
        body: String
    ): FinancialActionCandidate? {
        val amount = sentAmountRegex.find(body)
            ?.groups
            ?.get(1)
            ?: return null
        val party = sentPartyRegex.find(body)
            ?.groups
            ?.get(1)
            ?: return null

        return FinancialActionCandidate(
            kind =
                FinancialActionCandidateKind
                    .PAYMENT_SENT_TO,
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            partyLabel =
                cleanPersonPartyLabel(
                    party.value
                ),
            partySpan = party.range.toSpan(),
            partyPhone = sentPhoneRegex.find(body)
                ?.groups
                ?.get(1)
                ?.value
                ?.let(::normalizeKenyanPhone),
            ruleId = "payment.sent_to"
        )
    }

    private fun paidToCandidate(
        body: String
    ): FinancialActionCandidate? {
        val amount = paidAmountRegex.find(body)
            ?.groups
            ?.get(1)
            ?: return null
        val party = paidPartyRegex.find(body)
            ?.groups
            ?.get(1)
            ?: return null
        val accountReference =
            paidAccountReferenceRegex.find(body)
                ?.groups
                ?.get(1)

        return FinancialActionCandidate(
            kind =
                FinancialActionCandidateKind
                    .PAYMENT_PAID_TO,
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            partyLabel = party.value.trim(),
            partySpan = party.range.toSpan(),
            partyPhone = null,
            accountReference =
                accountReference?.value?.trim(),
            accountReferenceSpan =
                accountReference?.range?.toSpan(),
            ruleId = "payment.paid_to"
        )
    }

    private fun accountDebitWithoutCounterpartyCandidate(
        body: String
    ): AccountDebitCandidate? {
        val indexedMatch =
            accountDebitWithoutCounterpartyRegexes
                .withIndex()
                .asSequence()
                .mapNotNull {
                    (index, regex) ->
                    regex.find(body)
                        ?.let {
                            index to it
                        }
                }
                .firstOrNull()
                ?: return null
        val (index, match) =
            indexedMatch
        val amountGroupIndex =
            if (index == 0) {
                2
            } else {
                1
            }
        val accountGroupIndex =
            if (index == 0) {
                1
            } else {
                2
            }
        val amount =
            match.groups[
                amountGroupIndex
            ] ?: return null
        val accountReference =
            match.groups[
                accountGroupIndex
            ]

        return AccountDebitCandidate(
            amountMinor =
                amountToMinor(
                    amount.value
                ),
            amountSpan =
                amount.range.toSpan(),
            accountReference =
                accountReference
                    ?.value
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    ),
            accountReferenceSpan =
                accountReference
                    ?.range
                    ?.toSpan(),
            ruleId =
                "payment.account_debit"
        )
    }

    private fun accountDebitCandidate(
        body: String
    ): FinancialActionCandidate? {
        val match = accountDebitToRegexes
            .asSequence()
            .mapNotNull { it.find(body) }
            .firstOrNull()
            ?: return null

        return actionCandidate(
            match = match,
            kind =
                FinancialActionCandidateKind
                    .ACCOUNT_DEBIT_TO,
            ruleId = "payment.account_debit_to",
            cleanPersonParty = true
        )
    }

    private fun transactionCreditedToCandidate(
        body: String
    ): FinancialActionCandidate? {
        val match =
            transactionCreditedToRegex.find(body)
                ?: return null

        return actionCandidate(
            match = match,
            kind =
                FinancialActionCandidateKind
                    .TRANSACTION_CREDITED_TO,
            ruleId =
                "payment.transaction_credited_to",
            partyPhone =
                transactionCreditedToPhoneRegex
                    .find(body)
                    ?.groups
                    ?.get(1)
                    ?.value
                    ?.let(::normalizeKenyanPhone),
            cleanPersonParty = true
        )
    }

    private fun airtimePurchaseCandidate(
        body: String
    ): FinancialActionCandidate? {
        val match = airtimePurchaseRegex.find(body)
            ?: return null
        val amount = match.groups[1] ?: return null
        val service = match.groups[2] ?: return null

        return FinancialActionCandidate(
            kind =
                FinancialActionCandidateKind
                    .AIRTIME_PURCHASE,
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            partyLabel = "Airtime",
            partySpan = service.range.toSpan(),
            partyPhone = null,
            ruleId = "payment.airtime"
        )
    }

    private fun receiptCandidate(
        body: String
    ): FinancialActionCandidate? {
        val match = receiptRegex.find(body)
            ?: return null
        return actionCandidate(
            match = match,
            kind =
                FinancialActionCandidateKind.RECEIPT_FROM,
            ruleId = "receipt.received_from",
            partyPhone = receivedPhoneRegex
                .find(body)
                ?.groups
                ?.get(1)
                ?.value
                ?.let(::normalizeKenyanPhone),
            cleanPersonParty = true
        )
    }

    private fun creditedReceiptCandidate(
        body: String
    ): FinancialActionCandidate? {
        val match = creditedFromRegexes
            .asSequence()
            .mapNotNull { it.find(body) }
            .firstOrNull()
            ?: return null

        return actionCandidate(
            match = match,
            kind =
                FinancialActionCandidateKind
                    .RECEIPT_CREDITED_FROM,
            ruleId =
                "receipt.credited_from",
            cleanPersonParty = true
        )
    }

    private fun actionCandidate(
        match: MatchResult,
        kind: FinancialActionCandidateKind,
        ruleId: String,
        partyPhone: String? = null,
        cleanPersonParty: Boolean = false
    ): FinancialActionCandidate? {
        val amount = match.groups[1] ?: return null
        val party = match.groups[2] ?: return null
        return FinancialActionCandidate(
            kind = kind,
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            partyLabel =
                if (cleanPersonParty) {
                    cleanPersonPartyLabel(
                        party.value
                    )
                } else {
                    party.value.trim()
                },
            partySpan = party.range.toSpan(),
            partyPhone = partyPhone,
            ruleId = ruleId
        )
    }

    private fun MatchResult.groupCandidate(
        groupIndex: Int,
        ruleId: String
    ): TextValueCandidate? =
        groups[groupIndex]?.let { group ->
            TextValueCandidate(
                value = group.value,
                span = group.range.toSpan(),
                ruleId = ruleId
            )
        }

    private fun MatchResult.moneyCandidate(
        groupIndex: Int,
        ruleId: String
    ): MoneyValueCandidate? =
        groups[groupIndex]?.let { group ->
            MoneyValueCandidate(
                amountMinor = amountToMinor(group.value),
                span = group.range.toSpan(),
                ruleId = ruleId
            )
        }

    private fun IntRange.toSpan(): MessageSpan =
        MessageSpan(
            startOffset = first,
            endOffsetExclusive = last + 1
        )

    private fun cleanPersonPartyLabel(
        raw: String
    ): String =
        raw.trim()
            .replace(
                trailingMaskedPhoneRegex,
                ""
            )
            .trim()
            .trimEnd('.', ',')

    private fun normalizeKenyanPhone(
        raw: String
    ): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.startsWith("254") &&
                digits.length == 12 ->
                "+" + digits
            digits.startsWith("0") &&
                digits.length == 10 ->
                "+254" + digits.drop(1)
            else -> digits
        }
    }

    private fun amountToMinor(
        raw: String
    ): Long =
        raw.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
}
