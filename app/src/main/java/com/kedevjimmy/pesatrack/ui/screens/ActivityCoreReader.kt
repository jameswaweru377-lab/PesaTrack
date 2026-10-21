package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.ActivityMonthBucket
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
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
    ActivityCoreData(
        monthBuckets = months.sortedByDescending { it.monthStartEpochMs },
        snapshot = transactions.activitySnapshot(nowEpochMs = monthEpochMs),
        previousSnapshot = transactions.activitySnapshot(
            nowEpochMs = shiftMonth(monthEpochMs, -1),
            performMaintenance = false
        ),
        accountsById = AccountStore.get(app)
            .listAccounts(enabledOnly = false).associateBy { it.id },
        confirmedAccountIds = FinancialUnderstandingStore.get(app).confirmedAccountIds()
    )
}
