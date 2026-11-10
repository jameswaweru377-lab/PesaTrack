package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.ActivityMonthBucket
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.PaydaySnapshotStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared Home→Activity preloader and Activity's normal first-stage reader.
 * This reader never writes caches, edits money, or publishes a ledger revision.
 * Callers are responsible for rejecting results from a superseded revision.
 */
internal suspend fun readActivityCoreForMonth(
    context: Context,
    monthEpochMs: Long
): ActivityCoreData = withContext(Dispatchers.IO) {
    val app = context.applicationContext
    val transactions = TransactionStore.get(app)
    val currentMonthEpochMs = monthStart(System.currentTimeMillis())
    val months = transactions.activityMonths().toMutableList()
    if (months.none { it.monthStartEpochMs == currentMonthEpochMs }) {
        months.add(0, ActivityMonthBucket(currentMonthEpochMs, 0))
    }
    val accounts = AccountStore.get(app).listAccounts(enabledOnly = false)
    val confirmed = FinancialUnderstandingStore.get(app).confirmedAccountIds()
    val owned = activityRecordedBalanceAccounts(accounts, confirmed)
    // A previous month's balance must not silently display today's money.
    val endOfMonth = shiftMonth(monthEpochMs, 1) - 1L
    val balanceAsOf = minOf(System.currentTimeMillis(), endOfMonth)
    val recordedBalance = if (balanceAsOf < monthEpochMs) null else
        activityRecordedSpendableBalance(
            PaydaySnapshotStore.get(app).currentAccountSnapshots(owned, balanceAsOf)
        )
    ActivityCoreData(
        monthBuckets = months.sortedByDescending { it.monthStartEpochMs },
        snapshot = transactions.activitySnapshot(nowEpochMs = monthEpochMs),
        previousSnapshot = transactions.activitySnapshot(
            nowEpochMs = shiftMonth(monthEpochMs, -1),
            performMaintenance = false
        ),
        accountsById = accounts.associateBy { it.id },
        confirmedAccountIds = confirmed,
        recordedBalanceMinor = recordedBalance
    )
}
