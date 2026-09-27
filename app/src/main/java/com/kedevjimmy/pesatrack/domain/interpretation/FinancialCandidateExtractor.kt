package com.kedevjimmy.pesatrack.domain.interpretation

import java.math.RoundingMode

enum class FinancialActionCandidateKind {
    TRANSFER_FROM_ACCOUNT,
    TRANSFER_TO_ACCOUNT,
    PAYMENT_SENT_TO,
    PAYMENT_PAID_TO,
    RECEIPT_FROM
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
    val accountSpan: MessageSpan,
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val ruleId: String
)

data class FeeCandidate(
    val amountMinor: Long,
    val amountSpan: MessageSpan,
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

data class FinancialMessageCandidates(
    val transactionCode: TextValueCandidate?,
    val occurredAt: TextValueCandidate?,
    val balances: List<BalanceCandidate>,
    val fee: FeeCandidate?,
    val actions: List<FinancialActionCandidate>,
    val debt: DebtMessageCandidates
)

object FinancialCandidateExtractor {
    private val codeRegex = Regex(
        """\b([A-Z0-9]{10})\s+Confirmed\b""",
        RegexOption.IGNORE_CASE
    )
    private val occurredAtRegex = Regex(
        """\bon\s+(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\s+at\s+\d{1,2}:\d{2}\s*(?:AM|PM)?)""",
        RegexOption.IGNORE_CASE
    )
    private val balanceRegex = Regex(
        """(?:^|[.!?]\s+)(?:New\s+|Your\s+)?([A-Za-z][A-Za-z0-9&'’.-]*(?:\s+[A-Za-z0-9&'’.-]+){0,4})\s+balance\s+is\s*(?:(?:Ksh|KES)\s*)?([0-9,]+(?:\.\d{1,2})?)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )
    private val feeRegex = Regex(
        """(?:transaction\s+(?:cost|fee)|access\s+fee|charge)\s*[,.:]?\s*(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val transferFromRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:transferred|withdrawn|moved)\s+from\s+(.+?)\s+account\b""",
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
    private val receiptRegex = Regex(
        """(?:you\s+have\s+)?received\s+(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+from\s+(.+?)(?=\s+(?:\+?254|0)\d{9}\b|\s+on\s+\d|\.\s*(?:New\s+)?M-PESA|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )
    private val receivedPhoneRegex = Regex(
        """received\s+(?:Ksh|KES)\s*[0-9,]+(?:\.\d{1,2})?\s+from\s+.+?\s+((?:\+?254|0)\d{9})\b""",
        RegexOption.IGNORE_CASE
    )

    fun extract(
        message: NormalizedFinancialMessage
    ): FinancialMessageCandidates {
        if (!message.hasCurrencyHint) {
            return FinancialMessageCandidates(
                transactionCode = null,
                occurredAt = null,
                balances = emptyList(),
                fee = null,
                actions = emptyList(),
                debt = DebtMessageCandidates(
                    drawdown = null,
                    repayment = null
                )
            )
        }

        val body = message.body
        val actions = buildList {
            transferFromCandidate(body)?.let(::add)
            transferToCandidate(body)?.let(::add)
            paymentCandidate(body)?.let(::add)
            paidToCandidate(body)?.let(::add)
            receiptCandidate(body)?.let(::add)
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
            balances = balanceRegex
                .findAll(body)
                .mapNotNull(::balanceCandidate)
                .toList(),
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
            actions = actions,
            debt = DebtCandidateExtractor.extract(
                message
            )
        )
    }

    private fun balanceCandidate(
        match: MatchResult
    ): BalanceCandidate? {
        val account = match.groups[1] ?: return null
        val amount = match.groups[2] ?: return null
        return BalanceCandidate(
            rawAccountLabel = account.value,
            accountSpan = account.range.toSpan(),
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            ruleId = "balance.account_amount"
        )
    }

    private fun transferFromCandidate(
        body: String
    ): FinancialActionCandidate? {
        val match =
            transferFromRegex.find(body)
                ?: receivedFromAccountRegex.find(body)
                ?: return null
        return actionCandidate(
            match = match,
            kind =
                FinancialActionCandidateKind
                    .TRANSFER_FROM_ACCOUNT,
            ruleId = "transfer_from.account"
        )
    }

    private fun transferToCandidate(
        body: String
    ): FinancialActionCandidate? {
        val match = transferToRegex.find(body)
            ?: return null
        return actionCandidate(
            match = match,
            kind =
                FinancialActionCandidateKind
                    .TRANSFER_TO_ACCOUNT,
            ruleId = "transfer_to.account"
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
            partyLabel = party.value.trim(),
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
                ?.let(::normalizeKenyanPhone)
        )
    }

    private fun actionCandidate(
        match: MatchResult,
        kind: FinancialActionCandidateKind,
        ruleId: String,
        partyPhone: String? = null
    ): FinancialActionCandidate? {
        val amount = match.groups[1] ?: return null
        val party = match.groups[2] ?: return null
        return FinancialActionCandidate(
            kind = kind,
            amountMinor = amountToMinor(amount.value),
            amountSpan = amount.range.toSpan(),
            partyLabel = party.value.trim(),
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
