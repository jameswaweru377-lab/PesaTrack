package com.kedevjimmy.pesatrack.data.local

import com.kedevjimmy.pesatrack.domain.intelligence.OfflineMoneyPolicy
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Called on IO. Reads trusted aggregate snapshots; never reads SMS or writes classifications. */
object OfflineMoneyInsightStore {
    fun spendingComparison(store: TransactionStore, nowEpochMs: Long): String? {
        val end = Calendar.getInstance().apply {
            timeInMillis = nowEpochMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val recent = (end.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val previous = (end.clone() as Calendar).apply { add(Calendar.MONTH, -2) }
        val first = store.activitySnapshotBetween(previous.timeInMillis, recent.timeInMillis,
            nowEpochMs, performMaintenance = false)
        val second = store.activitySnapshotBetween(recent.timeInMillis, end.timeInMillis,
            nowEpochMs, performMaintenance = false)
        val increase = OfflineMoneyPolicy.spendingIncrease(first.summary.spentMinor, second.summary.spentMinor)
            ?: return null
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val money = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }.format(java.math.BigDecimal.valueOf(increase, 2))
        return "Recorded net spending was KSh $money higher in ${month.format(recent.time)} than " +
            "${month.format(previous.time)}. Linked refunds are included; fees are separate. Missing records may change this comparison."
    }
}
