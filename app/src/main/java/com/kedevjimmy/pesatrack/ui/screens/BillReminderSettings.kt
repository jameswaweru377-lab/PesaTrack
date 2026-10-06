package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.planning.BillCadence
import com.kedevjimmy.pesatrack.planning.BillSchedules
import com.kedevjimmy.pesatrack.planning.ScheduledBill
import java.text.DateFormatSymbols
import java.util.Locale

@Composable
internal fun BillReminderSettings(
    bill: ScheduledBill,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var enabled by remember(bill.rule.id) {
        mutableStateOf(bill.reminders)
    }
    var denied by remember {
        mutableStateOf(false)
    }

    val hasSchedule =
        when (bill.cadence) {
            BillCadence.MONTHLY ->
                bill.dueDay != null
            BillCadence.WEEKLY ->
                bill.dueDayOfWeek != null
        }

    val permission =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) { granted ->
            enabled =
                granted &&
                    hasSchedule
            denied = !granted
        }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                bill.rule.displayLabel
                    ?: bill.rule.name
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(
                    rememberScrollState()
                ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                Text(
                    if (hasSchedule) {
                        billScheduleText(
                            bill
                        )
                    } else {
                        "No due schedule yet. Edit this bill first to choose when it repeats."
                    },
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )

                Row {
                    Text(
                        "Remind me before it is due",
                        Modifier.weight(1f)
                    )
                    Switch(
                        checked =
                            enabled &&
                                hasSchedule,
                        enabled =
                            hasSchedule,
                        onCheckedChange = {
                                on ->
                            if (
                                on &&
                                Build.VERSION.SDK_INT >=
                                33
                            ) {
                                permission.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                enabled =
                                    on &&
                                        hasSchedule
                            }
                        }
                    )
                }

                Text(
                    "PesaTrack checks the trusted ledger before reminding you. A bill already paid in the current month is not shown as still due.",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                if (denied) {
                    Text(
                        "Allow notifications in Android Settings to receive bill reminders.",
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
                onClick = {
                    if (hasSchedule) {
                        BillSchedules(
                            context
                        ).saveSchedule(
                            ruleId =
                                bill.rule.id,
                            cadence =
                                bill.cadence,
                            dueDay =
                                bill.dueDay,
                            dueDayOfWeek =
                                bill.dueDayOfWeek,
                            enabled =
                                enabled
                        )
                    }
                    onDismiss()
                }
            ) {
                Text(
                    if (hasSchedule) {
                        "Save reminder"
                    } else {
                        "Done"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun billScheduleText(
    bill: ScheduledBill
): String =
    when (bill.cadence) {
        BillCadence.MONTHLY ->
            "Monthly • due on day " +
                bill.dueDay
        BillCadence.WEEKLY -> {
            val names =
                DateFormatSymbols(
                    Locale.getDefault()
                ).weekdays
            "Weekly • due every " +
                names[
                    bill.dueDayOfWeek
                        ?: 1
                ]
        }
    }
