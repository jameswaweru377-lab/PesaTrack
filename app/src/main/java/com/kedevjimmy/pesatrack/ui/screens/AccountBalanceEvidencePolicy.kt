package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney

internal data class VerifiedAccountBalanceSummary(
    val totalMinor: Long?,
    val recordedCount: Int,
    val accountCount: Int
) {
    val complete: Boolean get() = totalMinor != null && recordedCount == accountCount
}

/** Count canonical accounts before they are grouped for display. */
internal fun accountVerifiedBalanceSummary(
    snapshots: Collection<PaydayAccountSnapshot>
): VerifiedAccountBalanceSummary {
    val recorded = snapshots.mapNotNull(VerifiedAccountBalancePolicy::trustedAmount)
    val total = if (recorded.isEmpty()) null else CheckedMoney.orNull {
        CheckedMoney.sum(recorded.asSequence())
    }
    return VerifiedAccountBalanceSummary(total, recorded.size, snapshots.size)
}
