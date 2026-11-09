package com.kedevjimmy.pesatrack.data.local

import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.rules.DebtMovementDuplicatePolicy
import com.kedevjimmy.pesatrack.domain.rules.DebtMovementEvidence

/** Only linked, same-liability loan legs have enough proof to collapse. */
internal object DebtMovementAccountingReader {
    fun duplicateEventIds(db: SQLiteDatabase, accountId: String? = null): Set<Long> {
        val args = accountId?.let { arrayOf(it) }
        val accountFilter = if (accountId == null) "" else " AND debt_account_id = ?"
        val evidence = db.rawQuery(
            """
            SELECT id, debt_account_id, event_type, amount_minor, fee_minor,
                   linked_transaction_id, occurred_at_epoch_ms, outstanding_after_minor,
                   amount_is_net_proceeds, repayment_completion,
                   funding_link_user_decision, balance_link_user_decision
            FROM debt_events
            WHERE linked_transaction_id IS NOT NULL$accountFilter
            ORDER BY id
            """.trimIndent(), args
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(DebtMovementEvidence(
                    eventId = cursor.getLong(0), debtAccountId = cursor.getString(1),
                    type = cursor.getString(2), amountMinor = cursor.getLong(3),
                    feeMinor = cursor.getLong(4), linkedTransactionId = cursor.getLong(5),
                    occurredAtEpochMs = cursor.getLong(6),
                    outstandingAfterMinor = if (cursor.isNull(7)) null else cursor.getLong(7),
                    amountIsNetProceeds = if (cursor.isNull(8)) null else cursor.getInt(8) == 1,
                    repaymentCompletion = if (cursor.isNull(9)) null else cursor.getString(9),
                    hasUserDecision = !cursor.isNull(10) || !cursor.isNull(11)
                ))
            }
        }
        val retained = DebtMovementDuplicatePolicy.uniqueEventIds(evidence)
        return evidence.mapNotNull { it.eventId.takeUnless(retained::contains) }.toSet()
    }
}
