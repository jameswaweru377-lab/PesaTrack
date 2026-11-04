package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.rules.PossibleDuplicateObservation
import com.kedevjimmy.pesatrack.domain.rules.PossibleDuplicatePolicy

/**
 * The only write boundary for possible-duplicate review decisions.
 * Keep weak evidence review-only until the user's current card is verified.
 */
internal class DuplicateResolutionWriter(
    context: Context,
    private val database: PesaTrackDatabase,
    private val loadTransaction: (Long) -> StoredTransaction?
) {
    private val appContext = context.applicationContext

    fun resolve(
        pairKey: String,
        sameTransaction: Boolean,
        expectedFirst: StoredTransaction?,
        expectedSecond: StoredTransaction?
    ): Boolean = synchronized(CanonicalStoreLock.monitor) {
        val ids = PossibleDuplicatePolicy.parsePairIds(pairKey)
            ?: return@synchronized false
        if (listOf(ids.first, ids.second).any(
                OwnedTransferReviewStore(appContext)::isConfirmedPair
            )) return@synchronized false

        val db = database.writableDatabase
        var applied = false
        db.beginTransaction()
        try {
            val first = loadTransaction(ids.first) ?: return@synchronized false
            val second = loadTransaction(ids.second) ?: return@synchronized false

            // A displayed card must still refer to *exactly* the same saved rows.
            // This covers fees, account assignment and other fields outside the
            // weaker candidate-matching policy's observation.
            if (expectedFirst != null || expectedSecond != null) {
                if (first != expectedFirst || second != expectedSecond) {
                    return@synchronized false
                }
            }
            if (sameTransaction) {
                val shownFirst = expectedFirst ?: return@synchronized false
                val shownSecond = expectedSecond ?: return@synchronized false
                if (!PossibleDuplicatePolicy.canConfirm(
                        pairKey,
                        shownFirst.toPossibleDuplicateObservation(),
                        shownSecond.toPossibleDuplicateObservation(),
                        first.toPossibleDuplicateObservation(),
                        second.toPossibleDuplicateObservation()
                    )) return@synchronized false
            }
            if (hasExistingDecision(db, ids.first, ids.second)) {
                return@synchronized false
            }

            if (sameTransaction) {
                val canonical = chooseDuplicateCanonical(first, second)
                val duplicate = if (canonical.id == first.id) second else first
                check(db.update("transactions", ContentValues().apply {
                    put("review_state", ReviewState.EXCLUDED.name)
                    putNull("cycle_candidate_reason")
                }, "id = ?", arrayOf(duplicate.id.toString())) == 1) {
                    "Duplicate source row changed during confirmation."
                }
            }
            val now = System.currentTimeMillis()
            val canonicalKey = minOf(ids.first, ids.second).toString() +
                ":" + maxOf(ids.first, ids.second)
            check(db.insertWithOnConflict(
                "duplicate_resolutions", null, ContentValues().apply {
                    put("pair_key", canonicalKey)
                    put("first_transaction_id", ids.first)
                    put("second_transaction_id", ids.second)
                    put("decision", if (sameTransaction) "SAME_TRANSACTION" else "KEEP_BOTH")
                    put("created_at_epoch_ms", now)
                    put("updated_at_epoch_ms", now)
                }, SQLiteDatabase.CONFLICT_ABORT
            ) != -1L) { "Could not persist duplicate decision." }

            db.setTransactionSuccessful()
            applied = true
        } finally {
            db.endTransaction()
        }

        if (applied) {
            try { PayCycleStore.get(appContext).rebuild() }
            finally { LedgerUpdateBus.notifyChanged() }
        }
        applied
    }

    private fun hasExistingDecision(db: SQLiteDatabase, first: Long, second: Long): Boolean =
        db.rawQuery(
            """SELECT 1 FROM duplicate_resolutions
               WHERE (first_transaction_id=? AND second_transaction_id=?)
                  OR (first_transaction_id=? AND second_transaction_id=?)
               LIMIT 1""",
            arrayOf(first.toString(), second.toString(), second.toString(), first.toString())
        ).use { it.moveToFirst() }

    private fun chooseDuplicateCanonical(
        first: StoredTransaction,
        second: StoredTransaction
    ): StoredTransaction {
        fun score(item: StoredTransaction): Int {
            var value = 0
            if (!item.transactionCode.isNullOrBlank()) value += 8
            if (!item.counterparty.isNullOrBlank()) value += 4
            if (!item.counterpartyPhone.isNullOrBlank()) value += 3
            if (!item.evidenceLabel.isNullOrBlank()) value += 2
            if (item.type != MpesaTransactionType.OTHER.name) value += 2
            if (item.direction != MoneyDirection.NEUTRAL.name) value += 1
            return value
        }
        val firstScore = score(first)
        val secondScore = score(second)
        return when {
            firstScore > secondScore -> first
            secondScore > firstScore -> second
            else -> if (first.id <= second.id) first else second
        }
    }
}

internal fun StoredTransaction.toPossibleDuplicateObservation(): PossibleDuplicateObservation =
    PossibleDuplicateObservation(
        id = id,
        amountMinor = amountMinor,
        receivedAtEpochMs = receivedAtEpochMs,
        transactionCode = transactionCode,
        sourcePackage = sourcePackage,
        sourceKind = sourceKind,
        direction = direction,
        reviewState = reviewState,
        category = category,
        counterparty = counterparty,
        evidenceLabel = evidenceLabel,
        type = type
    )
