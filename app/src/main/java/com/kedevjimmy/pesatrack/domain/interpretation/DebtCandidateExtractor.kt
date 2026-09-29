package com.kedevjimmy.pesatrack.domain.interpretation

import java.math.RoundingMode

data class DebtDrawdownCandidate(
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val liabilityLabel: String,
    val liabilitySpan: MessageSpan,
    val outstandingAfterMinor: Long?,
    val outstandingSpan: MessageSpan?,
    val availableCreditAfterMinor: Long?,
    val availableCreditSpan: MessageSpan?,
    val proceedsAccountLabel: String? = null,
    val proceedsAccountSpan: MessageSpan? = null,
    val amountIsNetProceeds: Boolean = false,
    val ruleId: String
)

data class DebtRepaymentCandidate(
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val fundingAccountLabel: String?,
    val fundingAccountSpan: MessageSpan?,
    val liabilityLabel: String,
    val liabilitySpan: MessageSpan?,
    val completion: DebtCompletion,
    val outstandingAfterMinor: Long? = null,
    val outstandingSpan: MessageSpan? = null,
    val availableCreditAfterMinor: Long?,
    val availableCreditSpan: MessageSpan?,
    val ruleId: String
)

data class DebtMessageCandidates(
    val drawdown: DebtDrawdownCandidate?,
    val repayment: DebtRepaymentCandidate?
)

object DebtCandidateExtractor {
    private val productAmountRegex = Regex(
        """(?:^|[.!?]\s+)([A-Za-z][A-Za-z0-9&'’-]*(?:\s+[A-Za-z0-9&'’-]+){0,5})\s+amount\s*(?:is\s*)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )

    private val outstandingRegex = Regex(
        """(?:^|[.!?]\s+)(?:Total\s+)?([A-Za-z][A-Za-z0-9&'’-]*(?:\s+[A-Za-z0-9&'’-]+){0,5})\s+outstanding\s+(?:amount|balance)\s*(?:is\s*)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )

    private val availableCreditRegex = Regex(
        """(?:^|[.!?]\s+)(?:Your\s+)?available\s+([A-Za-z][A-Za-z0-9&'’-]*(?:\s+[A-Za-z0-9&'’-]+){0,5})\s+(?:balance\s+)?(?:limit|credit\s+limit)\s*(?:is\s*)?(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )

    private val repaymentRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+from\s+your\s+([A-Za-z][A-Za-z0-9&'’-]*(?:\s+[A-Za-z0-9&'’-]+){0,5})\s+has\s+been\s+used\s+to\s+(?:(partially|fully)\s+)?pay\s+your\s+outstanding\s+([A-Za-z][A-Za-z0-9&'’-]*(?:\s+[A-Za-z0-9&'’-]+){0,5})(?=\.|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )

    private val genericLoanRepaymentRegexes = listOf(
        Regex(
            """(?:loan\s+)?repayment(?:\s+of)?\s+(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:received|successful|processed|paid)\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:loan\s+)?repayment\s+(?:has\s+been\s+)?(?:received|successful|processed|paid)\b""",
            RegexOption.IGNORE_CASE
        )
    )

    private val genericLoanBalanceRegex = Regex(
        """(?:^|[.!?]\s+)(?:Your\s+)?([A-Za-z][A-Za-z0-9&'’-]*(?:\s+[A-Za-z0-9&'’-]+){0,5}\s+loan)\s+(?:outstanding\s+)?balance\s*(?:is|:)?\s*(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )

    private val approvedLoanRegex = Regex(
        """(?:^|[.!?]\s+)(?:Your\s+)?([A-Za-z][A-Za-z0-9&'’-]*(?:\s+[A-Za-z0-9&'’-]+){0,5}\s+loan)\s+has\s+been\s+(?:approved|disbursed)\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )

    private val loanProceedsRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)(?:\s+less\s+[^.!?]{1,80})?\s+has\s+been\s+(?:deposited|credited|disbursed)\s+(?:into|to)\s+(?:your\s+)?(.+?)(?=\s+account\b|[.!?]|$)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    fun extract(
        message: NormalizedFinancialMessage
    ): DebtMessageCandidates {
        if (!message.hasCurrencyHint) {
            return DebtMessageCandidates(
                drawdown = null,
                repayment = null
            )
        }

        val body = message.body
        val available =
            availableCreditRegex.find(body)
        val repayment =
            repaymentRegex.find(body)
                ?.let { match ->
                    val amount =
                        match.groups[1]
                            ?: return@let null
                    val funding =
                        match.groups[2]
                            ?: return@let null
                    val completionText =
                        match.groups[3]?.value
                    val liability =
                        match.groups[4]
                            ?: return@let null
                    val availableMatch =
                        available?.takeIf {
                            sameFinancialLabel(
                                liability.value,
                                it.groups[1]?.value
                            )
                        }
                    DebtRepaymentCandidate(
                        amountMinor =
                            amountToMinor(
                                amount.value
                            ),
                        amountSpan =
                            amount.range.toSpan(),
                        fundingAccountLabel =
                            funding.value.trim(),
                        fundingAccountSpan =
                            funding.range.toSpan(),
                        liabilityLabel =
                            liability.value.trim(),
                        liabilitySpan =
                            liability.range.toSpan(),
                        completion = when (
                            completionText
                                ?.lowercase()
                        ) {
                            "partially" ->
                                DebtCompletion.PARTIAL
                            "fully" ->
                                DebtCompletion.FULL
                            else ->
                                DebtCompletion.UNKNOWN
                        },
                        availableCreditAfterMinor =
                            availableMatch
                                ?.groups
                                ?.get(2)
                                ?.value
                                ?.let(::amountToMinor),
                        availableCreditSpan =
                            availableMatch
                                ?.groups
                                ?.get(2)
                                ?.range
                                ?.toSpan(),
                        ruleId =
                            "debt.repayment.outstanding"
                    )
                }

        if (repayment != null) {
            return DebtMessageCandidates(
                drawdown = null,
                repayment = repayment
            )
        }

        val genericRepaymentMatch =
            genericLoanRepaymentRegexes
                .asSequence()
                .mapNotNull { it.find(body) }
                .firstOrNull()
        if (genericRepaymentMatch != null) {
            val amount =
                genericRepaymentMatch.groups[1]
            val balanceMatch =
                genericLoanBalanceRegex.find(body)
            val liability =
                balanceMatch?.groups?.get(1)
                    ?.value
                    ?.trim()
                    ?: message.title
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "$it Loan" }

            if (amount != null && liability != null) {
                return DebtMessageCandidates(
                    drawdown = null,
                    repayment =
                        DebtRepaymentCandidate(
                            amountMinor =
                                amountToMinor(amount.value),
                            amountSpan =
                                amount.range.toSpan(),
                            fundingAccountLabel = null,
                            fundingAccountSpan = null,
                            liabilityLabel = liability,
                            liabilitySpan =
                                balanceMatch
                                    ?.groups
                                    ?.get(1)
                                    ?.range
                                    ?.toSpan(),
                            completion =
                                if (
                                    body.contains(
                                        "fully paid",
                                        ignoreCase = true
                                    ) ||
                                    body.contains(
                                        "loan cleared",
                                        ignoreCase = true
                                    )
                                ) {
                                    DebtCompletion.FULL
                                } else {
                                    DebtCompletion.UNKNOWN
                                },
                            outstandingAfterMinor =
                                balanceMatch
                                    ?.groups
                                    ?.get(2)
                                    ?.value
                                    ?.let(::amountToMinor),
                            outstandingSpan =
                                balanceMatch
                                    ?.groups
                                    ?.get(2)
                                    ?.range
                                    ?.toSpan(),
                            availableCreditAfterMinor =
                                null,
                            availableCreditSpan = null,
                            ruleId =
                                "debt.repayment.generic"
                        )
                )
            }
        }

        val approvedLoan =
            approvedLoanRegex.find(body)
        val loanProceeds =
            loanProceedsRegex.find(body)

        if (
            approvedLoan != null &&
            loanProceeds != null
        ) {
            val liability =
                approvedLoan.groups[1]
                    ?: return DebtMessageCandidates(
                        null,
                        null
                    )
            val amount =
                loanProceeds.groups[1]
                    ?: return DebtMessageCandidates(
                        null,
                        null
                    )
            val destination =
                loanProceeds.groups[2]
                    ?: return DebtMessageCandidates(
                        null,
                        null
                    )

            return DebtMessageCandidates(
                drawdown =
                    DebtDrawdownCandidate(
                        amountMinor =
                            amountToMinor(amount.value),
                        amountSpan =
                            amount.range.toSpan(),
                        liabilityLabel =
                            liability.value.trim(),
                        liabilitySpan =
                            liability.range.toSpan(),
                        outstandingAfterMinor = null,
                        outstandingSpan = null,
                        availableCreditAfterMinor =
                            available
                                ?.groups
                                ?.get(2)
                                ?.value
                                ?.let(::amountToMinor),
                        availableCreditSpan =
                            available
                                ?.groups
                                ?.get(2)
                                ?.range
                                ?.toSpan(),
                        proceedsAccountLabel =
                            destination.value.trim(),
                        proceedsAccountSpan =
                            destination.range.toSpan(),
                        amountIsNetProceeds = true,
                        ruleId =
                            "debt.drawdown.approved_disbursement"
                    ),
                repayment = null
            )
        }

        val outstanding =
            outstandingRegex.find(body)
        val primary = productAmountRegex
            .findAll(body)
            .firstOrNull { match ->
                val label =
                    match.groups[1]
                        ?.value
                        .orEmpty()
                !label.contains(
                    "outstanding",
                    ignoreCase = true
                ) &&
                    !label.contains(
                        "balance",
                        ignoreCase = true
                    ) &&
                    !label.contains(
                        "limit",
                        ignoreCase = true
                    )
            }

        val drawdown = if (
            primary != null &&
            outstanding != null &&
            sameFinancialLabel(
                primary.groups[1]?.value,
                outstanding.groups[1]?.value
            )
        ) {
            val liability =
                primary.groups[1]
                    ?: return DebtMessageCandidates(
                        null,
                        null
                    )
            val amount =
                primary.groups[2]
                    ?: return DebtMessageCandidates(
                        null,
                        null
                    )
            val outstandingAmount =
                outstanding.groups[2]
            val availableMatch =
                available?.takeIf {
                    sameFinancialLabel(
                        liability.value,
                        it.groups[1]?.value
                    )
                }

            DebtDrawdownCandidate(
                amountMinor =
                    amountToMinor(amount.value),
                amountSpan = amount.range.toSpan(),
                liabilityLabel =
                    liability.value.trim(),
                liabilitySpan =
                    liability.range.toSpan(),
                outstandingAfterMinor =
                    outstandingAmount
                        ?.value
                        ?.let(::amountToMinor),
                outstandingSpan =
                    outstandingAmount
                        ?.range
                        ?.toSpan(),
                availableCreditAfterMinor =
                    availableMatch
                        ?.groups
                        ?.get(2)
                        ?.value
                        ?.let(::amountToMinor),
                availableCreditSpan =
                    availableMatch
                        ?.groups
                        ?.get(2)
                        ?.range
                        ?.toSpan(),
                ruleId =
                    "debt.drawdown.outstanding"
            )
        } else {
            null
        }

        return DebtMessageCandidates(
            drawdown = drawdown,
            repayment = null
        )
    }

    private fun sameFinancialLabel(
        first: String?,
        second: String?
    ): Boolean {
        if (
            first.isNullOrBlank() ||
            second.isNullOrBlank()
        ) {
            return false
        }
        val left = normalizeLabel(first)
        val right = normalizeLabel(second)
        return left == right ||
            left.startsWith("$right ") ||
            right.startsWith("$left ")
    }

    private fun normalizeLabel(
        raw: String
    ): String =
        raw.uppercase()
            .replace(
                Regex("""[^A-Z0-9]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
            .removeSuffix(" BALANCE")
            .trim()

    private fun IntRange.toSpan(): MessageSpan =
        MessageSpan(
            startOffset = first,
            endOffsetExclusive = last + 1
        )

    private fun amountToMinor(
        raw: String
    ): Long =
        raw.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(
                0,
                RoundingMode.HALF_UP
            )
            .longValueExact()
}
