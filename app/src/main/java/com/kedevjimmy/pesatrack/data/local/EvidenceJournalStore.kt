package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.evidence.EvidenceCashFact
import com.kedevjimmy.pesatrack.domain.evidence.EvidenceCanonicalFact
import com.kedevjimmy.pesatrack.domain.evidence.EvidenceLinkSafetyPolicy
import java.security.MessageDigest
import java.util.Locale

data class EvidenceSourceSummary(val sourceKind: String, val reason: String, val needsReview: Boolean)

/** Local hashed source identity; never stores full SMS or imported document contents. */
internal object EvidenceJournalStore {
    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 255) }

    private fun normalizedReference(value: String?): String? =
        value?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }

    private fun canonical(db: SQLiteDatabase, id: Long): EvidenceCanonicalFact? =
        db.rawQuery("""
            SELECT account_id,direction,amount_minor,occurred_at_epoch_ms,transaction_code,source_kind
            FROM transactions WHERE id=?
        """.trimIndent(), arrayOf(id.toString())).use { cursor ->
            if (!cursor.moveToFirst()) null else EvidenceCanonicalFact(
                if (cursor.isNull(0)) null else cursor.getString(0),
                cursor.getString(1),cursor.getLong(2),cursor.getLong(3),
                if (cursor.isNull(4)) null else cursor.getString(4),cursor.getString(5)
            )
        }

    private fun snapshot(row: EvidenceCanonicalFact): String =
        hash(listOf(row.accountId.orEmpty(),row.direction,row.amountMinor.toString(),
            row.occurredAtEpochMs.toString(),normalizedReference(row.transactionReference).orEmpty()).joinToString("|"))

    /**
     * Call only inside an existing SQLite write transaction and CanonicalStoreLock.
     * A failed match produces pending evidence, never a new transaction.
     */
    fun observe(
        db: SQLiteDatabase, sourceKind: String, scope: String, nativeKey: String,
        accountId: String, signedAmountMinor: Long, occurredAtEpochMs: Long,
        reference: String?, candidateTransactionId: Long?, reason: String
    ) {
        require(sourceKind in setOf("MANUAL","STATEMENT","SMS","NOTIFICATION"))
        require(scope.isNotBlank() && nativeKey.isNotBlank() && accountId.isNotBlank())
        require(signedAmountMinor != 0L && signedAmountMinor != Long.MIN_VALUE && occurredAtEpochMs > 0L)
        val direction = if (signedAmountMinor > 0L) "INCOME" else "EXPENSE"
        val amount = kotlin.math.abs(signedAmountMinor)
        val referenceKey = normalizedReference(reference)
        val id = hash(sourceKind+"|"+scope+"|"+nativeKey)
        val fact = EvidenceCashFact(accountId,direction,amount,occurredAtEpochMs,referenceKey)
        val factHash = hash(accountId+"|"+direction+"|"+amount+"|"+occurredAtEpochMs+"|"+referenceKey.orEmpty())
        val existing = db.rawQuery(
            "SELECT fact_hash,status FROM evidence_observations WHERE id=?",arrayOf(id)
        ).use { c -> if (c.moveToFirst()) c.getString(0) to c.getString(1) else null }
        require(existing == null || existing.first == factHash) {
            "Source evidence conflicts with an earlier import. Review it before continuing."
        }
        if (existing == null) db.insertOrThrow("evidence_observations",null,ContentValues().apply {
            put("id",id);put("source_kind",sourceKind);put("source_scope_hash",hash(scope))
            put("source_key_hash",hash(nativeKey));put("fact_hash",factHash)
            put("account_id",accountId);put("direction",direction);put("amount_minor",amount)
            put("occurred_at_epoch_ms",occurredAtEpochMs)
            if(referenceKey==null) putNull("reference_hash") else put("reference_hash",hash(referenceKey))
            put("status","PENDING");put("created_at_epoch_ms",System.currentTimeMillis().coerceAtLeast(1L))
        })
        if (existing?.second=="REJECTED" || candidateTransactionId==null || candidateTransactionId<=0L) return
        val alreadyLinked = db.rawQuery(
            "SELECT 1 FROM evidence_links WHERE observation_id=?",arrayOf(id)
        ).use { it.moveToFirst() }
        if(alreadyLinked) return // Replay must never change the link target.
        val target = canonical(db,candidateTransactionId) ?: return
        if(!EvidenceLinkSafetyPolicy.canLink(fact,target,reason)) return
        if(reason=="CREATED") {
            if((sourceKind=="MANUAL" && target.sourceKind!="manual_entry") ||
                (sourceKind=="STATEMENT" && target.sourceKind!="statement_import")) return
        } else if(reason=="REFERENCE") {
            val code = normalizedReference(target.transactionReference) ?: return
            val count = db.rawQuery(
                "SELECT count(*) FROM transactions WHERE UPPER(TRIM(transaction_code))=?",
                arrayOf(code)
            ).use { it.moveToFirst();it.getLong(0) }
            if(count!=1L) return
        } else return

        db.insertOrThrow("evidence_links",null,ContentValues().apply {
            put("observation_id",id);put("transaction_id",candidateTransactionId)
            put("reason",reason);put("transaction_snapshot_hash",snapshot(target))
            put("created_at_epoch_ms",System.currentTimeMillis().coerceAtLeast(1L))
        })
        db.update("evidence_observations",ContentValues().apply {put("status","LINKED")},"id=?",arrayOf(id))
        db.insertOrThrow("evidence_link_audit",null,ContentValues().apply {
            put("observation_id",id);put("transaction_id",candidateTransactionId)
            put("action","LINKED");put("reason",reason)
            put("created_at_epoch_ms",System.currentTimeMillis().coerceAtLeast(1L))
        })
    }

    /** Unlink is evidence-only and blocks silent re-linking on replay. */
    fun rejectLink(context: Context, observationId: String): Boolean =
        synchronized(CanonicalStoreLock.monitor) {
            val db=PesaTrackDatabase.get(context.applicationContext).writableDatabase
            db.beginTransaction()
            try {
                val prior=db.rawQuery("SELECT transaction_id FROM evidence_links WHERE observation_id=?",
                    arrayOf(observationId)).use {c->if(c.moveToFirst())c.getLong(0)else null}
                val changed=db.update("evidence_observations",
                    ContentValues().apply {put("status","REJECTED")},
                    "id=? AND status!='REJECTED'",arrayOf(observationId))>0
                if(changed) {
                    db.delete("evidence_links","observation_id=?",arrayOf(observationId))
                    db.insertOrThrow("evidence_link_audit",null,ContentValues().apply {
                        put("observation_id",observationId)
                        if(prior==null) putNull("transaction_id") else put("transaction_id",prior)
                        put("action","UNLINKED");put("reason","USER_REJECTED")
                        put("created_at_epoch_ms",System.currentTimeMillis().coerceAtLeast(1L))
                    })
                }
                db.setTransactionSuccessful()
                changed
            } finally {db.endTransaction()}
        }

    fun sourcesFor(context: Context, transactionId: Long): List<EvidenceSourceSummary> =
        synchronized(CanonicalStoreLock.monitor) {
            val db=PesaTrackDatabase.get(context.applicationContext).readableDatabase
            val row=canonical(db,transactionId) ?: return@synchronized emptyList()
            val currentHash=snapshot(row)
            db.rawQuery("""
                SELECT o.source_kind,l.reason,l.transaction_snapshot_hash
                FROM evidence_links l JOIN evidence_observations o ON o.id=l.observation_id
                WHERE l.transaction_id=? AND o.status='LINKED'
                ORDER BY o.created_at_epoch_ms DESC LIMIT 12
            """.trimIndent(),arrayOf(transactionId.toString())).use {c->
                buildList {
                    while(c.moveToNext()) add(EvidenceSourceSummary(
                        c.getString(0),c.getString(1),c.getString(2)!=currentHash))
                }
            }
        }
}
