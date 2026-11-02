package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.ingestion.FingerprintConflictPolicy
import com.kedevjimmy.pesatrack.domain.ingestion.FingerprintFacts
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import java.security.MessageDigest

/** Sidecar evidence only: these rows never participate in the canonical money ledger. */
internal object FingerprintConflictStore {
    data class PendingConflict(
        val id: Long,
        val existingTransactionId: Long,
        val sourceKind: String,
        val transactionCode: String,
        val type: String,
        val amountMinor: Long,
        val feeMinor: Long,
        val occurredAtEpochMs: Long
    )

    fun listPending(db: SQLiteDatabase): List<PendingConflict> = db.rawQuery(
        """
        SELECT id, existing_transaction_id, source_kind, transaction_code,
               type, amount_minor, fee_minor, occurred_at_epoch_ms
        FROM fingerprint_conflicts
        WHERE review_state = 'PENDING'
        ORDER BY id DESC
        LIMIT 200
        """.trimIndent(), null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(PendingConflict(
                    id = cursor.getLong(0),
                    existingTransactionId = cursor.getLong(1),
                    sourceKind = cursor.getString(2),
                    transactionCode = cursor.getString(3),
                    type = cursor.getString(4),
                    amountMinor = cursor.getLong(5),
                    feeMinor = cursor.getLong(6),
                    occurredAtEpochMs = cursor.getLong(7)
                ))
            }
        }
    }

    fun capture(
        db: SQLiteDatabase,
        stored: StoredTransaction,
        parsed: ParsedMpesaTransaction,
        occurredAtEpochMs: Long,
        sourceKind: String
    ): Boolean {
        val original = FingerprintFacts(
            stored.transactionCode, stored.type, stored.amountMinor,
            stored.feeMinor, stored.occurredAtEpochMs
        )
        val incoming = FingerprintFacts(
            parsed.transactionCode, parsed.type.name, parsed.amountMinor,
            parsed.feeMinor, occurredAtEpochMs
        )
        if (!FingerprintConflictPolicy.isConflicting(original, incoming)) return false
        recordIfConflicting(db, stored.id, original, incoming, sourceKind)
        return true
    }

    fun recordIfConflicting(
        db: SQLiteDatabase,
        existingTransactionId: Long,
        stored: FingerprintFacts,
        incoming: FingerprintFacts,
        sourceKind: String
    ): Boolean {
        if (!FingerprintConflictPolicy.isConflicting(stored, incoming)) return false
        val digestInput = listOf(
            existingTransactionId, incoming.code?.trim()?.uppercase(), incoming.type,
            incoming.amountMinor, incoming.feeMinor, incoming.occurredAtEpochMs, sourceKind
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val values = ContentValues().apply {
            put("existing_transaction_id", existingTransactionId)
            put("evidence_digest", digest)
            put("source_kind", sourceKind)
            put("transaction_code", incoming.code?.trim()?.uppercase())
            put("type", incoming.type)
            put("amount_minor", incoming.amountMinor)
            put("fee_minor", incoming.feeMinor)
            put("occurred_at_epoch_ms", incoming.occurredAtEpochMs)
            put("review_state", "PENDING")
        }
        return db.insertWithOnConflict(
            "fingerprint_conflicts", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }
}
