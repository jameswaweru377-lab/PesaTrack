package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydaySpendability
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.PlanSpendableAccount

/** Shared, non-income-dependent evidence policy for current owned account balances. */
internal object VerifiedAccountBalancePolicy {
    private const val MAX_AGE_MS = 14L * 24L * 60L * 60L * 1000L

    fun eligibleAccounts(
        accounts: List<MoneyAccount>, confirmedAccountIds: Set<String>
    ): List<MoneyAccount> = accounts.filter {
        it.enabled && it.type in setOf(
            AccountType.BANK, AccountType.MOBILE_WALLET, AccountType.SAVINGS
        ) &&
            AccountDisplayPolicy.isConfirmedOwned(it, confirmedAccountIds)
    }.distinctBy { it.id }

    fun trustedAmount(snapshot: PaydayAccountSnapshot): Long? =
        snapshot.balanceMinor.takeIf {
            snapshot.quality == PaydayBalanceQuality.OBSERVED ||
                snapshot.quality == PaydayBalanceQuality.USER_ENTERED
        }

    fun spendableTotal(snapshots: List<PaydayAccountSnapshot>): Long? =
        sumTrusted(snapshots.filter { it.spendability == PaydaySpendability.SPENDABLE })

    fun groupTotal(
        accountIds: Set<String>, snapshots: List<PaydayAccountSnapshot>
    ): Long? {
        if (accountIds.isEmpty()) return null
        val selected = snapshots.filter { it.accountId in accountIds }
        if (selected.map { it.accountId }.toSet() != accountIds) return null
        return sumTrusted(selected)
    }

    fun planAccounts(
        accounts: List<MoneyAccount>, confirmedAccountIds: Set<String>,
        snapshots: List<PaydayAccountSnapshot>, nowEpochMs: Long
    ): List<PlanSpendableAccount> = eligibleAccounts(accounts, confirmedAccountIds).map { account ->
        val candidates = snapshots.filter { it.accountId == account.id }.distinct()
        val snapshot = candidates.singleOrNull()
        val at = snapshot?.referenceEpochMs
        val stale = at == null || at > nowEpochMs || nowEpochMs - at > MAX_AGE_MS
        PlanSpendableAccount(
            id = account.id,
            balanceMinor = snapshot?.let(::trustedAmount),
            isProtected = snapshot?.spendability == PaydaySpendability.PROTECTED ||
                (snapshot == null && account.type == AccountType.SAVINGS),
            provisional = stale || snapshot?.let(::trustedAmount) == null
        )
    }

    private fun sumTrusted(snapshots: List<PaydayAccountSnapshot>): Long? {
        if (snapshots.isEmpty() || snapshots.groupBy { it.accountId }.values.any {
                same -> same.distinct().size != 1
            }) return null
        val distinct = snapshots.distinctBy { it.accountId }
        if (distinct.any { trustedAmount(it) == null }) return null
        return CheckedMoney.orNull {
            distinct.fold(0L) { total, snapshot ->
                CheckedMoney.add(total, requireNotNull(trustedAmount(snapshot)))
            }
        }
    }
}
