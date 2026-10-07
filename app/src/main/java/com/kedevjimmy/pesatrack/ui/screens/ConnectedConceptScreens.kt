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
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.*
import com.kedevjimmy.pesatrack.ui.components.ConceptLoadError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import com.kedevjimmy.pesatrack.planning.*
import java.util.Date
import java.util.Locale
import kotlin.math.max

private fun conceptMoney(minor: Long): String {
    val hasCents =
        kotlin.math.abs(minor) % 100L != 0L
    val f =
        NumberFormat
            .getNumberInstance(Locale.US)
            .apply {
                minimumFractionDigits =
                    if (hasCents) 2 else 0
                maximumFractionDigits =
                    if (hasCents) 2 else 0
            }
    return (
        if (minor < 0) {
            "-KES "
        } else {
            "KES "
        }
        ) +
        f.format(
            kotlin.math.abs(minor) /
                100.0
        )
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
            spending.forEachIndexed { index, entry ->
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
                "Before refunds",
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

private data class ReportLoadedState(
    val current: ActivityLedgerSnapshot,
    val previous: ActivityLedgerSnapshot,
    val labels: Map<String, String>,
    val health: MoneyPictureHealth
)

private fun reportChangePercent(
    current: Long,
    previous: Long
): Int? {
    if (previous <= 0L) {
        return null
    }
    return (
        ((current - previous) * 100.0) /
            previous.toDouble()
        )
        .toInt()
}

@Composable
fun ReportsInsightsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onReviewClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var period by remember { mutableStateOf("Month") }
    var snapshot by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var previousSnapshot by remember { mutableStateOf<ActivityLedgerSnapshot?>(null) }
    var labels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var moneyHealth by remember {
        mutableStateOf<MoneyPictureHealth?>(null)
    }

    var loadError by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision.revision, period, retryLoad) {
        loadError = null
        try {
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
            val start =
                cal.timeInMillis
            val end =
                (cal.clone() as Calendar)
                    .apply {
                        when (period) {
                            "Week" ->
                                add(
                                    Calendar.DAY_OF_MONTH,
                                    7
                                )
                            "Year" ->
                                add(
                                    Calendar.YEAR,
                                    1
                                )
                            else ->
                                add(
                                    Calendar.MONTH,
                                    1
                                )
                        }
                    }
                    .timeInMillis
            val previousStart =
                (cal.clone() as Calendar)
                    .apply {
                        when (period) {
                            "Week" ->
                                add(
                                    Calendar.DAY_OF_MONTH,
                                    -7
                                )
                            "Year" ->
                                add(
                                    Calendar.YEAR,
                                    -1
                                )
                            else ->
                                add(
                                    Calendar.MONTH,
                                    -1
                                )
                        }
                    }
                    .timeInMillis
            val store =
                TransactionStore.get(context)
            ReportLoadedState(
                current =
                    store.activitySnapshotBetween(
                        start,
                        end,
                        now
                    ),
                previous =
                    store.activitySnapshotBetween(
                        previousStart,
                        start,
                        now
                    ),
                labels =
                    CategoryStore.get(context)
                        .all()
                        .associate {
                            it.id to it.label
                        },
                health =
                    MoneyPictureHealthEvaluator(
                        context
                    ).evaluate(now)
            )
        }
        snapshot = loaded.current
        previousSnapshot = loaded.previous
        labels = loaded.labels
        moneyHealth = loaded.health

        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackScreens", "Could not load screen", failure)
            loadError = "Couldn't load this page. Your saved data is unchanged."
        }
    }

    val data = snapshot
    val spending = remember(data, labels) {
        data?.trustedTransactions
            ?.filter { it.direction == MoneyDirection.EXPENSE.name && it.reviewState != "EXCLUDED" && it.reviewState != "NEEDS_REVIEW" }
            ?.groupBy { it.category }
            ?.map { (category, rows) -> (labels[category] ?: category.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) to rows.sumOf { it.amountMinor } }
            ?.sortedByDescending { it.second }
            .orEmpty()
    }
    val total = max(1L, spending.sumOf { it.second })
    val lowQualityMinor =
        spending
            .filter {
                it.first.equals(
                    "Other",
                    ignoreCase = true
                ) ||
                    it.first.equals(
                        "Uncategorized",
                        ignoreCase = true
                    )
            }
            .sumOf { it.second }
    val lowQualityShare =
        if (spending.isEmpty()) {
            0
        } else {
            ((lowQualityMinor * 100L) / total)
                .toInt()
        }

    val currentSpent =
        data?.summary?.spentMinor ?: 0L
    val previousSpent =
        previousSnapshot
            ?.summary
            ?.spentMinor ?: 0L
    val spendingChange =
        reportChangePercent(
            currentSpent,
            previousSpent
        )
    val topCategory =
        spending.firstOrNull()

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ConceptLoadError(loadError) { retryLoad++ } }
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
                subtitle =
                    if (
                        (data?.summary?.feesMinor ?: 0L) >
                        0L
                    ) {
                        conceptMoney(
                            data?.summary?.feesMinor
                                ?: 0L
                        ) +
                            " in fees tracked separately"
                    } else {
                        "Fees are tracked separately from spending"
                    },
                eyebrow = period
            )
        }
        item {
            PesaCard {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconBubble(
                            Icons.Rounded.Insights,
                            size = 42.dp,
                            background =
                                pesaToneContainer(
                                    "green"
                                ),
                            tint =
                                pesaToneContent(
                                    "green"
                                )
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(
                                    start = 11.dp
                                )
                        ) {
                            Text(
                                "What changed",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.Bold
                            )
                            Text(
                                when {
                                    spendingChange == null &&
                                        currentSpent == 0L ->
                                        "No spending recorded in this period."
                                    spendingChange == null ->
                                        "No comparable spending in the previous " +
                                            period.lowercase() +
                                            "."
                                    spendingChange > 0 ->
                                        "Spending is " +
                                            kotlin.math.abs(
                                                spendingChange
                                            ) +
                                            "% higher than the previous " +
                                            period.lowercase() +
                                            "."
                                    spendingChange < 0 ->
                                        "Spending is " +
                                            kotlin.math.abs(
                                                spendingChange
                                            ) +
                                            "% lower than the previous " +
                                            period.lowercase() +
                                            "."
                                    else ->
                                        "Spending is unchanged from the previous " +
                                            period.lowercase() +
                                            "."
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
                        }
                    }

                    topCategory?.let {
                            category ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Largest category",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                            Text(
                                category.first +
                                    " • " +
                                    conceptMoney(
                                        category.second
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }

                    moneyHealth?.let {
                            health ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Money picture confidence",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                            Text(
                                health.score
                                    .toString() +
                                    "% • " +
                                    health.label,
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        if (spending.isEmpty()) {
            item {
                PesaCard { Text("No trusted spending in this period yet.", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } else {
            if (lowQualityShare >= 70) {
                item {
                    PesaCard {
                        Column(
                            Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                IconBubble(
                                    Icons.Rounded.Info,
                                    size = 42.dp,
                                    background =
                                        pesaToneContainer(
                                            "amber"
                                        ),
                                    tint =
                                        pesaToneContent(
                                            "amber"
                                        )
                                )
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .padding(
                                            start = 11.dp
                                        )
                                ) {
                                    Text(
                                        "Your spending picture needs a little cleanup",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                    Text(
                                        lowQualityShare.toString() +
                                            "% is still Other or Uncategorized.",
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
                            }
                            Button(
                                onClick = onReviewClick,
                                modifier =
                                    Modifier.padding(
                                        top = 12.dp
                                    )
                            ) {
                                Text("Review spending")
                            }
                        }
                    }
                }
            }
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
                        data.trustedTransactions
                            .filter {
                                it.direction == MoneyDirection.EXPENSE.name &&
                                    it.reviewState != "EXCLUDED" &&
                                    it.reviewState != "NEEDS_REVIEW" &&
                                    Calendar.getInstance().apply {
                                        timeInMillis = it.occurredAtEpochMs
                                    }.get(Calendar.WEEK_OF_MONTH)
                                        .coerceIn(1, 4) == week
                            }
                            .sumOf { it.amountMinor }
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
                        Text(
                            "Money picture",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )
                        Text(
                            "Money in ${conceptMoney(data?.summary?.incomeMinor ?: 0L)} • Transfers ${conceptMoney(data?.summary?.transferMinor ?: 0L)}",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                        moneyHealth?.let {
                                health ->
                            Text(
                                if (
                                    health.issues.isEmpty()
                                ) {
                                    "Your current money picture has no open confidence issues."
                                } else {
                                    health.issues
                                        .first()
                                        .title +
                                        if (
                                            health.issues.size >
                                            1
                                        ) {
                                            " • +" +
                                                (
                                                    health.issues.size -
                                                        1
                                                    ) +
                                                " more"
                                        } else {
                                            ""
                                        }
                                },
                                modifier =
                                    Modifier.padding(
                                        top = 4.dp
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        item { ReportExportButton(data?.trustedTransactions.orEmpty()) }
    }
}

@Composable
fun BillsRemindersScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onPlanClick: () -> Unit
) {
    val context = LocalContext.current
    var billEditor by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<com.kedevjimmy.pesatrack.domain.model.SmartRule?>(null) }
    val revision by LedgerUpdateBus.state.collectAsState()
    var snapshot by remember { mutableStateOf<PayCycleSnapshot?>(null) }
    var bills by remember { mutableStateOf<List<ScheduledBill>>(emptyList()) }
    var reminder by remember { mutableStateOf<ScheduledBill?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision.revision, retryLoad) {
        loadError = null
        try {
        bills = withContext(Dispatchers.IO) { BillSchedules(context).list() }

        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackScreens", "Could not load screen", failure)
            loadError = "Couldn't load this page. Your saved data is unchanged."
        }
    }
    val commitments = bills.map { bill ->
        val configuredAmount =
            bill.rule.plannedAmountMinor
                ?: 0L
        val periodTarget =
            bill.expectedMinor
                .takeIf { it > 0L }
                ?: configuredAmount
        CommitmentProgress(
            bill.rule.id,
            bill.rule.displayLabel
                ?: bill.rule.name,
            bill.rule.category.name,
            periodTarget,
            (periodTarget -
                bill.remainingMinor)
                .coerceAtLeast(0L),
            bill.remainingMinor
        )
    }
    val comingUp = bills.filter { it.dueThisMonth }.sumOf { it.remainingMinor }

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ConceptLoadError(loadError) { retryLoad++ } }
        item { ConceptInnerHeader("Bills & reminders", onBack) }
        if (bills.isNotEmpty()) {
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
                        SimpleDateFormat("MMMM yyyy", androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
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
                                        SimpleDateFormat("EEE", androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
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
                subtitle = if (bills.isEmpty()) "No bills configured" else "${bills.count { it.dueThisMonth && it.remainingMinor > 0L }} bills awaiting payment this month",
                artwork = false,
                eyebrow = "This month"
            )
        }
        item { ConceptSectionTitle("Your bills") }
        }
        if (commitments.isEmpty()) {
            item {
                PesaCard {
                    Column(
                        Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            IconBubble(
                                Icons.Rounded.ReceiptLong,
                                size = 44.dp,
                                background =
                                    pesaToneContainer(
                                        "amber"
                                    ),
                                tint =
                                    pesaToneContent(
                                        "amber"
                                    )
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(
                                        start = 12.dp
                                    )
                            ) {
                                Text(
                                    "No bills yet",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                                Text(
                                    "Add only the recurring payments you want PesaTrack to track.",
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
                        }
                        Text(
                            "Matching trusted payments will update the bill automatically. PesaTrack never invents or pays a bill.",
                            modifier =
                                Modifier.padding(
                                    top = 12.dp
                                ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                editingBill = null
                                billEditor = true
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 14.dp
                                    )
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(7.dp)
                            )
                            Text("Add your first bill")
                        }
                    }
                }
            }
        } else {
            items(commitments, key = { it.ruleId }) { entry ->
                PesaCard {
                    Column {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(if (entry.remainingMinor == 0L) Icons.Rounded.CheckCircle else Icons.Rounded.ReceiptLong, size = 42.dp)
                        Column(Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(entry.name, style = MaterialTheme.typography.titleMedium)
                            val bill = bills.firstOrNull { it.rule.id == entry.ruleId }
                            Text(
                                when {
                                    bill == null -> "Bill schedule unavailable"
                                    !bill.dueThisMonth -> billManagementScheduleLabel(bill)
                                    entry.remainingMinor == 0L -> "Paid this month"
                                    else -> conceptMoney(entry.remainingMinor) + " remaining • " + billManagementScheduleLabel(bill)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BillAmountPill(
                            if (entry.remainingMinor > 0L) {
                                conceptMoney(entry.remainingMinor)
                            } else {
                                conceptMoney(entry.plannedMinor)
                            }
                        )
                    }
                    bills.firstOrNull { it.rule.id == entry.ruleId }?.let { bill ->
                        TextButton(onClick = { editingBill = bill.rule; billEditor = true }) { Text("Edit bill") }
                        TextButton(onClick = { reminder = bill }, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(if (bill.reminders) "Reminder on" else "Reminder settings")
                        }
                    }
                    }
                }
            }
        }
        if (bills.isNotEmpty()) {
            item {
                PesaCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                        Column(
                            Modifier.padding(
                                start = 12.dp
                            )
                        ) {
                            Text(
                                "Message-aware reminders",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleSmall
                            )
                            Text(
                                "PesaTrack checks real payment activity before treating a bill as paid.",
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
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        editingBill = null
                        billEditor = true
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            18.dp
                        )
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null
                    )
                    Spacer(
                        Modifier.width(7.dp)
                    )
                    Text("Add another bill")
                }
            }
        }
    }
    reminder?.let { BillReminderSettings(it) { reminder = null } }
    if (billEditor) SavingGoalEditor(editingBill, bill = true) { billEditor = false }
}

private fun billManagementScheduleLabel(
    bill: ScheduledBill
): String =
    when (bill.cadence) {
        BillCadence.MONTHLY ->
            bill.dueDay
                ?.let {
                    "Due day " + it
                }
                ?: "Choose a monthly due day"
        BillCadence.WEEKLY -> {
            val names =
                java.text.DateFormatSymbols(
                    Locale.getDefault()
                ).weekdays
            bill.dueDayOfWeek
                ?.let {
                    "Every " + names[it]
                }
                ?: "Choose a weekly due day"
        }
    }

private data class GoalLoadedState(
    val rules:
        List<com.kedevjimmy.pesatrack.domain.model.SmartRule>,
    val monthlySavings:
        Map<String?, Long>,
    val totalSavings:
        Map<String?, Long>,
    val metadata:
        Map<String, SavingGoalMetadata>
)

@Composable
fun SavingGoalsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onPlanClick: () -> Unit
) {
    val context = LocalContext.current
    var goalEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<com.kedevjimmy.pesatrack.domain.model.SmartRule?>(null) }
    val revision by LedgerUpdateBus.state.collectAsState()
    var rules by remember { mutableStateOf(emptyList<com.kedevjimmy.pesatrack.domain.model.SmartRule>()) }
    var monthlySavingsByRule by remember { mutableStateOf<Map<String?, Long>>(emptyMap()) }
    var totalSavingsByRule by remember { mutableStateOf<Map<String?, Long>>(emptyMap()) }
    var goalMetadataByRule by remember {
        mutableStateOf<Map<String, SavingGoalMetadata>>(emptyMap())
    }

    var loadError by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision.revision, retryLoad) {
        loadError = null
        try {
        val loaded = withContext(Dispatchers.IO) {
            val savedRules =
                RuleStore.get(context)
                    .listRules(true)
                    .filter {
                        it.category ==
                            TransactionCategory.SAVINGS &&
                            (it.plannedAmountMinor ?: 0L) >
                            0L
                    }
            val transactionStore =
                TransactionStore.get(context)
            val monthlyActivity =
                transactionStore.activitySnapshot()
            val now =
                System.currentTimeMillis()
            val allActivity =
                transactionStore.activitySnapshotBetween(
                    startEpochMs = 0L,
                    endEpochMsExclusive =
                        now + 1L,
                    nowEpochMs = now
                )
            val types =
                AccountStore.get(context)
                    .listAccounts(false)
                    .associate {
                        it.id to it.type
                    }

            fun savingsByRule(
                rows: List<StoredTransaction>
            ): Map<String?, Long> =
                rows
                    .filter {
                        it.category ==
                            TransactionCategory.SAVINGS.name &&
                            it.reviewState !=
                            ReviewState.EXCLUDED.name &&
                            it.reviewState !=
                            ReviewState.NEEDS_REVIEW.name
                    }
                    .filter {
                        it.direction ==
                            MoneyDirection.TRANSFER.name
                    }
                    .groupBy { it.ruleId }
                    .mapValues {
                            (_, groupedRows) ->
                        groupedRows
                            .sumOf {
                                ConceptLedgerPolicy
                                    .savingsDelta(
                                        it,
                                        types
                                    )
                            }
                            .coerceAtLeast(0)
                    }

            GoalLoadedState(
                rules = savedRules,
                monthlySavings =
                    savingsByRule(
                        monthlyActivity
                            .trustedTransactions
                    ),
                totalSavings =
                    savingsByRule(
                        allActivity
                            .trustedTransactions
                    ),
                metadata =
                    SavingGoalMetadataStore
                        .get(context)
                        .listForRules(
                            savedRules.map {
                                it.id
                            }
                        )
            )
        }
        rules = loaded.rules
        monthlySavingsByRule =
            loaded.monthlySavings
        totalSavingsByRule =
            loaded.totalSavings
        goalMetadataByRule =
            loaded.metadata

        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackScreens", "Could not load screen", failure)
            loadError = "Couldn't load this page. Your saved data is unchanged."
        }
    }

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ConceptLoadError(loadError) { retryLoad++ } }
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
                        onClick = { editingGoal = null; goalEditor = true },
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
                Text(
                    "Goals you create will appear here. PesaTrack only tracks goals you explicitly set.",
                    modifier =
                        Modifier.padding(
                            horizontal = 6.dp,
                            vertical = 2.dp
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
            }
        } else {
            items(
                rules,
                key = { it.id }
            ) { rule ->
                val monthlyTarget =
                    rule.plannedAmountMinor
                        ?: 0L
                val metadata =
                    goalMetadataByRule[
                        rule.id
                    ]
                val overallTarget =
                    metadata
                        ?.totalTargetMinor
                val monthlySaved =
                    monthlySavingsByRule[
                        rule.id
                    ] ?: 0L
                val totalSaved =
                    totalSavingsByRule[
                        rule.id
                    ] ?: 0L
                val usesOverallTarget =
                    overallTarget != null &&
                        overallTarget > 0L
                val progressTarget =
                    if (usesOverallTarget) {
                        overallTarget!!
                    } else {
                        monthlyTarget
                    }
                val progressValue =
                    if (usesOverallTarget) {
                        minOf(
                            totalSaved,
                            progressTarget
                        )
                    } else {
                        minOf(
                            monthlySaved,
                            progressTarget
                        )
                    }
                val dateLabel =
                    metadata
                        ?.targetDateEpochMs
                        ?.let {
                            SimpleDateFormat(
                                "d MMM yyyy",
                                Locale.getDefault()
                            ).format(
                                Date(it)
                            )
                        }

                ConceptProgressCard(
                    title =
                        rule.displayLabel
                            ?: rule.name,
                    amount =
                        conceptMoney(
                            progressValue
                        ),
                    helper =
                        buildString {
                            append(
                                conceptMoney(
                                    progressValue
                                )
                            )
                            append(" of ")
                            append(
                                conceptMoney(
                                    progressTarget
                                )
                            )
                            append(
                                if (
                                    usesOverallTarget
                                ) {
                                    " overall"
                                } else {
                                    " this month"
                                }
                            )
                            if (
                                dateLabel != null
                            ) {
                                append(" • by ")
                                append(dateLabel)
                            }
                        },
                    progress =
                        if (
                            progressTarget <= 0L
                        ) {
                            0f
                        } else {
                            (
                                progressValue
                                    .toFloat() /
                                    progressTarget
                                        .toFloat()
                                )
                                .coerceIn(
                                    0f,
                                    1f
                                )
                        },
                    icon =
                        Icons.Rounded.Savings
                )

                if (
                    usesOverallTarget &&
                    monthlyTarget > 0L
                ) {
                    Text(
                        "Monthly contribution target: " +
                            conceptMoney(
                                monthlyTarget
                            ),
                        modifier =
                            Modifier.padding(
                                horizontal = 6.dp
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

                TextButton(
                    onClick = {
                        editingGoal = rule
                        goalEditor = true
                    }
                ) {
                    Text("Edit goal")
                }
            }
        }
    }
    if (goalEditor) SavingGoalEditor(editingGoal) { goalEditor = false }
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

    var loadError by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision.revision, accountId, retryLoad) {
        loadError = null
        try {
        val loaded = withContext(Dispatchers.IO) {
            val a = AccountStore.get(context).listAccounts(false).firstOrNull { it.id == accountId }
            val b = a?.let { PaydaySnapshotStore.get(context).currentAccountSnapshot(it) }
            val t = TransactionStore.get(context).activitySnapshot().trustedTransactions.filter { it.accountId == accountId || it.linkedAccountId == accountId }
            Triple(a, b, t)
        }
        account = loaded.first
        balance = loaded.second
        rows = loaded.third

        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackScreens", "Could not load screen", failure)
            loadError = "Couldn't load this page. Your saved data is unchanged."
        }
    }

    val moneyIn = rows.filter { it.direction == MoneyDirection.INCOME.name }.sumOf { it.amountMinor }
    val moneyOut = rows.filter { it.direction == MoneyDirection.EXPENSE.name }.sumOf { it.amountMinor }

    LazyColumn(
        modifier = Modifier.padding(scaffoldPadding).background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ConceptLoadError(loadError) { retryLoad++ } }
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
        item { ConceptSectionTitle("This month’s activity") }
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
