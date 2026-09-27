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
import com.kedevjimmy.pesatrack.domain.rules.ReviewTriagePolicy
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine
import java.util.Calendar
import java.util.TimeZone

class TransactionStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase(appContext)

    @Synchronized
    fun insertIfNew(
        parsed: ParsedMpesaTransaction,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        occurredAtEpochMs: Long = receivedAtEpochMs,
        matchedRule: SmartRule? = null,
        rebuildPayCycles: Boolean = true
    ): Boolean {
        val fingerprint = TransactionFingerprint.of(parsed, occurredAtEpochMs)
        if (parsed.transactionCode.isNullOrBlank()) {
            val legacyFingerprint = TransactionFingerprint.of(parsed)
            if (
                legacyFingerprint != fingerprint &&
                hasLegacyFingerprintAt(legacyFingerprint, occurredAtEpochMs)
            ) {
                return false
            }
        }
        val classification = classify(parsed, matchedRule, occurredAtEpochMs)
        val displayCounterparty = parsed.counterparty
            ?: matchedRule?.displayLabel?.takeIf { it.isNotBlank() }
            ?: matchedRule?.name?.takeIf { it.isNotBlank() }

        val defaultMpesa =
            sourceKind.contains("mpesa", ignoreCase = true) ||
                parsed.type != MpesaTransactionType.OTHER
        val accountId = matchedRule?.accountId ?: when {
            parsed.type == MpesaTransactionType.WITHDRAWAL -> AccountStore.DEFAULT_MPESA_ID
            defaultMpesa -> AccountStore.DEFAULT_MPESA_ID
            else -> null
        }
        val linkedAccountId = matchedRule?.linkedAccountId ?: when (parsed.type) {
            MpesaTransactionType.WITHDRAWAL -> AccountStore.DEFAULT_CASH_ID
            else -> null
        }

        val values = ContentValues().apply {
            put("transaction_code", parsed.transactionCode)
            put("fingerprint", fingerprint)
            put("type", parsed.type.name)
            put("amount_minor", parsed.amountMinor)
            put("fee_minor", parsed.feeMinor)
            if (parsed.balanceMinor != null) put("balance_minor", parsed.balanceMinor) else putNull("balance_minor")
            put("counterparty", displayCounterparty)
            if (parsed.counterpartyPhone != null) put("counterparty_phone", parsed.counterpartyPhone) else putNull("counterparty_phone")
            put("occurred_at_text", parsed.occurredAtText)
            put("occurred_at_epoch_ms", occurredAtEpochMs)
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
            if (accountId != null) put("account_id", accountId) else putNull("account_id")
            if (linkedAccountId != null) put("linked_account_id", linkedAccountId) else putNull("linked_account_id")
            putNull("pay_cycle_id")
            putNull("cycle_candidate_reason")
            putNull("cycle_override")
        }

        val rowId = database.writableDatabase.insertWithOnConflict(
            "transactions",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        val inserted = rowId != -1L
        if (inserted) {
            FulizaStore.get(appContext).linkPendingForTransaction(
                transactionId = rowId,
                parsed = parsed,
                occurredAtEpochMs = occurredAtEpochMs
            )
            if (matchedRule == null) {
                reconcileReviewTriageForMonth(occurredAtEpochMs)
            }
        }
        if (inserted && rebuildPayCycles) {
            PayCycleStore.get(appContext).rebuild()
        }
        return inserted
    }

    @Synchronized
    fun insertManualIncome(
        amountMinor: Long,
        occurredAtEpochMs: Long,
        label: String,
        matchedRule: SmartRule
    ): Boolean {
        require(amountMinor > 0L) { "Income amount must be greater than zero." }

        val parsed = ParsedMpesaTransaction(
            transactionCode = null,
            type = MpesaTransactionType.OTHER,
            amountMinor = amountMinor,
            counterparty = label.trim().ifBlank { matchedRule.displayLabel ?: matchedRule.name },
            counterpartyPhone = null,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = "manual-income|" + matchedRule.id + "|" + occurredAtEpochMs,
            reversalOfCode = null
        )

        return insertIfNew(
            parsed = parsed,
            sourcePackage = null,
            sourceKind = "manual_setup",
            receivedAtEpochMs = System.currentTimeMillis(),
            occurredAtEpochMs = occurredAtEpochMs,
            matchedRule = matchedRule
        )
    }

    @Synchronized
    fun snapshot(): LedgerSnapshot {
        val db = database.readableDatabase

        val count = db.rawQuery("SELECT COUNT(*) FROM transactions", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

        val latest = db.rawQuery(
            BASE_SELECT + " ORDER BY occurred_at_epoch_ms DESC, id DESC LIMIT 1",
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.toStoredTransaction()
        }

        val balance = db.rawQuery(
            """
            SELECT balance_minor, occurred_at_epoch_ms
            FROM transactions
            WHERE balance_minor IS NOT NULL
            ORDER BY occurred_at_epoch_ms DESC, id DESC
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
            BASE_SELECT + " WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toStoredTransaction() else null
        }

    @Synchronized
    fun recent(limit: Int = 100): List<StoredTransaction> =
        database.readableDatabase.rawQuery(
            BASE_SELECT + " ORDER BY occurred_at_epoch_ms DESC, id DESC LIMIT ?",
            arrayOf(limit.coerceIn(1, 500).toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toStoredTransaction())
            }
        }

    @Synchronized
    fun activitySnapshot(
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): ActivityLedgerSnapshot {
        reconcileRecentReviewTriage(nowEpochMs, timeZone)

        val monthStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonthStart = (monthStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        val reversedCodes = reversedTransactionCodes()
        val monthTransactions = transactionsBetween(
            startEpochMs = monthStart.timeInMillis,
            endEpochMsExclusive = nextMonthStart.timeInMillis
        )

        val effective = monthTransactions.filter { item ->
            item.transactionCode !in reversedCodes &&
                item.type != MpesaTransactionType.REVERSAL.name &&
                item.reviewState != ReviewState.EXCLUDED.name &&
                item.reviewState != ReviewState.NEEDS_REVIEW.name &&
                item.cycleCandidateReason == null
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

        val fees = effective.sumOf { it.feeMinor } +
            FulizaStore.get(appContext).sumAccessFeesBetween(
                startEpochMs = monthStart.timeInMillis,
                endEpochMsExclusive = nextMonthStart.timeInMillis
            )

        return ActivityLedgerSnapshot(
            transactions = monthTransactions,
            summary = ActivitySummary(
                incomeMinor = income,
                spentMinor = spent,
                feesMinor = fees,
                transferMinor = transfers,
                netMinor = income - spent - fees,
                needsReviewCount = monthTransactions.count {
                    it.reviewState == ReviewState.NEEDS_REVIEW.name ||
                        it.cycleCandidateReason != null
                }
            ),
            reviewGroups = buildReviewGroups(monthTransactions)
        )
    }

    @Synchronized
    fun reconcileRecentReviewTriage(
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ) {
        var changed = 0
        repeat(3) { monthOffset ->
            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowEpochMs
                add(Calendar.MONTH, -monthOffset)
            }
            changed += reconcileReviewTriageForMonth(
                calendar.timeInMillis,
                timeZone
            )
        }
        if (changed > 0) {
            PayCycleStore.get(appContext).rebuild()
        }
    }

    @Synchronized
    fun reconcileReviewTriageForMonth(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        val monthStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonthStart = (monthStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }

        val eligible = transactionsBetween(
            startEpochMs = monthStart.timeInMillis,
            endEpochMsExclusive = nextMonthStart.timeInMillis
        ).filter { item ->
            if (item.ruleId != null) return@filter false
            if (
                item.reviewState == ReviewState.USER_CLASSIFIED.name ||
                item.reviewState == ReviewState.EXCLUDED.name
            ) return@filter false
            if (item.direction != MoneyDirection.EXPENSE.name) return@filter false
            if (item.type !in OUTGOING_REVIEW_TYPES) return@filter false

            ReviewTriagePolicy.identityKey(
                item.counterparty,
                item.counterpartyPhone
            ) != null
        }

        val db = database.writableDatabase
        var changed = 0
        eligible.groupBy { item ->
            ReviewTriagePolicy.identityKey(
                item.counterparty,
                item.counterpartyPhone
            )!!
        }.values.forEach { group ->
            val oneOff = ReviewTriagePolicy.shouldDefaultToOther(group.size)
            val values = ContentValues().apply {
                put(
                    "category",
                    if (oneOff) {
                        TransactionCategory.OTHER.name
                    } else {
                        TransactionCategory.UNCATEGORIZED.name
                    }
                )
                put(
                    "review_state",
                    if (oneOff) {
                        ReviewState.AUTO_CLASSIFIED.name
                    } else {
                        ReviewState.NEEDS_REVIEW.name
                    }
                )
            }

            group.forEach { item ->
                val desiredCategory = values.getAsString("category")
                val desiredReviewState = values.getAsString("review_state")
                if (
                    item.category != desiredCategory ||
                    item.reviewState != desiredReviewState
                ) {
                    changed += db.update(
                        "transactions",
                        values,
                        "id = ?",
                        arrayOf(item.id.toString())
                    )
                }
            }
        }
        return changed
    }

    private fun buildReviewGroups(
        transactions: List<StoredTransaction>
    ): List<ReviewGroup> =
        transactions
            .filter { item ->
                item.reviewState == ReviewState.NEEDS_REVIEW.name ||
                    item.cycleCandidateReason != null
            }
            .groupBy { item ->
                ReviewTriagePolicy.identityKey(
                    item.counterparty,
                    item.counterpartyPhone
                ) ?: "transaction:${item.id}"
            }
            .map { (key, items) ->
                val latest = items.maxByOrNull(StoredTransaction::occurredAtEpochMs)!!
                ReviewGroup(
                    key = key,
                    label = latest.counterparty
                        ?: latest.counterpartyPhone
                        ?: latest.type.lowercase()
                            .replace('_', ' ')
                            .replaceFirstChar { it.uppercase() },
                    representativeTransactionId = latest.id,
                    transactionCount = items.size,
                    totalMinor = items.sumOf(StoredTransaction::amountMinor),
                    latestEpochMs = latest.occurredAtEpochMs
                )
            }
            .sortedByDescending(ReviewGroup::latestEpochMs)

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
                counterpartyPhone = stored.counterpartyPhone,
                feeMinor = stored.feeMinor,
                balanceMinor = stored.balanceMinor,
                occurredAtText = stored.occurredAtText,
                rawMessage = "",
                reversalOfCode = stored.reversalOfCode
            )
            val rule = SmartRuleEngine.matchTransaction(parsed, rules) ?: return@forEach
            val classification = classify(parsed, rule, stored.occurredAtEpochMs)

            ContentValues().apply {
                put("direction", classification.direction.name)
                put("category", classification.category.name)
                put("review_state", ReviewState.AUTO_CLASSIFIED.name)
                put("rule_id", rule.id)
                put("planning_month", classification.planningMonth)
                if (rule.accountId != null) put("account_id", rule.accountId)
                if (rule.linkedAccountId != null) put("linked_account_id", rule.linkedAccountId)
                rule.displayLabel?.takeIf { it.isNotBlank() }?.let { put("counterparty", it) }
            }.also { values ->
                db.update("transactions", values, "id = ?", arrayOf(stored.id.toString()))
            }
        }
        PayCycleStore.get(appContext).rebuild()
    }

    @Synchronized
    fun matchingPendingCount(
        rule: SmartRule,
        excludingId: Long? = null
    ): Int =
        recent(500).count { stored ->
            isLearnableHistoricalMatch(stored) &&
                stored.id != excludingId &&
                SmartRuleEngine.matchTransaction(
                    stored.toParsedTransaction(),
                    listOf(rule)
                ) != null
        }

    @Synchronized
    fun reapplyRule(rule: SmartRule): Int {
        val db = database.writableDatabase
        var changed = 0

        recent(500).forEach { stored ->
            if (!isLearnableHistoricalMatch(stored)) return@forEach
            val parsed = stored.toParsedTransaction()
            if (SmartRuleEngine.matchTransaction(parsed, listOf(rule)) == null) return@forEach

            val classification = classify(parsed, rule, stored.occurredAtEpochMs)
            val values = ContentValues().apply {
                put("direction", classification.direction.name)
                put("category", classification.category.name)
                put("review_state", ReviewState.AUTO_CLASSIFIED.name)
                put("rule_id", rule.id)
                put("planning_month", classification.planningMonth)
                if (rule.accountId != null) put("account_id", rule.accountId)
                if (rule.linkedAccountId != null) put("linked_account_id", rule.linkedAccountId)
                rule.displayLabel?.takeIf { it.isNotBlank() }?.let {
                    put("counterparty", it)
                }
            }
            changed += db.update(
                "transactions",
                values,
                "id = ?",
                arrayOf(stored.id.toString())
            )
        }

        if (changed > 0) {
            PayCycleStore.get(appContext).rebuild()
        }
        return changed
    }

    private fun isLearnableHistoricalMatch(
        stored: StoredTransaction
    ): Boolean =
        stored.reviewState == ReviewState.NEEDS_REVIEW.name ||
            (
                stored.reviewState == ReviewState.AUTO_CLASSIFIED.name &&
                    stored.ruleId == null &&
                    stored.category == TransactionCategory.OTHER.name
            )

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
        PayCycleStore.get(appContext).rebuild()
    }

    @Synchronized
    fun excludeFromBudget(id: Long) {
        val values = ContentValues().apply {
            put("review_state", ReviewState.EXCLUDED.name)
        }
        database.writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(id.toString())
        )
        PayCycleStore.get(appContext).rebuild()
    }

    private fun transactionsBetween(
        startEpochMs: Long,
        endEpochMsExclusive: Long
    ): List<StoredTransaction> =
        database.readableDatabase.rawQuery(
            BASE_SELECT +
                " WHERE occurred_at_epoch_ms >= ? AND occurred_at_epoch_ms < ?" +
                " ORDER BY occurred_at_epoch_ms DESC, id DESC",
            arrayOf(startEpochMs.toString(), endEpochMsExclusive.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toStoredTransaction())
            }
        }

    private fun hasLegacyFingerprintAt(
        fingerprint: String,
        occurredAtEpochMs: Long
    ): Boolean =
        database.readableDatabase.rawQuery(
            """
            SELECT 1
            FROM transactions
            WHERE fingerprint = ?
              AND occurred_at_epoch_ms = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(fingerprint, occurredAtEpochMs.toString())
        ).use { cursor ->
            cursor.moveToFirst()
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
        occurredAtEpochMs: Long
    ): Classification {
        if (matchedRule != null) {
            return Classification(
                direction = matchedRule.direction,
                category = matchedRule.category,
                reviewState = ReviewState.AUTO_CLASSIFIED,
                ruleId = matchedRule.id,
                planningMonth = PlanningPeriodResolver.resolve(
                    observedAtEpochMs = occurredAtEpochMs,
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
                if (parsed.reversalOfCode != null) ReviewState.AUTO_CLASSIFIED else ReviewState.NEEDS_REVIEW
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
            planningMonth = PlanningPeriodResolver.resolve(occurredAtEpochMs, 0)
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
        private val OUTGOING_REVIEW_TYPES = setOf(
            MpesaTransactionType.SENT_MONEY.name,
            MpesaTransactionType.PAY_BILL.name,
            MpesaTransactionType.BUY_GOODS.name
        )

        private const val BASE_SELECT = """
            SELECT id, transaction_code, fingerprint, type, amount_minor, fee_minor,
                   balance_minor, counterparty, counterparty_phone, occurred_at_text,
                   occurred_at_epoch_ms, received_at_epoch_ms, source_package, source_kind,
                   direction, category, review_state, rule_id, planning_month, reversal_of_code,
                   account_id, linked_account_id, pay_cycle_id, cycle_candidate_reason,
                   cycle_override, fuliza_financed_minor, fuliza_access_fee_minor,
                   fuliza_outstanding_minor, fuliza_event_code
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
        counterpartyPhone = getStringOrNull(8),
        occurredAtText = getStringOrNull(9),
        occurredAtEpochMs = getLong(10),
        receivedAtEpochMs = getLong(11),
        sourcePackage = getStringOrNull(12),
        sourceKind = getString(13),
        direction = getString(14),
        category = getString(15),
        reviewState = getString(16),
        ruleId = getStringOrNull(17),
        planningMonth = getStringOrNull(18),
        reversalOfCode = getStringOrNull(19),
        accountId = getStringOrNull(20),
        linkedAccountId = getStringOrNull(21),
        payCycleId = getStringOrNull(22),
        cycleCandidateReason = getStringOrNull(23),
        cycleOverride = getStringOrNull(24),
        fulizaFinancedMinor = getLong(25),
        fulizaAccessFeeMinor = getLong(26),
        fulizaOutstandingMinor = getLongOrNull(27),
        fulizaEventCode = getStringOrNull(28)
    )

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private fun StoredTransaction.toParsedTransaction(): ParsedMpesaTransaction =
    ParsedMpesaTransaction(
        transactionCode = transactionCode,
        type = runCatching { MpesaTransactionType.valueOf(type) }
            .getOrDefault(MpesaTransactionType.OTHER),
        amountMinor = amountMinor,
        counterparty = counterparty,
        counterpartyPhone = counterpartyPhone,
        feeMinor = feeMinor,
        balanceMinor = balanceMinor,
        occurredAtText = occurredAtText,
        rawMessage = "",
        reversalOfCode = reversalOfCode
    )
