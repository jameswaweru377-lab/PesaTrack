package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySnapshotStore
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.PlanSpendableAccount

/** Reuse Accounts' ownership and balance evidence instead of the salary-cycle estimate. */
internal fun readPlanSpendableAccounts(context: Context): List<PlanSpendableAccount> {
    val accounts = AccountStore.get(context).listAccounts(enabledOnly = true)
    val confirmed = FinancialUnderstandingStore.get(context).confirmedAccountIds()
    val owned = AccountDisplayPolicy.groups(accounts, includeCash = true)
        .flatMap { group ->
            group.accounts.filter {
                AccountDisplayPolicy.isConfirmedOwned(it, confirmed, includeCash = true)
            }
        }.distinctBy { it.id }
    val now = System.currentTimeMillis()
    return PaydaySnapshotStore.get(context).currentAccountSnapshots(owned, now).map {
        val old = it.referenceEpochMs?.let { at ->
            now - at > 14L * 24L * 60L * 60L * 1000L
        } ?: true
        PlanSpendableAccount(
            id = it.accountId,
            balanceMinor = it.balanceMinor,
            isProtected = it.spendability == PaydaySpendability.PROTECTED,
            provisional = old || it.quality == PaydayBalanceQuality.RECONSTRUCTED
        )
    }
}
