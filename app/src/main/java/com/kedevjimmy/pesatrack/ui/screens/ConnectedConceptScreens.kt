package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

private fun conceptMoney(minor: Long): String {
    val f = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    return (if (minor < 0) "-KES " else "KES ") + f.format(kotlin.math.abs(minor) / 100.0)
}

private fun conceptDate(epoch: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epoch))


@Composable
private fun SpendingDonut(
    spending: List<Pair<String, Long>>,
    total: Long,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        Color(0xFF087A58),
        Color(0xFF2F9E73),
        Color(0xFF67B993),
        Color(0xFFA3D9BE),
        Color(0xFFCCECDD),
        Color(0xFF4F8D70)
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            var start = -90f
            spending.take(6).forEachIndexed { index, entry ->
                val sweep =
                    if (total <= 0L) 0f
                    else 360f * entry.second.toFloat() / total.toFloat()
                drawArc(
                    color = palette[index % palette.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = size.minDimension * .16f)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                conceptMoney(total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklySpendingBars(
    weeks: List<Long>,
    modifier: Modifier = Modifier
) {
    val maxValue = (weeks.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        weeks.forEachIndexed { index, amount ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    conceptMoney(amount).removePrefix("KES "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            (28 + 68 * amount.toFloat() / maxValue.toFloat()).dp
                        ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {}
                Text(
                    "Week ${index + 1}",
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConceptInnerHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReportsInsightsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var period by remember { mutableStateOf("Month") }
    var snapshot by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var labels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(revision.revision, period) {
        val loaded = withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            when (period) {
                "Week" -> {
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                }
                "Year" -> {
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                }
                else -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                }
            }
            val end = (cal.clone() as Calendar).apply {
                when (period) {
                    "Week" -> add(Calendar.DAY_OF_MONTH, 7)
                    "Year" -> add(Calendar.YEAR, 1)
                    else -> add(Calendar.MONTH, 1)
                }
            }.timeInMillis
            TransactionStore.get(context).activitySnapshotBetween(cal.timeInMillis, end) to
                CategoryStore.get(context).all().associate { it.id to it.label }
        }
        snapshot = loaded.first
        labels = loaded.second
    }

    val data = snapshot
    val spending = remember(data, labels) {
        data?.transactions
            ?.filter { it.direction == MoneyDirection.EXPENSE.name && it.reviewState != "EXCLUDED" && it.reviewState != "NEEDS_REVIEW" }
            ?.groupBy { it.category }
            ?.map { (category, rows) -> (labels[category] ?: category.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) to rows.sumOf { it.amountMinor + it.feeMinor } }
            ?.sortedByDescending { it.second }
            .orEmpty()
    }
    val total = max(1L, spending.sumOf { it.second })

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ConceptInnerHeader("Reports & insights", onBack) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Week", "Month", "Year").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = period == label,
                        onClick = { period = label },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                        label = { Text(label) }
                    )
                }
            }
        }
        item {
            ConceptEmeraldHero(
                title = "Money spent",
                amount = conceptMoney(data?.summary?.spentMinor ?: 0L),
                subtitle = "Fees are tracked separately from spending",
                eyebrow = period
            )
        }
        if (spending.isEmpty()) {
            item {
                PesaCard { Text("No trusted spending in this period yet.", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } else {
            item {
                PesaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpendingDonut(
                            spending = spending,
                            total = spending.sumOf { it.second },
                            modifier = Modifier.size(150.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            spending.take(4).forEachIndexed { index, entry ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(9.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = listOf(
                                            Color(0xFF087A58),
                                            Color(0xFF2F9E73),
                                            Color(0xFF67B993),
                                            Color(0xFFA3D9BE)
                                        )[index]
                                    ) {}
                                    Text(
                                        entry.first,
                                        modifier = Modifier.weight(1f).padding(start = 7.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        ((entry.second * 100) / total).toString() + "%",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { ConceptSectionTitle("Where your money went") }
            items(spending.take(6), key = { it.first }) { (label, amount) ->
                val progress = amount.toFloat() / total.toFloat()
                PesaCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Text(conceptMoney(amount), fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text("${(progress * 100).toInt()}% of trusted spending", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (period == "Month" && data != null) {
                item {
                    val weeks = (1..4).map { week ->
                        data.transactions
                            .filter {
                                it.direction == MoneyDirection.EXPENSE.name &&
                                    it.reviewState != "EXCLUDED" &&
                                    it.reviewState != "NEEDS_REVIEW" &&
                                    Calendar.getInstance().apply {
                                        timeInMillis = it.occurredAtEpochMs
                                    }.get(Calendar.WEEK_OF_MONTH)
                                        .coerceIn(1, 4) == week
                            }
                            .sumOf { it.amountMinor + it.feeMinor }
                    }
                    PesaCard {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Weekly spending",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            WeeklySpendingBars(
                                weeks = weeks,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Insights, size = 42.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Money picture", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Money in ${conceptMoney(data?.summary?.incomeMinor ?: 0L)} • Transfers ${conceptMoney(data?.summary?.transferMinor ?: 0L)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BillsRemindersScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onPlanClick: () -> Unit
) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var snapshot by remember { mutableStateOf<PayCycleSnapshot?>(null) }
    LaunchedEffect(revision.revision) {
        snapshot = withContext(Dispatchers.IO) { PayCycleStore.get(context).snapshot() }
    }
    val commitments = snapshot?.commitments.orEmpty()
    val comingUp = commitments.sumOf { it.remainingMinor }

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ConceptInnerHeader("Bills & reminders", onBack) }
        item {
            val today = Calendar.getInstance()
            val days = (0 until 7).map { offset ->
                (today.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, offset)
                }
            }
            PesaCard {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            .format(today.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        days.forEachIndexed { index, day ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color =
                                    if (index == 0)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        SimpleDateFormat("EEE", Locale.getDefault())
                                            .format(day.time)
                                            .take(1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        day.get(Calendar.DAY_OF_MONTH).toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight =
                                            if (index == 0) FontWeight.Bold
                                            else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            ConceptEmeraldHero(
                title = "Coming up",
                amount = conceptMoney(comingUp),
                subtitle = if (commitments.isEmpty()) "No protected commitments in this cycle" else "${commitments.count { it.remainingMinor > 0L }} commitments still reserved",
                artwork = false,
                eyebrow = snapshot?.cycle?.let { "This pay cycle" } ?: "Planning"
            )
        }
        item { ConceptSectionTitle("Upcoming bills") }
        if (commitments.isEmpty()) {
            item {
                PesaCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("No bills protected yet", style = MaterialTheme.typography.titleMedium)
                        Text("Add recurring commitments in Plan. Matching payment messages can update them automatically.", Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(commitments, key = { it.name }) { entry ->
                PesaCard {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(if (entry.remainingMinor == 0L) Icons.Rounded.CheckCircle else Icons.Rounded.ReceiptLong, size = 42.dp)
                        Column(Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(entry.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (entry.remainingMinor == 0L) "Paid in this cycle" else "${conceptMoney(entry.remainingMinor)} still reserved",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(conceptMoney(entry.plannedMinor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            PesaCard {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Message-aware reminders", style = MaterialTheme.typography.titleSmall)
                        Text("PesaTrack keeps bill status tied to real payment activity instead of marking bills paid by a calendar alone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Button(
                onClick = onPlanClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Add or manage a bill")
            }
        }
    }
}

@Composable
fun SavingGoalsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onPlanClick: () -> Unit
) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var rules by remember { mutableStateOf(emptyList<com.kedevjimmy.pesatrack.domain.model.SmartRule>()) }
    var savedThisMonth by remember { mutableLongStateOf(0L) }

    LaunchedEffect(revision.revision) {
        val loaded = withContext(Dispatchers.IO) {
            val savedRules = RuleStore.get(context).listRules(true).filter {
                it.category == TransactionCategory.SAVINGS && (it.plannedAmountMinor ?: 0L) > 0L
            }
            val activity = TransactionStore.get(context).activitySnapshot()
            val saved = activity.transactions
                .filter { it.category == TransactionCategory.SAVINGS.name && it.reviewState != "EXCLUDED" && it.reviewState != "NEEDS_REVIEW" }
                .sumOf { it.amountMinor }
            savedRules to saved
        }
        rules = loaded.first
        savedThisMonth = loaded.second
    }

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ConceptInnerHeader("Your goals", onBack) }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Build your better days",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Set goals, stay consistent and make progress.",
                                Modifier.padding(top = 5.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box(
                            modifier = Modifier.size(92.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(74.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)
                            ) {}
                            Icon(
                                Icons.Rounded.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(50.dp)
                            )
                            Icon(
                                Icons.Rounded.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(36.dp)
                            )
                        }
                    }
                    Button(
                        onClick = onPlanClick,
                        modifier = Modifier.padding(top = 14.dp)
                    ) {
                        Text("Create a goal")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Add, contentDescription = null)
                    }
                }
            }
        }
        if (rules.isEmpty()) {
            item {
                PesaCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("No savings goals yet", style = MaterialTheme.typography.titleMedium)
                        Text("Planned savings rules become goals here. Nothing is invented when you have not set a target.", Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                val target = rule.plannedAmountMinor ?: 0L
                val progressMinor = minOf(savedThisMonth, target)
                ConceptProgressCard(
                    title = rule.displayLabel ?: rule.name,
                    amount = conceptMoney(progressMinor),
                    helper = "${conceptMoney(progressMinor)} of ${conceptMoney(target)} this month",
                    progress = if (target <= 0L) 0f else progressMinor.toFloat() / target.toFloat(),
                    icon = Icons.Rounded.Savings
                )
            }
        }
    }
}

@Composable
fun AccountDetailsScreen(
    scaffoldPadding: PaddingValues,
    accountId: String,
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var account by remember { mutableStateOf<com.kedevjimmy.pesatrack.domain.model.MoneyAccount?>(null) }
    var balance by remember { mutableStateOf<PaydayAccountSnapshot?>(null) }
    var rows by remember { mutableStateOf(emptyList<StoredTransaction>()) }
    var detailTab by remember { mutableStateOf("Transactions") }

    LaunchedEffect(revision.revision, accountId) {
        val loaded = withContext(Dispatchers.IO) {
            val a = AccountStore.get(context).listAccounts(false).firstOrNull { it.id == accountId }
            val b = a?.let { PaydaySnapshotStore.get(context).currentAccountSnapshot(it) }
            val t = TransactionStore.get(context).recent(250).filter { it.accountId == accountId || it.linkedAccountId == accountId }
            Triple(a, b, t)
        }
        account = loaded.first
        balance = loaded.second
        rows = loaded.third
    }

    val moneyIn = rows.filter { it.direction == MoneyDirection.INCOME.name }.sumOf { it.amountMinor }
    val moneyOut = rows.filter { it.direction == MoneyDirection.EXPENSE.name }.sumOf { it.amountMinor + it.feeMinor }

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ConceptInnerHeader(account?.name ?: "Account", onBack) }
        item {
            ConceptEmeraldHero(
                title = "Balance from latest evidence",
                amount = balance?.balanceMinor?.let(::conceptMoney) ?: "Waiting for balance",
                subtitle = balance?.referenceEpochMs?.let { "Observed ${conceptDate(it)}" } ?: "PesaTrack does not guess missing balances",
                artwork = accountId == AccountStore.DEFAULT_MPESA_ID
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ConceptMetricTile(Icons.Rounded.SouthWest, "Money in", conceptMoney(moneyIn), Modifier.weight(1f), true)
                ConceptMetricTile(Icons.Rounded.NorthEast, "Money out", conceptMoney(moneyOut), Modifier.weight(1f), false)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Transactions", "Transfers").forEach { label ->
                    FilterChip(
                        selected = detailTab == label,
                        onClick = { detailTab = label },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item { ConceptSectionTitle("Recent activity") }
        val visibleRows =
            rows.filter {
                if (detailTab == "Transfers") {
                    it.direction == MoneyDirection.TRANSFER.name
                } else {
                    it.direction != MoneyDirection.TRANSFER.name
                }
            }
        if (visibleRows.isEmpty()) {
            item {
                PesaCard {
                    Text(
                        if (detailTab == "Transfers")
                            "No transfers linked to this account yet."
                        else
                            "No transactions linked to this account yet.",
                        Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(visibleRows.take(20), key = { it.id }) { transaction ->
                PesaCard(modifier = Modifier.fillMaxWidth().clickable { onTransactionClick(transaction.id) }) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(
                            when (transaction.direction) {
                                MoneyDirection.INCOME.name -> Icons.Rounded.SouthWest
                                MoneyDirection.TRANSFER.name -> Icons.Rounded.SwapHoriz
                                else -> Icons.Rounded.NorthEast
                            },
                            size = 40.dp
                        )
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(transaction.counterparty?.takeIf { it.isNotBlank() } ?: transaction.category.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
                            Text(conceptDate(transaction.occurredAtEpochMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (if (transaction.direction == MoneyDirection.INCOME.name) "+" else if (transaction.direction == MoneyDirection.EXPENSE.name) "−" else "") + conceptMoney(transaction.amountMinor),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (transaction.direction == MoneyDirection.INCOME.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
