package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf

/** Read-only card values. The ledger owns income and spending calculations. */
internal data class ActivityDesign3Totals(
    val incomeMinor: Long,
    val expensesMinor: Long?,
    val transfersMinor: Long?,
    val investedMinor: Long?
)

/**
 * Investment purchases and ordinary account moves are distinct cards; a
 * transfer to a broker is not proof that a security was purchased.
 */
internal fun activityDesign3Totals(snapshot: ActivityLedgerSnapshot): ActivityDesign3Totals {
    val trusted = snapshot.trustedTransactions
    val invested = CheckedMoney.orNull {
        // The confirmed investment-buy action is stored as EXPENSE in the
        // canonical ledger. This card reclassifies its presentation only.
        trusted.filter {
            it.direction == MoneyDirection.EXPENSE.name &&
                it.relationType == TransactionStore.RELATION_INVESTMENT_BUY
        }.checkedMoneySumOf { it.amountMinor }
    }
    val transfers = CheckedMoney.orNull {
        trusted.filter {
            it.direction == MoneyDirection.TRANSFER.name &&
                it.relationType != TransactionStore.RELATION_REFUND &&
                it.relationType != TransactionStore.RELATION_INVESTMENT_BUY &&
                it.relationType != TransactionStore.RELATION_INVESTMENT_SELL &&
                it.category != "INVESTMENT" && it.category != "DEBT_LOAN"
        }.checkedMoneySumOf { it.amountMinor }
    }
    return ActivityDesign3Totals(
        incomeMinor = snapshot.summary.incomeMinor,
        expensesMinor = invested?.let {
            CheckedMoney.orNull {
                CheckedMoney.subtract(snapshot.summary.spentMinor, it)
            }?.takeIf { remainder -> remainder >= 0L }
        },
        transfersMinor = transfers,
        investedMinor = invested
    )
}

/**
 * Net *recorded* movement for one date, using the ledger's trusted cash rows.
 * Owned transfers and investment reallocations are neutral; refunds are cash
 * coming back. Fee amounts attached to recorded rows remain outflows.
 * Separately journalled debt fees are not allocated to a date by this view.
 */
internal fun activityDesign3DailyNet(dayEpochMs: Long, trusted: List<StoredTransaction>): Long? =
    CheckedMoney.orNull {
        trusted.asSequence().filter {
            dayStart(it.occurredAtEpochMs) == dayEpochMs
        }.map { row ->
            val cash = when {
                row.relationType == TransactionStore.RELATION_REFUND -> row.amountMinor
                row.direction == MoneyDirection.INCOME.name -> row.amountMinor
                row.direction == MoneyDirection.EXPENSE.name -> CheckedMoney.negate(row.amountMinor)
                else -> 0L
            }
            CheckedMoney.subtract(cash, row.feeMinor)
        }.let(CheckedMoney::sum)
    }
