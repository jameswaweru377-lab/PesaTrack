package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionFingerprint
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.PlanningPeriodResolver
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth

class TransactionStore private constructor(context: Context) {
    private val database = PesaTrackDatabase(context)

    @Synchronized
    fun insertIfNew(
        parsed: ParsedMpesaTransaction,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        matchedRule: SmartRule? = null
    ): Boolean {
        val fingerprint = TransactionFingerprint.of(parsed)
        val classification = classify(parsed, matchedRule, receivedAtEpochMs)
        val displayCounterparty = parsed.counterparty
            ?: matchedRule?.displayLabel?.takeIf { it.isNotBlank() }
            ?: matchedRule?.name?.takeIf { it.isNotBlank() }

        val values = ContentValues().apply {
            put("transaction_code", parsed.transactionCode)
            put("fingerprint", fingerprint)
            put("type", parsed.type.name)
            put("amount_minor", parsed.amountMinor)
            put("fee_minor", parsed.feeMinor)
            if (parsed.balanceMinor != null) put("balance_minor", parsed.balanceMinor) else putNull("balance_minor")
            put("counterparty", displayCounterparty)
            put("occurred_at_text", parsed.occurredAtText)
            put("received_at_epoch_ms", receivedAtEpochMs)
            put("source_package", sourcePackage)
            put("source_kind", sourceKind)
            put("created_at_epoch_ms", System.currentTimeMillis())
            put("direction", classification.direction.name)
            put("category", classification.category.name)
            put("review_state", classification.reviewState.name)
            if (classification.ruleId != null) put("rule_id", classification.ruleId) else putNull("rule_id")
            put("planning_month", classification.planningMonth)
            if (parsed.reversalOfCode != null) put("reversal_of_code", parsed.reversalOfCode) else putNull("reversal_of_code")
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
            BASE_SELECT + " ORDER BY received_at_epoch_ms DESC, id DESC LIMIT 1",
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.toStoredTransaction()
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

    @Synchronized
    fun getById(id: Long): StoredTransaction? =
        database.readableDatabase.rawQuery(
            "${BASE_SELECT} WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toStoredTransaction() else null
        }

    @Synchronized
    fun recent(limit: Int = 100): List<StoredTransaction> =
        database.readableDatabase.rawQuery(
            BASE_SELECT + " ORDER BY received_at_epoch_ms DESC, id DESC LIMIT ?",
            arrayOf(limit.coerceIn(1, 500).toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toStoredTransaction())
            }
        }

    @Synchronized
    fun activitySnapshot(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ActivityLedgerSnapshot {
        val currentMonth = YearMonth.from(Instant.ofEpochMilli(nowEpochMs).atZone(zoneId))
        val transactions = recent(200)
        val reversedCodes = reversedTransactionCodes()

        val monthTransactions = transactions.filter { item ->
            val observedMonth = YearMonth.from(
                Instant.ofEpochMilli(item.receivedAtEpochMs).atZone(zoneId)
            )
            observedMonth == currentMonth
        }

        val effective = monthTransactions.filter { item ->
            item.transactionCode !in reversedCodes &&
                item.type != MpesaTransactionType.REVERSAL.name &&
                item.reviewState != ReviewState.EXCLUDED.name
        }

        val income = effective
            .filter { it.direction == MoneyDirection.INCOME.name }
            .sumOf { it.amountMinor }

        val spent = effective
            .filter { it.direction == MoneyDirection.EXPENSE.name }
            .sumOf { it.amountMinor }

        val transfers = effective
            .filter { it.direction == MoneyDirection.TRANSFER.name }
            .sumOf { it.amountMinor }

        val fees = effective.sumOf { it.feeMinor }

        return ActivityLedgerSnapshot(
            transactions = transactions,
            summary = ActivitySummary(
                incomeMinor = income,
                spentMinor = spent,
                feesMinor = fees,
                transferMinor = transfers,
                netMinor = income - spent - fees,
                needsReviewCount = transactions.count {
                    it.reviewState == ReviewState.NEEDS_REVIEW.name
                }
            )
        )
    }

    @Synchronized
    fun reapplyCounterpartyRules(rules: List<SmartRule>) {
        if (rules.isEmpty()) return

        val db = database.writableDatabase
        recent(500).forEach { stored ->
            if (stored.reviewState != ReviewState.NEEDS_REVIEW.name) return@forEach
            val type = runCatching { MpesaTransactionType.valueOf(stored.type) }
                .getOrDefault(MpesaTransactionType.OTHER)
            val parsed = ParsedMpesaTransaction(
                transactionCode = stored.transactionCode,
                type = type,
                amountMinor = stored.amountMinor,
                counterparty = stored.counterparty,
                feeMinor = stored.feeMinor,
                balanceMinor = stored.balanceMinor,
                occurredAtText = stored.occurredAtText,
                rawMessage = "",
                reversalOfCode = stored.reversalOfCode
            )
            val rule = SmartRuleEngine.matchTransaction(parsed, rules) ?: return@forEach
            val classification = classify(parsed, rule, stored.receivedAtEpochMs)

            ContentValues().apply {
                put("direction", classification.direction.name)
                put("category", classification.category.name)
                put("review_state", ReviewState.AUTO_CLASSIFIED.name)
                put("rule_id", rule.id)
                put("planning_month", classification.planningMonth)
                rule.displayLabel?.takeIf { it.isNotBlank() }?.let { put("counterparty", it) }
            }.also { values ->
                db.update("transactions", values, "id = ?", arrayOf(stored.id.toString()))
            }
        }
    }

    @Synchronized
    fun classifyManually(
        id: Long,
        category: TransactionCategory,
        direction: MoneyDirection,
        planningMonth: String? = null,
        markTransfer: Boolean = false
    ) {
        val values = ContentValues().apply {
            put("category", if (markTransfer) TransactionCategory.TRANSFER.name else category.name)
            put("direction", if (markTransfer) MoneyDirection.TRANSFER.name else direction.name)
            put("review_state", ReviewState.USER_CLASSIFIED.name)
            if (planningMonth != null) put("planning_month", planningMonth)
        }
        database.writableDatabase.update("transactions", values, "id = ?", arrayOf(id.toString()))
    }

    private fun reversedTransactionCodes(): Set<String> =
        database.readableDatabase.rawQuery(
            "SELECT reversal_of_code FROM transactions WHERE reversal_of_code IS NOT NULL",
            null
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    private fun classify(
        parsed: ParsedMpesaTransaction,
        matchedRule: SmartRule?,
        observedAtEpochMs: Long
    ): Classification {
        if (matchedRule != null) {
            return Classification(
                direction = matchedRule.direction,
                category = matchedRule.category,
                reviewState = ReviewState.AUTO_CLASSIFIED,
                ruleId = matchedRule.id,
                planningMonth = PlanningPeriodResolver.resolve(
                    observedAtEpochMs = observedAtEpochMs,
                    monthOffset = matchedRule.planningMonthOffset
                )
            )
        }

        val fallback = when (parsed.type) {
            MpesaTransactionType.WITHDRAWAL -> Triple(
                MoneyDirection.TRANSFER,
                TransactionCategory.CASH,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.AIRTIME -> Triple(
                MoneyDirection.EXPENSE,
                TransactionCategory.PHONE_AND_DATA,
                ReviewState.AUTO_CLASSIFIED
            )
            MpesaTransactionType.REVERSAL -> Triple(
                MoneyDirection.NEUTRAL,
                TransactionCategory.OTHER,
                if (parsed.reversalOfCode != null) {
                    ReviewState.AUTO_CLASSIFIED
                } else {
                    ReviewState.NEEDS_REVIEW
                }
            )
            MpesaTransactionType.RECEIVED,
            MpesaTransactionType.DEPOSIT -> Triple(
                MoneyDirection.INCOME,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.NEEDS_REVIEW
            )
            MpesaTransactionType.SENT_MONEY,
            MpesaTransactionType.PAY_BILL,
            MpesaTransactionType.BUY_GOODS -> Triple(
                MoneyDirection.EXPENSE,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.NEEDS_REVIEW
            )
            MpesaTransactionType.OTHER -> Triple(
                MoneyDirection.NEUTRAL,
                TransactionCategory.UNCATEGORIZED,
                ReviewState.NEEDS_REVIEW
            )
        }

        return Classification(
            direction = fallback.first,
            category = fallback.second,
            reviewState = fallback.third,
            ruleId = null,
            planningMonth = PlanningPeriodResolver.resolve(observedAtEpochMs, 0)
        )
    }

    private data class Classification(
        val direction: MoneyDirection,
        val category: TransactionCategory,
        val reviewState: ReviewState,
        val ruleId: String?,
        val planningMonth: String
    )

    companion object {
        private const val BASE_SELECT = """
            SELECT id, transaction_code, fingerprint, type, amount_minor, fee_minor,
                   balance_minor, counterparty, occurred_at_text, received_at_epoch_ms,
                   source_package, source_kind, direction, category, review_state,
                   rule_id, planning_month, reversal_of_code
            FROM transactions
        """

        @Volatile
        private var instance: TransactionStore? = null

        fun get(context: Context): TransactionStore =
            instance ?: synchronized(this) {
                instance ?: TransactionStore(context.applicationContext).also { instance = it }
            }
    }
}

private fun android.database.Cursor.toStoredTransaction(): StoredTransaction =
    StoredTransaction(
        id = getLong(0),
        transactionCode = getStringOrNull(1),
        fingerprint = getString(2),
        type = getString(3),
        amountMinor = getLong(4),
        feeMinor = getLong(5),
        balanceMinor = getLongOrNull(6),
        counterparty = getStringOrNull(7),
        occurredAtText = getStringOrNull(8),
        receivedAtEpochMs = getLong(9),
        sourcePackage = getStringOrNull(10),
        sourceKind = getString(11),
        direction = getString(12),
        category = getString(13),
        reviewState = getString(14),
        ruleId = getStringOrNull(15),
        planningMonth = getStringOrNull(16),
        reversalOfCode = getStringOrNull(17)
    )

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)
