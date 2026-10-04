package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

data class CycleProtection(
    val cycleStartTransactionId: Long,
    val amountMinor: Long,
    val label: String
)

class CycleProtectionStore private constructor(
    context: Context
) {
    private val database =
        PesaTrackDatabase.get(
            context.applicationContext
        )

    @Synchronized
    fun get(
        cycleStartTransactionId: Long
    ): CycleProtection? =
        database.readableDatabase
            .rawQuery(
                """
                SELECT cycle_start_transaction_id,
                       amount_minor,
                       label
                FROM cycle_protections
                WHERE cycle_start_transaction_id = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(
                    cycleStartTransactionId
                        .toString()
                )
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    CycleProtection(
                        cycleStartTransactionId =
                            cursor.getLong(0),
                        amountMinor =
                            cursor.getLong(1),
                        label =
                            cursor.getString(2)
                    )
                }
            }

    @Synchronized
    fun set(
        cycleStartTransactionId: Long,
        amountMinor: Long,
        label: String =
            "Protected money"
    ) {
        require(amountMinor >= 0L) {
            "Protected amount cannot be negative."
        }

        if (amountMinor == 0L) {
            clear(
                cycleStartTransactionId
            )
            return
        }

        val now =
            System.currentTimeMillis()
        val existing =
            get(
                cycleStartTransactionId
            )
        val values =
            ContentValues().apply {
                put(
                    "cycle_start_transaction_id",
                    cycleStartTransactionId
                )
                put(
                    "amount_minor",
                    amountMinor
                )
                put(
                    "label",
                    label.trim()
                        .ifBlank {
                            "Protected money"
                        }
                )
                put(
                    "created_at_epoch_ms",
                    existing?.let {
                        database.readableDatabase
                            .rawQuery(
                                """
                                SELECT created_at_epoch_ms
                                FROM cycle_protections
                                WHERE cycle_start_transaction_id = ?
                                LIMIT 1
                                """.trimIndent(),
                                arrayOf(
                                    cycleStartTransactionId
                                        .toString()
                                )
                            ).use { cursor ->
                                if (
                                    cursor.moveToFirst()
                                ) {
                                    cursor.getLong(0)
                                } else {
                                    now
                                }
                            }
                    } ?: now
                )
                put(
                    "updated_at_epoch_ms",
                    now
                )
            }

        database.writableDatabase
            .insertWithOnConflict(
                "cycle_protections",
                null,
                values,
                SQLiteDatabase
                    .CONFLICT_REPLACE
            )
        LedgerUpdateBus.notifyChanged()
    }

    @Synchronized
    fun clear(
        cycleStartTransactionId: Long
    ) {
        val changed =
            database.writableDatabase
                .delete(
                    "cycle_protections",
                    "cycle_start_transaction_id = ?",
                    arrayOf(
                        cycleStartTransactionId
                            .toString()
                    )
                ) > 0
        if (changed) {
            LedgerUpdateBus
                .notifyChanged()
        }
    }

    companion object {
        @Volatile
        private var instance:
            CycleProtectionStore? = null

        fun get(
            context: Context
        ): CycleProtectionStore =
            instance ?: synchronized(this) {
                instance
                    ?: CycleProtectionStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
