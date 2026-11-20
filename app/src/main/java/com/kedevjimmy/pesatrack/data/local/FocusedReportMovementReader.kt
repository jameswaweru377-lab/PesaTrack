package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.reports.FocusedMovement

/** Read-only period evidence; no new financial classification or ledger writes. */
class FocusedReportMovementReader(context: Context) {
    private val database = PesaTrackDatabase.get(context.applicationContext)

    fun debts(start: Long, end: Long): List<FocusedMovement> = synchronized(CanonicalStoreLock.monitor) {
        val db = database.readableDatabase
        val duplicates = DebtMovementAccountingReader.duplicateEventIds(db)
        db.rawQuery("""
            SELECT e.id, a.name, e.event_type, e.amount_minor, e.fee_minor,
                   e.occurred_at_epoch_ms, e.linked_transaction_id
            FROM debt_events e JOIN debt_accounts a ON a.id = e.debt_account_id
            WHERE e.occurred_at_epoch_ms >= ? AND e.occurred_at_epoch_ms < ?
              AND e.event_type IN ('DRAWDOWN', 'REPAYMENT', 'FEE')
            ORDER BY e.occurred_at_epoch_ms DESC, e.id DESC
        """.trimIndent(), arrayOf(start.toString(), end.toString())).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    if (id in duplicates) continue
                    add(FocusedMovement("debt:$id", cursor.getString(1), cursor.getString(2),
                        cursor.getLong(3), cursor.getLong(4), cursor.getLong(5),
                        if (cursor.isNull(6)) null else cursor.getLong(6)))
                }
            }
        }
    }

    fun investments(start: Long, end: Long): List<FocusedMovement> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery("""
            SELECT id, instrument_label, value_minor, occurred_at_epoch_ms, linked_transaction_id
            FROM investment_events
            WHERE event_type = 'BUY_EXECUTED' AND occurred_at_epoch_ms >= ? AND occurred_at_epoch_ms < ?
            ORDER BY occurred_at_epoch_ms DESC, id DESC
        """.trimIndent(), arrayOf(start.toString(), end.toString())).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(FocusedMovement(
                    "investment:${cursor.getLong(0)}", cursor.getString(1), "INVESTED",
                    if (cursor.isNull(2)) null else cursor.getLong(2),
                    atEpochMs = cursor.getLong(3),
                    transactionId = if (cursor.isNull(4)) null else cursor.getLong(4)
                ))
            }
        }
    }
}
