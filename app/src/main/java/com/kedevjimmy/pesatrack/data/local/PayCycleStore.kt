package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import com.kedevjimmy.pesatrack.domain.model.checkedMoneySumOf
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.rules.PayCyclePolicy
import com.kedevjimmy.pesatrack.domain.rules.PaydayCarryoverEvidence
import com.kedevjimmy.pesatrack.domain.rules.PaydayCarryoverPolicy
import com.kedevjimmy.pesatrack.domain.rules.FundingGapPolicy
import kotlin.math.max
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class PayCycleStore private constructor(context: Context) {
    private val appContext =
        context.applicationContext
    private val database =
        PesaTrackDatabase.get(appContext)
    private val paydaySnapshotStore =
        PaydaySnapshotStore.get(appContext)
    private val cycleProtectionStore =
        CycleProtectionStore.get(appContext)

    fun rebuild() {
        synchronized(CanonicalStoreLock.monitor) {
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
                        arrayOf<Any?>(ReviewState.USER_CLASSIFIED.name, event.transactionId)
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
                        arrayOf<Any?>(reason, ReviewState.NEEDS_REVIEW.name, event.transactionId)
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
                        arrayOf<Any?>(
                            ReviewState.NEEDS_REVIEW.name,
                            ReviewState.AUTO_CLASSIFIED.name,
                            event.transactionId
                        )
                    )
                }
            }

            paydaySnapshotStore.rebuildForSalaryAnchors(
                db = db,
                anchors =
                    accepted.map { event ->
                        PaydaySalaryAnchor(
                            transactionId =
                                event.transactionId,
                            occurredAtEpochMs =
                                event.occurredAtEpochMs,
                            amountMinor =
                                event.amountMinor,
                            accountId =
                                event.accountId
                        )
                    }
            )

            var previousEnding: Long? = null
            accepted.forEachIndexed { index, event ->
                val nextStart =
                    accepted.getOrNull(index + 1)
                        ?.occurredAtEpochMs
                val cycleId =
                    UUID.randomUUID().toString()
                val payday =
                    paydaySnapshotStore
                        .snapshotForSalaryTransaction(
                            db = db,
                            transactionId =
                                event.transactionId
                        )
                val carryover =
                    PaydayCarryoverPolicy
                        .resolve(
                            cycleIndex = index,
                            payday =
                                payday?.let {
                                    PaydayCarryoverEvidence(
                                        knownLiquidMinor =
                                            it.liquidCarryoverMinor,
                                        knownLiquidAccountCount =
                                            it.knownLiquidAccountCount,
                                        unknownLiquidAccountCount =
                                            it.unknownLiquidAccountCount
                                    )
                                },
                            previousTrackedEndingMinor =
                                previousEnding
                        )
                val ending =
                    nextStart?.let {
                        trackedRemainder(
                            startTransactionId = event.transactionId,
                            startEpochMs = event.occurredAtEpochMs,
                            endEpochMs = it,
                            openingIncomeMinor = event.amountMinor,
                            carryoverMinor = carryover
                        )
                    }

                val now = System.currentTimeMillis()
                val values = ContentValues().apply {
                    put("id", cycleId)
                    put("start_transaction_id", event.transactionId)
                    put("start_epoch_ms", event.occurredAtEpochMs)
                    if (nextStart != null) put("end_epoch_ms", nextStart)
                    else putNull("end_epoch_ms")
                    put("opening_income_minor", event.amountMinor)
                    put("carryover_minor", carryover)
                    if (ending != null) put("ending_tracked_minor", ending)
                    else putNull("ending_tracked_minor")
                    put("state", if (nextStart == null) STATE_ACTIVE else STATE_CLOSED)
                    put("created_at_epoch_ms", now)
                    put("updated_at_epoch_ms", now)
                }
                db.insertOrThrow("pay_cycles", null, values)

                val where =
                    if (nextStart == null) {
                        "occurred_at_epoch_ms >= ?"
                    } else {
                        "occurred_at_epoch_ms >= ? AND occurred_at_epoch_ms < ?"
                    }
                val args =
                    if (nextStart == null) {
                        arrayOf(event.occurredAtEpochMs.toString())
                    } else {
                        arrayOf(
                            event.occurredAtEpochMs.toString(),
                            nextStart.toString()
                        )
                    }
                ContentValues().apply {
                    put("pay_cycle_id", cycleId)
                }.also { transactionValues ->
                    db.update(
                        "transactions",
                        transactionValues,
                        where,
                        args
                    )
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
    }

    fun snapshot(
        nowEpochMs: Long = System.currentTimeMillis()
    ): PayCycleSnapshot {
        synchronized(CanonicalStoreLock.monitor) {
        val cycle = currentCycle()
        val pending = pendingSalaryCandidate()
        val paydaySnapshot =
            cycle?.let {
                paydaySnapshotStore
                    .snapshotForCycleStart(
                        it.startTransactionId
                    )
            }

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
                pendingSalary = pending,
                unexplainedFundingGapMinor = 0L,
                paydaySnapshot = null
            )
        }

        val totals = cycleTotals(cycle)
        val commitments = commitmentsFor(cycle)
        val remainingCommitments = commitments.checkedMoneySumOf { it.remainingMinor }
        val userProtectedMinor =
            cycleProtectionStore
                .get(
                    cycle.startTransactionId
                )
                ?.amountMinor
                ?: 0L
        val startingFunds = CheckedMoney.add(cycle.openingIncomeMinor, cycle.carryoverMinor)
        val available = CheckedMoney.total(startingFunds, totals.additionalIncomeMinor, totals.refundsMinor,
            CheckedMoney.negate(totals.spentMinor), CheckedMoney.negate(totals.feesMinor),
            CheckedMoney.negate(totals.savingsMinor), CheckedMoney.negate(remainingCommitments),
            CheckedMoney.negate(userProtectedMinor))
        val unexplainedFundingGap = FundingGapPolicy.unexplainedGap(
            startingFundsMinor = startingFunds,
            additionalIncomeMinor =
                CheckedMoney.add(totals.additionalIncomeMinor, totals.refundsMinor),
            spentMinor = totals.spentMinor,
            feesMinor = totals.feesMinor
        )

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
            pendingSalary = pending,
            unexplainedFundingGapMinor = unexplainedFundingGap,
            refundsMinor = totals.refundsMinor,
            paydaySnapshot = paydaySnapshot,
            userProtectedMinor =
                userProtectedMinor
        )
    }
    }

    fun confirmNewCycle(transactionId: Long) {
        synchronized(CanonicalStoreLock.monitor) {
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
    }

    fun keepAsAdditionalIncome(transactionId: Long) {
        synchronized(CanonicalStoreLock.monitor) {
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
    }

    fun listCycles(): List<PayCycle> = synchronized(CanonicalStoreLock.monitor) {
        database.readableDatabase.rawQuery(
            """
            SELECT id, start_transaction_id, start_epoch_ms, end_epoch_ms,
                   opening_income_minor, carryover_minor, ending_tracked_minor, state
            FROM pay_cycles ORDER BY start_epoch_ms DESC, start_transaction_id DESC
            """.trimIndent(), null
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toPayCycle()) } }
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
                   r.pay_window_end_day,
                   t.account_id
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
                            payWindowStartDay =
                                if (cursor.isNull(5)) null else cursor.getInt(5),
                            payWindowEndDay =
                                if (cursor.isNull(6)) null else cursor.getInt(6),
                            accountId =
                                if (cursor.isNull(7)) null else cursor.getString(7)
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
        return CheckedMoney.total(openingIncomeMinor, carryoverMinor, totals.additionalIncomeMinor,
            totals.refundsMinor, CheckedMoney.negate(totals.spentMinor), CheckedMoney.negate(totals.feesMinor),
            CheckedMoney.negate(totals.savingsMinor))
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
            add(ReviewState.NEEDS_REVIEW.name)
            add(MpesaTransactionType.REVERSAL.name)
        }.toTypedArray()

        return database.readableDatabase.rawQuery(
            """
            SELECT id, amount_minor, fee_minor, direction, category,
                   relation_type, related_transaction_id
            FROM transactions
            WHERE $selection
            """.trimIndent(),
            args
        ).use { cursor ->
            val personalShares = PersonalReceivableStore(appContext).linkedShares()
            var additionalIncome = 0L
            var spent = 0L
            var fees = 0L
            var savings = 0L
            var refunds = 0L

            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val amount = cursor.getLong(1)
                val fee = cursor.getLong(2)
                val direction = cursor.getString(3)
                val category = cursor.getString(4)
                val relationType =
                    if (cursor.isNull(5)) null else cursor.getString(5)
                val relatedTransactionId =
                    if (cursor.isNull(6)) null else cursor.getLong(6)

                fees = CheckedMoney.add(fees, fee)

                when {
                    relationType == TransactionStore.RELATION_REFUND &&
                        relatedTransactionId != null &&
                        isTrustedExpense(relatedTransactionId) ->
                        refunds = CheckedMoney.add(refunds, amount)
                    id != startTransactionId && direction == MoneyDirection.INCOME.name ->
                        additionalIncome = CheckedMoney.add(additionalIncome, Math.subtractExact(amount, personalShares[id]?.repaidMinor ?: 0L))
                    direction == MoneyDirection.EXPENSE.name ->
                        spent = CheckedMoney.add(spent, Math.subtractExact(amount, personalShares[id]?.lentMinor ?: 0L))
                    direction == MoneyDirection.TRANSFER.name &&
                        category in setOf(
                            "SAVINGS",
                            "INVESTMENT",
                            "DEBT_LOAN"
                        ) ->
                        savings = CheckedMoney.add(savings, amount)
                }
            }

            fees = CheckedMoney.add(fees, debtFeesForRange(startEpochMs, endEpochMs))

            CycleTotals(
                additionalIncomeMinor = additionalIncome,
                spentMinor = spent,
                feesMinor = fees,
                savingsMinor = savings,
                refundsMinor = refunds
            )
        }
    }

    private fun debtFeesForRange(
        startEpochMs: Long,
        endEpochMs: Long?
    ): Long {
        val endExclusive = endEpochMs ?: Long.MAX_VALUE
        return database.readableDatabase.rawQuery(
            """
            SELECT fee_minor
            FROM debt_events
            WHERE occurred_at_epoch_ms >= ?
              AND occurred_at_epoch_ms < ?
            """.trimIndent(),
            arrayOf(
                startEpochMs.toString(),
                endExclusive.toString()
            )
        ).use { cursor ->
            var total = 0L
            while (cursor.moveToNext()) total = CheckedMoney.add(total, cursor.getLong(0))
            total
        }
    }

    private fun isTrustedExpense(
        transactionId: Long
    ): Boolean =
        database.readableDatabase.rawQuery(
            """
            SELECT 1
            FROM transactions
            WHERE id = ?
              AND direction = ?
              AND review_state != ?
              AND review_state != ?
              AND type != ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                transactionId.toString(),
                MoneyDirection.EXPENSE.name,
                ReviewState.EXCLUDED.name,
                ReviewState.NEEDS_REVIEW.name,
                MpesaTransactionType.REVERSAL.name
            )
        ).use { cursor ->
            cursor.moveToFirst()
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
                            preset IN ('RENT', 'INTERNET', 'ELECTRICITY', 'FAMILY_SUPPORT')
                            OR (
                                planned_amount_minor IS NOT NULL
                                AND category IN ('RENT', 'INTERNET', 'ELECTRICITY', 'WATER', 'FAMILY')
                            )
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
                SELECT amount_minor
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
                var total = 0L
                while (cursor.moveToNext()) total = CheckedMoney.add(total, cursor.getLong(0))
                total
            }

            CommitmentProgress(
                ruleId = rule.id,
                name = rule.name,
                category = rule.category,
                plannedMinor = inferred,
                paidMinor = paid,
                remainingMinor = max(0L, CheckedMoney.subtract(inferred, paid))
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
              AND review_state != ?
            ORDER BY occurred_at_epoch_ms DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                ruleId,
                MoneyDirection.EXPENSE.name,
                ReviewState.EXCLUDED.name,
                ReviewState.NEEDS_REVIEW.name
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
        val payWindowEndDay: Int?,
        val accountId: String?
    )

    private data class CycleTotals(
        val additionalIncomeMinor: Long,
        val spentMinor: Long,
        val feesMinor: Long,
        val savingsMinor: Long,
        val refundsMinor: Long
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

