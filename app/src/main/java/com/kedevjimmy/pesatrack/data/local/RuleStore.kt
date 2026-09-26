package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.util.UUID

class RuleStore private constructor(context: Context) {
    private val database = PesaTrackDatabase(context)

    @Synchronized
    fun listRules(enabledOnly: Boolean = false): List<SmartRule> {
        val where = if (enabledOnly) "WHERE enabled = 1" else ""
        return database.readableDatabase.rawQuery(
            """
            SELECT id, name, counterparty_contains, notification_title_contains,
                   message_contains, category, direction, maximum_amount_minor,
                   planning_month_offset, display_label, enabled
            FROM smart_rules
            $where
            ORDER BY updated_at_epoch_ms DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SmartRule(
                            id = cursor.getString(0),
                            counterpartyContains = cursor.getString(2),
                            category = TransactionCategory.valueOf(cursor.getString(5)),
                            maximumAmountMinor = if (cursor.isNull(7)) null else cursor.getLong(7),
                            name = cursor.getString(1),
                            notificationTitleContains = cursor.getString(3),
                            messageContains = cursor.getString(4),
                            direction = MoneyDirection.valueOf(cursor.getString(6)),
                            planningMonthOffset = cursor.getInt(8),
                            displayLabel = if (cursor.isNull(9)) null else cursor.getString(9),
                            enabled = cursor.getInt(10) == 1
                        )
                    )
                }
            }
        }
    }

    @Synchronized
    fun save(
        name: String,
        counterpartyContains: String,
        category: TransactionCategory,
        direction: MoneyDirection,
        notificationTitleContains: String = "",
        messageContains: String = "",
        planningMonthOffset: Int = 0,
        displayLabel: String? = null,
        maximumAmountMinor: Long? = null,
        enabled: Boolean = true,
        id: String = UUID.randomUUID().toString()
    ): SmartRule {
        require(
            counterpartyContains.isNotBlank() ||
                notificationTitleContains.isNotBlank() ||
                messageContains.isNotBlank()
        ) { "A smart rule needs at least one match condition." }

        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("id", id)
            put("name", name.trim())
            put("counterparty_contains", counterpartyContains.trim())
            put("notification_title_contains", notificationTitleContains.trim())
            put("message_contains", messageContains.trim())
            put("category", category.name)
            put("direction", direction.name)
            if (maximumAmountMinor != null) put("maximum_amount_minor", maximumAmountMinor) else putNull("maximum_amount_minor")
            put("planning_month_offset", planningMonthOffset)
            if (displayLabel.isNullOrBlank()) putNull("display_label") else put("display_label", displayLabel.trim())
            put("enabled", if (enabled) 1 else 0)
            put("updated_at_epoch_ms", now)
            put("created_at_epoch_ms", now)
        }

        database.writableDatabase.insertWithOnConflict(
            "smart_rules",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )

        return SmartRule(
            id = id,
            counterpartyContains = counterpartyContains.trim(),
            category = category,
            maximumAmountMinor = maximumAmountMinor,
            name = name.trim(),
            notificationTitleContains = notificationTitleContains.trim(),
            messageContains = messageContains.trim(),
            direction = direction,
            planningMonthOffset = planningMonthOffset,
            displayLabel = displayLabel?.trim()?.takeIf { it.isNotBlank() },
            enabled = enabled
        )
    }

    @Synchronized
    fun delete(id: String) {
        database.writableDatabase.delete("smart_rules", "id = ?", arrayOf(id))
    }

    companion object {
        @Volatile
        private var instance: RuleStore? = null

        fun get(context: Context): RuleStore =
            instance ?: synchronized(this) {
                instance ?: RuleStore(context.applicationContext).also { instance = it }
            }
    }
}
