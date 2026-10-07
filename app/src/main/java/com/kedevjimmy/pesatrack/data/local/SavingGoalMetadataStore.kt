package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context

data class SavingGoalMetadata(
    val ruleId: String,
    val totalTargetMinor: Long? = null,
    val targetDateEpochMs: Long? = null
)

class SavingGoalMetadataStore private constructor(
    context: Context
) {
    private val database =
        PesaTrackDatabase.get(
            context.applicationContext
        )

    fun get(
        ruleId: String
    ): SavingGoalMetadata? =
        synchronized(
            CanonicalStoreLock.monitor
        ) {
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT rule_id,
                           total_target_minor,
                           target_date_epoch_ms
                    FROM saving_goal_metadata
                    WHERE rule_id = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(ruleId)
                )
                .use { cursor ->
                    if (!cursor.moveToFirst()) {
                        null
                    } else {
                        SavingGoalMetadata(
                            ruleId =
                                cursor.getString(0),
                            totalTargetMinor =
                                if (cursor.isNull(1)) {
                                    null
                                } else {
                                    cursor.getLong(1)
                                },
                            targetDateEpochMs =
                                if (cursor.isNull(2)) {
                                    null
                                } else {
                                    cursor.getLong(2)
                                }
                        )
                    }
                }
        }

    fun listForRules(
        ruleIds: Collection<String>
    ): Map<String, SavingGoalMetadata> {
        if (ruleIds.isEmpty()) {
            return emptyMap()
        }

        return synchronized(
            CanonicalStoreLock.monitor
        ) {
            val placeholders =
                ruleIds.joinToString(",") {
                    "?"
                }
            database.readableDatabase
                .rawQuery(
                    """
                    SELECT rule_id,
                           total_target_minor,
                           target_date_epoch_ms
                    FROM saving_goal_metadata
                    WHERE rule_id IN ($placeholders)
                    """.trimIndent(),
                    ruleIds.toTypedArray()
                )
                .use { cursor ->
                    buildMap {
                        while (
                            cursor.moveToNext()
                        ) {
                            val metadata =
                                SavingGoalMetadata(
                                    ruleId =
                                        cursor
                                            .getString(0),
                                    totalTargetMinor =
                                        if (
                                            cursor
                                                .isNull(1)
                                        ) {
                                            null
                                        } else {
                                            cursor
                                                .getLong(1)
                                        },
                                    targetDateEpochMs =
                                        if (
                                            cursor
                                                .isNull(2)
                                        ) {
                                            null
                                        } else {
                                            cursor
                                                .getLong(2)
                                        }
                                )
                            put(
                                metadata.ruleId,
                                metadata
                            )
                        }
                    }
                }
        }
    }

    fun save(
        ruleId: String,
        totalTargetMinor: Long?,
        targetDateEpochMs: Long?
    ) {
        synchronized(
            CanonicalStoreLock.monitor
        ) {
            require(
                totalTargetMinor == null ||
                    totalTargetMinor > 0L
            ) {
                "Total goal target must be positive."
            }

            val now =
                System.currentTimeMillis()
            val existingCreated =
                database.readableDatabase
                    .rawQuery(
                        """
                        SELECT created_at_epoch_ms
                        FROM saving_goal_metadata
                        WHERE rule_id = ?
                        LIMIT 1
                        """.trimIndent(),
                        arrayOf(ruleId)
                    )
                    .use { cursor ->
                        if (
                            cursor.moveToFirst()
                        ) {
                            cursor.getLong(0)
                        } else {
                            now
                        }
                    }

            val values =
                ContentValues().apply {
                    put("rule_id", ruleId)
                    if (
                        totalTargetMinor != null
                    ) {
                        put(
                            "total_target_minor",
                            totalTargetMinor
                        )
                    } else {
                        putNull(
                            "total_target_minor"
                        )
                    }
                    if (
                        targetDateEpochMs != null
                    ) {
                        put(
                            "target_date_epoch_ms",
                            targetDateEpochMs
                        )
                    } else {
                        putNull(
                            "target_date_epoch_ms"
                        )
                    }
                    put(
                        "created_at_epoch_ms",
                        existingCreated
                    )
                    put(
                        "updated_at_epoch_ms",
                        now
                    )
                }

            database.writableDatabase
                .insertWithOnConflict(
                    "saving_goal_metadata",
                    null,
                    values,
                    android.database.sqlite
                        .SQLiteDatabase
                        .CONFLICT_REPLACE
                )
            LedgerUpdateBus.notifyChanged()
        }
    }

    fun delete(
        ruleId: String
    ) {
        synchronized(
            CanonicalStoreLock.monitor
        ) {
            database.writableDatabase.delete(
                "saving_goal_metadata",
                "rule_id = ?",
                arrayOf(ruleId)
            )
            LedgerUpdateBus.notifyChanged()
        }
    }

    companion object {
        @Volatile
        private var instance:
            SavingGoalMetadataStore? =
            null

        fun get(
            context: Context
        ): SavingGoalMetadataStore =
            instance ?: synchronized(this) {
                instance
                    ?: SavingGoalMetadataStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
