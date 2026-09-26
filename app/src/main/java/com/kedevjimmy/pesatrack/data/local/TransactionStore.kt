package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionFingerprint
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction

class TransactionStore private constructor(context: Context) {
    private val database = PesaTrackDatabase(context)

    @Synchronized
    fun insertIfNew(
        parsed: ParsedMpesaTransaction,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long
    ): Boolean {
        val fingerprint = TransactionFingerprint.of(parsed)
        val values = ContentValues().apply {
            put("transaction_code", parsed.transactionCode)
            put("fingerprint", fingerprint)
            put("type", parsed.type.name)
            put("amount_minor", parsed.amountMinor)
            put("fee_minor", parsed.feeMinor)
            if (parsed.balanceMinor != null) put("balance_minor", parsed.balanceMinor) else putNull("balance_minor")
            put("counterparty", parsed.counterparty)
            put("occurred_at_text", parsed.occurredAtText)
            put("received_at_epoch_ms", receivedAtEpochMs)
            put("source_package", sourcePackage)
            put("source_kind", sourceKind)
            put("created_at_epoch_ms", System.currentTimeMillis())
        }

        return database.writableDatabase.insertWithOnConflict(
            "transactions",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    @Synchronized
    fun snapshot(): LedgerSnapshot {
        val db = database.readableDatabase

        val count = db.rawQuery("SELECT COUNT(*) FROM transactions", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

        val latest = db.rawQuery(
            """
            SELECT id, transaction_code, fingerprint, type, amount_minor, fee_minor,
                   balance_minor, counterparty, occurred_at_text, received_at_epoch_ms,
                   source_package, source_kind
            FROM transactions
            ORDER BY received_at_epoch_ms DESC, id DESC
            LIMIT 1
            """.trimIndent(),
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                StoredTransaction(
                    id = cursor.getLong(0),
                    transactionCode = cursor.getStringOrNull(1),
                    fingerprint = cursor.getString(2),
                    type = cursor.getString(3),
                    amountMinor = cursor.getLong(4),
                    feeMinor = cursor.getLong(5),
                    balanceMinor = cursor.getLongOrNull(6),
                    counterparty = cursor.getStringOrNull(7),
                    occurredAtText = cursor.getStringOrNull(8),
                    receivedAtEpochMs = cursor.getLong(9),
                    sourcePackage = cursor.getStringOrNull(10),
                    sourceKind = cursor.getString(11)
                )
            }
        }

        val balance = db.rawQuery(
            """
            SELECT balance_minor, received_at_epoch_ms
            FROM transactions
            WHERE balance_minor IS NOT NULL
            ORDER BY received_at_epoch_ms DESC, id DESC
            LIMIT 1
            """.trimIndent(),
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) to cursor.getLong(1) else null
        }

        return LedgerSnapshot(
            transactionCount = count,
            latestTransaction = latest,
            latestBalanceMinor = balance?.first,
            latestBalanceObservedAtEpochMs = balance?.second
        )
    }

    companion object {
        @Volatile
        private var instance: TransactionStore? = null

        fun get(context: Context): TransactionStore =
            instance ?: synchronized(this) {
                instance ?: TransactionStore(context.applicationContext).also { instance = it }
            }
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)
