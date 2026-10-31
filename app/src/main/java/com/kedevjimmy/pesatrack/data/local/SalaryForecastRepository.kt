package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.rules.ExpectedPayWindow
import com.kedevjimmy.pesatrack.domain.rules.SalaryBalanceValidityPolicy
import com.kedevjimmy.pesatrack.domain.rules.SalaryExpectationPolicy
import com.kedevjimmy.pesatrack.domain.rules.SalaryForecastBill
import com.kedevjimmy.pesatrack.domain.rules.SalaryForecastBillCadence
import com.kedevjimmy.pesatrack.domain.rules.SalaryForecastBillPolicy
import com.kedevjimmy.pesatrack.domain.rules.SalaryForecastCommitment
import com.kedevjimmy.pesatrack.domain.rules.SalaryForecastFacts
import com.kedevjimmy.pesatrack.domain.rules.SalaryForecastPolicy
import com.kedevjimmy.pesatrack.domain.rules.SalaryForecastResult
import com.kedevjimmy.pesatrack.domain.rules.SalarySpendingCycle
import com.kedevjimmy.pesatrack.planning.BillCadence
import com.kedevjimmy.pesatrack.planning.BillSchedules
import com.kedevjimmy.pesatrack.planning.ScheduledBill
import java.util.concurrent.TimeUnit

data class SalaryForecastConfirmation(val dateEpochMs: Long, val amountMinor: Long)

data class SalaryForecastRead(
    val result: SalaryForecastResult,
    val confirmation: SalaryForecastConfirmation?,
    val balanceSource: String,
    val allowsBlankBalance: Boolean,
    val hasActiveDebt: Boolean,
    val confirmedDebtDueMinor: Long?
)

/** Read-only evidence adapter; user forecast preferences never create salary or ledger entries. */
class SalaryForecastRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = PesaTrackDatabase.get(appContext)
    private val prefs = appContext.getSharedPreferences("salary_forecast_inputs", Context.MODE_PRIVATE)

    fun read(
        snapshot: PayCycleSnapshot,
        nowEpochMs: Long = System.currentTimeMillis()
    ): SalaryForecastRead {
        val currentStart = snapshot.cycle?.startTransactionId
        val matchingCycle = prefs.getLong("next_cycle_start", -1L) == (currentStart ?: -1L)
        val confirmation = if (matchingCycle) storedConfirmation() else null
        val rules = RuleStore.get(appContext).listRules(enabledOnly = true)
            .filter { it.startsPayCycle }
        val expected = rules.singleOrNull()?.let { rule ->
            snapshot.cycle?.let { cycle ->
                SalaryExpectationPolicy.nextWindow(
                    cycle.startEpochMs, rule.payWindowStartDay, rule.payWindowEndDay
                )
            }
        }
        val manual = confirmedBalance(nowEpochMs, currentStart, matchingCycle)
        val debtDue = confirmedDebtDue(nowEpochMs, currentStart, matchingCycle)
        val activeDebt = hasOutstandingDebtWithoutSchedule()
        val direct = observedSpendableBalance(nowEpochMs)
        val balance = manual ?: direct
        val bills = BillSchedules(appContext)
        val scheduled = bills.list(nowEpochMs)
        val paydayEnd = confirmation?.dateEpochMs ?: expected?.endEpochMs ?: nowEpochMs
        val commitments = commitments(snapshot, scheduled, paydayEnd, nowEpochMs) +
            if (activeDebt && debtDue != null) listOf(SalaryForecastCommitment(
                "forecast:debt", "User-confirmed loan/Fuliza repayments due before payday",
                debtDue, null
            )) else emptyList()
        val fixedIds = snapshot.commitments.map { it.ruleId }.toSet() +
            scheduled.map { it.rule.id }.toSet()
        val facts = SalaryForecastFacts(
            nowEpochMs = nowEpochMs,
            confirmedPaydayEpochMs = confirmation?.dateEpochMs,
            confirmedSalaryAmountMinor = confirmation?.amountMinor,
            expectedWindow = expected,
            balanceMinor = balance,
            balanceAsOfEpochMs = when {
                manual != null -> prefs.getLong("balance_confirmed_at", 0L)
                direct != null -> nowEpochMs
                else -> null
            },
            cycles = spendingHistory(fixedIds, nowEpochMs),
            commitments = commitments,
            hasUnscheduledDebt = activeDebt && debtDue == null,
            hasUnscheduledBills = bills.unconfiguredRecurringRules().isNotEmpty(),
            balanceLabel = if (manual != null) "Your confirmed spendable balance" else "Directly observed spendable accounts"
        )
        return SalaryForecastRead(
            result = SalaryForecastPolicy.evaluate(facts),
            confirmation = confirmation,
            balanceSource = when {
                manual != null -> "You confirmed the total across your spendable accounts."
                direct != null -> "All enabled spendable accounts have fresh direct observations."
                else -> "No complete fresh spendable balance is available."
            },
            allowsBlankBalance = direct != null,
            hasActiveDebt = activeDebt,
            confirmedDebtDueMinor = debtDue
        )
    }

    fun confirm(
        nextPaydayEpochMs: Long,
        expectedAmountMinor: Long,
        spendableBalanceMinor: Long?,
        debtDueBeforePaydayMinor: Long?,
        currentCycleStartId: Long?,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        require(nextPaydayEpochMs >= nowEpochMs - TimeUnit.DAYS.toMillis(1)) {
            "Choose today or a future payday."
        }
        require(nextPaydayEpochMs <= nowEpochMs + TimeUnit.DAYS.toMillis(45)) {
            "Choose a payday within the next 45 days."
        }
        require(expectedAmountMinor > 0L) { "Enter a positive expected salary amount." }
        require(debtDueBeforePaydayMinor == null || debtDueBeforePaydayMinor >= 0L) {
            "Enter a non-negative loan/Fuliza repayment amount."
        }
        val edit = prefs.edit()
            .putLong("next_payday", nextPaydayEpochMs)
            .putLong("next_amount", expectedAmountMinor)
            .putLong("next_cycle_start", currentCycleStartId ?: -1L)
        if (spendableBalanceMinor != null) {
            edit.putLong("balance_minor", spendableBalanceMinor)
                .putLong("balance_confirmed_at", nowEpochMs)
                .putString("balance_ledger_fingerprint", ledgerFingerprint())
        } else {
            edit.remove("balance_minor").remove("balance_confirmed_at")
                .remove("balance_ledger_fingerprint")
        }
        if (debtDueBeforePaydayMinor != null) {
            edit.putLong("debt_due_minor", debtDueBeforePaydayMinor)
                .putLong("debt_confirmed_at", nowEpochMs)
                .putString("debt_ledger_fingerprint", ledgerFingerprint())
        } else {
            edit.remove("debt_due_minor").remove("debt_confirmed_at")
                .remove("debt_ledger_fingerprint")
        }
        edit.apply()
    }

    private fun storedConfirmation(): SalaryForecastConfirmation? {
        val date = prefs.getLong("next_payday", 0L)
        val amount = prefs.getLong("next_amount", 0L)
        return if (date > 0L && amount > 0L) SalaryForecastConfirmation(date, amount) else null
    }

    private fun confirmedBalance(now: Long, cycleStart: Long?, matchingCycle: Boolean): Long? {
        if (!matchingCycle || !prefs.contains("balance_minor")) return null
        val confirmedAt = prefs.getLong("balance_confirmed_at", 0L)
        return prefs.getLong("balance_minor", 0L).takeIf {
            SalaryBalanceValidityPolicy.validManualBalance(
                confirmedAtEpochMs = confirmedAt,
                nowEpochMs = now,
                savedCycleStartId = prefs.getLong("next_cycle_start", -1L),
                currentCycleStartId = cycleStart ?: -1L,
                savedLedgerFingerprint = prefs.getString("balance_ledger_fingerprint", "").orEmpty(),
                currentLedgerFingerprint = ledgerFingerprint()
            )
        }
    }

    private fun confirmedDebtDue(now: Long, cycleStart: Long?, matchingCycle: Boolean): Long? {
        if (!matchingCycle || !prefs.contains("debt_due_minor")) return null
        return prefs.getLong("debt_due_minor", -1L).takeIf { it >= 0L &&
            SalaryBalanceValidityPolicy.validManualBalance(
                confirmedAtEpochMs = prefs.getLong("debt_confirmed_at", 0L),
                nowEpochMs = now,
                savedCycleStartId = prefs.getLong("next_cycle_start", -1L),
                currentCycleStartId = cycleStart ?: -1L,
                savedLedgerFingerprint = prefs.getString("debt_ledger_fingerprint", "").orEmpty(),
                currentLedgerFingerprint = ledgerFingerprint()
            )
        }
    }

    private fun observedSpendableBalance(now: Long): Long? {
        val accounts = AccountStore.get(appContext).listAccounts()
        val spendable = PaydaySnapshotStore.get(appContext)
            .currentAccountSnapshots(accounts, now)
            .filter { it.spendability == PaydaySpendability.SPENDABLE }
        if (spendable.isEmpty() || spendable.any {
            it.balanceMinor == null || it.quality != PaydayBalanceQuality.OBSERVED ||
                it.referenceEpochMs == null || now - it.referenceEpochMs !in 0L..TimeUnit.DAYS.toMillis(1)
        }) return null
        return spendable.fold(0L) { sum, row -> Math.addExact(sum, requireNotNull(row.balanceMinor)) }
    }

    private fun spendingHistory(fixedIds: Set<String>, now: Long): List<SalarySpendingCycle> =
        PayCycleStore.get(appContext).listCycles()
            .filter { it.endEpochMs != null && it.endEpochMs <= now }.take(3)
            .map { cycle ->
                val history = TransactionStore.get(appContext).activitySnapshotBetween(
                    cycle.startEpochMs, requireNotNull(cycle.endEpochMs),
                    nowEpochMs = now, performMaintenance = false
                )
                val repayments = repaymentTransactionIds(cycle.startEpochMs, cycle.endEpochMs)
                // Gross variable spending is deliberately conservative: refunds and
                // receivable adjustments must not make the forecast too optimistic.
                val variableSpent = history.trustedTransactions.filter {
                    it.direction == MoneyDirection.EXPENSE.name &&
                        it.ruleId !in fixedIds && it.id !in repayments
                }.fold(0L) { total, row -> Math.addExact(total, row.amountMinor) }
                val variable = Math.addExact(variableSpent, history.summary.feesMinor)
                SalarySpendingCycle(
                    cycle.startEpochMs, cycle.endEpochMs, variable,
                    unreviewedCount = history.summary.needsReviewCount + history.possibleDuplicates.size,
                    variableTransactionCount = history.trustedTransactions.count {
                        it.direction == MoneyDirection.EXPENSE.name &&
                            it.ruleId !in fixedIds && it.id !in repayments
                    }
                )
            }

    private fun commitments(
        snapshot: PayCycleSnapshot,
        bills: List<ScheduledBill>,
        paydayEnd: Long,
        now: Long
    ): List<SalaryForecastCommitment> {
        val billRuleIds = bills.map { it.rule.id }.toSet()
        val planned = snapshot.commitments.filter {
            it.remainingMinor > 0L && it.ruleId !in billRuleIds
        }.map { SalaryForecastCommitment(it.ruleId, it.name, it.remainingMinor, null) }
        val due = bills.flatMap { bill ->
            val config = BillSchedules(appContext).config(bill.rule.id)
            SalaryForecastBillPolicy.occurrences(
                SalaryForecastBill(
                    id = bill.rule.id,
                    name = bill.rule.displayLabel ?: bill.rule.name,
                    amountPerOccurrenceMinor = bill.rule.plannedAmountMinor ?: 0L,
                    cadence = if (config.cadence == BillCadence.WEEKLY)
                        SalaryForecastBillCadence.WEEKLY else SalaryForecastBillCadence.MONTHLY,
                    dayOfMonth = config.dueDay,
                    dayOfWeek = config.dueDayOfWeek,
                    paidMinorThisMonth = bill.paidMinor
                ), now, minOf(paydayEnd, now + TimeUnit.DAYS.toMillis(46))
            )
        }
        return planned + due
    }

    private fun repaymentTransactionIds(start: Long, end: Long?): Set<Long> =
        database.readableDatabase.rawQuery(
            """SELECT linked_transaction_id FROM debt_events
               WHERE event_type = 'REPAYMENT' AND linked_transaction_id IS NOT NULL
                 AND occurred_at_epoch_ms >= ? AND occurred_at_epoch_ms < ?""".trimIndent(),
            arrayOf(start.toString(), (end ?: Long.MAX_VALUE).toString())
        ).use { rows -> buildSet { while (rows.moveToNext()) add(rows.getLong(0)) } }

    private fun hasOutstandingDebtWithoutSchedule(): Boolean =
        database.readableDatabase.rawQuery(
            "SELECT 1 FROM debt_accounts WHERE outstanding_minor > 0 LIMIT 1", null
        ).use { it.moveToFirst() } ||
            database.readableDatabase.rawQuery(
                """SELECT fuliza_outstanding_minor FROM transactions
                   WHERE fuliza_outstanding_minor IS NOT NULL
                   ORDER BY occurred_at_epoch_ms DESC, id DESC LIMIT 1""".trimIndent(), null
            ).use { it.moveToFirst() && it.getLong(0) > 0L }

    private fun ledgerFingerprint(): String =
        database.readableDatabase.rawQuery(
            """SELECT (SELECT COUNT(*) FROM transactions),
                      (SELECT COALESCE(MAX(id), 0) FROM transactions),
                      (SELECT COUNT(*) FROM debt_events),
                      (SELECT COALESCE(MAX(id), 0) FROM debt_events)""".trimIndent(), null
        ).use { cursor ->
            if (!cursor.moveToFirst()) "" else (0..3).joinToString(":") { cursor.getLong(it).toString() }
        }
}
