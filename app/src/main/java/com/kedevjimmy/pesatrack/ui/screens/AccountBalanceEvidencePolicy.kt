package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney

/** A recorded account total must never quietly include ledger-reconstructed guesses. */
internal data class VerifiedAccountBalanceSummary(
    val totalMinor: Long?,
    val recordedCount: Int,
    val complete: Boolean
)

internal fun accountVerifiedBalanceSummary(lines: List<AccountDisplayLine>): VerifiedAccountBalanceSummary {
    val recorded = lines.filter {
        it.balanceMinor != null && it.quality in setOf(
            PaydayBalanceQuality.OBSERVED, PaydayBalanceQuality.USER_ENTERED
        )
    }
    val total = if (recorded.isEmpty()) null else CheckedMoney.orNull {
        recorded.fold(0L) { sum, line ->
            CheckedMoney.add(sum, requireNotNull(line.balanceMinor))
        }
    }
    return VerifiedAccountBalanceSummary(
        totalMinor = total,
        recordedCount = recorded.size,
        complete = lines.isNotEmpty() && recorded.size == lines.size && total != null
    )
}
