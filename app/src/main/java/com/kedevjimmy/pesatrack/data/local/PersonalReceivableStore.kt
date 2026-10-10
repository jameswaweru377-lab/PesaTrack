package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import java.util.UUID
import com.kedevjimmy.pesatrack.domain.receivables.SharedBillPolicy
import com.kedevjimmy.pesatrack.domain.receivables.DebtMovement
import com.kedevjimmy.pesatrack.domain.receivables.ReceivableBatchPolicy

/** Personal money owed TO the user. Separate from institutional liabilities in DebtStore. */
data class ReceivablePerson(
    val id: String,
    val name: String,
    val phone: String?,
    val outstandingMinor: Long?,
    val lastActivityAt: Long?
)

data class ReceivableEvent(
    val id: Long,
    val personId: String,
    val kind: String,
    val amountMinor: Long,
    val occurredAt: Long,
    val transactionId: Long?,
    val memo: String?,
    val active: Boolean,
    val sharedBill: Boolean = false
)

data class ReceivableShare(val lentMinor: Long = 0L, val repaidMinor: Long = 0L)

class PersonalReceivableStore(context: Context) {
    private val app = context.applicationContext
    private val database = PesaTrackDatabase.get(app)
    private val transactions = TransactionStore.get(app)

    companion object {
        const val LENT = "LENT"
        const val REPAID = "REPAID"
    }

    /** Does not guess that two people sharing a name are the same. */
    fun people(): List<ReceivablePerson> = synchronized(CanonicalStoreLock.monitor) {
        val db = database.readableDatabase
        db.rawQuery("SELECT id, display_name, phone FROM personal_receivable_people ORDER BY display_name COLLATE NOCASE", null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val history = events(id)
                    val outstanding = try { balance(history) }
                    catch (_: IllegalStateException) { null }
                    catch (_: ArithmeticException) { null }
                    add(ReceivablePerson(id, c.getString(1),
                        if (c.isNull(2)) null else c.getString(2),
                        outstanding, history.firstOrNull()?.occurredAt))
                }
            }
        }
    }

    fun events(personId: String): List<ReceivableEvent> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            """SELECT id, kind, amount_minor, occurred_at_epoch_ms,
                      linked_transaction_id, memo, event_key FROM personal_receivable_events
               WHERE person_id = ? ORDER BY occurred_at_epoch_ms DESC, id DESC""",
            arrayOf(personId)
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val tx = if (c.isNull(4)) null else c.getLong(4)
                    val kind = c.getString(1)
                    val amount = c.getLong(2)
                    add(ReceivableEvent(c.getLong(0), personId, kind, amount, c.getLong(3),
                        tx, if (c.isNull(5)) null else c.getString(5),
                        tx == null || activeLinkedTransaction(tx, kind, amount, SharedBillPolicy.isSharedEvent(c.getString(6))),
                        SharedBillPolicy.isSharedEvent(c.getString(6))))
                }
            }
        }
    }

    fun eventForTransaction(transactionId: Long): ReceivableEvent? =
        synchronized(CanonicalStoreLock.monitor) {
            database.readableDatabase.rawQuery(
                "SELECT id, person_id, kind, amount_minor, occurred_at_epoch_ms, memo, event_key FROM personal_receivable_events WHERE linked_transaction_id = ? LIMIT 1",
                arrayOf(transactionId.toString())
            ).use { c ->
                if (!c.moveToFirst()) null else ReceivableEvent(
                    c.getLong(0), c.getString(1), c.getString(2), c.getLong(3),
                    c.getLong(4), transactionId, if (c.isNull(5)) null else c.getString(5),
                    activeLinkedTransaction(transactionId, c.getString(2), c.getLong(3), SharedBillPolicy.isSharedEvent(c.getString(6))),
                    SharedBillPolicy.isSharedEvent(c.getString(6)))
            }
        }

    /** Recomputed from journal events; invalidated source movements are excluded from totals. */
    private fun balance(history: List<ReceivableEvent>): Long {
        var total = 0L
        history.filter { it.active }.forEach {
            total = if (it.kind == LENT) Math.addExact(total, it.amountMinor)
                    else Math.subtractExact(total, it.amountMinor)
        }
        check(total >= 0L) { "A repayment exceeds valid loan evidence. Review the source transactions." }
        return total
    }

    private fun activeLinkedTransaction(id: Long, kind: String, amount: Long, sharedBill: Boolean = false): Boolean {
        val original = transactions.getById(id) ?: return false
        if (sharedBill && (kind != LENT || !SharedBillPolicy.validAmount(original.amountMinor,amount))) return false
        if (original.reviewState == ReviewState.EXCLUDED.name ||
            original.reviewState == ReviewState.NEEDS_REVIEW.name ||
            original.relationType != null || original.reversalOfCode != null ||
            original.amountMinor < amount) return false
        if (original.direction != (if (kind == LENT) MoneyDirection.EXPENSE.name else MoneyDirection.INCOME.name)) return false
        // Once a refund has been observed, we cannot know whether the merchant
        // refunded the user's share or the friend's share. Exclude the loan link
        // until the user reviews the original accounting evidence.
        val refundObserved = database.readableDatabase.rawQuery(
            "SELECT 1 FROM transactions WHERE related_transaction_id = ? AND relation_type = 'REFUND' AND review_state != 'EXCLUDED' LIMIT 1",
            arrayOf(id.toString())
        ).use { it.moveToFirst() }
        if (refundObserved) return false
        if (original.transactionCode != null) {
            val canceled = database.readableDatabase.rawQuery(
                "SELECT 1 FROM transactions WHERE reversal_of_code = ? LIMIT 1",
                arrayOf(original.transactionCode)
            ).use { it.moveToFirst() }
            if (canceled) return false
        }
        return true
    }

    /** Maps person-specific principal to canonical events without altering wallet amounts. */
    fun linkedShares(): Map<Long, ReceivableShare> = synchronized(CanonicalStoreLock.monitor) {
        val result = linkedMapOf<Long, ReceivableShare>()
        database.readableDatabase.rawQuery(
            "SELECT linked_transaction_id, kind, amount_minor, event_key FROM personal_receivable_events WHERE linked_transaction_id IS NOT NULL", null
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val kind = c.getString(1)
                val amount = c.getLong(2)
                if (!activeLinkedTransaction(id, kind, amount, SharedBillPolicy.isSharedEvent(c.getString(3)))) continue
                val old = result[id] ?: ReceivableShare()
                result[id] = if (kind == LENT) old.copy(lentMinor = Math.addExact(old.lentMinor, amount))
                             else old.copy(repaidMinor = Math.addExact(old.repaidMinor, amount))
            }
        }
        result
    }

    /** One authoritative user decision at a time; the existing SMS transaction is never duplicated. */
    fun record(
        personId: String?,
        name: String,
        phone: String?,
        kind: String,
        amountMinor: Long,
        occurredAt: Long,
        transactionId: Long? = null,
        memo: String? = null,
        sharedBill: Boolean = false,
        expectedTransaction: StoredTransaction? = null
    ): Long = synchronized(CanonicalStoreLock.monitor) {
        require(!sharedBill || (kind == LENT && transactionId != null)) { "Shared bills must link an outgoing payment." }
        require(expectedTransaction == null || expectedTransaction.id == transactionId) { "Check the source payment." }
        require(kind == LENT || kind == REPAID) { "Select loan or repayment." }
        require(amountMinor > 0 && occurredAt > 0 && occurredAt <= System.currentTimeMillis()) { "Check the amount and date." }
        require(name.trim().length in 2..100) { "Enter the person's name." }
        require(memo == null || memo.length <= 250) { "Keep notes below 250 characters." }
        val normalizedPhone = phone?.trim()?.takeIf { it.isNotEmpty() }
        require(normalizedPhone == null || Regex("[+0-9 -]{7,20}").matches(normalizedPhone)) { "Check the phone number." }
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            require(expectedTransaction == null || transactions.getById(expectedTransaction.id) == expectedTransaction) {
                "The payment changed. Close and reopen it before saving."
            }
            val eventDate = transactionId?.let { transactions.getById(it)?.occurredAtEpochMs } ?: occurredAt
            require(eventDate > 0 && eventDate <= System.currentTimeMillis()) { "Review the transaction date." }
            if (transactionId != null) {
                require(activeLinkedTransaction(transactionId, kind, amountMinor)) { "Confirm a valid money movement before linking it." }
                val original = transactions.getById(transactionId) ?: error("Transaction not found.")
                require(amountMinor <= original.amountMinor) { "Amount exceeds the payment." }
                require(!sharedBill || SharedBillPolicy.validAmount(original.amountMinor,amountMinor)) {
                    "The shared amount must be greater than zero and less than the bill. Use Money lent for a full loan."
                }
                require(eventForTransaction(transactionId) == null) { "This transaction is already linked to a personal debt." }
                val hasSplit = db.rawQuery(
                    "SELECT 1 FROM transaction_allocations WHERE transaction_id = ? LIMIT 1",
                    arrayOf(transactionId.toString())
                ).use { it.moveToFirst() }
                require(!hasSplit) { "Remove the existing category split before linking a borrowed share." }
            }
            val id = if (personId != null) {
                val existing = db.rawQuery("SELECT id FROM personal_receivable_people WHERE id = ?", arrayOf(personId))
                    .use { if (it.moveToFirst()) it.getString(0) else null }
                require(existing != null) { "Person no longer available." }
                existing
            } else {
                UUID.randomUUID().toString().also { generated ->
                    db.insertOrThrow("personal_receivable_people", null, ContentValues().apply {
                        put("id", generated); put("display_name", name.trim()); put("phone", normalizedPhone)
                        put("created_at_epoch_ms", System.currentTimeMillis())
                    })
                }
            }
            val existing = events(id)
            require(existing.all { it.active }) { "Review changed source evidence before adding a record." }
            ReceivableBatchPolicy.validate(
                existing.map { DebtMovement(it.transactionId,it.kind,it.amountMinor,it.occurredAt) },
                listOf(DebtMovement(transactionId,kind,amountMinor,eventDate))
            )
            if (transactionId != null) db.delete("personal_receivable_review_exclusions",
                "person_id = ? AND transaction_id = ?",arrayOf(id,transactionId.toString()))
            val eventId = db.insertOrThrow("personal_receivable_events", null, ContentValues().apply {
                put("event_key", (if (sharedBill) SharedBillPolicy.EVENT_PREFIX else "user:") + UUID.randomUUID())
                put("person_id", id); put("kind", kind); put("amount_minor", amountMinor)
                put("occurred_at_epoch_ms", eventDate); put("linked_transaction_id", transactionId)
                put("memo", memo?.trim()); put("created_at_epoch_ms", System.currentTimeMillis())
            })
            db.setTransactionSuccessful()
            eventId
        } finally {
            db.endTransaction()
            LedgerUpdateBus.notifyChanged()
        }
    }

    /** Removes only the loan annotation, never the canonical bank/M-Pesa movement. */
    fun removeEvent(eventId: Long) = synchronized(CanonicalStoreLock.monitor) {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val person = db.rawQuery(
                "SELECT person_id, kind FROM personal_receivable_events WHERE id = ?",
                arrayOf(eventId.toString())
            ).use { if (it.moveToFirst()) it.getString(0) to it.getString(1) else null }
            if (person != null) {
                db.delete("personal_receivable_events", "id = ?", arrayOf(eventId.toString()))
                // A loan must not be removed while retained repayments would create a negative balance.
                try { balance(events(person.first)) }
                catch (failure: IllegalStateException) {
                    // Removing a repayment improves an invalid negative history.
                    // Keep allowing correction one annotation at a time.
                    if (person.second != REPAID) throw failure
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            LedgerUpdateBus.notifyChanged()
        }
    }

    /** Reused by history search and revalidated inside its atomic save. */
    internal fun linkProblem(id: Long, kind: String, amount: Long): String? = synchronized(CanonicalStoreLock.monitor) {
        val row = transactions.getById(id) ?: return@synchronized "Transaction is no longer available."
        if (kind !in setOf(LENT, REPAID) || amount <= 0 || amount > row.amountMinor)
            return@synchronized "Check the loan or repayment amount."
        if (row.occurredAtEpochMs <= 0 || row.occurredAtEpochMs > System.currentTimeMillis())
            return@synchronized "Review the transaction date."
        if (eventForTransaction(id) != null) return@synchronized "Already linked to a personal debt."
        if (!activeLinkedTransaction(id,kind,amount)) return@synchronized "Source requires review or is a transfer, reversal or refund."
        val split = database.readableDatabase.rawQuery(
            "SELECT 1 FROM transaction_allocations WHERE transaction_id = ? LIMIT 1",arrayOf(id.toString())
        ).use { it.moveToFirst() }
        if (split) "Remove the existing category split before linking a borrowed share." else null
    }

    fun confirmHistory(personId: String?, name: String, phone: String?,
        selections: List<ReceivableHistorySelection>, exclusions: List<StoredTransaction>): String =
        ReceivableHistoryRepository(app).confirm(personId,name,phone,selections,exclusions)

    fun linkedEvents(): Map<Long, ReceivableEvent> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            "SELECT linked_transaction_id FROM personal_receivable_events WHERE linked_transaction_id IS NOT NULL",null
        ).use { c -> buildMap { while (c.moveToNext()) {
            val id = c.getLong(0)
            eventForTransaction(id)?.let { put(id,it) }
        } } }
    }

    fun nameFor(personId: String): String? = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            "SELECT display_name FROM personal_receivable_people WHERE id = ?", arrayOf(personId)
        ).use { if (it.moveToFirst()) it.getString(0) else null }
    }
}
