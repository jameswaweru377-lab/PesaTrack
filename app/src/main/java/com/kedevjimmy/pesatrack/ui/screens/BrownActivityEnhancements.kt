package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import java.util.Calendar
import java.util.TimeZone

private val pochiMethodEvidence = Regex("\\bpochi\\s+la\\s+biashara\\b", RegexOption.IGNORE_CASE)
private val payBillAccountEvidence = Regex("\\bpay\\s*bill\\s+account\\b", RegexOption.IGNORE_CASE)
private val bankToMpesaEvidence = Regex("\\bbank\\s+to\\s+m[ -]?pesa\\b", RegexOption.IGNORE_CASE)

/** A method badge describes observed payment evidence, not the spending category. */
internal fun activityPaymentMethod(item: StoredTransaction): String? {
    val evidence = item.counterparty.orEmpty()
    return when {
        pochiMethodEvidence.containsMatchIn(evidence) &&
            item.direction == MoneyDirection.EXPENSE.name -> "Pochi"
        item.type == MpesaTransactionType.PAY_BILL.name -> "Pay Bill"
        payBillAccountEvidence.containsMatchIn(evidence) &&
            item.direction == MoneyDirection.EXPENSE.name -> "Pay Bill"
        item.type == MpesaTransactionType.BUY_GOODS.name -> "Buy Goods"
        item.type == MpesaTransactionType.SENT_MONEY.name -> "Send Money"
        item.type == MpesaTransactionType.AIRTIME.name -> "Airtime"
        item.type == MpesaTransactionType.WITHDRAWAL.name -> "Withdrawal"
        item.type == MpesaTransactionType.DEPOSIT.name -> "Deposit"
        item.direction == MoneyDirection.TRANSFER.name &&
            bankToMpesaEvidence.containsMatchIn(evidence) -> "Bank to M-Pesa"
        item.direction == MoneyDirection.TRANSFER.name -> "Transfer"
        else -> null
    }
}

/** Never fabricate fee information for transactions without a recorded charge. */
internal fun activityFeeBadgeAmount(item: StoredTransaction): Long? =
    item.feeMinor.takeIf { it > 0L }

/** A zero field is also used when fee evidence is absent; never label it fee-free. */
internal fun activityFeeIsUnrecorded(item: StoredTransaction): Boolean =
    item.feeMinor <= 0L && item.direction in setOf(
        MoneyDirection.EXPENSE.name, MoneyDirection.TRANSFER.name
    )

internal const val ACTIVITY_TICKER_PERIOD_DURATION_MS = 40_000L
internal const val ACTIVITY_TICKER_SCROLL_DURATION_MS = 20_000

/** Match Home's fee-inclusive outflow; a transfer's principal is not spending. */
internal fun activityTickerMoneyOut(summary: ActivitySummary): Long? =
    RecordedOutflowPolicy.available(summary.spentMinor, summary.feesMinor)

internal enum class ActivityTickerPeriod(val label: String) {
    YEAR("THIS YEAR"), MONTH("THIS MONTH"), TODAY("TODAY")
}

internal data class ActivityTickerRange(val startEpochMs: Long, val endEpochMsExclusive: Long)

/** Use local calendar boundaries; UTC midnight is not a Kenyan financial day. */
internal fun activityTickerRange(
    period: ActivityTickerPeriod,
    nowEpochMs: Long,
    zone: TimeZone = TimeZone.getDefault()
): ActivityTickerRange {
    val start = Calendar.getInstance(zone).apply {
        timeInMillis = nowEpochMs
        when (period) {
            ActivityTickerPeriod.YEAR -> {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            ActivityTickerPeriod.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
            ActivityTickerPeriod.TODAY -> Unit
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = (start.clone() as Calendar).apply {
        when (period) {
            ActivityTickerPeriod.YEAR -> add(Calendar.YEAR, 1)
            ActivityTickerPeriod.MONTH -> add(Calendar.MONTH, 1)
            ActivityTickerPeriod.TODAY -> add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    return ActivityTickerRange(start.timeInMillis, end.timeInMillis)
}
