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
        sumTrusted(snapshots.filter { it.spendability == PaydaySpendability.SPENDABLE }, allowPartial = true)

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
        val freshThrough = at?.let { minOf(it, Long.MAX_VALUE - MAX_AGE_MS) + MAX_AGE_MS }
        val stale = at == null || at > nowEpochMs || nowEpochMs > requireNotNull(freshThrough)
        val amount = snapshot?.let(::trustedAmount)
        PlanSpendableAccount(
            id = account.id,
            balanceMinor = amount,
            isProtected = snapshot?.spendability == PaydaySpendability.PROTECTED ||
                (snapshot == null && account.type == AccountType.SAVINGS),
            provisional = stale || amount == null,
            freshThroughEpochMs = freshThrough,
            conflicting = candidates.size > 1
        )
    }

    private fun sumTrusted(
        snapshots: List<PaydayAccountSnapshot>, allowPartial: Boolean = false
    ): Long? {
        if (snapshots.isEmpty() || snapshots.groupBy { it.accountId }.values.any {
                same -> same.distinct().size != 1
            }) return null
        val distinct = snapshots.distinctBy { it.accountId }
        val amounts = distinct.mapNotNull(::trustedAmount)
        if (amounts.isEmpty() || (!allowPartial && amounts.size != distinct.size)) return null
        return CheckedMoney.orNull { amounts.fold(0L, CheckedMoney::add) }
    }
}
