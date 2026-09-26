package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.rules.PayCyclePolicy
import kotlin.math.max
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class PayCycleStore private constructor(context: Context) {
    private val database = PesaTrackDatabase(context)

    @Synchronized
    fun rebuild() {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            db.delete("pay_cycles", null, null)
            db.execSQL(
                """
                UPDATE transactions
                SET pay_cycle_id = NULL,
                    cycle_candidate_reason = NULL
                """.trimIndent()
            )

            val salaryEvents = readSalaryEvents()
            val accepted = mutableListOf<SalaryEvent>()
            var previousAccepted: SalaryEvent? = null

            salaryEvents.forEach { event ->
                if (event.override == OVERRIDE_ADDITIONAL_INCOME) {
                    db.execSQL(
                        "UPDATE transactions SET review_state = ? WHERE id = ?",
                        arrayOf(ReviewState.USER_CLASSIFIED.name, event.transactionId)
                    )
                    return@forEach
                }

                val reason = if (event.override == OVERRIDE_FORCE_START) {
                    null
                } else {
                    PayCyclePolicy.candidateReason(
                        previousCycleStartEpochMs = previousAccepted?.occurredAtEpochMs,
                        currentSalaryEpochMs = event.occurredAtEpochMs,
                        payWindowStartDay = event.payWindowStartDay,
                        payWindowEndDay = event.payWindowEndDay
                    )
                }

                if (reason != null) {
                    db.execSQL(
                        """
                        UPDATE transactions
                        SET cycle_candidate_reason = ?,
                            review_state = ?
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf(reason, ReviewState.NEEDS_REVIEW.name, event.transactionId)
                    )
                } else {
                    accepted += event
                    previousAccepted = event
                    db.execSQL(
                        """
                        UPDATE transactions
                        SET cycle_candidate_reason = NULL,
                            review_state = CASE
                                WHEN review_state = ? THEN ?
                                ELSE review_state
                            END
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf(
                            ReviewState.NEEDS_REVIEW.name,
                            ReviewState.AUTO_CLASSIFIED.name,
                            event.transactionId
                        )
                    )
                }
            }

            var previousEnding = 0L
            accepted.forEachIndexed { index, event ->
                val nextStart = accepted.getOrNull(index + 1)?.occurredAtEpochMs
                val cycleId = UUID.randomUUID().toString()
                val ending = nextStart?.let {
                    trackedRemainder(
                        startTransactionId = event.transactionId,
                        startEpochMs = event.occurredAtEpochMs,
                        endEpochMs = it,
                        openingIncomeMinor = event.amountMinor,
                        carryoverMinor = previousEnding
                    )
                }

                val now = System.currentTimeMillis()
                val values = ContentValues().apply {
                    put("id", cycleId)
                    put("start_transaction_id", event.transactionId)
                    put("start_epoch_ms", event.occurredAtEpochMs)
                    if (nextStart != null) put("end_epoch_ms", nextStart) else putNull("end_epoch_ms")
                    put("opening_income_minor", event.amountMinor)
                    put("carryover_minor", previousEnding)
                    if (ending != null) put("ending_tracked_minor", ending) else putNull("ending_tracked_minor")
                    put("state", if (nextStart == null) STATE_ACTIVE else STATE_CLOSED)
                    put("created_at_epoch_ms", now)
                    put("updated_at_epoch_ms", now)
                }
                db.insertOrThrow("pay_cycles", null, values)

                val where = if (nextStart == null) {
                    "occurred_at_epoch_ms >= ?"
                } else {
                    "occurred_at_epoch_ms >= ? AND occurred_at_epoch_ms < ?"
                }
                val args = if (nextStart == null) {
                    arrayOf(event.occurredAtEpochMs.toString())
                } else {
                    arrayOf(event.occurredAtEpochMs.toString(), nextStart.toString())
                }

                ContentValues().apply {
                    put("pay_cycle_id", cycleId)
                }.also { transactionValues ->
                    db.update("transactions", transactionValues, where, args)
                }

                if (ending != null) {
                    previousEnding = ending
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun snapshot(
        nowEpochMs: Long = System.currentTimeMillis()
    ): PayCycleSnapshot {
        val cycle = currentCycle()
        val pending = pendingSalaryCandidate()

        if (cycle == null) {
            return PayCycleSnapshot(
                cycle = null,
                startingFundsMinor = 0L,
                additionalIncomeMinor = 0L,
                spentMinor = 0L,
                feesMinor = 0L,
                savingsMinor = 0L,
                remainingCommitmentsMinor = 0L,
                availableToLiveMinor = 0L,
                safeTodayMinor = null,
                estimatedDaysRemaining = null,
                commitments = emptyList(),
                pendingSalary = pending
            )
        }

        val totals = cycleTotals(cycle)
        val commitments = commitmentsFor(cycle)
        val remainingCommitments = commitments.sumOf { it.remainingMinor }
        val startingFunds = cycle.openingIncomeMinor + cycle.carryoverMinor
        val available = startingFunds +
            totals.additionalIncomeMinor -
            totals.spentMinor -
            totals.feesMinor -
            totals.savingsMinor -
            remainingCommitments

        val averageLength = averageCompletedCycleLengthDays()
        val daysRemaining = averageLength?.let { average ->
            val elapsed = max(
                0L,
                TimeUnit.MILLISECONDS.toDays(nowEpochMs - cycle.startEpochMs)
            ).toInt()
            max(1, average - elapsed)
        }

        val safeToday = daysRemaining?.let {
            max(0L, available) / it
        }

        return PayCycleSnapshot(
            cycle = cycle,
            startingFundsMinor = startingFunds,
            additionalIncomeMinor = totals.additionalIncomeMinor,
            spentMinor = totals.spentMinor,
            feesMinor = totals.feesMinor,
            savingsMinor = totals.savingsMinor,
            remainingCommitmentsMinor = remainingCommitments,
            availableToLiveMinor = available,
            safeTodayMinor = safeToday,
            estimatedDaysRemaining = daysRemaining,
            commitments = commitments,
            pendingSalary = pending
        )
    }

    @Synchronized
    fun confirmNewCycle(transactionId: Long) {
        ContentValues().apply {
            put("cycle_override", OVERRIDE_FORCE_START)
            put("review_state", ReviewState.USER_CLASSIFIED.name)
        }.also {
            database.writableDatabase.update(
                "transactions",
                it,
                "id = ?",
                arrayOf(transactionId.toString())
            )
        }
        rebuild()
    }

    @Synchronized
    fun keepAsAdditionalIncome(transactionId: Long) {
        ContentValues().apply {
            put("cycle_override", OVERRIDE_ADDITIONAL_INCOME)
            put("review_state", ReviewState.USER_CLASSIFIED.name)
            putNull("cycle_candidate_reason")
        }.also {
            database.writableDatabase.update(
                "transactions",
                it,
                "id = ?",
                arrayOf(transactionId.toString())
            )
        }
        rebuild()
    }

    fun currentCycle(): PayCycle? =
        database.readableDatabase.rawQuery(
            """
            SELECT id, start_transaction_id, start_epoch_ms, end_epoch_ms,
                   opening_income_minor, carryover_minor, ending_tracked_minor, state
            FROM pay_cycles
            WHERE state = ?
            ORDER BY start_epoch_ms DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(STATE_ACTIVE)
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.toPayCycle()
        }

    private fun readSalaryEvents(): List<SalaryEvent> =
        database.readableDatabase.rawQuery(
            """
            SELECT t.id, t.occurred_at_epoch_ms, t.amount_minor,
                   COALESCE(t.counterparty, r.display_label, r.name),
                   t.cycle_override,
                   r.pay_window_start_day,
                   r.pay_window_end_day
            FROM transactions t
            JOIN smart_rules r ON t.rule_id = r.id
            WHERE r.starts_pay_cycle = 1
              AND r.enabled = 1
              AND t.direction = ?
              AND t.review_state != ?
            ORDER BY t.occurred_at_epoch_ms ASC, t.id ASC
            """.trimIndent(),
            arrayOf(MoneyDirection.INCOME.name, ReviewState.EXCLUDED.name)
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SalaryEvent(
                            transactionId = cursor.getLong(0),
                            occurredAtEpochMs = cursor.getLong(1),
                            amountMinor = cursor.getLong(2),
                            label = cursor.getString(3) ?: "Salary",
                            override = if (cursor.isNull(4)) null else cursor.getString(4),
                            payWindowStartDay = if (cursor.isNull(5)) null else cursor.getInt(5),
                            payWindowEndDay = if (cursor.isNull(6)) null else cursor.getInt(6)
                        )
                    )
                }
            }
        }

    private fun trackedRemainder(
        startTransactionId: Long,
        startEpochMs: Long,
        endEpochMs: Long,
        openingIncomeMinor: Long,
        carryoverMinor: Long
    ): Long {
        val totals = totalsForRange(
            startTransactionId = startTransactionId,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs
        )
        return openingIncomeMinor +
            carryoverMinor +
            totals.additionalIncomeMinor -
            totals.spentMinor -
            totals.feesMinor -
            totals.savingsMinor
    }

    private fun cycleTotals(cycle: PayCycle): CycleTotals =
        totalsForRange(
            startTransactionId = cycle.startTransactionId,
            startEpochMs = cycle.startEpochMs,
            endEpochMs = cycle.endEpochMs
        )

    private fun totalsForRange(
        startTransactionId: Long,
        startEpochMs: Long,
        endEpochMs: Long?
    ): CycleTotals {
        val selection = buildString {
            append("occurred_at_epoch_ms >= ? ")
            if (endEpochMs != null) append("AND occurred_at_epoch_ms < ? ")
            append("AND review_state != ? ")
            append("AND type != ? ")
            append("AND cycle_candidate_reason IS NULL ")
            append("AND (transaction_code IS NULL OR transaction_code NOT IN (")
            append("SELECT reversal_of_code FROM transactions WHERE reversal_of_code IS NOT NULL")
            append("))")
        }
        val args = buildList {
            add(startEpochMs.toString())
            if (endEpochMs != null) add(endEpochMs.toString())
            add(ReviewState.EXCLUDED.name)
            add(MpesaTransactionType.REVERSAL.name)
        }.toTypedArray()

        return database.readableDatabase.rawQuery(
            """
            SELECT id, amount_minor, fee_minor, direction, category
            FROM transactions
            WHERE $selection
            """.trimIndent(),
            args
        ).use { cursor ->
            var additionalIncome = 0L
            var spent = 0L
            var fees = 0L
            var savings = 0L

            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val amount = cursor.getLong(1)
                val fee = cursor.getLong(2)
                val direction = cursor.getString(3)
                val category = cursor.getString(4)

                fees += fee

                when {
                    id != startTransactionId && direction == MoneyDirection.INCOME.name ->
                        additionalIncome += amount
                    direction == MoneyDirection.EXPENSE.name ->
                        spent += amount
                    direction == MoneyDirection.TRANSFER.name &&
                        category == "SAVINGS" ->
                        savings += amount
                }
            }

            CycleTotals(
                additionalIncomeMinor = additionalIncome,
                spentMinor = spent,
                feesMinor = fees,
                savingsMinor = savings
            )
        }
    }

    private fun commitmentsFor(cycle: PayCycle): List<CommitmentProgress> {
        val rules = database.readableDatabase.rawQuery(
            """
            SELECT id, name, category, direction, preset, planned_amount_minor
            FROM smart_rules
            WHERE enabled = 1
              AND starts_pay_cycle = 0
              AND (
                    (
                        direction = ?
                        AND (
                            planned_amount_minor IS NOT NULL
                            OR preset IN ('RENT', 'INTERNET', 'ELECTRICITY', 'FAMILY_SUPPORT', 'OTHER_RECURRING')
                        )
                    )
                    OR (
                        direction = ?
                        AND category = 'SAVINGS'
                        AND planned_amount_minor IS NOT NULL
                    )
              )
            ORDER BY name COLLATE NOCASE
            """.trimIndent(),
            arrayOf(
                MoneyDirection.EXPENSE.name,
                MoneyDirection.TRANSFER.name
            )
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        RuleCommitment(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            category = cursor.getString(2),
                            direction = cursor.getString(3),
                            plannedMinor = if (cursor.isNull(5)) null else cursor.getLong(5)
                        )
                    )
                }
            }
        }

        return rules.mapNotNull { rule ->
            val inferred = rule.plannedMinor ?: latestAmountForRule(rule.id)
            if (inferred == null || inferred <= 0L) return@mapNotNull null

            val paid = database.readableDatabase.rawQuery(
                """
                SELECT COALESCE(SUM(amount_minor), 0)
                FROM transactions
                WHERE pay_cycle_id = ?
                  AND rule_id = ?
                  AND direction = ?
                  AND review_state != ?
                  AND type != ?
                """.trimIndent(),
                arrayOf(
                    cycle.id,
                    rule.id,
                    rule.direction,
                    ReviewState.EXCLUDED.name,
                    MpesaTransactionType.REVERSAL.name
                )
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            }

            CommitmentProgress(
                ruleId = rule.id,
                name = rule.name,
                category = rule.category,
                plannedMinor = inferred,
                paidMinor = paid,
                remainingMinor = max(0L, inferred - paid)
            )
        }
    }

    private fun latestAmountForRule(ruleId: String): Long? =
        database.readableDatabase.rawQuery(
            """
            SELECT amount_minor
            FROM transactions
            WHERE rule_id = ?
              AND direction = ?
              AND review_state != ?
            ORDER BY occurred_at_epoch_ms DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                ruleId,
                MoneyDirection.EXPENSE.name,
                ReviewState.EXCLUDED.name
            )
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }

    private fun pendingSalaryCandidate(): SalaryCycleCandidate? =
        database.readableDatabase.rawQuery(
            """
            SELECT id, COALESCE(counterparty, 'Salary'), amount_minor,
                   occurred_at_epoch_ms, cycle_candidate_reason
            FROM transactions
            WHERE cycle_candidate_reason IS NOT NULL
            ORDER BY occurred_at_epoch_ms DESC
            LIMIT 1
            """.trimIndent(),
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                SalaryCycleCandidate(
                    transactionId = cursor.getLong(0),
                    label = cursor.getString(1),
                    amountMinor = cursor.getLong(2),
                    occurredAtEpochMs = cursor.getLong(3),
                    reason = cursor.getString(4)
                )
            }
        }

    private fun averageCompletedCycleLengthDays(): Int? =
        database.readableDatabase.rawQuery(
            """
            SELECT start_epoch_ms, end_epoch_ms
            FROM pay_cycles
            WHERE end_epoch_ms IS NOT NULL
            ORDER BY start_epoch_ms DESC
            LIMIT 4
            """.trimIndent(),
            null
        ).use { cursor ->
            val lengths = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                val start = cursor.getLong(0)
                val end = cursor.getLong(1)
                if (end > start) {
                    lengths += TimeUnit.MILLISECONDS.toDays(end - start)
                }
            }
            if (lengths.isEmpty()) null else max(1, lengths.average().toInt())
        }

    private data class SalaryEvent(
        val transactionId: Long,
        val occurredAtEpochMs: Long,
        val amountMinor: Long,
        val label: String,
        val override: String?,
        val payWindowStartDay: Int?,
        val payWindowEndDay: Int?
    )

    private data class CycleTotals(
        val additionalIncomeMinor: Long,
        val spentMinor: Long,
        val feesMinor: Long,
        val savingsMinor: Long
    )

    private data class RuleCommitment(
        val id: String,
        val name: String,
        val category: String,
        val direction: String,
        val plannedMinor: Long?
    )

    companion object {
        const val OVERRIDE_FORCE_START = "FORCE_START"
        const val OVERRIDE_ADDITIONAL_INCOME = "ADDITIONAL_INCOME"

        private const val STATE_ACTIVE = "ACTIVE"
        private const val STATE_CLOSED = "CLOSED"

        @Volatile
        private var instance: PayCycleStore? = null

        fun get(context: Context): PayCycleStore =
            instance ?: synchronized(this) {
                instance ?: PayCycleStore(context.applicationContext).also { instance = it }
            }
    }
}

private fun android.database.Cursor.toPayCycle(): PayCycle =
    PayCycle(
        id = getString(0),
        startTransactionId = getLong(1),
        startEpochMs = getLong(2),
        endEpochMs = if (isNull(3)) null else getLong(3),
        openingIncomeMinor = getLong(4),
        carryoverMinor = getLong(5),
        endingTrackedMinor = if (isNull(6)) null else getLong(6),
        state = getString(7)
    )
