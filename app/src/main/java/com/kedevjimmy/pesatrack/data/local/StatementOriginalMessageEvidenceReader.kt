package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.statements.StatementActivityObservationPolicy

/**
 * Narrow, bounded, read-only query of original Activity message evidence.
 * This never invokes Activity's maintenance/reclassification snapshot path.
 */
internal class StatementOriginalMessageEvidenceReader(context: Context) {
    private val app = context.applicationContext

    fun read(
        startInclusive: Long,
        endExclusive: Long,
        ownedAccountIds: Set<String>,
        maximumRows: Int
    ): List<StatementActivityObservationPolicy.ActivityLeg> {
        require(startInclusive > 0L && endExclusive > startInclusive)
        require(maximumRows in 1..20_000)
        require(ownedAccountIds.size <= 200)
        if (ownedAccountIds.isEmpty()) return emptyList()

        val accounts = ownedAccountIds.sorted()
        val parameters = accounts.toTypedArray() + arrayOf(
            startInclusive.toString(), endExclusive.toString(), (maximumRows + 1).toString()
        )
        val marks = accounts.joinToString(",") { "?" }
        return synchronized(CanonicalStoreLock.monitor) {
            val db = PesaTrackDatabase.get(app).readableDatabase
            db.rawQuery(
                """SELECT id,account_id,source_kind,type,direction,amount_minor,
                      occurred_at_epoch_ms,transaction_code,review_state,category,
                      linked_account_id,relation_type,related_transaction_id,reversal_of_code,
                      fee_minor,fuliza_financed_minor,fuliza_access_fee_minor,fuliza_event_code,
                      (EXISTS(SELECT 1 FROM debt_events d WHERE d.linked_transaction_id=transactions.id)
                       OR EXISTS(SELECT 1 FROM investment_events i WHERE i.linked_transaction_id=transactions.id)
                       OR EXISTS(SELECT 1 FROM personal_receivable_events p WHERE p.linked_transaction_id=transactions.id)
                       OR EXISTS(SELECT 1 FROM transaction_allocations a WHERE a.transaction_id=transactions.id)
                       OR EXISTS(SELECT 1 FROM payday_snapshots p WHERE p.salary_transaction_id=transactions.id)
                       OR EXISTS(SELECT 1 FROM transactions r WHERE r.related_transaction_id=transactions.id OR
                           (transactions.transaction_code IS NOT NULL AND
                            r.reversal_of_code=transactions.transaction_code))) AS linked_cash_role
                   FROM transactions
                   WHERE account_id IN ($marks)
                     AND occurred_at_epoch_ms >= ? AND occurred_at_epoch_ms < ?
                     AND (LOWER(source_kind) LIKE '%sms%'
                          OR LOWER(source_kind) LIKE '%notification%')
                     AND LOWER(source_kind) NOT LIKE '%manual%'
                     AND LOWER(source_kind) NOT LIKE '%statement%'
                   ORDER BY id ASC LIMIT ?""".trimIndent(),
                parameters
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(StatementActivityObservationPolicy.ActivityLeg(
                            transactionId = cursor.getLong(0),
                            accountId = cursor.getString(1),
                            sourceKind = cursor.getString(2).orEmpty(),
                            type = cursor.getString(3).orEmpty(),
                            direction = cursor.getString(4).orEmpty(), currency = "KES",
                            amountMinor = cursor.getLong(5),
                            occurredAt = cursor.getLong(6), reference = cursor.getString(7),
                            reviewState = cursor.getString(8).orEmpty(),
                            category = cursor.getString(9).orEmpty(),
                            structurallyLinked = !cursor.isNull(10) || !cursor.isNull(11) ||
                                !cursor.isNull(12) || cursor.getLong(14) != 0L ||
                                cursor.getLong(15) != 0L || cursor.getLong(16) != 0L ||
                                !cursor.isNull(17) || cursor.getInt(18) != 0,
                            reversalOfCode = cursor.getString(13)
                        ))
                        require(size <= maximumRows) {
                            "This evidence range is too large. Choose a shorter period."
                        }
                    }
                }
            }
        }
    }
}
