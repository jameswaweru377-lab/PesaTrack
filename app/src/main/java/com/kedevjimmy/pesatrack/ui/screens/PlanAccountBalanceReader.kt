package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.BalanceAccountVisibility
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.PaydaySnapshotStore
import com.kedevjimmy.pesatrack.domain.rules.PlanSpendableAccount

/** Reuse Accounts' ownership and balance evidence instead of the salary-cycle estimate. */
internal fun readPlanSpendableAccounts(context: Context): List<PlanSpendableAccount> {
    val accounts = AccountStore.get(context).listAccounts(enabledOnly = true)
    val confirmed = FinancialUnderstandingStore.get(context).confirmedAccountIds()
    val visible = BalanceAccountVisibility(context).visibleAccounts(accounts)
    val owned = VerifiedAccountBalancePolicy.eligibleAccounts(visible, confirmed)
    val now = System.currentTimeMillis()
    val snapshots = PaydaySnapshotStore.get(context).currentAccountSnapshots(owned, now)
    return VerifiedAccountBalancePolicy.planAccounts(owned, confirmed, snapshots, now)
}
