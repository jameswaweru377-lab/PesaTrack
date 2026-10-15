package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.data.local.EverydayMoneyStore
import com.kedevjimmy.pesatrack.data.local.StatementActivityStore
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityLine
import com.kedevjimmy.pesatrack.domain.everyday.StatementHistoryReport
import com.kedevjimmy.pesatrack.domain.everyday.StatementMonthlyUsage
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class StatementTab { MONTHLY, YEARLY, BROWSE }

private fun reportMoney(minor: Long): String =
    "KSh " + NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(minor, 2))

private fun reportDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
    }.format(Date(millis))

/** Independent, read-only local statement reporting. Never writes to SMS Activity. */
@Composable
internal fun StatementActivityDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { StatementActivityStore(context) }
    var sources by remember { mutableStateOf<List<StatementHistoryReport.SourceMonth>>(emptyList()) }
    var labels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var accountChoice by remember { mutableStateOf<String?>(null) }
    var monthChoice by remember { mutableStateOf<String?>(null) }
    var yearChoice by remember { mutableStateOf<Int?>(null) }
    var accountMenu by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(StatementTab.MONTHLY) }
    var browseQuery by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }

    var detail by remember { mutableStateOf<StatementHistoryReport.SelectedMonth?>(null) }
    var fullSource by remember { mutableStateOf(false) }
    var detailQuery by remember { mutableStateOf("") }
    var detailRows by remember { mutableStateOf<List<StatementActivityLine>>(emptyList()) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf(false) }

    LaunchedEffect(refresh) {
        loading = true
        error = false
        try {
            val result = withContext(Dispatchers.IO) {
                store.sourceMonths() to EverydayMoneyStore(context).ownedAccounts()
                    .associate { it.id to it.name }
            }
            sources = result.first
            labels = result.second
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { error = true }
        finally { loading = false }
    }

    val ids = remember(sources) { sources.map { it.accountId }.distinct() }
    val accountId = accountChoice?.takeIf { it in ids } ?: ids.firstOrNull()
    val months = remember(sources, accountId) {
        StatementHistoryReport.selectForAccount(sources, accountId.orEmpty())
    }
    val years = remember(months) { StatementHistoryReport.years(months) }
    val year = yearChoice?.takeIf { it in years } ?: years.firstOrNull()
    val selected = months.firstOrNull { it.key == monthChoice } ?: months.firstOrNull()
    val annual = year?.let { StatementHistoryReport.year(months, it) }

    LaunchedEffect(detail?.source?.batchId) {
        val batchId = detail?.source?.batchId
        detailRows = emptyList()
        detailError = false
        if (batchId != null) {
            detailLoading = true
            try {
                detailRows = withContext(Dispatchers.IO) { store.lines(batchId) }
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { detailError = true }
            finally { detailLoading = false }
        }
    }

    fun back() {
        when {
            importing -> importing = false
            detail != null -> { detail = null; fullSource = false; detailQuery = "" }
            else -> onDismiss()
        }
    }
    BackHandler { back() }

    Dialog(
        onDismissRequest = { back() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { back() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (detail == null) "Statements" else "Statement transactions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Saved PDFs · separate from SMS Activity",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (detail == null) IconButton(onClick = { importing = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Import another statement",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }

                when {
                    loading -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Reading saved statement months…", Modifier.padding(16.dp))
                    }
                    error -> Column(Modifier.padding(20.dp)) {
                        Text("Could not open reports. Your saved records are unchanged.",
                            color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { refresh++ }) { Text("Retry") }
                    }
                    detail != null -> {
                        val picked = requireNotNull(detail)
                        val rows = remember(detailRows, picked.key, fullSource, detailQuery) {
                            detailRows.filter { line ->
                                (fullSource || StatementMonthlyUsage.monthKey(line.occurredAt) == picked.key) &&
                                    (detailQuery.isBlank() ||
                                        line.description.contains(detailQuery, ignoreCase = true) ||
                                        line.reference.contains(detailQuery, ignoreCase = true))
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (fullSource) "Full saved PDF" else picked.usage.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                Text(rows.size.toString() + " original source rows",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { fullSource = !fullSource }) {
                                Text(if (fullSource) "This month" else "Full PDF")
                            }
                        }
                        OutlinedTextField(value = detailQuery, onValueChange = { detailQuery = it },
                            label = { Text("Find description or receipt") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                        if (detailLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        else if (detailError) Text("Could not read saved source rows.",
                            Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                        else LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            if (rows.isEmpty()) item { Text("No matching statement transactions.") }
                            items(rows, key = { it.page.toString() + ":" + it.row + ":" + it.reference }) { line ->
                                PesaCard {
                                    Column(Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(line.description, style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold)
                                        Text(reportDate(line.occurredAt) + " · " + line.reference +
                                            " · Page " + line.page + ", row " + line.row,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(when {
                                            line.informational -> "Informational · no wallet movement"
                                            line.movementMinor > 0 -> "Money In  " + reportMoney(line.paidInMinor)
                                            else -> "Money Out  " + reportMoney(line.withdrawnMinor)
                                        }, color = if (line.movementMinor > 0)
                                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                        Text("Reported balance " + reportMoney(line.balanceMinor),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatementTab.entries.forEach { choice ->
                                FilterChip(selected = tab == choice, onClick = { tab = choice },
                                    label = { Text(choice.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.weight(1f))
                            }
                        }
                        if (months.isNotEmpty()) Box(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            OutlinedButton(onClick = { accountMenu = true },
                                modifier = Modifier.fillMaxWidth()) {
                                Text(labels[accountId] ?: "M-Pesa wallet",
                                    Modifier.weight(1f), maxLines = 1)
                                Icon(Icons.Rounded.ExpandMore, contentDescription = "Choose statement account")
                            }
                            DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                                ids.forEach { id ->
                                    DropdownMenuItem(
                                        text = { Text(labels[id] ?: "M-Pesa wallet") },
                                        onClick = {
                                            accountChoice = id
                                            monthChoice = null
                                            yearChoice = null
                                            accountMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        if (months.isEmpty()) Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.ReceiptLong, contentDescription = null,
                                modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("No saved statement months", style = MaterialTheme.typography.titleLarge)
                            Text("Save an M-Pesa PDF statement to view monthly and yearly usage.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { importing = true }) { Text("Import statement") }
                        } else when (tab) {
                            StatementTab.MONTHLY -> {
                                val picked = requireNotNull(selected)
                                val index = months.indexOf(picked)
                                val usage = picked.usage
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    item {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween) {
                                            IconButton(enabled = index < months.lastIndex,
                                                onClick = { monthChoice = months[index + 1].key }) {
                                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous imported month")
                                            }
                                            Text(usage.label, style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold)
                                            IconButton(enabled = index > 0,
                                                onClick = { monthChoice = months[index - 1].key }) {
                                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next imported month")
                                            }
                                        }
                                    }
                                    item {
                                        Text("Source dates " + reportDate(picked.source.firstTransactionAt) +
                                            " – " + reportDate(picked.source.lastTransactionAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    item {
                                        StatementHero("MONEY OUT · NOT NECESSARILY SPENDING",
                                            reportMoney(usage.paidOutMinor),
                                            "Includes transfers, savings, fees and loan movements.")
                                    }
                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            StatementStat("Money in", reportMoney(usage.paidInMinor),
                                                Modifier.weight(1f))
                                            StatementStat("Net movement", reportMoney(usage.netMinor),
                                                Modifier.weight(1f))
                                        }
                                    }
                                    item {
                                        PesaCard {
                                            Column(Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text("Money movement", style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold)
                                                val max = maxOf(1L, usage.paidInMinor, usage.paidOutMinor)
                                                StatementBar("Money in", usage.paidInMinor, max,
                                                    MaterialTheme.colorScheme.primary)
                                                StatementBar("Money out", usage.paidOutMinor, max,
                                                    MaterialTheme.colorScheme.tertiary)
                                                Text(usage.financialMovements.toString() + " movements · " +
                                                    usage.informationalRows + " informational · " +
                                                    usage.sourceRows + " rows",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    if (picked.sourceCount > 1) item {
                                        Text(picked.sourceCount.toString() + " PDFs contain this month. " +
                                            "Only the highest-row-coverage source is used, not their sum. " +
                                            "Other source periods are excluded to prevent double counting.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    item {
                                        Text("Months may be incomplete. These are original wallet movements, " +
                                            "not verified expenses or a confirmed whole-month balance.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    item {
                                        Button(onClick = { detail = picked; fullSource = false },
                                            modifier = Modifier.fillMaxWidth()) {
                                            Text("View " + usage.label + " transactions")
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                        }
                                    }
                                }
                            }
                            StatementTab.YEARLY -> {
                                val data = requireNotNull(annual)
                                val index = years.indexOf(data.year)
                                val byMonth = data.months.associateBy { it.usage.month }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    item {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween) {
                                            IconButton(enabled = index < years.lastIndex,
                                                onClick = { yearChoice = years[index + 1] }) {
                                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Earlier year")
                                            }
                                            Text(data.year.toString(), style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold)
                                            IconButton(enabled = index > 0,
                                                onClick = { yearChoice = years[index - 1] }) {
                                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Later year")
                                            }
                                        }
                                    }
                                    item {
                                        Text(data.importedMonthCount.toString() + " imported months · " +
                                            data.missingMonthCount + " missing",
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    item {
                                        StatementHero("YEARLY MONEY OUT", reportMoney(data.paidOutMinor),
                                            "Only imported months · not verified purchase spending")
                                    }
                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            StatementStat("Money in", reportMoney(data.paidInMinor),
                                                Modifier.weight(1f))
                                            StatementStat("Average / imported month",
                                                reportMoney(data.averageOutMinor), Modifier.weight(1f))
                                        }
                                    }
                                    item {
                                        PesaCard {
                                            Column(Modifier.padding(14.dp)) {
                                                Text("Monthly usage", style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold)
                                                Text("Tap a bar for that month · grey is missing",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.height(16.dp))
                                                val max = maxOf(1L,
                                                    data.months.maxOfOrNull { it.usage.paidOutMinor } ?: 0L)
                                                val shortMonths = listOf("J","F","M","A","M","J","J","A","S","O","N","D")
                                                Row(Modifier.fillMaxWidth().height(170.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.Bottom) {
                                                    (1..12).forEach { number ->
                                                        val entry = byMonth[number]
                                                        Column(Modifier.weight(1f).fillMaxHeight(),
                                                            horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Spacer(Modifier.weight(1f))
                                                            val height = if (entry == null) 9.dp else
                                                                (10 + 122 * (entry.usage.paidOutMinor.toDouble() / max)).dp
                                                            Box(Modifier.fillMaxWidth().height(height)
                                                                .background(
                                                                    if (entry == null) MaterialTheme.colorScheme.surfaceVariant
                                                                    else MaterialTheme.colorScheme.primary,
                                                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                                .then(if (entry == null) Modifier else Modifier.clickable {
                                                                    monthChoice = entry.key
                                                                    tab = StatementTab.MONTHLY
                                                                }))
                                                            Spacer(Modifier.height(8.dp))
                                                            Text(shortMonths[number - 1],
                                                                style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (data.months.any { it.sourceCount > 1 }) item {
                                        Text("Overlapping PDFs were not summed. One original source per month is shown.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    item {
                                        Text("Missing months are excluded from averages, never counted as zero. " +
                                            "Some imported months may contain partial statements.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    items(data.months, key = { it.key }) { entry ->
                                        OutlinedButton(onClick = {
                                            monthChoice = entry.key
                                            tab = StatementTab.MONTHLY
                                        }, modifier = Modifier.fillMaxWidth()) {
                                            Text(entry.usage.label, Modifier.weight(1f))
                                            Text(reportMoney(entry.usage.paidOutMinor))
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                        }
                                    }
                                }
                            }
                            StatementTab.BROWSE -> {
                                val filtered = months.filter {
                                    browseQuery.isBlank() ||
                                        it.usage.label.contains(browseQuery.trim(), ignoreCase = true) ||
                                        it.key.contains(browseQuery.trim())
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Text("Browse every imported month",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold)
                                        Text("Jump to older years or search by month",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    item {
                                        OutlinedTextField(value = browseQuery,
                                            onValueChange = { browseQuery = it },
                                            label = { Text("Search month or year") }, singleLine = true,
                                            modifier = Modifier.fillMaxWidth())
                                    }
                                    item {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(years) { y ->
                                                FilterChip(selected = browseQuery == y.toString(),
                                                    onClick = { browseQuery =
                                                        if (browseQuery == y.toString()) "" else y.toString() },
                                                    label = { Text(y.toString()) })
                                            }
                                        }
                                    }
                                    if (filtered.isEmpty()) item { Text("No matching imported months.") }
                                    filtered.groupBy { it.usage.year }
                                        .toSortedMap(compareByDescending { it })
                                        .forEach { (y, entries) ->
                                            item(key = "year:" + y) {
                                                Text(y.toString() + " · " + entries.size + " imported months",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                            items(entries, key = { it.key }) { entry ->
                                                PesaCard {
                                                    Row(Modifier.fillMaxWidth().clickable {
                                                        monthChoice = entry.key
                                                        tab = StatementTab.MONTHLY
                                                    }.padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically) {
                                                        Column(Modifier.weight(1f)) {
                                                            Text(entry.usage.label,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.SemiBold)
                                                            Text(entry.usage.sourceRows.toString() + " source rows" +
                                                                if (entry.sourceCount > 1) " · Multiple PDFs"
                                                                else " · Saved PDF",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        Text(reportMoney(entry.usage.paidOutMinor),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.SemiBold)
                                                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                                    }
                                                }
                                            }
                                        }
                                    item {
                                        OutlinedButton(onClick = { importing = true },
                                            modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Rounded.Add, contentDescription = null)
                                            Text("Import another statement")
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
    // Reuse the established review/import route and refresh when it closes.
    if (importing) StatementImportSheet(
        onDismiss = { importing = false; refresh++ },
        onChanged = { refresh++ }
    )
}

@Composable
private fun StatementHero(title: String, value: String, note: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(value, style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(note, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun StatementStat(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatementBar(title: String, value: Long, max: Long, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(reportMoney(value), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxWidth((value.toDouble() / max).toFloat().coerceIn(0f, 1f))
                .fillMaxHeight().background(color, RoundedCornerShape(5.dp)))
        }
    }
}
