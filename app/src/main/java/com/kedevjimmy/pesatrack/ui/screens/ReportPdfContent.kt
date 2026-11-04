package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.ActivitySummary

internal data class ReportTrendValue(val label: String, val spentMinor: Long)

internal data class ReportPdfCategory(val label: String, val amountMinor: Long)

internal data class ReportPdfExpense(
    val label: String,
    val category: String,
    val amountMinor: Long
)

/** A print-ready, immutable view of the same Activity summary used on-screen. */
internal data class ReportPdfContent(
    val periodLabel: String,
    val incomeMinor: Long,
    val spentMinor: Long,
    val feesMinor: Long,
    val transferMinor: Long,
    val needsReviewCount: Int,
    val categories: List<ReportPdfCategory>,
    val trend: List<ReportTrendValue>,
    val largestExpenses: List<ReportPdfExpense>,
    val coverageNote: String
)

internal fun buildReportPdfContent(
    periodLabel: String,
    summary: ActivitySummary,
    categories: List<Pair<String, Long>>,
    trend: List<ReportTrendValue>,
    candidates: List<ReportReviewCandidate>
): ReportPdfContent = ReportPdfContent(
    periodLabel = periodLabel,
    incomeMinor = summary.incomeMinor,
    spentMinor = summary.spentMinor,
    feesMinor = summary.feesMinor,
    transferMinor = summary.transferMinor,
    needsReviewCount = summary.needsReviewCount,
    categories = categories.filter { it.second > 0L }
        .sortedByDescending { it.second }
        .map { ReportPdfCategory(it.first, it.second) },
    trend = trend.toList(),
    largestExpenses = reportLargestExpenses(candidates).take(12).map {
        ReportPdfExpense(
            label = it.counterparty?.takeIf(String::isNotBlank) ?: "Unnamed expense",
            category = it.category,
            amountMinor = it.amountMinor
        )
    },
    coverageNote = "Based on trusted Activity entries in this period. " +
        "Unreviewed evidence and separately imported bank statements are not added. " +
        "Own-account transfers are not spending; fees are shown separately."
)
