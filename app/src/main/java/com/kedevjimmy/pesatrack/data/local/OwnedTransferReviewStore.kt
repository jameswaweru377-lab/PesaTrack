package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.evidence.*
import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
import java.security.MessageDigest
import org.json.JSONObject

/** Link/review adapter over the canonical ledger; never imports or invents a cash movement. */
class OwnedTransferReviewStore(context: Context) {
    private val app = context.applicationContext
    private val database = PesaTrackDatabase.get(app)
    private val preferences = app.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)

    fun pendingCandidates(limit: Int = 20): List<TransferReviewCandidate> = synchronized(CanonicalStoreLock.monitor) {
        candidates(database.readableDatabase).take(limit.coerceIn(1, 100))
    }

    fun decide(candidate: TransferReviewCandidate, decision: TransferPairDecision): TransferDecisionResult =
        synchronized(CanonicalStoreLock.monitor) {
            val db = database.writableDatabase
            var result = TransferDecisionResult.STALE
            db.beginTransaction()
            try {
                val current = candidates(db).firstOrNull { it.pairKey == candidate.pairKey }
                if (current == candidate) {
                    val sourceBefore = if (decision == TransferPairDecision.CONFIRMED) captureProjection(db, current.source.id) else null
                    val destinationBefore = if (decision == TransferPairDecision.CONFIRMED) captureProjection(db, current.destination.id) else null
                    if (decision == TransferPairDecision.CONFIRMED) {
                        TransactionStore.get(app).applyReviewedOwnedTransfer(db, current)
                    }
                    db.insertWithOnConflict("owned_transfer_review_decisions", null, ContentValues().apply {
                        put("pair_key", current.pairKey)
                        put("source_transaction_id", current.source.id)
                        put("destination_transaction_id", current.destination.id)
                        put("source_snapshot_hash", current.source.snapshotHash)
                        put("destination_snapshot_hash", current.destination.snapshotHash)
                        put("source_ownership_revision", current.sourceOwnershipRevision)
                        put("destination_ownership_revision", current.destinationOwnershipRevision)
                        put("decision", decision.name)
                        put("source_before", sourceBefore)
                        put("destination_before", destinationBefore)
                        put("updated_at_epoch_ms", System.currentTimeMillis())
                    }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) { "Could not save transfer decision." } }
                    result = TransferDecisionResult.APPLIED
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            if (result == TransferDecisionResult.APPLIED) {
                try { if (decision == TransferPairDecision.CONFIRMED) PayCycleStore.get(app).rebuild() }
                finally { LedgerUpdateBus.notifyChanged() }
            }
            result
        }

    fun isConfirmedPair(transactionId: Long): Boolean = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery("SELECT 1 FROM owned_transfer_review_decisions WHERE decision='CONFIRMED' AND (source_transaction_id=? OR destination_transaction_id=?) LIMIT 1",
            arrayOf(transactionId.toString(), transactionId.toString())).use { it.moveToFirst() }
    }

    /** Undo both projections together. Only fields changed by confirmation are restored. */
    fun undoConfirmedPair(transactionId: Long): Boolean = synchronized(CanonicalStoreLock.monitor) {
        val db = database.writableDatabase
        var changed = false
        db.beginTransaction()
        try {
            db.rawQuery("SELECT * FROM owned_transfer_review_decisions WHERE decision='CONFIRMED' AND (source_transaction_id=? OR destination_transaction_id=?)",
                arrayOf(transactionId.toString(), transactionId.toString())).use { cursor ->
                if (cursor.moveToFirst()) {
                    val pairKey = cursor.string("pair_key")!!
                    val source = cursor.long("source_transaction_id")
                    val destination = cursor.long("destination_transaction_id")
                    val sourceBefore = decodeProjection(cursor.string("source_before"))
                    val destinationBefore = decodeProjection(cursor.string("destination_before"))
                    check(db.update("owned_transfer_review_decisions", ContentValues().apply {
                        put("decision", "REJECTED"); put("updated_at_epoch_ms", System.currentTimeMillis())
                    }, "pair_key=? AND decision='CONFIRMED'", arrayOf(pairKey)) == 1)
                    check(db.update("transactions", sourceBefore, "id=?", arrayOf(source.toString())) == 1)
                    check(db.update("transactions", destinationBefore, "id=?", arrayOf(destination.toString())) == 1)
                    listOf(source to cursor.string("source_snapshot_hash"), destination to cursor.string("destination_snapshot_hash")).forEach { (id, snapshot) ->
                        db.delete("account_balance_observations", "observation_key=?", arrayOf("reviewed-transfer-balance:$id:$snapshot"))
                    }
                    changed = true
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        if (changed) { try { PayCycleStore.get(app).rebuild() } finally { LedgerUpdateBus.notifyChanged() } }
        changed
    }

    private val projectionFields = listOf("direction", "category", "review_state", "linked_account_id", "evidence_source", "cycle_candidate_reason")
    private fun captureProjection(db: SQLiteDatabase, id: Long): String = db.query("transactions", projectionFields.toTypedArray(), "id=?", arrayOf(id.toString()), null, null, null).use { cursor ->
        check(cursor.moveToFirst())
        JSONObject().apply { projectionFields.forEach { field -> put(field, cursor.string(field) ?: JSONObject.NULL) } }.toString()
    }
    private fun decodeProjection(raw: String?): ContentValues {
        check(raw != null) { "This transfer match has no safe undo snapshot." }
        val json = JSONObject(raw)
        check(json.keys().asSequence().toSet() == projectionFields.toSet()) { "Invalid transfer undo snapshot." }
        return ContentValues().apply { projectionFields.forEach { field -> if (json.isNull(field)) putNull(field) else put(field, json.getString(field)) } }
    }

    private fun candidates(db: SQLiteDatabase): List<TransferReviewCandidate> {
        val consent = AccountOwnershipConsentStore.confirmed(db)
        if (consent.size < 2) return emptyList()
        // Refuse a truncated cohort before loading references or relationship snapshots.
        val rowCount = db.rawQuery("""
            SELECT COUNT(*) FROM (SELECT t.id FROM transactions t
              JOIN money_accounts a ON a.id=t.account_id
              WHERE t.direction IN ('EXPENSE','INCOME') LIMIT 5001)
        """.trimIndent(), null).use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }
        if (rowCount > 5000) return emptyList()
        val keptSeparate = db.rawQuery("SELECT first_transaction_id,second_transaction_id FROM duplicate_resolutions WHERE decision='KEEP_BOTH'", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) {
                val first = cursor.getLong(0); val second = cursor.getLong(1)
                add(minOf(first, second) to maxOf(first, second))
            } }
        }
        val references = mutableMapOf<Long, MutableSet<String>>()
        db.rawQuery("""
            SELECT r.transaction_id,r.reference_value FROM transaction_external_references r
            JOIN transactions t ON t.id=r.transaction_id
            JOIN money_accounts a ON a.id=t.account_id
            WHERE t.direction IN ('EXPENSE','INCOME')
        """.trimIndent(), null).use { cursor ->
            while (cursor.moveToNext()) references.getOrPut(cursor.getLong(0)) { mutableSetOf() }.add(cursor.getString(1))
        }
        val rejectedIdentities = db.rawQuery("SELECT identity_id FROM identity_review_decisions WHERE decision='REJECTED_SELF'", null)
            .use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) } }
        val rows = db.rawQuery("""
            SELECT t.*,a.type AS b2_account_type,
              (EXISTS(SELECT 1 FROM debt_events d WHERE d.linked_transaction_id=t.id)
               OR EXISTS(SELECT 1 FROM investment_events i WHERE i.linked_transaction_id=t.id)
               OR EXISTS(SELECT 1 FROM personal_receivable_events p WHERE p.linked_transaction_id=t.id)
               OR EXISTS(SELECT 1 FROM transaction_allocations s WHERE s.transaction_id=t.id)
               OR EXISTS(SELECT 1 FROM payday_snapshots p WHERE p.salary_transaction_id=t.id)
               OR EXISTS(SELECT 1 FROM transactions r WHERE r.related_transaction_id=t.id OR
                   (t.transaction_code IS NOT NULL AND r.reversal_of_code=t.transaction_code))) AS b2_related
            FROM transactions t JOIN money_accounts a ON a.id=t.account_id
            WHERE t.direction IN ('EXPENSE','INCOME')
            ORDER BY t.occurred_at_epoch_ms DESC,t.id DESC LIMIT 5001
        """.trimIndent(), null).use { cursor -> buildList {
            while (cursor.moveToNext()) {
                val id = cursor.long("id")
                val refs = references[id].orEmpty().toMutableSet()
                cursor.string("transaction_code")?.let { refs.add(it) }
                val protected = cursor.string("review_state") !in setOf("NEEDS_REVIEW", "AUTO_CLASSIFIED") ||
                    listOf("rule_id", "relation_type", "related_transaction_id", "reversal_of_code", "linked_account_id",
                        "pay_cycle_id", "cycle_override").any { cursor.string(it) != null } ||
                    cursor.string("type") == "REVERSAL" || cursor.string("category") !in setOf("UNCATEGORIZED", "OTHER", "INCOME") ||
                    cursor.long("fuliza_financed_minor") != 0L || cursor.long("fuliza_access_fee_minor") != 0L ||
                    cursor.string("fuliza_event_code") != null || cursor.long("b2_related") != 0L ||
                    cursor.string("source_kind").orEmpty().contains("payslip", ignoreCase = true) ||
                    cursor.string("source_kind").orEmpty().contains("planned", ignoreCase = true)
                val name = cursor.string("counterparty")
                val phone = cursor.string("counterparty_phone")
                val identityRejected = IdentityGraphPolicy.counterpartyIdentityId(name, phone) in rejectedIdentities ||
                    IdentityGraphPolicy.counterpartyIdentityId(name, null) in rejectedIdentities ||
                    (phone != null && IdentityGraphPolicy.counterpartyIdentityId(null, phone) in rejectedIdentities)
                // Hash every persisted transaction column and the independent reference set.
                // Null and empty are distinct; length-prefixes prevent ambiguous concatenation.
                val snapshot = (0 until cursor.columnCount).joinToString("") { index ->
                    if (cursor.isNull(index)) "N;" else cursor.getString(index).let { "${it.length}:$it;" }
                } + refs.sorted().joinToString("") { "${it.length}:$it;" }
                add(TransferReviewObservation(id, cursor.string("account_id").orEmpty(), cursor.string("b2_account_type").orEmpty(),
                    cursor.string("direction").orEmpty(), cursor.long("amount_minor"), cursor.long("fee_minor"), "KES",
                    cursor.long("occurred_at_epoch_ms"), refs, name, phone, hash(snapshot), protected, identityRejected,
                    cursor.string("transaction_code")))
            }
        } }
        // Fail closed rather than infer uniqueness from a truncated ledger.
        if (rows.size > 5000) return emptyList()
        val walletPhone = db.rawQuery("SELECT identifier FROM money_accounts WHERE id=? AND enabled=1", arrayOf(AccountStore.DEFAULT_MPESA_ID))
            .use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null }
        val decisions = db.rawQuery("SELECT * FROM owned_transfer_review_decisions", null).use { cursor -> buildMap {
            while (cursor.moveToNext()) put(cursor.string("pair_key").orEmpty(), listOf(
                cursor.string("decision"), cursor.string("source_snapshot_hash"), cursor.string("destination_snapshot_hash"),
                cursor.string("source_ownership_revision"), cursor.string("destination_ownership_revision")))
        } }
        return OwnedTransferReviewPolicy.candidates(rows, consent, preferences.getString("profile_name", null), walletPhone)
            .filter { candidate ->
                if ((minOf(candidate.source.id, candidate.destination.id) to maxOf(candidate.source.id, candidate.destination.id)) in keptSeparate) return@filter false
                val saved = decisions[candidate.pairKey]
                saved == null || (saved[0] == "DEFERRED" && (saved[1] != candidate.source.snapshotHash ||
                    saved[2] != candidate.destination.snapshotHash || saved[3] != candidate.sourceOwnershipRevision.toString() ||
                    saved[4] != candidate.destinationOwnershipRevision.toString()))
            }
    }
    private fun Cursor.string(column: String): String? = getColumnIndexOrThrow(column).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
    private fun hash(raw: String): String = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
