package com.kedevjimmy.pesatrack.ui.screens

/** The selected activity month must not redefine verified current account money. */
internal data class HomeSummaryScope(
    val currentAvailableMinor: Long?,
    val selectedNetFlowMinor: Long?,
    val monthLabel: String
) {
    val netFlowCaption: String get() = "net recorded flow · $monthLabel"
}

internal fun homeSummaryScope(
    currentAvailableMinor: Long?,
    selectedNetFlowMinor: Long?,
    monthLabel: String
): HomeSummaryScope = HomeSummaryScope(
    currentAvailableMinor = currentAvailableMinor,
    selectedNetFlowMinor = selectedNetFlowMinor,
    monthLabel = monthLabel
)
