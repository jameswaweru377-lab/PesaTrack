package com.kedevjimmy.pesatrack.planning

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Source-compatible scheduling API; the type is owned by the pure domain layer. */
typealias BillCadence = com.kedevjimmy.pesatrack.domain.planning.BillCadence

data class BillScheduleConfig(
    val cadence: BillCadence,
    val dueDay: Int?,
    val dueDayOfWeek: Int?,
    val reminders: Boolean
)

data class ScheduledBill(
    val rule: SmartRule,
    val remainingMinor: Long,
    val dueDay: Int?,
    val reminders: Boolean,
    val cadence: BillCadence = BillCadence.MONTHLY,
    val dueDayOfWeek: Int? = null,
    val expectedMinor: Long = 0L,
    val paidMinor: Long = 0L,
    val dueThisMonth: Boolean = false,
    val nextDueEpochMs: Long? = null
)

/**
 * User-defined bill schedules.
 *
 * A repeated transaction is not a bill by itself. Only rules explicitly accepted
 * as OTHER_RECURRING are eligible, and a due schedule is required before the bill
 * appears in Upcoming bills. Payment status is always derived from the trusted
 * ledger for the calendar month being viewed.
 */
class BillSchedules(private val context: Context) {
    private val prefs =
        context.getSharedPreferences(
            "bill_schedules",
            Context.MODE_PRIVATE
        )

    fun globalReminderLeadDays(): Int =
        prefs.getInt(
            "global_reminder_lead_days",
            3
        ).coerceIn(1, 30)

    fun remindersEnabled(): Boolean =
        prefs.getBoolean("reminders_allowed", true) && (globalRemindersEnabled() || list().any { it.reminders })

    fun globalRemindersEnabled(): Boolean =
        prefs.getBoolean(
            "global_reminders_enabled",
            false
        )

    fun setGlobalReminderLeadDays(
        days: Int,
        enable: Boolean = true
    ) {
        prefs.edit().putBoolean("reminders_allowed", enable).apply()
        require(days in 1..30) {
            "Reminder lead days must be between 1 and 30."
        }
        prefs.edit()
            .putInt(
                "global_reminder_lead_days",
                days
            )
            .putBoolean(
                "global_reminders_enabled",
                enable
            )
            .apply()
        schedule(context)
        LedgerUpdateBus.notifyChanged()
    }

    fun config(ruleId: String): BillScheduleConfig {
        val cadence =
            runCatching {
                BillCadence.valueOf(
                    prefs.getString(
                        "cadence:$ruleId",
                        BillCadence.MONTHLY.name
                    ) ?: BillCadence.MONTHLY.name
                )
            }.getOrDefault(BillCadence.MONTHLY)

        return BillScheduleConfig(
            cadence = cadence,
            dueDay =
                prefs.getInt("day:$ruleId", 0)
                    .takeIf { it in 1..31 },
            dueDayOfWeek =
                prefs.getInt("weekday:$ruleId", 0)
                    .takeIf {
                        it in Calendar.SUNDAY..Calendar.SATURDAY
                    },
            reminders =
                prefs.getBoolean(
                    "enabled:$ruleId",
                    false
                )
        )
    }

    /** All user-defined bills, with status calculated for the month containing [now]. */
    fun list(
        now: Long = System.currentTimeMillis()
    ): List<ScheduledBill> {
        val trusted =
            TransactionStore.get(context)
                .activitySnapshot(now)
                .trustedTransactions

        val explicitBillRules = RuleStore.get(context)
            .listRules(true)
            .filter {
                it.direction == MoneyDirection.EXPENSE &&
                    it.preset == RulePreset.OTHER_RECURRING &&
                    (it.plannedAmountMinor ?: 0L) > 0L &&
                    isExplicitBill(it.id)
            }
        return explicitBillRules
            .map { rule ->
                buildScheduledBill(
                    rule = rule,
                    trusted = trusted,
                    monthReference = now,
                    candidateBillRules = explicitBillRules
                )
            }
            .sortedWith(
                compareBy<ScheduledBill> {
                    it.nextDueEpochMs
                        ?: Long.MAX_VALUE
                }.thenBy {
                    it.rule.displayLabel
                        ?: it.rule.name
                }
            )
    }

    /**
     * Rules accepted using older Plan versions might lack an explicit due
     * schedule. Don't silently call them bills or create a guessed due date:
     * offer a one-time completion flow instead.
     */
    fun unconfiguredRecurringRules(): List<SmartRule> =
        RuleStore.get(context).listRules(true).filter {
            it.direction == MoneyDirection.EXPENSE &&
                it.preset == RulePreset.OTHER_RECURRING &&
                (it.plannedAmountMinor ?: 0L) > 0L &&
                !isExplicitBill(it.id)
        }

    /** Bills that are scheduled in the current month and still have money due. */
    fun upcomingThisMonth(
        now: Long = System.currentTimeMillis()
    ): List<ScheduledBill> =
        list(now).filter {
            it.dueThisMonth &&
                it.remainingMinor > 0L
        }

    fun saveSchedule(
        ruleId: String,
        cadence: BillCadence,
        dueDay: Int? = null,
        dueDayOfWeek: Int? = null,
        enabled: Boolean
    ) {
        when (cadence) {
            BillCadence.MONTHLY ->
                require(dueDay != null && dueDay in 1..31) {
                    "Choose a monthly due day."
                }
            BillCadence.WEEKLY ->
                require(
                    dueDayOfWeek != null &&
                        dueDayOfWeek in
                        Calendar.SUNDAY..
                            Calendar.SATURDAY
                ) {
                    "Choose a weekly due day."
                }
        }

        prefs.edit()
            .putBoolean(
                "bill:$ruleId",
                true
            )
            .putString(
                "cadence:$ruleId",
                cadence.name
            )
            .apply {
                if (cadence == BillCadence.MONTHLY) {
                    putInt(
                        "day:$ruleId",
                        requireNotNull(dueDay)
                    )
                    remove("weekday:$ruleId")
                } else {
                    putInt(
                        "weekday:$ruleId",
                        requireNotNull(
                            dueDayOfWeek
                        )
                    )
                    remove("day:$ruleId")
                }
            }
            .putBoolean(
                "enabled:$ruleId",
                enabled
            )
            .apply()

        if (!enabled) {
            (
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager
            ).cancel(ruleId, 921)
        }

        schedule(context)
        LedgerUpdateBus.notifyChanged()
    }

    /** Backward-compatible monthly reminder save. */
    fun save(
        ruleId: String,
        day: Int,
        enabled: Boolean
    ) {
        saveSchedule(
            ruleId = ruleId,
            cadence = BillCadence.MONTHLY,
            dueDay = day,
            enabled = enabled
        )
    }

    fun remove(ruleId: String) {
        prefs.edit()
            .remove("bill:$ruleId")
            .remove("cadence:$ruleId")
            .remove("day:$ruleId")
            .remove("weekday:$ruleId")
            .remove("enabled:$ruleId")
            .apply()
        (
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
        ).cancel(ruleId, 921)
        LedgerUpdateBus.notifyChanged()
    }

    fun notifyDue() {
        if (!prefs.getBoolean("reminders_allowed", true)) return
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) !in 8..20) return
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
        if (!manager.areNotificationsEnabled()) {
            return
        }

        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    "bills",
                    "Bill reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val now = Calendar.getInstance()

        val globalReminderOn =
            globalRemindersEnabled()
        val leadDays =
            globalReminderLeadDays()

        list(now.timeInMillis)
            .filter {
                (
                    it.reminders ||
                        globalReminderOn
                    ) &&
                    it.dueThisMonth &&
                    it.remainingMinor > 0L
            }
            .forEach { bill ->
                    val planned =
                        bill.rule.plannedAmountMinor
                            ?: return@forEach
                    val config =
                        config(bill.rule.id)
                    val paidOccurrences =
                        if (planned > 0L) {
                            (
                                bill.paidMinor /
                                    planned
                                ).toInt()
                        } else {
                            0
                        }

                    occurrenceDates(
                        config,
                        now.timeInMillis
                    )
                        .drop(paidOccurrences)
                        .forEach { dueEpochMs ->
                            val delta =
                                dueEpochMs -
                                    now.timeInMillis
                            if (
                                delta !in
                                0..
                                    TimeUnit.DAYS
                                        .toMillis(
                                            leadDays.toLong()
                                        )
                            ) {
                                return@forEach
                            }

                            val key =
                                "sent:" +
                                    bill.rule.id +
                                    ":" +
                                    dueEpochMs
                            if (
                                prefs.getBoolean(
                                    key,
                                    false
                                )
                            ) {
                                return@forEach
                            }

                            val open =
                                PendingIntent
                                    .getActivity(
                                        context,
                                        921,
                                        Intent(
                                            context,
                                            MainActivity::class.java
                                        )
                                            .putExtra(
                                                OPEN_BILLS,
                                                true
                                            )
                                            .addFlags(
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            ),
                                        PendingIntent.FLAG_IMMUTABLE or
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                    )

                            manager.notify(
                                bill.rule.id +
                                    dueEpochMs,
                                921,
                                NotificationCompat
                                    .Builder(
                                        context,
                                        "bills"
                                    )
                                    .setSmallIcon(
                                        android.R.drawable.ic_dialog_info
                                    )
                                    .setContentTitle(
                                        (
                                            bill.rule
                                                .displayLabel
                                                ?: bill.rule.name
                                            ) +
                                            " is coming up"
                                    )
                                    .setContentText(
                                        "Open PesaTrack to check this bill."
                                    )
                                    .setContentIntent(
                                        open
                                    )
                                    .setAutoCancel(true)
                                    .setVisibility(
                                        NotificationCompat.VISIBILITY_PRIVATE
                                    )
                                    .build()
                            )
                            prefs.edit()
                                .putBoolean(
                                    key,
                                    true
                                )
                                .apply()
                        }
            }
    }

    private fun isExplicitBill(
        ruleId: String
    ): Boolean =
        prefs.getBoolean(
            "bill:$ruleId",
            false
        ) ||
            prefs.contains(
                "day:$ruleId"
            ) ||
            prefs.contains(
                "weekday:$ruleId"
            )

    private fun buildScheduledBill(
        rule: SmartRule,
        trusted:
            List<StoredTransaction>,
        monthReference: Long,
        candidateBillRules: List<SmartRule>
    ): ScheduledBill {
        val config = config(rule.id)
        val occurrenceDates =
            occurrenceDates(
                config,
                monthReference
            )
        val planned =
            rule.plannedAmountMinor
                ?: 0L
        val expected =
            planned *
                occurrenceDates.size
        val paid =
            trusted
                .filter {
                    it.direction ==
                        MoneyDirection.EXPENSE.name &&
                        BillPaymentMatchingPolicy.matches(
                            transactionRuleId = it.ruleId,
                            rule = rule,
                            counterparty = it.counterparty,
                            counterpartyPhone = it.counterpartyPhone,
                            amountMinor = it.amountMinor,
                            candidateBills = candidateBillRules
                        )
                }
                .sumOf {
                    it.amountMinor
                }
        val remaining =
            (expected - paid)
                .coerceAtLeast(0L)

        val paidOccurrences =
            if (planned > 0L) {
                (paid / planned)
                    .toInt()
                    .coerceAtMost(
                        occurrenceDates.size
                    )
            } else {
                0
            }

        return ScheduledBill(
            rule = rule,
            remainingMinor = remaining,
            dueDay = config.dueDay,
            reminders = config.reminders,
            cadence = config.cadence,
            dueDayOfWeek =
                config.dueDayOfWeek,
            expectedMinor = expected,
            paidMinor = paid,
            dueThisMonth =
                occurrenceDates
                    .isNotEmpty(),
            nextDueEpochMs =
                occurrenceDates
                    .drop(
                        paidOccurrences
                    )
                    .firstOrNull()
        )
    }

    private fun occurrenceDates(
        config: BillScheduleConfig,
        monthReference: Long
    ): List<Long> {
        val month =
            Calendar.getInstance()
                .apply {
                    timeInMillis =
                        monthReference
                    set(
                        Calendar.DAY_OF_MONTH,
                        1
                    )
                    set(
                        Calendar.HOUR_OF_DAY,
                        23
                    )
                    set(
                        Calendar.MINUTE,
                        59
                    )
                    set(
                        Calendar.SECOND,
                        0
                    )
                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

        return when (
            config.cadence
        ) {
            BillCadence.MONTHLY -> {
                val dueDay =
                    config.dueDay
                        ?: return emptyList()
                listOf(
                    (
                        month.clone()
                            as Calendar
                        ).apply {
                            set(
                                Calendar.DAY_OF_MONTH,
                                minOf(
                                    dueDay,
                                    getActualMaximum(
                                        Calendar.DAY_OF_MONTH
                                    )
                                )
                            )
                        }.timeInMillis
                )
            }

            BillCadence.WEEKLY -> {
                val dayOfWeek =
                    config.dueDayOfWeek
                        ?: return emptyList()
                val dates =
                    mutableListOf<Long>()
                val cursor =
                    month.clone()
                        as Calendar
                val targetMonth =
                    month.get(
                        Calendar.MONTH
                    )
                while (
                    cursor.get(
                        Calendar.MONTH
                    ) == targetMonth
                ) {
                    if (
                        cursor.get(
                            Calendar.DAY_OF_WEEK
                        ) == dayOfWeek
                    ) {
                        dates +=
                            cursor
                                .timeInMillis
                    }
                    cursor.add(
                        Calendar.DAY_OF_MONTH,
                        1
                    )
                }
                dates
            }
        }
    }

    companion object {
        const val OPEN_BILLS =
            "open_bills"

        fun schedule(
            context: Context
        ) {
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    "bill-reminders",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<
                        BillReminderWorker
                        >(
                        12,
                        TimeUnit.HOURS
                    ).build()
                )
        }
    }
}

class BillReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result =
        try {
            BillSchedules(
                applicationContext
            ).notifyDue()
            Result.success()
        } catch (
            e: Exception
        ) {
            Result.retry()
        }
}
