package com.kedevjimmy.pesatrack.ui.screens

import kotlin.math.roundToInt

/**
 * Local, evidence-limited guidance — NOT the CFPB Financial Well-Being Scale,
 * the FinHealth Score, an affordability decision, a credit score or live balance.
 * A missing observation must never be interpreted as a healthy zero.
 */
internal data class HomeFinancialSignalsInput(
    val budgetLimitMinor: Long?,
    val budgetSpentMinor: Long?,
    val uncategorizedShare: Double?,
    val currentSpentMinor: Long?,
    val previousAlignedSpentMinor: Long?,
    val incomeMinor: Long?,
    val debtOutstandingMinor: Long?,
    val unknownDebtBalances: Boolean,
    val feesMinor: Long?,
    val currentTrustedRows: Int,
    val previousTrustedRows: Int,
    val pendingReviews: Int,
    val confirmedAccountCount: Int,
    val hasConfirmedSalaryCycle: Boolean,
    val debtAccountCount: Int = 0,
    val remainingCommitmentsMinor: Long? = null,
    /** Explicit verified emergency savings, NEVER inferred from bank balance or transfers. */
    val confirmedEmergencyReserveMinor: Long? = null,
    /** User-confirmed monthly essential costs (not all discretionary spending). */
    val confirmedEssentialCostsMinor: Long? = null,
    /** Required debt repayments due per month, not last repayment or principal. */
    val confirmedMonthlyDebtPaymentsMinor: Long? = null,
    val elapsedMonthFraction: Double? = null,
    val isCurrentMonth: Boolean = true
)

internal data class HomeSignal(
    val title: String,
    val score: Int?,
    val reason: String
)

internal data class HomeGuidanceAction(
    val title: String,
    val detail: String,
    /** Native app destination, never a network URL. */
    val destination: String
)

internal data class HomeFinancialSignals(
    val score: Int?,
    val status: String,
    val explanation: String,
    val indicators: List<HomeSignal>,
    val dataSufficient: Boolean,
    val confidence: HomeSignal,
    val actions: List<HomeGuidanceAction>
)

/** Safe, bounded arithmetic for trusted nonnegative financial amounts. */
private fun share(numerator: Long?, denominator: Long?): Double? {
    if (numerator == null || denominator == null ||
        numerator < 0L || denominator <= 0L) return null
    return (numerator.toDouble() / denominator.toDouble())
        .takeIf { it.isFinite() && it >= 0.0 }
}
private fun percentage(ratio: Double): String =
    "${(ratio * 100).roundToInt().coerceAtLeast(0)}%"

internal fun evaluateHomeFinancialSignals(input: HomeFinancialSignalsInput): HomeFinancialSignals {
    val validCategoryCoverage = input.uncategorizedShare?.let {
        it.isFinite() && it in 0.0..0.20
    } == true
    val confidencePoints =
        (if (input.confirmedAccountCount > 0) 20 else 0) +
        (if (input.hasConfirmedSalaryCycle) 15 else 0) +
        (input.currentTrustedRows.coerceAtLeast(0).coerceAtMost(5) * 5) +
        (if (input.pendingReviews == 0) 20 else 0) +
        (if (validCategoryCoverage) 20 else 0)
    val confident = confidencePoints == 100 &&
        input.isCurrentMonth && input.previousTrustedRows >= 5
    val confidence = HomeSignal(
        "Data confidence",
        confidencePoints.coerceIn(0, 100),
        when {
            input.confirmedAccountCount == 0 -> "Confirm your own accounts before assessing finances."
            !input.hasConfirmedSalaryCycle -> "Confirm a salary cycle or income source to assess this period."
            input.currentTrustedRows < 5 -> "Not enough trusted payments to assess the month."
            input.pendingReviews > 0 -> "${input.pendingReviews} unresolved review items; insights are provisional."
            !validCategoryCoverage -> "Categorize spending marked Other/Uncategorized to improve coverage."
            input.previousTrustedRows < 5 -> "Prior-month records are limited; comparisons are provisional."
            !input.isCurrentMonth -> "Historical month: review archived activity; no current-month prognosis."
            else -> "The required local activity checks are available, not independently verified."
        }
    )

    // Cash flow is observed income minus tracked outflow for this calendar
    // window, not the amount available after future bills and not bank balance.
    val cashRatio = if (input.currentTrustedRows >= 5 &&
        input.hasConfirmedSalaryCycle && input.pendingReviews == 0)
        share(input.currentSpentMinor, input.incomeMinor) else null
    val cashFlow = HomeSignal(
        "Cash flow",
        cashRatio?.let { ratio ->
            when {
                ratio <= .60 -> 95
                ratio <= .80 -> 82
                ratio <= 1.0 -> 65
                else -> (65 - (ratio - 1.0) * 130).roundToInt().coerceIn(0, 100)
            }
        },
        if (cashRatio == null)
            "Recorded income and reviewed outflow are needed. Missing income is not zero."
        else "Tracked outflow is ${percentage(cashRatio)} of this period's income. This excludes unpaid future bills."
    )

    // Savings resilience requires actual earmarked liquid reserves and
    // essential costs. A transfer, goal or protected budget is not a reserve.
    val reserveMonths = share(
        input.confirmedEmergencyReserveMinor,
        input.confirmedEssentialCostsMinor
    )
    val savings = HomeSignal(
        "Savings",
        reserveMonths?.let { months ->
            when {
                months < .5 -> 20
                months < 1.0 -> 40
                months < 3.0 -> 70
                else -> 95
            }
        },
        if (reserveMonths == null)
            "Emergency savings and monthly essentials are unconfirmed. Transfers are not proof of a cash reserve."
        else "Confirmed emergency reserve covers about ${(reserveMonths * 10).roundToInt() / 10.0} months of essentials."
    )

    // Debt principal is not the recurring payment obligation. Do not rate
    // debt affordability using last repayment, drawdown or principal alone.
    val repaymentEvidence = input.confirmedMonthlyDebtPaymentsMinor
    val hasConfirmedRepayments =
        (input.debtAccountCount > 0 && repaymentEvidence != null && repaymentEvidence > 0L) ||
        (input.debtAccountCount == 0 && repaymentEvidence == 0L)
    val repaymentRatio = if (!input.unknownDebtBalances &&
        hasConfirmedRepayments && input.hasConfirmedSalaryCycle)
        share(repaymentEvidence, input.incomeMinor) else null
    val debt = HomeSignal(
        "Debt",
        repaymentRatio?.let { ratio ->
            when {
                ratio <= .15 -> 95
                ratio <= .30 -> 78
                ratio <= .45 -> 55
                else -> 25
            }
        },
        when {
            input.unknownDebtBalances -> "One or more debt balances are unknown; affordability cannot be assessed."
            input.debtAccountCount == 0 && repaymentRatio != null ->
                "You entered no repayments due and no active debt is recorded. Unrecorded debts may still exist."
            input.debtAccountCount == 0 -> "No active debts are recorded. That does not prove no debt exists."
            repaymentRatio == null -> "${input.debtAccountCount} active debt account(s). Required monthly repayments are not confirmed."
            else -> "Confirmed required repayments use ${percentage(repaymentRatio)} of recorded period income (a planning signal)."
        }
    )

    // Month-to-date spending is compared with the elapsed part of the month
    // only when at least a quarter has passed. Never call a prediction a bill.
    val progress = input.elapsedMonthFraction?.takeIf {
        it.isFinite() && it in .25..1.0
    }
    val budgetRatio = if (input.isCurrentMonth && validCategoryCoverage &&
        input.currentTrustedRows >= 5 && input.pendingReviews == 0 && progress != null)
        share(input.budgetSpentMinor, input.budgetLimitMinor) else null
    val paceRatio = if (budgetRatio != null) budgetRatio / progress!! else null
    val spending = HomeSignal(
        "Spending",
        paceRatio?.let { pace ->
            when {
                pace <= .75 -> 95
                pace <= 1.0 -> 80
                pace <= 1.20 -> 60
                else -> (60 - (pace - 1.2) * 120).roundToInt().coerceIn(0, 100)
            }
        },
        when {
            input.budgetLimitMinor == null || input.budgetLimitMinor <= 0L ->
                "Set a positive monthly budget to assess spending pace."
            !validCategoryCoverage -> "Review broad or uncategorized spending before comparing budgets."
            !input.isCurrentMonth -> "Historical month: review actual results, not a current-month pace projection."
            progress == null -> "Early month or unavailable calendar progress; spending pace is not yet reliable."
            paceRatio == null -> "More trusted spending activity is needed."
            else -> "At the current rate, spending pace is about ${percentage(paceRatio)} of the budget allowance for this point in the month."
        }
    )

    val pillars = listOf(cashFlow, savings, debt, spending)
    // Suppress the composite until every material pillar is supported. The
    // absence of known savings or debt schedules must not score as healthy.
    val allSupported = confident && pillars.all { it.score != null }
    val composite = if (allSupported)
        ((cashFlow.score!! * 35 + savings.score!! * 25 +
            debt.score!! * 25 + spending.score!! * 15) / 100.0)
            .roundToInt().coerceIn(0, 100)
    else null

    val weakPillars = pillars.count { (it.score ?: 100) < 45 }
    val status = when {
        weakPillars >= 2 -> "Needs attention"
        composite == null && weakPillars > 0 -> "Worth reviewing"
        composite == null -> "Partial picture"
        weakPillars > 0 || composite!! < 75 -> "Worth reviewing"
        else -> "On track"
    }
    val summary = when {
        composite != null -> "An indicative PesaTrack summary based on locally confirmed signals — not a credit score."
        cashFlow.score != null || spending.score != null ->
            "Useful signs are available. Missing savings or debt evidence prevents a complete rating."
        else -> "PesaTrack will show what is known and what needs confirmation; it will not guess."
    }

    val actions = buildList {
        if (!validCategoryCoverage || input.pendingReviews > 0)
            add(HomeGuidanceAction("Review financial activity",
                "Resolve unclear transactions and broad categories before acting on comparisons.", "activity"))
        if ((input.remainingCommitmentsMinor ?: 0L) > 0L)
            add(HomeGuidanceAction("Keep upcoming commitments in view",
                "Your salary cycle has unpaid planned commitments. Current cash flow does not deduct future bills.", "plan"))
        if (cashRatio != null && cashRatio > 1.0)
            add(HomeGuidanceAction("Recheck outgoing costs",
                "Recorded spending and fees exceed this period's tracked income. Review the plan.", "plan"))
        if (paceRatio != null && paceRatio > 1.0)
            add(HomeGuidanceAction("Adjust the spending plan",
                "The pace is above the current monthly budget allowance. Check remaining commitments.", "plan"))
        if (reserveMonths == null)
            add(HomeGuidanceAction("Review emergency savings",
                "Track a separate liquid reserve and essential costs. PesaTrack cannot infer these from transfers.", "plan"))
        else if (reserveMonths < 1.0)
            add(HomeGuidanceAction("Build an emergency buffer",
                "Consider gradually putting aside money for essential expenses if affordable.", "plan"))
        if (input.unknownDebtBalances || (input.debtAccountCount > 0 && repaymentRatio == null))
            add(HomeGuidanceAction("Check debt repayments",
                "Review outstanding balances and actual required payments before judging affordability.", "debts"))
        if (!input.hasConfirmedSalaryCycle)
            add(HomeGuidanceAction("Confirm your income cycle",
                "A confirmed pay cycle improves cash-flow and spending context.", "salary"))
        if (input.confirmedAccountCount == 0)
            add(HomeGuidanceAction("Confirm your accounts",
                "Only explicitly owned accounts should shape your money picture.", "accounts"))
    }.distinctBy { it.title }

    return HomeFinancialSignals(
        composite, status, summary, pillars, allSupported, confidence, actions
    )
}
