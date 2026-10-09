package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.planning.VoicePlanKind
import com.kedevjimmy.pesatrack.planning.BillCadence
import com.kedevjimmy.pesatrack.planning.BillSchedules
import com.kedevjimmy.pesatrack.planning.ScheduledBill
import com.kedevjimmy.pesatrack.ui.components.ConceptLoadError
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private fun billMoney(minor: Long): String {
    val absolute = abs(minor)
    val hasCents = absolute % 100L != 0L
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (hasCents) 2 else 0
        maximumFractionDigits = if (hasCents) 2 else 0
    }
    return (if (minor < 0L) "-KSh " else "KSh ") +
        formatter.format(absolute / 100.0)
}

private fun billMonth(offset: Int): Calendar =
    Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, offset)
    }

private enum class BillSort(val label: String) {
    DUE_DATE("Due Date"),
    AMOUNT("Amount"),
    NAME("Name")
}

private data class BillVisual(
    val icon: ImageVector,
    val color: Color
)

private fun billVisual(bill: ScheduledBill): BillVisual {
    val title = (bill.rule.displayLabel ?: bill.rule.name)
        .lowercase(Locale.getDefault())
    return when {
        "rent" in title ->
            BillVisual(Icons.Rounded.Home, Color(0xFFFF6B82))
        "wifi" in title || "wi-fi" in title || "internet" in title ->
            BillVisual(Icons.Rounded.Wifi, Color(0xFF57C7FF))
        "electric" in title || "power" in title || "kplc" in title ->
            BillVisual(Icons.Rounded.Bolt, Color(0xFFF4C768))
        "water" in title ->
            BillVisual(Icons.Rounded.WaterDrop, Color(0xFF57C7FF))
        "subscription" in title || "netflix" in title || "youtube" in title ->
            BillVisual(Icons.Rounded.PlayCircle, Color(0xFFA69CFF))
        "loan" in title || "debt" in title || "repayment" in title ->
            BillVisual(Icons.Rounded.Savings, Color(0xFF55E6D1))
        else ->
            BillVisual(Icons.Rounded.ReceiptLong, Color(0xFF14B8A6))
    }
}

private fun billStatus(bill: ScheduledBill): Pair<String, String> {
    if (bill.remainingMinor <= 0L) {
        return "Paid" to "green"
    }
    val due = bill.nextDueEpochMs
        ?: return "Upcoming" to "amber"
    val days = TimeUnit.MILLISECONDS.toDays(
        due - System.currentTimeMillis()
    )
    return when {
        days < 0L -> "Overdue" to "red"
        days <= 3L -> "Due Soon" to "red"
        else -> "Upcoming" to "amber"
    }
}

private fun billDueLabel(
    bill: ScheduledBill,
    locale: Locale
): String {
    val due = bill.nextDueEpochMs
        ?: return when (bill.cadence) {
            BillCadence.MONTHLY -> "Due monthly"
            BillCadence.WEEKLY -> "Due weekly"
        }
    val diff = TimeUnit.MILLISECONDS.toDays(
        due - System.currentTimeMillis()
    ).toInt()
    val suffix = when {
        diff == 0 -> "today"
        diff == 1 -> "in 1 day"
        diff > 1 -> "in " + diff + " days"
        diff == -1 -> "1 day overdue"
        else -> abs(diff).toString() + " days overdue"
    }
    val date = SimpleDateFormat("d MMM yyyy", locale)
        .format(java.util.Date(due))
    return "Due " + date + " (" + suffix + ")"
}

@Composable
private fun BillsPageHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back"
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Bills & Reminders",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Stay on top of your payments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun BillHeroBars() {
    val heights = listOf(23, 39, 60, 45, 35, 50, 56, 42)
    Row(
        modifier = Modifier.width(118.dp).height(68.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF55E6D1),
                                Color(0xFF3933A8)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun BillMonthSelector(
    monthOffset: Int,
    onMonthSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(50.dp),
            color = Color.White.copy(alpha = .06f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = .14f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (monthOffset == 0) {
                        "This Month"
                    } else {
                        SimpleDateFormat("MMM yyyy", locale)
                            .format(billMonth(monthOffset).time)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 3.dp).size(17.dp),
                    tint = Color.White
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (0..5).forEach { offset ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (offset == 0) {
                                "This Month"
                            } else {
                                SimpleDateFormat("MMMM yyyy", locale)
                                    .format(billMonth(offset).time)
                            }
                        )
                    },
                    onClick = {
                        onMonthSelected(offset)
                        expanded = false
                    },
                    trailingIcon = if (offset == monthOffset) {
                        {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun BillsHero(
    bills: List<ScheduledBill>,
    monthOffset: Int,
    onMonthSelected: (Int) -> Unit
) {
    val dueBills = bills.filter { it.remainingMinor > 0L }
    val amount = dueBills.sumOf { it.remainingMinor }
    val earliest = dueBills.mapNotNull { it.nextDueEpochMs }.minOrNull()
    val days = earliest?.let {
        TimeUnit.MILLISECONDS.toDays(
            it - System.currentTimeMillis()
        ).coerceAtLeast(0L)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF111827),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF433BB8)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Color(0xFF55E6D1)
                        )
                        Text(
                            if (monthOffset == 0) {
                                "Upcoming Bills This Month"
                            } else {
                                "Upcoming Bills"
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = .78f)
                        )
                    }
                    Text(
                        billMoney(amount),
                        modifier = Modifier.padding(top = 7.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        buildString {
                            append(dueBills.size)
                            append(
                                if (dueBills.size == 1) {
                                    " bill left"
                                } else {
                                    " bills left"
                                }
                            )
                            days?.let {
                                append("  •  Due in ")
                                append(it)
                                append(if (it == 1L) " day" else " days")
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = .74f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    BillMonthSelector(monthOffset, onMonthSelected)
                    Spacer(Modifier.height(8.dp))
                    if (dueBills.isNotEmpty()) BillHeroBars()
                }
            }
        }
    }
}

private fun billDateColor(bill: ScheduledBill): Color =
    when (billStatus(bill).second) {
        "red" -> Color(0xFFEF5F78)
        "green" -> Color(0xFF55E6D1)
        else -> billVisual(bill).color
    }

@Composable
private fun BillWeekStrip(
    bills: List<ScheduledBill>,
    monthOffset: Int,
    onOpenCalendar: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val month = billMonth(monthOffset)
    val now = Calendar.getInstance()
    val start = if (monthOffset == 0) {
        now.clone() as Calendar
    } else {
        month.clone() as Calendar
    }
    if (start.get(Calendar.MONTH) != month.get(Calendar.MONTH)) {
        start.timeInMillis = month.timeInMillis
    }
    val days = (0 until 7).map { offset ->
        (start.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, offset)
        }
    }.filter {
        it.get(Calendar.MONTH) == month.get(Calendar.MONTH)
    }

    PesaCard {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    SimpleDateFormat("MMMM yyyy", locale).format(month.time),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenCalendar) {
                    Text("See calendar")
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEach { day ->
                    val dayBills = bills.filter { bill ->
                        bill.nextDueEpochMs?.let { due ->
                            val dueCalendar = Calendar.getInstance().apply {
                                timeInMillis = due
                            }
                            dueCalendar.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                                dueCalendar.get(Calendar.DAY_OF_YEAR) ==
                                day.get(Calendar.DAY_OF_YEAR)
                        } == true
                    }
                    val selected =
                        monthOffset == 0 &&
                            day.get(Calendar.DAY_OF_YEAR) ==
                            now.get(Calendar.DAY_OF_YEAR)

                    Surface(
                        modifier = Modifier.weight(1f).height(72.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = if (selected) {
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFF55E6D1)
                            )
                        } else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                SimpleDateFormat("EEE", locale)
                                    .format(day.time)
                                    .take(3),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                day.get(Calendar.DAY_OF_MONTH).toString(),
                                modifier = Modifier.padding(top = 3.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (dayBills.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.padding(top = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    dayBills.take(2).forEach {
                                        Box(
                                            Modifier
                                                .size(6.dp)
                                                .background(
                                                    billDateColor(it),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillCalendarSheet(
    bills: List<ScheduledBill>,
    monthOffset: Int,
    onDismiss: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val month = billMonth(monthOffset)
    val firstDay = (
        month.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7
        ) % 7
    val max = month.getActualMaximum(Calendar.DAY_OF_MONTH)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Text(
                SimpleDateFormat("MMMM yyyy", locale).format(month.time),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Bill calendar",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    .forEach {
                        Text(
                            it.take(1),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
            }

            val cells: List<Int?> =
                List(firstDay) {
                    null
                } +
                    (1..max).map {
                        it
                    }

            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    (0 until 7).forEach { index ->
                        val day = week.getOrNull(index)
                        if (day == null) {
                            Spacer(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        } else {
                            val dayBills = bills.filter { bill ->
                                bill.nextDueEpochMs?.let { due ->
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = due
                                    }
                                    cal.get(Calendar.YEAR) == month.get(Calendar.YEAR) &&
                                        cal.get(Calendar.MONTH) == month.get(Calendar.MONTH) &&
                                        cal.get(Calendar.DAY_OF_MONTH) == day
                                } == true
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    day.toString(),
                                    fontWeight = if (dayBills.isNotEmpty()) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                                if (dayBills.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        dayBills.take(3).forEach {
                                            Box(
                                                Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        billDateColor(it),
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun BillReferenceRow(
    bill: ScheduledBill,
    locale: Locale,
    onClick: () -> Unit
) {
    val visual = billVisual(bill)
    val status = billStatus(bill)
    val planned = bill.rule.plannedAmountMinor ?: bill.expectedMinor

    PesaCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(15.dp),
                color = visual.color.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        visual.icon,
                        contentDescription = null,
                        modifier = Modifier.size(29.dp),
                        tint = visual.color
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = 11.dp)
            ) {
                Text(
                    bill.rule.displayLabel ?: bill.rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    billMoney(planned),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        null,
                        Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        billDueLabel(bill, locale),
                        modifier = Modifier.padding(start = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (
                            status.first == "Due Soon" ||
                            status.first == "Overdue"
                        ) {
                            Color(0xFFFF6B82)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(
                        "Recurring · " +
                            when (bill.cadence) {
                                BillCadence.MONTHLY -> "Monthly"
                                BillCadence.WEEKLY -> "Weekly"
                            },
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = when (status.second) {
                        "red" -> Color(0xFF3A1D2A)
                        "green" -> Color(0xFF202B42)
                        else -> Color(0xFF352B19)
                    }
                ) {
                    Text(
                        status.first,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (status.second) {
                            "red" -> Color(0xFFFF92A3)
                            "green" -> Color(0xFF55E6D1)
                            else -> Color(0xFFF4C768)
                        }
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                null,
                Modifier.padding(start = 6.dp).size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BillReminderCard(
    schedules: BillSchedules
) {
    val context = LocalContext.current
    var leadDays by remember {
        mutableIntStateOf(schedules.globalReminderLeadDays())
    }
    var expanded by remember { mutableStateOf(false) }
    var pendingDays by remember { mutableIntStateOf(leadDays) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            leadDays = pendingDays
            schedules.setGlobalReminderLeadDays(
                days = pendingDays,
                enable = true
            )
        }
    }

    fun setDays(days: Int) {
        pendingDays = days
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            leadDays = days
            schedules.setGlobalReminderLeadDays(
                days = days,
                enable = true
            )
        }
    }

    PesaCard {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(15.dp),
                color = Color(0xFF29254F)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        null,
                        tint = Color(0xFF8B80FF)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = 11.dp)
            ) {
                Text(
                    "Bill Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Get notified " + leadDays + " " +
                        if (leadDays == 1) {
                            "day before due date"
                        } else {
                            "days before due date"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(
                        horizontal = 11.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text(
                        leadDays.toString() + " " +
                            if (leadDays == 1) {
                                "day before"
                            } else {
                                "days before"
                            }
                    )
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(1, 2, 3, 5, 7).forEach { days ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    days.toString() + " " +
                                        if (days == 1) {
                                            "day before"
                                        } else {
                                            "days before"
                                        }
                                )
                            },
                            onClick = {
                                setDays(days)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsRemindersScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onPlanClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val revision by LedgerUpdateBus.state.collectAsState()
    val schedules = remember { BillSchedules(context) }

    var monthOffset by remember { mutableIntStateOf(0) }
    var bills by remember {
        mutableStateOf<List<ScheduledBill>>(emptyList())
    }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableIntStateOf(0) }
    var billEditor by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<SmartRule?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(BillSort.DUE_DATE) }
    var sortExpanded by remember { mutableStateOf(false) }
    var voiceAdd by remember { mutableStateOf(false) }

    LaunchedEffect(revision.revision, monthOffset, retryLoad) {
        loadError = null
        try {
            bills = withContext(Dispatchers.IO) {
                schedules.list(
                    billMonth(monthOffset).timeInMillis
                )
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackScreens", "Could not load bills", failure)
            loadError = "Couldn't load this page. Your saved data is unchanged."
        }
    }

    val sortedBills = remember(bills, sort) {
        when (sort) {
            BillSort.DUE_DATE ->
                bills.sortedBy { it.nextDueEpochMs ?: Long.MAX_VALUE }
            BillSort.AMOUNT ->
                bills.sortedByDescending { it.rule.plannedAmountMinor ?: 0L }
            BillSort.NAME ->
                bills.sortedBy { it.rule.displayLabel ?: it.rule.name }
        }
    }

    if (voiceAdd) {
        VoicePlanningAssistantSheet(
            kind = VoicePlanKind.BILL,
            onDismiss = {
                voiceAdd = false
            },
            onSaved = {
                retryLoad++
            }
        )
    }

    if (billEditor) {
        SavingGoalEditor(
            rule = editingBill,
            bill = true
        ) {
            billEditor = false
            editingBill = null
        }
    }

    if (showCalendar) {
        BillCalendarSheet(
            bills = bills,
            monthOffset = monthOffset,
            onDismiss = { showCalendar = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ConceptLoadError(loadError) { retryLoad++ } }
        item { BillsPageHeader(onBack) }
        item {
            BillsHero(
                bills = bills,
                monthOffset = monthOffset,
                onMonthSelected = { monthOffset = it }
            )
        }
        item {
            BillWeekStrip(
                bills = bills,
                monthOffset = monthOffset,
                onOpenCalendar = { showCalendar = true }
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    voiceAdd = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add bill by voice",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bills (" + bills.size + ")",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box {
                    TextButton(onClick = { sortExpanded = true }) {
                        Text(
                            "Sort by: " + sort.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        BillSort.entries.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(choice.label) },
                                onClick = {
                                    sort = choice
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (sortedBills.isEmpty()) {
            item {
                PesaCard {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "No bills yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Add a bill to start tracking due dates and reminders.",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                editingBill = null
                                billEditor = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Add bill")
                        }
                    }
                }
            }
        } else {
            items(sortedBills, key = { it.rule.id }) { bill ->
                BillReferenceRow(
                    bill = bill,
                    locale = locale,
                    onClick = {
                        editingBill = bill.rule
                        billEditor = true
                    }
                )
            }
        }

        if (bills.isNotEmpty()) {
            item {
                BillReminderCard(schedules)
            }
        }
    }
}
