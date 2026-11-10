package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy

/** Activity's account balance is held at tracked bank/wallet accounts, not loose cash. */
internal fun activityRecordedBalanceAccounts(
    accounts: List<MoneyAccount>, confirmedAccountIds: Set<String>
): List<MoneyAccount> = accounts.filter {
    it.type != AccountType.CASH &&
        AccountDisplayPolicy.isConfirmedOwned(it, confirmedAccountIds)
}

/** An account balance, not monthly income minus expenses or a salary forecast. */
internal fun activityRecordedSpendableBalance(snapshots: List<PaydayAccountSnapshot>): Long? {
    val spendable = snapshots.filter { it.spendability == PaydaySpendability.SPENDABLE }
    if (spendable.isEmpty() || spendable.any {
            it.balanceMinor == null || it.quality !in setOf(
                PaydayBalanceQuality.OBSERVED, PaydayBalanceQuality.USER_ENTERED
            )
        }) return null
    return CheckedMoney.orNull {
        spendable.fold(0L) { sum, account ->
            CheckedMoney.add(sum, requireNotNull(account.balanceMinor))
        }
    }
}
