package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney

/** An account balance, not monthly income minus expenses or a salary forecast. */
internal fun activityRecordedSpendableBalance(snapshots: List<PaydayAccountSnapshot>): Long? {
    val spendable = snapshots.filter { it.spendability == PaydaySpendability.SPENDABLE }
    if (spendable.isEmpty() || spendable.any {
            it.balanceMinor == null || it.quality == PaydayBalanceQuality.UNKNOWN
        }) return null
    return CheckedMoney.orNull {
        spendable.fold(0L) { sum, account ->
            CheckedMoney.add(sum, requireNotNull(account.balanceMinor))
        }
    }
}
