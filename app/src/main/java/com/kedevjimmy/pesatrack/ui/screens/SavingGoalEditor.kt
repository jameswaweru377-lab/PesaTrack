package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    kotlinx.coroutines.CancellationException
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

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    ModalBottomSheet(
        onDismissRequest = {
            if (!busy) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor =
            MaterialTheme.colorScheme.surface,
        shape =
            RoundedCornerShape(
                topStart = 30.dp,
                topEnd = 30.dp
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 20.dp
                    )
        ) {
            Text(
                when {
                    remove ->
                        "Remove this $noun?"
                    rule == null && bill ->
                        "New bill"
                    rule == null ->
                        "New saving goal"
                    else ->
                        "Edit $noun"
                },
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            Text(
                if (remove) {
                    "This stops the $noun and its matching rule. Existing transactions and balances stay unchanged."
                } else if (bill) {
                    "Set the bill once. PesaTrack will use real payment activity to keep its status current."
                } else {
                    "Create a goal PesaTrack can match to real savings transfers."
                },
                modifier =
                    Modifier.padding(
                        top = 5.dp
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 18.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        13.dp
                    )
            ) {
                if (!remove) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = null
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
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
                            modifier =
                                Modifier.fillMaxWidth(),
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
                                },
                                modifier =
                                    Modifier.weight(1f)
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
                                },
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }

                        if (
                            cadence ==
                            BillCadence.MONTHLY
                        ) {
                            SalaryReceiptDateButton(
                                date =
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
                            Text(
                                "Repeats on day $dueDay of each month. Short months use their last day.",
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
                                    Calendar.MONDAY to "M",
                                    Calendar.TUESDAY to "T",
                                    Calendar.WEDNESDAY to "W",
                                    Calendar.THURSDAY to "T",
                                    Calendar.FRIDAY to "F",
                                    Calendar.SATURDAY to "S",
                                    Calendar.SUNDAY to "S"
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
                        value = target,
                        onValueChange = {
                            target = it
                            error = null
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                when {
                                    bill &&
                                        cadence ==
                                        BillCadence.WEEKLY ->
                                        "Amount each week (KES)"
                                    bill ->
                                        "Monthly amount (KES)"
                                    else ->
                                        "Monthly saving target (KES)"
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
                        value = payee,
                        onValueChange = {
                            payee = it
                            error = null
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                if (bill) {
                                    "Who do you pay?"
                                } else {
                                    "Where will you save this money?"
                                }
                            )
                        },
                        supportingText = {
                            Text(
                                if (bill) {
                                    "Use the payee/name that appears in the payment message."
                                } else {
                                    "Use the name that appears in transfers, for example M-Shwari, SACCO or a savings account."
                                }
                            )
                        },
                        singleLine = true
                    )

                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                16.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                                .copy(alpha = .55f)
                    ) {
                        Text(
                            if (bill) {
                                "Bills are added only when you choose them. Matching trusted payments mark the current month's bill as paid; PesaTrack never creates or pays a bill automatically."
                            } else {
                                "Only trusted transfers matched to this goal count toward progress. Creating a goal never moves money."
                            },
                            modifier =
                                Modifier.padding(
                                    13.dp
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    if (rule != null) {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                remove = true
                            }
                        ) {
                            Text(
                                "Remove $noun",
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error
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

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 20.dp,
                            bottom = 18.dp
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        if (remove) {
                            remove = false
                        } else {
                            onDismiss()
                        }
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    enabled = !busy,
                    onClick = {
                        save(remove)
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        if (remove) {
                            "Remove"
                        } else {
                            "Save $noun"
                        }
                    )
                }
            }
        }
    }
}
