package com.kedevjimmy.pesatrack.ui.screens

/** Read-only review hints; amounts are transaction principal, not report totals. */
internal data class ReportReviewCandidate(
    val id: Long,
    val counterparty: String?,
    val amountMinor: Long,
    val category: String,
    val isExpense: Boolean,
    val isRefund: Boolean,
    val hasAllocations: Boolean
)

internal fun reportCategoryReview(rows: List<ReportReviewCandidate>): List<ReportReviewCandidate> =
    rows.asSequence()
        .filter { it.isExpense && !it.isRefund && !it.hasAllocations && it.amountMinor > 0L }
        .filter { it.category == "OTHER" || it.category == "UNCATEGORIZED" }
        .sortedWith(compareByDescending<ReportReviewCandidate> { it.amountMinor }.thenBy { it.id })
        .take(3)
        .toList()
