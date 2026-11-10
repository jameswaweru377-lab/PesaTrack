package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount

/** Activity's account balance is held at tracked bank/wallet accounts, not loose cash. */
internal fun activityRecordedBalanceAccounts(
    accounts: List<MoneyAccount>, confirmedAccountIds: Set<String>
): List<MoneyAccount> = VerifiedAccountBalancePolicy.eligibleAccounts(accounts, confirmedAccountIds)

/** An account balance, not monthly income minus expenses or a salary forecast. */
internal fun activityRecordedSpendableBalance(snapshots: List<PaydayAccountSnapshot>): Long? {
    return VerifiedAccountBalancePolicy.spendableTotal(snapshots)
}
