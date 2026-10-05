package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.util.Calendar

data class SalaryReceipt(
    val transactionId: Long,
    val amountMinor: Long,
    val receivedAtEpochMs: Long,
    val label: String,
    val accountId: String?,
    val manualOnly: Boolean
)

/** Salary corrections change the ledger and its cycle boundaries in one transaction. */
class SalaryReceiptRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase.get(appContext)

    fun receipts(ruleId: String): List<SalaryReceipt> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            """SELECT id, amount_minor, occurred_at_epoch_ms, counterparty, account_id,
                      source_kind, transaction_code
               FROM transactions
               WHERE rule_id = ? AND direction = 'INCOME' AND review_state != 'EXCLUDED'
                 AND cycle_candidate_reason IS NULL
                 AND (cycle_override IS NULL OR cycle_override != 'ADDITIONAL_INCOME')
               ORDER BY occurred_at_epoch_ms DESC, id DESC LIMIT 100""",
            arrayOf(ruleId)
        ).use { cursor -> buildList {
            while (cursor.moveToNext()) add(SalaryReceipt(
                transactionId = cursor.getLong(0), amountMinor = cursor.getLong(1),
                receivedAtEpochMs = cursor.getLong(2), label = cursor.getString(3) ?: "Salary",
                accountId = if (cursor.isNull(4)) null else cursor.getString(4),
                manualOnly = cursor.getString(5) == "manual_setup" && cursor.isNull(6)
            ))
        } }
    }

    fun removedReceiptIds(): Set<Long> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            "SELECT id FROM transactions WHERE cycle_override = ?", arrayOf(REMOVED_SALARY)
        ).use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getLong(0)) } }
    }

    fun removeSalary(transactionId: Long, expectedRuleId: String): Boolean = synchronized(CanonicalStoreLock.monitor) {
        val db = database.writableDatabase
        db.beginTransaction()
        var changed = false
        try {
            val stored = TransactionStore.get(appContext).getById(transactionId)
            val rule = RuleStore.get(appContext).getById(expectedRuleId)
            if (stored != null && rule?.startsPayCycle == true && stored.ruleId == expectedRuleId &&
                stored.direction == MoneyDirection.INCOME.name && stored.reviewState != ReviewState.EXCLUDED.name) {
                val manualOnly = stored.sourceKind == "manual_setup" && stored.transactionCode == null
                val values = ContentValues().apply {
                    put("review_state", if (manualOnly) ReviewState.EXCLUDED.name else ReviewState.USER_CLASSIFIED.name)
                    if (!manualOnly) {
                        putNull("rule_id")
                        put("category", TransactionCategory.UNCATEGORIZED.name)
                    }
                    put("cycle_override", REMOVED_SALARY)
                    putNull("cycle_candidate_reason")
                    putNull("pay_cycle_id")
                }
                changed = db.update("transactions", values, "id = ?", arrayOf(transactionId.toString())) > 0
                if (changed) PayCycleStore.get(appContext).rebuild()
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        if (changed) LedgerUpdateBus.notifyChanged()
        changed
    }

    /** An explicit re-entry may restore a removed manual row; SMS replay cannot. */
    fun recordManualSalary(amountMinor: Long, date: Long, rule: SmartRule): Boolean = synchronized(CanonicalStoreLock.monitor) {
        require(amountMinor > 0 && date > 0 && date <= System.currentTimeMillis()) { "Enter an amount and a date when salary arrived." }
        val db = database.writableDatabase
        db.beginTransaction()
        var changed = false
        try {
            val currentRule = RuleStore.get(appContext).getById(rule.id)
            require(currentRule?.enabled == true && currentRule.startsPayCycle && currentRule.accountId == rule.accountId && !rule.accountId.isNullOrBlank()) {
                "Save your salary source and receiving account first."
            }
            val day = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val next = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            val active = db.rawQuery(
                """SELECT 1 FROM transactions WHERE rule_id = ? AND account_id = ? AND amount_minor = ?
                   AND direction = 'INCOME' AND review_state != 'EXCLUDED'
                   AND occurred_at_epoch_ms >= ? AND occurred_at_epoch_ms < ? LIMIT 1""",
                arrayOf(rule.id, rule.accountId.orEmpty(), amountMinor.toString(), day.timeInMillis.toString(), next.timeInMillis.toString())
            ).use { it.moveToFirst() }
            if (!active) {
                val removedId = db.rawQuery(
                    """SELECT id FROM transactions WHERE rule_id = ? AND occurred_at_epoch_ms = ? AND amount_minor = ?
                       AND account_id = ? AND source_kind = 'manual_setup' AND transaction_code IS NULL
                       AND review_state = 'EXCLUDED' AND cycle_override = ? LIMIT 1""",
                    arrayOf(rule.id, date.toString(), amountMinor.toString(), rule.accountId.orEmpty(), REMOVED_SALARY)
                ).use { if (it.moveToFirst()) it.getLong(0) else null }
                changed = if (removedId != null) {
                    db.update("transactions", ContentValues().apply {
                        put("review_state", ReviewState.USER_CLASSIFIED.name)
                        put("rule_id", rule.id)
                        put("cycle_override", PayCycleStore.OVERRIDE_FORCE_START)
                    }, "id = ?", arrayOf(removedId.toString())) > 0
                } else TransactionStore.get(appContext).insertManualIncome(
                    amountMinor, date, rule.displayLabel ?: rule.name, rule, rebuildPayCycles = false
                )
                if (changed) PayCycleStore.get(appContext).rebuild()
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        if (changed) LedgerUpdateBus.notifyChanged()
        changed
    }

    companion object { const val REMOVED_SALARY = "REMOVED_SALARY" }
}
