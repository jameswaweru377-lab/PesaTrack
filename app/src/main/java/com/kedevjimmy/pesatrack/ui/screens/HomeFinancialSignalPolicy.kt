package com.kedevjimmy.pesatrack.ui.screens

import kotlin.math.roundToInt

/**
 * A PesaTrack *activity-derived* snapshot, not a CFPB well-being scale,
 * credit score, clinical assessment or standardized FinHealth Score.
 *
 * We deliberately decline to assign "At risk" when key sources or periods
 * are incomplete. No raw SMS, balances or telemetry leaves the device.
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
    val hasConfirmedSalaryCycle: Boolean
)

internal data class HomeSignal(val title: String, val score: Int?, val reason: String)
internal data class HomeFinancialSignals(
    val score: Int?,
    val status: String,
    val explanation: String,
    val indicators: List<HomeSignal>,
    val dataSufficient: Boolean
)

internal fun evaluateHomeFinancialSignals(input: HomeFinancialSignalsInput): HomeFinancialSignals {
    val categoryReliable = input.uncategorizedShare != null &&
        input.uncategorizedShare.isFinite() && input.uncategorizedShare <= .20
    val recognizedIncome = input.incomeMinor?.takeIf { it > 0L }
    val budget = if (categoryReliable &&
        (input.budgetLimitMinor ?: 0L) > 0 &&
        input.budgetSpentMinor != null && input.budgetSpentMinor >= 0L) {
        val ratio = input.budgetSpentMinor.toDouble() / input.budgetLimitMinor!!.toDouble()
        (if (ratio <= 1.0) 100.0 - (ratio * 15.0)
        else 85.0 - ((ratio - 1.0) * 140.0)).roundToInt().coerceIn(0, 100)
    } else null

    // Comparable time windows and multiple distinct trusted records are
    // required; missing imports must not be read as a sudden spending change.
    val stability = if (input.currentTrustedRows >= 5 &&
        input.previousTrustedRows >= 5 &&
        input.pendingReviews == 0 &&
        categoryReliable &&
        (input.previousAlignedSpentMinor ?: 0L) > 0L &&
        (input.currentSpentMinor ?: -1L) >= 0L) {
        val ratio = input.currentSpentMinor!!.toDouble() /
            input.previousAlignedSpentMinor!!.toDouble()
        (100.0 - ((ratio - 1.0).coerceAtLeast(0.0) * 140.0))
            .roundToInt().coerceIn(0, 100)
    } else null

    val debt = if (!input.unknownDebtBalances && recognizedIncome != null &&
        input.debtOutstandingMinor != null && input.debtOutstandingMinor >= 0L &&
        input.confirmedAccountCount > 0) {
        // Outstanding principal versus one recorded month of income is only
        // a *rough debt exposure signal*, NOT an affordability verdict.
        val ratio = input.debtOutstandingMinor.toDouble() / recognizedIncome.toDouble()
        (100.0 - ratio * 40.0).roundToInt().coerceIn(0, 100)
    } else null

    val fees = if ((input.currentSpentMinor ?: 0L) > 0L &&
        input.feesMinor != null && input.feesMinor >= 0L &&
        input.currentTrustedRows >= 5) {
        val ratio = input.feesMinor.toDouble() / input.currentSpentMinor!!.toDouble()
        (100.0 - ratio * 2000.0).roundToInt().coerceIn(0, 100)
    } else null

    val indicators = listOf(
        HomeSignal("Budget", budget,
            if (budget == null) "Configure budgets and review Other/Uncategorized." else "Budget use"),
        HomeSignal("Spending", stability,
            if (stability == null) "Comparable reviewed monthly activity is needed." else "Change vs aligned prior month"),
        HomeSignal("Debt", debt,
            if (debt == null) "Confirmed income and known debt balances needed." else "Known principal vs recorded income"),
        HomeSignal("Fees", fees,
            if (fees == null) "More reviewed transactions are needed." else "Fees vs tracked spending")
    )
    val safeToClassify = indicators.all { it.score != null } &&
        input.hasConfirmedSalaryCycle && input.confirmedAccountCount > 0 &&
        input.pendingReviews == 0 && categoryReliable &&
        input.currentTrustedRows >= 5 && input.previousTrustedRows >= 5
    val overall = if (safeToClassify) indicators.mapNotNull { it.score }.average()
        .roundToInt().coerceIn(0, 100) else null
    val severe = indicators.count { (it.score ?: 100) < 40 }
    val status = when {
        overall == null -> "Limited data"
        overall < 45 && severe >= 2 -> "At risk"
        overall < 70 -> "Watch closely"
        else -> "On track"
    }
    val details = when (status) {
        "Limited data" -> "More complete activity is needed before judging financial risk."
        "At risk" -> "Multiple recorded indicators suggest financial pressure. Review the details."
        "Watch closely" -> "Some spending or debt indicators deserve attention."
        else -> "Recorded indicators look stable; this is not a live balance or credit score."
    }
    return HomeFinancialSignals(overall,status,details,indicators,overall != null)
}
