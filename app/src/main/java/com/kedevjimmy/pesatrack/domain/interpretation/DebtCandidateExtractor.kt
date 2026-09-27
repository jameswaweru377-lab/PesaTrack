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
    val ruleId: String
)

data class DebtRepaymentCandidate(
    val amountMinor: Long,
    val amountSpan: MessageSpan,
    val fundingAccountLabel: String,
    val fundingAccountSpan: MessageSpan,
    val liabilityLabel: String,
    val liabilitySpan: MessageSpan,
    val completion: DebtCompletion,
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
