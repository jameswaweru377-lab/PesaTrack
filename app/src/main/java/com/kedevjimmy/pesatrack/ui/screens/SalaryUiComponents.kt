package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.SalaryDatePickerPolicy
import java.text.SimpleDateFormat
import java.util.Locale

internal fun salaryDisplayDate(epoch: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(epoch)

@Composable
internal fun SalaryAccountGrid(accounts: List<MoneyAccount>, selectedId: String, enabled: Boolean, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { account ->
                    val selected = account.id == selectedId
                    Surface(
                        modifier = Modifier.weight(1f).testTag("salary-account-${account.id}")
                            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(account.id) }),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(Modifier.defaultMinSize(minHeight = 60.dp).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(account.name, style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalaryReceiptDateButton(date: Long, enabled: Boolean, onDate: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().testTag("salary-receipt-date"), shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(14.dp)) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text("Received on", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(salaryDisplayDate(date), style = MaterialTheme.typography.bodyLarge)
        }
        Text("Choose", style = MaterialTheme.typography.labelLarge)
    }
    if (open) {
        val today = remember { SalaryDatePickerPolicy.toPickerMillis(System.currentTimeMillis()) }
        val state = rememberDatePickerState(initialSelectedDateMillis = SalaryDatePickerPolicy.toPickerMillis(date),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            })
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(enabled = state.selectedDateMillis != null, onClick = {
                state.selectedDateMillis?.let { onDate(SalaryDatePickerPolicy.toReceiptMillis(it)) }
                open = false
            }, modifier = Modifier.testTag("salary-date-confirm")) { Text("Use date") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }) {
            DatePicker(state = state, showModeToggle = false, title = { Text("Receipt date", Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}

@Composable
internal fun SalarySection(title: String, highlighted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
