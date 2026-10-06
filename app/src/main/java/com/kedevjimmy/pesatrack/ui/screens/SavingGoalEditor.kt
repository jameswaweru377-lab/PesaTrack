package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.planning.BillCadence
import com.kedevjimmy.pesatrack.planning.BillSchedules
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SavingGoalEditor(
    rule: SmartRule?,
    bill: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val schedules = remember { BillSchedules(context) }
    val noun = if (bill) "bill" else "goal"

    val existingSchedule =
        remember(rule?.id, bill) {
            if (bill && rule != null) {
                schedules.config(rule.id)
            } else {
                null
            }
        }

    var name by remember {
        mutableStateOf(
            rule?.displayLabel
                ?: rule?.name.orEmpty()
        )
    }
    var payee by remember {
        mutableStateOf(
            rule?.counterpartyExact
                ?.ifBlank {
                    rule.counterpartyContains
                }
                .orEmpty()
        )
    }
    var target by remember {
        mutableStateOf(
            rule?.plannedAmountMinor
                ?.let {
                    java.math.BigDecimal
                        .valueOf(it, 2)
                        .toPlainString()
                }
                .orEmpty()
        )
    }
    var cadence by remember(rule?.id, bill) {
        mutableStateOf(
            existingSchedule?.cadence
                ?: BillCadence.MONTHLY
        )
    }
    var monthlyDueDate by remember(rule?.id, bill) {
        val calendar =
            Calendar.getInstance()
        val preferredDay =
            existingSchedule?.dueDay
                ?: calendar.get(
                    Calendar.DAY_OF_MONTH
                )
        calendar.set(
            Calendar.DAY_OF_MONTH,
            minOf(
                preferredDay,
                calendar.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )
            )
        )
        mutableLongStateOf(
            calendar.timeInMillis
        )
    }
    var weeklyDueDay by remember(rule?.id, bill) {
        mutableIntStateOf(
            existingSchedule
                ?.dueDayOfWeek
                ?: Calendar.MONDAY
        )
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }
    var busy by remember {
        mutableStateOf(false)
    }
    var remove by remember {
        mutableStateOf(false)
    }

    fun save(delete: Boolean = false) {
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    if (
                        delete &&
                        rule != null
                    ) {
                        PlanningRuleRepository(
                            context
                        ).disable(rule.id)
                        if (bill) {
                            schedules.remove(
                                rule.id
                            )
                        }
                    } else {
                        require(
                            name.isNotBlank() &&
                                payee.isNotBlank()
                        ) {
                            "Add a name and the payee shown in its receipts."
                        }

                        val minor =
                            requireNotNull(
                                parseSalaryAmount(
                                    target
                                )
                            ) {
                                if (bill) {
                                    "Enter a positive bill amount."
                                } else {
                                    "Enter a positive monthly target."
                                }
                            }

                        val saved =
                            PlanningRuleRepository(
                                context
                            ).save(
                                name,
                                payee,
                                minor,
                                bill,
                                rule
                            )

                        if (bill) {
                            // Link safe historical matches so a bill already
                            // paid earlier this month does not reappear as due.
                            TransactionStore
                                .get(context)
                                .reapplyRule(
                                    rule = saved,
                                    rebuildPayCycles =
                                        true
                                )

                            val dueDay =
                                Calendar
                                    .getInstance()
                                    .apply {
                                        timeInMillis =
                                            monthlyDueDate
                                    }
                                    .get(
                                        Calendar.DAY_OF_MONTH
                                    )

                            schedules.saveSchedule(
                                ruleId = saved.id,
                                cadence = cadence,
                                dueDay =
                                    if (
                                        cadence ==
                                        BillCadence.MONTHLY
                                    ) {
                                        dueDay
                                    } else {
                                        null
                                    },
                                dueDayOfWeek =
                                    if (
                                        cadence ==
                                        BillCadence.WEEKLY
                                    ) {
                                        weeklyDueDay
                                    } else {
                                        null
                                    },
                                enabled =
                                    existingSchedule
                                        ?.reminders
                                        ?: false
                            )
                        }
                    }
                }
                onDismiss()
            } catch (
                cancelled:
                    kotlin.coroutines.cancellation.CancellationException
            ) {
                throw cancelled
            } catch (
                failure: Exception
            ) {
                error =
                    failure.message
                        ?: "Couldn't save. Please try again."
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!busy) {
                onDismiss()
            }
        },
        title = {
            Text(
                if (remove) {
                    "Remove this $noun?"
                } else if (rule == null) {
                    if (bill) {
                        "New bill"
                    } else {
                        "New saving goal"
                    }
                } else {
                    "Edit $noun"
                }
            )
        },
        text = {
            Column(
                Modifier
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                if (remove) {
                    Text(
                        "This stops the $noun and its matching rule. Existing transactions and balances stay unchanged."
                    )
                } else {
                    OutlinedTextField(
                        name,
                        {
                            name = it
                            error = null
                        },
                        label = {
                            Text(
                                if (bill) {
                                    "Bill name"
                                } else {
                                    "Goal name"
                                }
                            )
                        },
                        singleLine = true
                    )

                    if (bill) {
                        Text(
                            "How often is it due?",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge
                        )
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            FilterChip(
                                selected =
                                    cadence ==
                                        BillCadence.MONTHLY,
                                onClick = {
                                    cadence =
                                        BillCadence.MONTHLY
                                },
                                label = {
                                    Text("Monthly")
                                }
                            )
                            FilterChip(
                                selected =
                                    cadence ==
                                        BillCadence.WEEKLY,
                                onClick = {
                                    cadence =
                                        BillCadence.WEEKLY
                                },
                                label = {
                                    Text("Weekly")
                                }
                            )
                        }

                        if (
                            cadence ==
                            BillCadence.MONTHLY
                        ) {
                            SalaryReceiptDateButton(
                                dateEpochMs =
                                    monthlyDueDate,
                                enabled = !busy,
                                label =
                                    "Due day each month",
                                allowFuture =
                                    true
                            ) {
                                monthlyDueDate =
                                    it
                            }
                            Text(
                                "Only the day number repeats each month. Short months use their last day.",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Due every",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium
                            )
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        4.dp
                                    )
                            ) {
                                listOf(
                                    Calendar.MONDAY to
                                        "M",
                                    Calendar.TUESDAY to
                                        "T",
                                    Calendar.WEDNESDAY to
                                        "W",
                                    Calendar.THURSDAY to
                                        "T",
                                    Calendar.FRIDAY to
                                        "F",
                                    Calendar.SATURDAY to
                                        "S",
                                    Calendar.SUNDAY to
                                        "S"
                                ).forEach {
                                        (
                                            day,
                                            label
                                        ) ->
                                    FilterChip(
                                        selected =
                                            weeklyDueDay ==
                                                day,
                                        onClick = {
                                            weeklyDueDay =
                                                day
                                        },
                                        label = {
                                            Text(label)
                                        },
                                        modifier =
                                            Modifier
                                                .weight(
                                                    1f
                                                )
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        target,
                        {
                            target = it
                            error = null
                        },
                        label = {
                            Text(
                                if (bill) {
                                    if (
                                        cadence ==
                                        BillCadence.WEEKLY
                                    ) {
                                        "Amount each week (KES)"
                                    } else {
                                        "Monthly amount (KES)"
                                    }
                                } else {
                                    "Monthly target (KES)"
                                }
                            )
                        },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            )
                    )

                    OutlinedTextField(
                        payee,
                        {
                            payee = it
                            error = null
                        },
                        label = {
                            Text(
                                if (bill) {
                                    "Payee on receipts"
                                } else {
                                    "Savings payee on receipts"
                                }
                            )
                        },
                        singleLine = true
                    )

                    Text(
                        if (bill) {
                            "Bills are added only when you choose them. Matching trusted payments mark the current month's bill as paid; PesaTrack never creates or pays a bill automatically."
                        } else {
                            "Only trusted transfers matched to this goal count toward its monthly progress. This does not move money."
                        },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    if (rule != null) {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                remove = true
                            }
                        ) {
                            Text(
                                "Remove $noun"
                            )
                        }
                    }
                }

                error?.let {
                    Text(
                        it,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    save(remove)
                }
            ) {
                Text(
                    if (remove) {
                        "Remove"
                    } else {
                        "Save $noun"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    if (remove) {
                        remove = false
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
