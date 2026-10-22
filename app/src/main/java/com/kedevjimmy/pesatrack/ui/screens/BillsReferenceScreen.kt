package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer

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

@Composable
private fun billVisual(bill: ScheduledBill): BillVisual {
    val title = (bill.rule.displayLabel ?: bill.rule.name)
        .lowercase(Locale.ROOT)
    return when {
        "rent" in title ->
            BillVisual(Icons.Rounded.Home, com.kedevjimmy.pesatrack.ui.components.pesaToneContent("red"))
        "wifi" in title || "wi-fi" in title || "internet" in title ->
            BillVisual(Icons.Rounded.Wifi, com.kedevjimmy.pesatrack.ui.components.pesaToneContent("blue"))
        "electric" in title || "power" in title || "kplc" in title ->
            BillVisual(Icons.Rounded.Bolt, com.kedevjimmy.pesatrack.ui.components.pesaToneContent("amber"))
        "water" in title ->
            BillVisual(Icons.Rounded.WaterDrop, com.kedevjimmy.pesatrack.ui.components.pesaToneContent("blue"))
        "subscription" in title || "netflix" in title || "youtube" in title ->
            BillVisual(Icons.Rounded.PlayCircle, com.kedevjimmy.pesatrack.ui.components.pesaToneContent("violet"))
        "loan" in title || "debt" in title || "repayment" in title ->
            BillVisual(Icons.Rounded.Savings, com.kedevjimmy.pesatrack.ui.components.pesaToneContent("green"))
        else ->
            BillVisual(Icons.Rounded.ReceiptLong, com.kedevjimmy.pesatrack.ui.components.pesaToneContent("green"))
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        VibrantPlanPageTitle("Bills", "Keep track of your upcoming bills.",
            Modifier.weight(1f))
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
            color = Color(0xFFF4F0FF),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFFE5DFF9)
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
                    color = PlanVibrant.ink
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 3.dp).size(17.dp),
                    tint = PlanVibrant.violet
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
    bills: List<ScheduledBill>, monthOffset: Int, onMonthSelected: (Int) -> Unit
) {
    val due = bills.filter { it.remainingMinor > 0L }
    val total = due.sumOf { it.remainingMinor }
    val earliest = due.mapNotNull { it.nextDueEpochMs }.minOrNull()
    val days = earliest?.let { java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
        it - System.currentTimeMillis()) }
    VibrantPlanSurface {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            VibrantPlanIcon(Icons.Rounded.EventNote, "orange", size = 55)
            Column(Modifier.weight(1f)) {
                Text("Upcoming commitments", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(billMoney(total), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold)
                Text(if(due.isEmpty()) "No outstanding bills"
                    else if (days != null) {
                        if (days < 0) "Overdue commitments need attention"
                        else "Next due in " + days + if(days == 1L) " day" else " days"
                    } else "${due.size} commitments pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BillMonthSelector(monthOffset, onMonthSelected)
        }
    }
}

@Composable
private fun billDateColor(bill: ScheduledBill): Color =
    when (billStatus(bill).second) {
        "red" -> pesaToneContent("red")
        "green" -> pesaToneContent("green")
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
                        color = if (selected) PlanVibrant.violet
                            else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (selected) {
                            androidx.compose.foundation.BorderStroke(
                                1.dp, PlanVibrant.violet
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
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                day.get(Calendar.DAY_OF_MONTH).toString(),
                                modifier = Modifier.padding(top = 3.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
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
private fun BillReferenceRow(bill: ScheduledBill, locale: Locale, onClick: () -> Unit) {
    val visual = billVisual(bill)
    val status = billStatus(bill)
    val planned = bill.rule.plannedAmountMinor ?: bill.expectedMinor
    val tone = if (status.second == "red") "pink" else if (status.second == "green") "mint" else "blue"
    VibrantPlanSurface(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VibrantPlanIcon(visual.icon, tone, size = 44)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(bill.rule.displayLabel ?: bill.rule.name,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(billDueLabel(bill, locale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall)
                Text(status.first, style = MaterialTheme.typography.labelSmall,
                    color = if(status.second == "red") PlanVibrant.pink
                    else if(status.second == "green") PlanVibrant.mint else PlanVibrant.orange)
            }
            Text(billMoney(planned), style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Edit bill", Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
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
    var incompleteBills by remember { mutableStateOf<List<SmartRule>>(emptyList()) }
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
            val result = withContext(Dispatchers.IO) {
                schedules.list(billMonth(monthOffset).timeInMillis) to
                    schedules.unconfiguredRecurringRules()
            }
            bills = result.first
            incompleteBills = result.second
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackScreens", "Could not load bills")
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
        if (incompleteBills.isNotEmpty()) item {
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Finish setting up a bill", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A recurring payment was saved before its due date was confirmed. Choose the schedule so it appears on your calendar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    incompleteBills.forEach { rule ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(rule.displayLabel ?: rule.name, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                editingBill = rule
                                billEditor = true
                            }) { Text("Set due date") }
                        }
                    }
                }
            }
        }

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
        item { SuggestedBillsPanel() }
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
                    "Upcoming bills (" + bills.size + ")",
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

        item {
            VibrantActionButton("+ Add bill", onClick = {
                editingBill = null
                billEditor = true
            })
        }
        if (bills.isNotEmpty()) {
            item {
                BillReminderCard(schedules)
            }
        }
    }
}
