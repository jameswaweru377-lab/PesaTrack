package com.kedevjimmy.pesatrack.ui.screens

internal fun homeQuickReviewTitle(pending: Int?): String = when {
    pending == null -> "Review status unavailable"
    pending == 0 -> "You're all caught up"
    pending == 1 -> "1 item needs review"
    else -> "$pending items need review"
}

internal fun homeInsightCardText(
    guidance: List<HomeGuidanceAction>,
    trustedRowCount: Int
): Pair<String, String> {
    val first = guidance.firstOrNull()
    return when {
        first != null -> first.title to first.detail
        trustedRowCount == 0 -> "Insights need more records" to
            "Import or review your transactions to see reliable guidance."
        else -> "No financial guidance needs attention" to
            "Explore your reports for more detail about recorded spending."
    }
}
