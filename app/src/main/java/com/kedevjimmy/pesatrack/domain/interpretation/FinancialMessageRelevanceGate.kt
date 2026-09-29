package com.kedevjimmy.pesatrack.domain.interpretation

enum class FinancialRelevanceSignal {
    CURRENCY_AMOUNT,
    FINANCIAL_ACTION,
    ACCOUNT_CONTEXT,
    DEBT_CONTEXT,
    INVESTMENT_CONTEXT,
    FINANCIAL_CONFIRMATION,
    PURCHASE_CONTEXT,
    SECURITY_NOISE,
    PROMOTIONAL_NOISE,
    FINANCIAL_SOLICITATION,
    TELECOM_INFORMATION_ONLY
}

data class FinancialMessageRelevance(
    val isRelevant: Boolean,
    val score: Int,
    val signals: Set<FinancialRelevanceSignal>
)

/**
 * Cheap, local, provider-agnostic filter that decides whether a
 * message deserves full financial interpretation.
 *
 * This deliberately reasons from financial language, not sender
 * brands. A future bank or lender should pass the same gate if its
 * wording carries the same financial meaning.
 */
object FinancialMessageRelevanceGate {
    private val numericAmountRegex = Regex(
        """\b[0-9][0-9,]*(?:\.\d{1,2})?\b"""
    )

    private val strongFinancialActionRegex = Regex(
        """\b(?:credited|debited|credit|debit|cr|dr|paid|payment|withdrawn|withdrawal|deposited|deposit|disbursed|disbursement|repay|repaid|repayment|refund|refunded|reversal|reversed)\b""",
        RegexOption.IGNORE_CASE
    )

    private val contextualMovementActionRegex = Regex(
        """\b(?:received|sent|transferred|transfer|moved|purchase|purchased|bought|sold)\b""",
        RegexOption.IGNORE_CASE
    )

    private val accountContextRegex = Regex(
        """\b(?:account|a/c|acct|wallet|balance|reference|ref|txn|transaction|paybill|till|merchant|savings|saving|bank|funds)\b""",
        RegexOption.IGNORE_CASE
    )

    private val debtContextRegex = Regex(
        """\b(?:loan|borrowed|borrowing|disbursed|disbursement|repay|repaid|repayment|outstanding|total\s+due|amount\s+due|facility\s+fee|credit\s+limit|overdraft)\b""",
        RegexOption.IGNORE_CASE
    )

    private val investmentContextRegex = Regex(
        """\b(?:shares?|stocks?|units?|buy\s+order|sell\s+order|order\s+submitted|order\s+executed|successfully\s+bought|successfully\s+sold|dividend)\b""",
        RegexOption.IGNORE_CASE
    )

    private val financialConfirmationRegex = Regex(
        """\b(?:transaction|transfer|payment|loan|funds)\b.{0,60}\b(?:processed|completed|successful|successfully|confirmed|approved)\b|\b(?:processed|completed|successful|successfully|confirmed|approved)\b.{0,60}\b(?:transaction|transfer|payment|loan|funds)\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val purchaseContextRegex = Regex(
        """\b(?:airtime|data|bundle|subscription|internet|electricity|power|water)\b.{0,50}\b(?:bought|purchased|paid|payment|renewed)\b|\b(?:bought|purchased|paid|payment)\b.{0,50}\b(?:airtime|data|bundle|subscription|internet|electricity|power|water)\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val securityNoiseRegex = Regex(
        """\b(?:otp|one[- ]time\s+password|verification\s+code|password\s+reset|reset\s+pin|login\s+code|sign[- ]in\s+code|do\s+not\s+share\s+(?:this\s+)?(?:pin|code))\b""",
        RegexOption.IGNORE_CASE
    )

    private val promotionalNoiseRegex = Regex(
        """\b(?:flash\s+offer|special\s+offer|promo(?:tion)?|reward\s+program|you\s+have\s+won|congratulations.{0,40}(?:reward|prize)|dial\s+\*\d+#)\b""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val financialSolicitationRegex = Regex(
        """\b(?:you\s+(?:are\s+)?(?:eligible|qualify)|apply\s+now|borrow\s+now|loan\s+offer|credit\s+offer|pre[- ]approved\s+(?:loan|limit)|increase\s+your\s+(?:loan\s+)?limit)\b""",
        RegexOption.IGNORE_CASE
    )

    private val telecomInformationOnlyRegex = Regex(
        """\b(?:airtime|data|bundle)\s+(?:balance|remaining|expires?|expiring)\b|\b(?:bundle|data)\s+will\s+expire\b""",
        RegexOption.IGNORE_CASE
    )

    fun assess(
        message: NormalizedFinancialMessage
    ): FinancialMessageRelevance {
        val body = message.body
        val signals =
            linkedSetOf<FinancialRelevanceSignal>()
        var score = 0

        val hasNumericAmount =
            numericAmountRegex.containsMatchIn(body)
        val hasAccountContext =
            accountContextRegex.containsMatchIn(body)
        val hasDebtContext =
            debtContextRegex.containsMatchIn(body)
        val hasInvestmentContext =
            investmentContextRegex
                .containsMatchIn(body)
        val hasFinancialConfirmation =
            financialConfirmationRegex
                .containsMatchIn(body)
        val hasPurchaseContext =
            purchaseContextRegex.containsMatchIn(body)
        val hasStrongFinancialAction =
            strongFinancialActionRegex
                .containsMatchIn(body)
        val hasContextualMovement =
            contextualMovementActionRegex
                .containsMatchIn(body)

        if (
            message.hasCurrencyHint &&
            hasNumericAmount
        ) {
            signals +=
                FinancialRelevanceSignal.CURRENCY_AMOUNT
            score += 1
        }

        val contextualMovementIsFinancial =
            hasContextualMovement &&
                (
                    message.hasCurrencyHint ||
                        hasAccountContext ||
                        hasDebtContext ||
                        hasInvestmentContext ||
                        hasFinancialConfirmation ||
                        hasPurchaseContext
                    )

        if (
            hasStrongFinancialAction ||
            contextualMovementIsFinancial
        ) {
            signals +=
                FinancialRelevanceSignal.FINANCIAL_ACTION
            score += 2
        }

        if (
            hasAccountContext &&
            (
                message.hasCurrencyHint ||
                    hasNumericAmount
                )
        ) {
            signals +=
                FinancialRelevanceSignal.ACCOUNT_CONTEXT
            score += 1
        }

        if (hasDebtContext) {
            signals +=
                FinancialRelevanceSignal.DEBT_CONTEXT
            score += 2
        }

        if (hasInvestmentContext) {
            signals +=
                FinancialRelevanceSignal.INVESTMENT_CONTEXT
            score += 2
        }

        if (hasFinancialConfirmation) {
            signals +=
                FinancialRelevanceSignal
                    .FINANCIAL_CONFIRMATION
            score += 2
        }

        if (hasPurchaseContext) {
            signals +=
                FinancialRelevanceSignal.PURCHASE_CONTEXT
            score += 2
        }

        if (securityNoiseRegex.containsMatchIn(body)) {
            signals +=
                FinancialRelevanceSignal.SECURITY_NOISE
            score -= 4
        }

        if (
            promotionalNoiseRegex
                .containsMatchIn(body)
        ) {
            signals +=
                FinancialRelevanceSignal.PROMOTIONAL_NOISE
            score -= 3
        }

        if (
            financialSolicitationRegex
                .containsMatchIn(body)
        ) {
            signals +=
                FinancialRelevanceSignal
                    .FINANCIAL_SOLICITATION
            score -= 3
        }

        if (
            telecomInformationOnlyRegex
                .containsMatchIn(body) &&
            !hasPurchaseContext
        ) {
            signals +=
                FinancialRelevanceSignal
                    .TELECOM_INFORMATION_ONLY
            score -= 4
        }

        return FinancialMessageRelevance(
            isRelevant = score >= MIN_RELEVANCE_SCORE,
            score = score,
            signals = signals
        )
    }

    private const val MIN_RELEVANCE_SCORE = 2
}
