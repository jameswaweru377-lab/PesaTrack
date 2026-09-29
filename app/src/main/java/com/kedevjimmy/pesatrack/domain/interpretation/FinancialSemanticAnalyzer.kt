package com.kedevjimmy.pesatrack.domain.interpretation

import java.math.RoundingMode

enum class FinancialSemanticIntent {
    PAYMENT,
    RECEIPT,
    TRANSFER,
    LOAN_DISBURSEMENT,
    LOAN_REPAYMENT,
    LOAN_BALANCE,
    BALANCE_UPDATE,
    REVERSAL,
    REFUND,
    FEE,
    INVESTMENT_ORDER,
    INVESTMENT_EXECUTED,
    CONFIRMATION_ONLY,
    UNKNOWN
}

enum class FinancialSemanticRole {
    NEW_EVENT,
    PENDING_ACTION,
    CONFIRMATION,
    REMINDER,
    INFORMATION,
    NEEDS_REVIEW
}

enum class SemanticAmountRole {
    PRIMARY,
    BALANCE,
    OUTSTANDING,
    FEE,
    INVESTMENT_VALUE,
    UNKNOWN
}

enum class SemanticAccountRole {
    SOURCE,
    DESTINATION,
    OBSERVED,
    LIABILITY,
    UNKNOWN
}

data class SemanticAmountFact(
    val amountMinor: Long,
    val role: SemanticAmountRole,
    val span: MessageSpan
)

data class SemanticReferenceFact(
    val value: String,
    val label: String?,
    val span: MessageSpan
)

data class SemanticAccountFact(
    val label: String,
    val role: SemanticAccountRole,
    val span: MessageSpan
)

data class FinancialSemanticAssessment(
    val primaryIntent: FinancialSemanticIntent,
    val intents: Set<FinancialSemanticIntent>,
    val role: FinancialSemanticRole,
    val amounts: List<SemanticAmountFact>,
    val references: List<SemanticReferenceFact>,
    val accounts: List<SemanticAccountFact>,
    val confidence: InterpretationConfidence,
    val ruleIds: Set<String>
) {
    val understood: Boolean
        get() =
            primaryIntent !=
                FinancialSemanticIntent.UNKNOWN
}

/**
 * Provider-agnostic semantic reading of a normalized financial
 * message. This layer does not mutate the ledger. It identifies
 * meaning and facts so deterministic accounting can decide what is
 * safe to persist.
 */
object FinancialSemanticAnalyzer {
    private val moneyRegex = Regex(
        """\bKES\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val reversalRegex = Regex(
        """\b(?:reversal|reversed)\b""",
        RegexOption.IGNORE_CASE
    )

    private val refundRegex = Regex(
        """\b(?:refund|refunded)\b""",
        RegexOption.IGNORE_CASE
    )

    private val loanDisbursementRegex = Regex(
        """(?:\bloan\b.{0,100}\b(?:disbursed|approved)\b|\b(?:disbursed|approved)\b.{0,100}\bloan\b)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val loanRepaymentRegex = Regex(
        """\b(?:loan\s+repayment|repayment\s+of\s+(?:the\s+)?loan|repaid\s+(?:the\s+)?loan|loan\s+(?:has\s+been\s+)?repaid|partially\s+pay\s+(?:your\s+)?(?:outstanding\s+)?loan|fully\s+pay\s+(?:your\s+)?(?:outstanding\s+)?loan)\b""",
        RegexOption.IGNORE_CASE
    )

    private val loanBalanceRegex = Regex(
        """\b(?:total\s+loan\s+balance|loan\s+(?:outstanding\s+)?balance|outstanding\s+loan\s+balance|outstanding\s+amount|amount\s+due|total\s+due)\b""",
        RegexOption.IGNORE_CASE
    )

    private val investmentOrderRegex = Regex(
        """\b(?:buy|sell)\s+order\b""",
        RegexOption.IGNORE_CASE
    )

    private val investmentExecutedRegex = Regex(
        """(?:\b(?:successfully\s+)?(?:bought|sold)\b.{0,100}\b(?:shares?|stocks?|units?)\b|\b(?:shares?|stocks?|units?)\b.{0,100}\b(?:bought|sold|executed)\b)""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val transferRegex = Regex(
        """\b(?:transferred|transfer|moved)\b""",
        RegexOption.IGNORE_CASE
    )

    private val paymentRegex = Regex(
        """\b(?:paid|payment|debited|sent)\b""",
        RegexOption.IGNORE_CASE
    )

    private val receiptRegex = Regex(
        """\b(?:received|credited)\b""",
        RegexOption.IGNORE_CASE
    )

    private val balanceRegex = Regex(
        """\bbalance\b""",
        RegexOption.IGNORE_CASE
    )

    private val feeRegex = Regex(
        """\b(?:fee|fees|charge|charged|transaction\s+cost)\b""",
        RegexOption.IGNORE_CASE
    )

    private val confirmationRegex = Regex(
        """\b(?:processed|completed|successful|successfully|confirmed)\b""",
        RegexOption.IGNORE_CASE
    )

    private val financialNounRegex = Regex(
        """\b(?:transaction|transfer|payment|loan|funds|account|wallet|order)\b""",
        RegexOption.IGNORE_CASE
    )

    private val reminderRegex = Regex(
        """\b(?:due\s+(?:on|by)|repay\s+by|payment\s+due|loan\s+due|repayment\s+due|reminder)\b""",
        RegexOption.IGNORE_CASE
    )

    private val primaryAmountRegexes = listOf(
        Regex(
            """\bloan\s+of\s+KES\s*([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b(?:repayment|repaid|paid)\s+(?:of\s+)?KES\s*([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\bKES\s*([0-9,]+(?:\.\d{1,2})?)\s+(?:has\s+been\s+)?(?:credited|debited|received|sent|paid|transferred|moved|withdrawn|deposited|disbursed)\b""",
            RegexOption.IGNORE_CASE
        )
    )

    private val balanceAmountRegex = Regex(
        """\b(?:available\s+|current\s+|closing\s+|new\s+)?balance\s*(?:is|:)?\s*KES\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val outstandingAmountRegex = Regex(
        """\b(?:loan\s+(?:outstanding\s+)?balance|outstanding\s+(?:loan\s+)?balance|outstanding\s+amount|amount\s+due|total\s+due)\s*(?:is|:)?\s*KES\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val feeAmountRegex = Regex(
        """\b(?:fee|fees|charge|charged|transaction\s+cost)(?:\s+(?:is|of))?\s*[:,.-]?\s*KES\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val investmentValueRegex = Regex(
        """\b(?:valued|value)\s+at\s+KES\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val labeledReferenceRegex = Regex(
        """\b((?:[A-Za-z][A-Za-z0-9&'’.-]*\s+)?(?:ref(?:erence)?(?:\s*(?:no|number))?|txn(?:\s*(?:id|ref))?|transaction\s*(?:id|ref(?:erence)?)?|code))\s*[:#-]?\s*([A-Z0-9][A-Z0-9-]{4,31})\b""",
        RegexOption.IGNORE_CASE
    )

    private val confirmedCodeRegex = Regex(
        """\b([A-Z0-9]{10})\s+Confirmed\b""",
        RegexOption.IGNORE_CASE
    )

    private val sourceAccountRegex = Regex(
        """\bfrom\s+(?:your\s+)?([A-Za-z][A-Za-z0-9&'’.-]*(?:\s+[A-Za-z0-9&'’.-]+){0,5}\s+(?:account|wallet|savings))\b""",
        RegexOption.IGNORE_CASE
    )

    private val destinationAccountRegex = Regex(
        """\bto\s+(?:your\s+)?([A-Za-z][A-Za-z0-9&'’.-]*(?:\s+[A-Za-z0-9&'’.-]+){0,5}\s+(?:account|wallet|savings))\b""",
        RegexOption.IGNORE_CASE
    )

    private val observedAccountRegex = Regex(
        """\b(?:your\s+)?((?:a/?c|account)\s+[A-Za-z0-9*Xx-]{2,}|[A-Za-z][A-Za-z0-9&'’.-]*(?:\s+[A-Za-z0-9&'’.-]+){0,4}\s+(?:account|wallet|savings))\b""",
        RegexOption.IGNORE_CASE
    )

    fun analyze(
        message: NormalizedFinancialMessage
    ): FinancialSemanticAssessment {
        val body = message.body
        val intents =
            linkedSetOf<FinancialSemanticIntent>()
        val rules = linkedSetOf<String>()

        fun addIntent(
            intent: FinancialSemanticIntent,
            ruleId: String
        ) {
            intents += intent
            rules += ruleId
        }

        if (reversalRegex.containsMatchIn(body)) {
            addIntent(
                FinancialSemanticIntent.REVERSAL,
                "semantic.reversal"
            )
        }

        if (refundRegex.containsMatchIn(body)) {
            addIntent(
                FinancialSemanticIntent.REFUND,
                "semantic.refund"
            )
        }

        if (
            loanDisbursementRegex
                .containsMatchIn(body)
        ) {
            addIntent(
                FinancialSemanticIntent
                    .LOAN_DISBURSEMENT,
                "semantic.loan_disbursement"
            )
        }

        if (
            loanRepaymentRegex
                .containsMatchIn(body)
        ) {
            addIntent(
                FinancialSemanticIntent
                    .LOAN_REPAYMENT,
                "semantic.loan_repayment"
            )
        }

        if (loanBalanceRegex.containsMatchIn(body)) {
            addIntent(
                FinancialSemanticIntent.LOAN_BALANCE,
                "semantic.loan_balance"
            )
        }

        if (
            investmentExecutedRegex
                .containsMatchIn(body)
        ) {
            addIntent(
                FinancialSemanticIntent
                    .INVESTMENT_EXECUTED,
                "semantic.investment_executed"
            )
        } else if (
            investmentOrderRegex
                .containsMatchIn(body)
        ) {
            addIntent(
                FinancialSemanticIntent
                    .INVESTMENT_ORDER,
                "semantic.investment_order"
            )
        }

        val hasMoney =
            moneyRegex.containsMatchIn(body)
        val hasFinancialNoun =
            financialNounRegex.containsMatchIn(body)

        if (
            transferRegex.containsMatchIn(body) &&
            (
                hasMoney ||
                    hasFinancialNoun ||
                    message.hasCurrencyHint
                )
        ) {
            addIntent(
                FinancialSemanticIntent.TRANSFER,
                "semantic.transfer"
            )
        }

        if (
            paymentRegex.containsMatchIn(body) &&
            (
                hasMoney ||
                    hasFinancialNoun ||
                    message.hasCurrencyHint
                ) &&
            FinancialSemanticIntent
                .LOAN_REPAYMENT !in intents
        ) {
            addIntent(
                FinancialSemanticIntent.PAYMENT,
                "semantic.payment"
            )
        }

        if (
            receiptRegex.containsMatchIn(body) &&
            (
                hasMoney ||
                    hasFinancialNoun ||
                    message.hasCurrencyHint
                ) &&
            FinancialSemanticIntent.REFUND !in intents
        ) {
            addIntent(
                FinancialSemanticIntent.RECEIPT,
                "semantic.receipt"
            )
        }

        if (
            balanceRegex.containsMatchIn(body) &&
            hasMoney &&
            FinancialSemanticIntent
                .LOAN_BALANCE !in intents
        ) {
            addIntent(
                FinancialSemanticIntent.BALANCE_UPDATE,
                "semantic.balance_update"
            )
        }

        if (
            feeRegex.containsMatchIn(body) &&
            hasMoney
        ) {
            addIntent(
                FinancialSemanticIntent.FEE,
                "semantic.fee"
            )
        }

        val confirmationOnly =
            confirmationRegex.containsMatchIn(body) &&
                hasFinancialNoun &&
                !hasSpecificEventEvidence(
                    intents = intents,
                    hasMoney = hasMoney
                )

        if (confirmationOnly) {
            addIntent(
                FinancialSemanticIntent
                    .CONFIRMATION_ONLY,
                "semantic.confirmation_only"
            )
        }

        val primaryIntent =
            if (confirmationOnly) {
                FinancialSemanticIntent
                    .CONFIRMATION_ONLY
            } else {
                PRIMARY_INTENT_PRIORITY
                    .firstOrNull { it in intents }
                    ?: FinancialSemanticIntent.UNKNOWN
            }

        val role = when {
            primaryIntent ==
                FinancialSemanticIntent.UNKNOWN ->
                FinancialSemanticRole.NEEDS_REVIEW
            primaryIntent ==
                FinancialSemanticIntent
                    .CONFIRMATION_ONLY ->
                FinancialSemanticRole.CONFIRMATION
            reminderRegex.containsMatchIn(body) &&
                primaryIntent in setOf(
                    FinancialSemanticIntent.LOAN_BALANCE,
                    FinancialSemanticIntent.UNKNOWN
                ) ->
                FinancialSemanticRole.REMINDER
            primaryIntent ==
                FinancialSemanticIntent
                    .INVESTMENT_ORDER ->
                FinancialSemanticRole.PENDING_ACTION
            primaryIntent in setOf(
                FinancialSemanticIntent.LOAN_BALANCE,
                FinancialSemanticIntent.BALANCE_UPDATE
            ) ->
                FinancialSemanticRole.INFORMATION
            else ->
                FinancialSemanticRole.NEW_EVENT
        }

        val amounts = extractAmounts(body)
        val references = extractReferences(body)
        val accounts = extractAccounts(body)

        val confidence = when {
            primaryIntent ==
                FinancialSemanticIntent.UNKNOWN ->
                InterpretationConfidence.LOW
            role in setOf(
                FinancialSemanticRole.CONFIRMATION,
                FinancialSemanticRole.REMINDER,
                FinancialSemanticRole.PENDING_ACTION,
                FinancialSemanticRole.INFORMATION
            ) ->
                InterpretationConfidence.HIGH
            amounts.any {
                it.role != SemanticAmountRole.UNKNOWN
            } ->
                InterpretationConfidence.HIGH
            amounts.isNotEmpty() ->
                InterpretationConfidence.MEDIUM
            else ->
                InterpretationConfidence.MEDIUM
        }

        return FinancialSemanticAssessment(
            primaryIntent = primaryIntent,
            intents =
                if (intents.isEmpty()) {
                    setOf(
                        FinancialSemanticIntent.UNKNOWN
                    )
                } else {
                    intents
                },
            role = role,
            amounts = amounts,
            references = references,
            accounts = accounts,
            confidence = confidence,
            ruleIds = rules
        )
    }

    private fun hasSpecificEventEvidence(
        intents: Set<FinancialSemanticIntent>,
        hasMoney: Boolean
    ): Boolean {
        val specific = intents.any {
            it in setOf(
                FinancialSemanticIntent.REVERSAL,
                FinancialSemanticIntent.REFUND,
                FinancialSemanticIntent.LOAN_DISBURSEMENT,
                FinancialSemanticIntent.LOAN_REPAYMENT,
                FinancialSemanticIntent.INVESTMENT_EXECUTED
            )
        }
        if (specific) {
            return true
        }

        return hasMoney &&
            intents.any {
                it in setOf(
                    FinancialSemanticIntent.PAYMENT,
                    FinancialSemanticIntent.RECEIPT,
                    FinancialSemanticIntent.TRANSFER
                )
            }
    }

    private fun extractAmounts(
        body: String
    ): List<SemanticAmountFact> {
        val bySpan =
            linkedMapOf<Pair<Int, Int>, SemanticAmountFact>()

        fun add(
            match: MatchResult,
            groupIndex: Int,
            role: SemanticAmountRole
        ) {
            val group =
                match.groups[groupIndex] ?: return
            val span = group.range.toSpan()
            bySpan[
                span.startOffset to
                    span.endOffsetExclusive
            ] =
                SemanticAmountFact(
                    amountMinor =
                        amountToMinor(group.value),
                    role = role,
                    span = span
                )
        }

        outstandingAmountRegex
            .findAll(body)
            .forEach {
                add(
                    it,
                    1,
                    SemanticAmountRole.OUTSTANDING
                )
            }

        balanceAmountRegex
            .findAll(body)
            .forEach {
                add(
                    it,
                    1,
                    SemanticAmountRole.BALANCE
                )
            }

        feeAmountRegex
            .findAll(body)
            .forEach {
                add(
                    it,
                    1,
                    SemanticAmountRole.FEE
                )
            }

        investmentValueRegex
            .findAll(body)
            .forEach {
                add(
                    it,
                    1,
                    SemanticAmountRole.INVESTMENT_VALUE
                )
            }

        primaryAmountRegexes.forEach { regex ->
            regex.findAll(body).forEach {
                add(
                    it,
                    1,
                    SemanticAmountRole.PRIMARY
                )
            }
        }

        moneyRegex.findAll(body).forEach { match ->
            val group = match.groups[1]
                ?: return@forEach
            val span = group.range.toSpan()
            val key =
                span.startOffset to
                    span.endOffsetExclusive
            if (key !in bySpan) {
                bySpan[key] =
                    SemanticAmountFact(
                        amountMinor =
                            amountToMinor(
                                group.value
                            ),
                        role =
                            SemanticAmountRole.UNKNOWN,
                        span = span
                    )
            }
        }

        return bySpan.values
            .sortedBy { it.span.startOffset }
    }

    private fun extractReferences(
        body: String
    ): List<SemanticReferenceFact> {
        val references =
            mutableListOf<SemanticReferenceFact>()

        labeledReferenceRegex
            .findAll(body)
            .forEach { match ->
                val label =
                    match.groups[1]
                val value =
                    match.groups[2]
                        ?: return@forEach
                references +=
                    SemanticReferenceFact(
                        value =
                            value.value.uppercase(),
                        label =
                            label?.value
                                ?.trim(),
                        span =
                            value.range.toSpan()
                    )
            }

        confirmedCodeRegex
            .findAll(body)
            .forEach { match ->
                val value =
                    match.groups[1]
                        ?: return@forEach
                if (
                    references.none {
                        it.value.equals(
                            value.value,
                            ignoreCase = true
                        )
                    }
                ) {
                    references +=
                        SemanticReferenceFact(
                            value =
                                value.value.uppercase(),
                            label = "confirmed code",
                            span =
                                value.range.toSpan()
                        )
                }
            }

        return references
    }

    private fun extractAccounts(
        body: String
    ): List<SemanticAccountFact> =
        buildList {
            sourceAccountRegex
                .findAll(body)
                .forEach {
                    it.groups[1]?.let { group ->
                        add(
                            SemanticAccountFact(
                                label =
                                    group.value.trim(),
                                role =
                                    SemanticAccountRole.SOURCE,
                                span =
                                    group.range.toSpan()
                            )
                        )
                    }
                }

            destinationAccountRegex
                .findAll(body)
                .forEach {
                    it.groups[1]?.let { group ->
                        add(
                            SemanticAccountFact(
                                label =
                                    group.value.trim(),
                                role =
                                    SemanticAccountRole
                                        .DESTINATION,
                                span =
                                    group.range.toSpan()
                            )
                        )
                    }
                }

            observedAccountRegex
                .findAll(body)
                .forEach {
                    it.groups[1]?.let { group ->
                        val span =
                            group.range.toSpan()
                        val duplicate =
                            any {
                                it.span == span
                            }
                        if (!duplicate) {
                            add(
                                SemanticAccountFact(
                                    label =
                                        group.value.trim(),
                                    role =
                                        SemanticAccountRole
                                            .OBSERVED,
                                    span = span
                                )
                            )
                        }
                    }
                }
        }

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

    private val PRIMARY_INTENT_PRIORITY = listOf(
        FinancialSemanticIntent.REVERSAL,
        FinancialSemanticIntent.REFUND,
        FinancialSemanticIntent.LOAN_DISBURSEMENT,
        FinancialSemanticIntent.LOAN_REPAYMENT,
        FinancialSemanticIntent.INVESTMENT_EXECUTED,
        FinancialSemanticIntent.INVESTMENT_ORDER,
        FinancialSemanticIntent.TRANSFER,
        FinancialSemanticIntent.PAYMENT,
        FinancialSemanticIntent.RECEIPT,
        FinancialSemanticIntent.LOAN_BALANCE,
        FinancialSemanticIntent.BALANCE_UPDATE,
        FinancialSemanticIntent.FEE,
        FinancialSemanticIntent.CONFIRMATION_ONLY
    )
}
