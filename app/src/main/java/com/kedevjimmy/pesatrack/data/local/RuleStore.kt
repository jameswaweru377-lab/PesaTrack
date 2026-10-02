package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
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
                   planning_month_offset, display_label, enabled, preset,
                   planned_amount_minor, starts_pay_cycle, account_id,
                   linked_account_id, pay_window_start_day, pay_window_end_day,
                   counterparty_exact, counterparty_phone_equals
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
                            maximumAmountMinor = cursor.getLongOrNull(7),
                            name = cursor.getString(1),
                            notificationTitleContains = cursor.getString(3),
                            messageContains = cursor.getString(4),
                            direction = MoneyDirection.valueOf(cursor.getString(6)),
                            planningMonthOffset = cursor.getInt(8),
                            displayLabel = cursor.getStringOrNull(9),
                            enabled = cursor.getInt(10) == 1,
                            preset = runCatching {
                                RulePreset.valueOf(cursor.getString(11))
                            }.getOrDefault(RulePreset.CUSTOM),
                            plannedAmountMinor = cursor.getLongOrNull(12),
                            startsPayCycle = cursor.getInt(13) == 1,
                            accountId = cursor.getStringOrNull(14),
                            linkedAccountId = cursor.getStringOrNull(15),
                            payWindowStartDay = cursor.getIntOrNull(16),
                            payWindowEndDay = cursor.getIntOrNull(17),
                            counterpartyExact = cursor.getString(18),
                            counterpartyPhoneEquals = cursor.getString(19)
                        )
                    )
                }
            }
        }
    }

    @Synchronized
    fun getById(
        id: String
    ): SmartRule? =
        listRules(
            enabledOnly = false
        ).firstOrNull {
            it.id == id
        }

    @Synchronized
    fun setEnabled(
        id: String,
        enabled: Boolean
    ): Boolean {
        val values =
            ContentValues().apply {
                put(
                    "enabled",
                    if (enabled) 1 else 0
                )
                put(
                    "updated_at_epoch_ms",
                    System.currentTimeMillis()
                )
            }

        val changed =
            database.writableDatabase
                .update(
                    "smart_rules",
                    values,
                    "id = ?",
                    arrayOf(id)
                ) > 0

        if (changed) {
            LedgerUpdateBus.notifyChanged()
        }

        return changed
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
        preset: RulePreset = RulePreset.CUSTOM,
        plannedAmountMinor: Long? = null,
        startsPayCycle: Boolean = false,
        accountId: String? = null,
        linkedAccountId: String? = null,
        payWindowStartDay: Int? = null,
        payWindowEndDay: Int? = null,
        counterpartyExact: String = "",
        counterpartyPhoneEquals: String = "",
        id: String = UUID.randomUUID().toString()
    ): SmartRule {
        require(
            counterpartyContains.isNotBlank() ||
                counterpartyExact.isNotBlank() ||
                counterpartyPhoneEquals.isNotBlank() ||
                notificationTitleContains.isNotBlank() ||
                messageContains.isNotBlank()
        ) { "A smart rule needs at least one match condition." }

        val now = System.currentTimeMillis()
        val existingCreatedAt = database.readableDatabase.rawQuery(
            "SELECT created_at_epoch_ms FROM smart_rules WHERE id = ? LIMIT 1",
            arrayOf(id)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else now
        }

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
            put("preset", preset.name)
            if (plannedAmountMinor != null) put("planned_amount_minor", plannedAmountMinor) else putNull("planned_amount_minor")
            put("starts_pay_cycle", if (startsPayCycle) 1 else 0)
            if (accountId != null) put("account_id", accountId) else putNull("account_id")
            if (linkedAccountId != null) put("linked_account_id", linkedAccountId) else putNull("linked_account_id")
            if (payWindowStartDay != null) put("pay_window_start_day", payWindowStartDay) else putNull("pay_window_start_day")
            if (payWindowEndDay != null) put("pay_window_end_day", payWindowEndDay) else putNull("pay_window_end_day")
            put("counterparty_exact", counterpartyExact.trim())
            put("counterparty_phone_equals", normalizePhone(counterpartyPhoneEquals))
            put("updated_at_epoch_ms", now)
            put("created_at_epoch_ms", existingCreatedAt)
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
            enabled = enabled,
            preset = preset,
            plannedAmountMinor = plannedAmountMinor,
            startsPayCycle = startsPayCycle,
            accountId = accountId,
            linkedAccountId = linkedAccountId,
            payWindowStartDay = payWindowStartDay,
            payWindowEndDay = payWindowEndDay,
            counterpartyExact = counterpartyExact.trim(),
            counterpartyPhoneEquals = normalizePhone(counterpartyPhoneEquals)
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

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private fun android.database.Cursor.getIntOrNull(index: Int): Int? =
    if (isNull(index)) null else getInt(index)

private fun normalizePhone(value: String): String {
    val digits = value.filter(Char::isDigit)
    return when {
        digits.startsWith("254") && digits.length == 12 -> "+" + digits
        digits.startsWith("0") && digits.length == 10 -> "+254" + digits.drop(1)
        else -> value.trim()
    }
}
