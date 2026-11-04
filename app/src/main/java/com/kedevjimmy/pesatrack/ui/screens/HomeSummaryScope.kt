package com.kedevjimmy.pesatrack.ui.screens

/** The selected activity month must not redefine the current confirmed pay-cycle balance. */
internal data class HomeSummaryScope(
    val currentAvailableMinor: Long?,
    val selectedNetFlowMinor: Long?,
    val monthLabel: String
) {
    val netFlowCaption: String get() = "net recorded flow · $monthLabel"
}

internal fun homeSummaryScope(
    confirmedCycle: Boolean,
    currentAvailableMinor: Long?,
    selectedNetFlowMinor: Long?,
    monthLabel: String
): HomeSummaryScope = HomeSummaryScope(
    currentAvailableMinor = if (confirmedCycle) currentAvailableMinor else null,
    selectedNetFlowMinor = selectedNetFlowMinor,
    monthLabel = monthLabel
)
