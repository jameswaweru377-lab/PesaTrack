package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount

data class ActivityRecordedBalance(val amountMinor: Long, val partial: Boolean)

internal fun activityRecordedBalance(snapshots: List<PaydayAccountSnapshot>): ActivityRecordedBalance? =
    activityRecordedSpendableBalance(snapshots)?.let { amount ->
        ActivityRecordedBalance(amount, partial = snapshots.any {
            it.spendability == PaydaySpendability.SPENDABLE &&
                VerifiedAccountBalancePolicy.trustedAmount(it) == null
        })
    }

/** Activity's account balance is held at tracked bank/wallet accounts, not loose cash. */
internal fun activityRecordedBalanceAccounts(
    accounts: List<MoneyAccount>, confirmedAccountIds: Set<String>
): List<MoneyAccount> = VerifiedAccountBalancePolicy.eligibleAccounts(accounts, confirmedAccountIds)

/** An account balance, not monthly income minus expenses or a salary forecast. */
internal fun activityRecordedSpendableBalance(snapshots: List<PaydayAccountSnapshot>): Long? {
    return VerifiedAccountBalancePolicy.spendableTotal(snapshots)
}
