package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.everyday.*
import com.kedevjimmy.pesatrack.ui.components.MainTab
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class StatementPage { SUMMARY, CATEGORIES, TRENDS, TRANSACTIONS, DEBTS, BROWSE }
private val nairobiStatementZone = TimeZone.getTimeZone("Africa/Nairobi")

internal fun statementDate(timestamp: Long, template: String = "dd MMM yyyy"): String =
    SimpleDateFormat(template, Locale.ENGLISH).apply { timeZone = nairobiStatementZone }
        .format(Date(timestamp))

private fun shiftedDate(timestamp: Long, days: Int): Long =
    Calendar.getInstance(nairobiStatementZone).apply {
        timeInMillis = timestamp
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis

private fun statementCategoryGroup(id: String?, labels: Map<String, String>): String =
    StatementExpenseCatalog.name(id, labels)

@Composable
internal fun StatementIntelligenceDashboard(
    onDismiss: () -> Unit,
    onTabSelected: ((MainTab) -> Unit)? = null
) {
    val context = LocalContext.current
    val store = remember(context) { StatementActivityStore(context) }
    val reader = remember(context) { StatementReportReader(context) }
    val crossCategory = remember(context) { StatementCrossCategoryRepository(context) }
    val ledgerRevision by LedgerUpdateBus.state.collectAsState()
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<StatementHistoryReport.SourceMonth>>(emptyList()) }
    var accountLabels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var categoryLabels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var categoryKinds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var reports by remember { mutableStateOf<List<StatementClassified>>(emptyList()) }
    var loadedAccount by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    var dataError by remember { mutableStateOf(false) }
    var refreshed by remember { mutableIntStateOf(0) }
    var labelsChanged by remember { mutableIntStateOf(0) }
    var accountSelection by remember { mutableStateOf<String?>(null) }
    var accountMenu by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf(StatementFinancialPolicy.Period.MONTH) }
    var allHistory by remember { mutableStateOf(true) }
    var dateMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var showReviewHelp by remember { mutableStateOf(false) }
    var monthSelection by remember { mutableStateOf<String?>(null) }
    var yearSelection by remember { mutableStateOf<Int?>(null) }
    var dayAnchor by remember { mutableStateOf<Long?>(null) }
    var page by remember { mutableStateOf(StatementPage.SUMMARY) }
    var previousPage by remember { mutableStateOf(StatementPage.SUMMARY) }
    var importing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var browseQuery by remember { mutableStateOf("") }
    var flowFilter by remember { mutableStateOf<StatementFlow?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var onlyClassified by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<StatementClassified?>(null) }
    var editCategory by remember { mutableStateOf<String?>(null) }
    var editRemember by remember { mutableStateOf(false) }
    var editRememberBoth by remember { mutableStateOf(false) }
    var bridgePreview by remember {
        mutableStateOf<StatementCrossCategoryRepository.Preview?>(null)
    }
    var bridgeScanning by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var reconciliationExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(refreshed) {
        busy = true
        dataError = false
        try {
            val info = withContext(Dispatchers.IO) {
                val bridge = StatementCategoryBridge(context)
                Triple(store.sourceMonths(),
                    EverydayMoneyStore(context).ownedAccounts().associate { it.id to it.name },
                    bridge.categoryLabels() to bridge.categoryKinds())
            }
            sources = info.first
            accountLabels = info.second
            categoryLabels = info.third.first
            categoryKinds = info.third.second
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { dataError = true }
        finally { busy = false }
    }

    val accounts = remember(sources) { sources.map { it.accountId }.distinct() }
    val accountId = accountSelection?.takeIf { it in accounts } ?: accounts.firstOrNull()
    val months = remember(sources, accountId) {
        StatementHistoryReport.selectForAccount(sources, accountId.orEmpty())
    }
    val statementFileCount = remember(sources) { sources.map { it.batchId }.distinct().size }
    val years = remember(months) { StatementHistoryReport.years(months) }
    val month = months.firstOrNull { it.key == monthSelection } ?: months.firstOrNull()
    val year = yearSelection?.takeIf { it in years } ?: years.firstOrNull()
    val dateAnchor = dayAnchor ?: month?.source?.lastTransactionAt ?: 0L

    // Preload all data for the currently selected account once; changing Day,
    // Week, Month, Year or page never triggers repeated PDF/database reads.
    LaunchedEffect(accountId, refreshed, labelsChanged, months, ledgerRevision.revision) {
        if (accountId == null) {
            reports = emptyList()
            loadedAccount = null
            return@LaunchedEffect
        }
        if (loadedAccount != accountId) loadedAccount = null
        try {
            reports = withContext(Dispatchers.IO) { reader.read(months) }
            loadedAccount = accountId
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { dataError = true; reports = emptyList() }
    }

    val visible = remember(reports, period, month?.key, year, dateAnchor, allHistory) {
        reports.filter { row ->
            allHistory || when (period) {
                StatementFinancialPolicy.Period.YEAR ->
                    year != null && StatementMonthlyUsage.monthKey(row.line.occurredAt).startsWith("$year-")
                StatementFinancialPolicy.Period.MONTH -> month != null &&
                    StatementMonthlyUsage.monthKey(row.line.occurredAt) == month.key
                else -> StatementFinancialPolicy.within(row.line.occurredAt, dateAnchor, period)
            }
        }
    }
    val totals = remember(visible) { StatementFinancialPolicy.totals(visible) }
    val sourceCashFlow = remember(visible) { StatementFinancialPolicy.sourceCashFlow(visible) }
    val breakdown = remember(visible, categoryLabels) {
        StatementExpenseCatalog.breakdown(visible, categoryLabels)
    }
    val periodTitle = if (allHistory) {
        val oldest = reports.minOfOrNull { it.line.occurredAt }
        val newest = reports.maxOfOrNull { it.line.occurredAt }
        if (oldest != null && newest != null)
            statementDate(oldest, "MMM yyyy") + " – " + statementDate(newest, "MMM yyyy")
        else "All imported dates"
    } else when (period) {
        StatementFinancialPolicy.Period.YEAR -> (year ?: "").toString()
        StatementFinancialPolicy.Period.MONTH -> month?.usage?.label ?: "No month"
        StatementFinancialPolicy.Period.WEEK -> "Week of " + statementDate(dateAnchor, "dd MMM yyyy")
        StatementFinancialPolicy.Period.DAY -> statementDate(dateAnchor)
    }

    fun navigate(target: StatementPage, selectedFlow: StatementFlow? = null,
                 selectedCategory: String? = null) {
        previousPage = page
        page = target
        flowFilter = selectedFlow
        categoryFilter = selectedCategory
        query = ""
    }
    fun back() {
        when {
            showReviewHelp -> showReviewHelp = false
            importing -> importing = false
            edit != null -> edit = null
            page == StatementPage.DEBTS || page == StatementPage.BROWSE ->
                page = previousPage.takeIf { it != page } ?: StatementPage.SUMMARY
            page != StatementPage.SUMMARY -> page = StatementPage.SUMMARY
            else -> onDismiss()
        }
    }
    BackHandler { back() }

    Dialog(onDismissRequest = { back() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                if (page == StatementPage.SUMMARY) {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { back() }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Statements", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("Your imported records, in one place",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { navigate(StatementPage.BROWSE) },
                            enabled = months.isNotEmpty()) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search statements",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        if (months.isNotEmpty()) {
                            Box {
                                IconButton(onClick = { moreMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert,
                                        contentDescription = "More statement options")
                                }
                                DropdownMenu(expanded = moreMenu,
                                    onDismissRequest = { moreMenu = false }) {
                                    listOf(
                                        "Categories" to StatementPage.CATEGORIES,
                                        "Trends" to StatementPage.TRENDS,
                                        "Transactions" to StatementPage.TRANSACTIONS,
                                        "Debts" to StatementPage.DEBTS,
                                        "Statement archive" to StatementPage.BROWSE
                                    ).forEach { (label, destination) ->
                                        DropdownMenuItem(text = { Text(label) }, onClick = {
                                            moreMenu = false
                                            navigate(destination)
                                        })
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(text = { Text("Import statement") }, onClick = {
                                        moreMenu = false
                                        importing = true
                                    })
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { back() }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Column(Modifier.weight(1f)) {
                            Text(if (page == StatementPage.DEBTS) "Statement debts"
                                else if (page == StatementPage.BROWSE) "Statement history"
                                else "Statements",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                            Text("Your money, from imported statements",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { importing = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Import statement",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                if (dataError) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Could not open the saved statement report. Your financial records were not changed.",
                            color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { refreshed++ }) { Text("Try again") }
                    }
                } else if (busy || (accountId != null && loadedAccount != accountId)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Preparing your saved statement report…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (months.isEmpty()) {
                    StatementEmptyLanding(
                        statementFileCount = statementFileCount,
                        onImport = { importing = true },
                        onExplainReview = { showReviewHelp = true },
                        onBankCsv = { onTabSelected?.invoke(MainTab.ACTIVITY) },
                        bankCsvImportEnabled = onTabSelected != null
                    )
                } else {
                    if (page == StatementPage.SUMMARY) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1.08f)) {
                                OutlinedButton(onClick = { dateMenu = true },
                                    modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 9.dp)) {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(periodTitle, modifier = Modifier.weight(1f), maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium)
                                    Icon(Icons.Rounded.ExpandMore, contentDescription = "Choose date range",
                                        modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                                    DropdownMenuItem(text = { Text("All imported dates") }, onClick = {
                                        allHistory = true
                                        dateMenu = false
                                    })
                                    if (years.isNotEmpty()) HorizontalDivider()
                                    years.forEach { availableYear ->
                                        DropdownMenuItem(text = { Text("Year · $availableYear") }, onClick = {
                                            period = StatementFinancialPolicy.Period.YEAR
                                            yearSelection = availableYear
                                            allHistory = false
                                            dateMenu = false
                                        })
                                    }
                                    if (months.isNotEmpty()) HorizontalDivider()
                                    months.forEach { availableMonth ->
                                        DropdownMenuItem(text = { Text(availableMonth.usage.label) }, onClick = {
                                            period = StatementFinancialPolicy.Period.MONTH
                                            monthSelection = availableMonth.key
                                            dayAnchor = null
                                            allHistory = false
                                            dateMenu = false
                                        })
                                    }
                                }
                            }
                            Box(Modifier.weight(0.92f)) {
                                OutlinedButton(onClick = { accountMenu = true },
                                    modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 9.dp)) {
                                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(accountLabels[accountId] ?: "M-Pesa wallet",
                                        modifier = Modifier.weight(1f), maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium)
                                    Icon(Icons.Rounded.ExpandMore, contentDescription = "Choose account",
                                        modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = accountMenu,
                                    onDismissRequest = { accountMenu = false }) {
                                    accounts.forEach { id ->
                                        DropdownMenuItem(
                                            text = { Text(accountLabels[id] ?: "Saved M-Pesa wallet") },
                                            onClick = {
                                                accountSelection = id
                                                monthSelection = null
                                                yearSelection = null
                                                dayAnchor = null
                                                allHistory = true
                                                accountMenu = false
                                            })
                                    }
                                }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                OutlinedButton(onClick = { accountMenu = true },
                                    modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(accountLabels[accountId] ?: "M-Pesa wallet",
                                        modifier = Modifier.weight(1f), maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Rounded.ExpandMore, contentDescription = "Choose account")
                                }
                                DropdownMenu(expanded = accountMenu,
                                    onDismissRequest = { accountMenu = false }) {
                                    accounts.forEach { id ->
                                        DropdownMenuItem(
                                            text = { Text(accountLabels[id] ?: "Saved M-Pesa wallet") },
                                            onClick = {
                                                accountSelection = id
                                                monthSelection = null
                                                yearSelection = null
                                                dayAnchor = null
                                                allHistory = true
                                                accountMenu = false
                                            })
                                    }
                                }
                            }
                            IconButton(onClick = { navigate(StatementPage.BROWSE) }) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = "Browse saved statements")
                            }
                        }
                    }
                    if (page != StatementPage.SUMMARY && page != StatementPage.BROWSE && page != StatementPage.DEBTS) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listOf(
                                "Summary" to StatementPage.SUMMARY,
                                "Categories" to StatementPage.CATEGORIES,
                                "Trends" to StatementPage.TRENDS,
                                "Transactions" to StatementPage.TRANSACTIONS
                            )) { (label, target) ->
                                FilterChip(selected = page == target,
                                    onClick = { navigate(target) }, label = { Text(label) })
                            }
                        }
                    }
                    if (page != StatementPage.SUMMARY && page != StatementPage.BROWSE) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            FilterChip(selected = allHistory,
                                onClick = { allHistory = true; flowFilter = null; categoryFilter = null },
                                label = { Text("All") }, modifier = Modifier.weight(1f))
                            StatementFinancialPolicy.Period.entries.forEach { item ->
                                FilterChip(selected = !allHistory && period == item,
                                    onClick = { period = item; allHistory = false; flowFilter = null; categoryFilter = null },
                                    label = { Text(item.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.weight(1f))
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                when (period) {
                                    StatementFinancialPolicy.Period.YEAR -> {
                                        val i = years.indexOf(year)
                                        if (i < years.lastIndex) yearSelection = years[i + 1]
                                    }
                                    StatementFinancialPolicy.Period.MONTH -> {
                                        val i = months.indexOf(month)
                                        if (i < months.lastIndex) {
                                            monthSelection = months[i + 1].key
                                            dayAnchor = null
                                        }
                                    }
                                    StatementFinancialPolicy.Period.WEEK -> dayAnchor = shiftedDate(dateAnchor, -7)
                                    StatementFinancialPolicy.Period.DAY -> dayAnchor = shiftedDate(dateAnchor, -1)
                                }
                            }) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Earlier period") }
                            Text(periodTitle, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = {
                                when (period) {
                                    StatementFinancialPolicy.Period.YEAR -> {
                                        val i = years.indexOf(year)
                                        if (i > 0) yearSelection = years[i - 1]
                                    }
                                    StatementFinancialPolicy.Period.MONTH -> {
                                        val i = months.indexOf(month)
                                        if (i > 0) {
                                            monthSelection = months[i - 1].key
                                            dayAnchor = null
                                        }
                                    }
                                    StatementFinancialPolicy.Period.WEEK -> dayAnchor = shiftedDate(dateAnchor, 7)
                                    StatementFinancialPolicy.Period.DAY -> dayAnchor = shiftedDate(dateAnchor, 1)
                                }
                            }) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Later period") }
                        }
                    }

                    when (page) {
                        StatementPage.SUMMARY -> {
                            val scopedMonths = months.filter { selected ->
                                allHistory || when (period) {
                                    StatementFinancialPolicy.Period.YEAR -> selected.usage.year == year
                                    StatementFinancialPolicy.Period.MONTH -> selected.key == month?.key
                                    else -> true
                                }
                            }
                            StatementOverviewPage(
                                sourceMonths = scopedMonths,
                                visible = visible,
                                sourceCashFlow = sourceCashFlow,
                                breakdown = breakdown,
                                reconciliationExpanded = reconciliationExpanded,
                                onReconciliationToggle = {
                                    reconciliationExpanded = !reconciliationExpanded
                                },
                                onNavigate = { destination, flow, category ->
                                    navigate(destination, flow, category)
                                },
                                onImport = { importing = true },
                                onArchive = { navigate(StatementPage.BROWSE) }
                            )
                        }
                        StatementPage.CATEGORIES -> {
                            LazyColumn(Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                item {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = !onlyClassified, onClick = { onlyClassified = false },
                                            label = { Text("All expenses") })
                                        FilterChip(selected = onlyClassified, onClick = { onlyClassified = true },
                                            label = { Text("Identified categories") })
                                    }
                                }
                                val shown = if (onlyClassified)
                                    breakdown.filterNot { it.first == "Other" } else breakdown
                                val chartTotal = shown.fold(0L) { a, b -> Math.addExact(a, b.second) }
                                item {
                                    Surface(shape = RoundedCornerShape(19.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                        Column(Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Where your money went",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold)
                                            StatementDonut(shown, chartTotal,
                                                Modifier.size(225.dp).padding(12.dp))
                                            if (shown.isEmpty()) Text("No classified expenses in this period.")
                                            else Text("Tap a category to see its original transactions",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                if (shown.isNotEmpty()) item {
                                    val first = shown.first()
                                    Text(first.first + " · " + statementMoney(first.second) + " · largest expense",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                items(shown, key = { it.first }) { (category, amount) ->
                                    val i = shown.indexOfFirst { it.first == category }
                                    StatementCategoryLine(category, amount, chartTotal, i) {
                                        navigate(StatementPage.TRANSACTIONS,
                                            StatementFlow.EXPENSE, category)
                                    }
                                }
                                item {
                                    Text("Categories follow PesaTrack's existing labels and remembered merchant " +
                                        "rules. Unknown movements are not invented as expenses.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        StatementPage.TRENDS -> {
                            val buckets = StatementTrendBuckets.build(
                                visible, period,
                                months.filter { it.usage.year == year }.map { it.usage.month }.toSet()
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    Text("Your spending over time",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold)
                                    Text("See when expenses happened in imported statements, without counting transfers or loans as purchases.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                item {
                                    StatementHeroMetric("SPENT · " + periodTitle,
                                        totals.expenseMinor,
                                        "Classified expenses · partial statement periods may be missing")
                                }
                                item {
                                    Surface(shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                        Column(Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(when (period) {
                                                StatementFinancialPolicy.Period.DAY -> "Every 4 hours"
                                                StatementFinancialPolicy.Period.WEEK -> "Each day of the week"
                                                StatementFinancialPolicy.Period.MONTH -> "Each week of the month"
                                                StatementFinancialPolicy.Period.YEAR -> "Each month of the year"
                                            }, style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold)
                                            StatementTrendBars(buckets) { slot ->
                                                when (period) {
                                                    StatementFinancialPolicy.Period.YEAR -> {
                                                        val chosen = months.firstOrNull {
                                                            it.usage.year == year && it.usage.month == slot + 1
                                                        }
                                                        if (chosen != null) {
                                                            monthSelection = chosen.key
                                                            period = StatementFinancialPolicy.Period.MONTH
                                                            dayAnchor = null
                                                            page = StatementPage.SUMMARY
                                                        }
                                                    }
                                                    StatementFinancialPolicy.Period.MONTH -> {
                                                        val target = requireNotNull(month)
                                                        val calendar = Calendar.getInstance(nairobiStatementZone).apply {
                                                            timeInMillis = target.source.lastTransactionAt
                                                            set(Calendar.DAY_OF_MONTH, minOf(slot * 7 + 1,
                                                                getActualMaximum(Calendar.DAY_OF_MONTH)))
                                                        }
                                                        dayAnchor = calendar.timeInMillis
                                                        period = StatementFinancialPolicy.Period.WEEK
                                                        page = StatementPage.SUMMARY
                                                    }
                                                    StatementFinancialPolicy.Period.WEEK -> {
                                                        val calendar = Calendar.getInstance(nairobiStatementZone).apply {
                                                            timeInMillis = dateAnchor
                                                            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
                                                                add(Calendar.DAY_OF_MONTH, -1)
                                                            add(Calendar.DAY_OF_MONTH, slot)
                                                        }
                                                        dayAnchor = calendar.timeInMillis
                                                        period = StatementFinancialPolicy.Period.DAY
                                                        page = StatementPage.SUMMARY
                                                    }
                                                    StatementFinancialPolicy.Period.DAY -> {
                                                        navigate(StatementPage.TRANSACTIONS, StatementFlow.EXPENSE)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (breakdown.isNotEmpty()) item {
                                    Text("Biggest category · " + breakdown.first().first +
                                        " · " + statementMoney(breakdown.first().second),
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                                item {
                                    Text("An empty or grey bucket can mean unavailable evidence, not zero usage. " +
                                        "Only statement-originated transactions are displayed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        StatementPage.TRANSACTIONS -> {
                            val filtered = visible.filter {
                                (flowFilter == null || it.flow == flowFilter) &&
                                    (categoryFilter == null ||
                                        statementCategoryGroup(it.categoryId, categoryLabels) == categoryFilter) &&
                                    (query.isBlank() || it.line.description.contains(query, ignoreCase = true) ||
                                        it.line.reference.contains(query, ignoreCase = true))
                            }
                            Column(Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = query, onValueChange = { query = it },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    label = { Text("Search merchant or receipt") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    singleLine = true)
                                LazyRow(contentPadding = PaddingValues(horizontal = 13.dp),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    val choices = listOf(
                                        null to "All", StatementFlow.EXPENSE to "Expenses",
                                        StatementFlow.EXCLUDED to "Excluded",
                                        StatementFlow.INCOME to "Income", StatementFlow.TRANSFER to "Transfers",
                                        StatementFlow.DEBT to "Debts", StatementFlow.SAVINGS to "Savings",
                                        StatementFlow.FEE to "Fees", StatementFlow.UNKNOWN to "Needs review")
                                    items(choices) { (value, title) ->
                                        FilterChip(selected = flowFilter == value,
                                            onClick = { flowFilter = value; categoryFilter = null },
                                            label = { Text(title) })
                                    }
                                }
                                if (categoryFilter != null) TextButton(onClick = { categoryFilter = null }) {
                                    Text("Category: " + categoryFilter + "  ×")
                                }
                                LazyColumn(Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    if (filtered.isEmpty()) item {
                                        Text("No transactions match this period and filter.")
                                    }
                                    items(filtered, key = {
                                        it.batchId + ":" + it.line.page + ":" + it.line.row
                                    }) { classified ->
                                        Surface(shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                edit = classified
                                                editCategory = classified.categoryId
                                                editRemember = false
                                                editRememberBoth = false
                                                saveError = null
                                            }) {
                                            Row(Modifier.padding(13.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(classified.line.description,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    Text((when (classified.flow) {
                                                        StatementFlow.EXCLUDED -> "Excluded"
                                                        else -> when (classified.categoryId) {
                                                            "MONEY_LENT" -> "Money lent"
                                                            "RECEIVABLE_REPAYMENT" -> "Loan repayment received"
                                                            else -> classified.categoryId?.let { categoryLabels[it] }
                                                                ?: statementCategoryGroup(classified.categoryId, categoryLabels)
                                                        }
                                                    }) +
                                                        " · " + statementDate(classified.line.occurredAt, "dd MMM yyyy"),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.bodySmall)
                                                    Text(classified.evidence,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.labelSmall)
                                                    if (classified.flow == StatementFlow.EXPENSE &&
                                                        classified.reportAmountMinor != classified.amountMinor)
                                                        Text("Your share: " +
                                                            statementMoney(classified.reportAmountMinor) +
                                                            " of " + statementMoney(classified.amountMinor),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            style = MaterialTheme.typography.labelSmall)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text((if (classified.line.withdrawnMinor > 0) "−" else "+") +
                                                        statementMoney(classified.amountMinor),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (classified.line.paidInMinor > 0)
                                                            MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface)
                                                    Icon(Icons.Rounded.ChevronRight,
                                                        contentDescription = "Review category")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        StatementPage.DEBTS -> {
                            val debts = visible.filter { it.debtProvider != null }
                            val providers = debts.groupBy { it.debtProvider ?: "Unknown lender" }
                            val personalLent = visible.filter { it.categoryId == "MONEY_LENT" }
                            val personalRepaid = visible.filter { it.categoryId == "RECEIVABLE_REPAYMENT" }
                            val shared = visible.filter {
                                it.flow == StatementFlow.EXPENSE &&
                                    it.activityReceiptVerified &&
                                    it.reportAmountMinor < it.amountMinor
                            }
                            LazyColumn(Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(13.dp)) {
                                item {
                                    Text("Debts found in this statement",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold)
                                    Text("Historical evidence only. This doesn't change your current debts or prove today's outstanding balance.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (providers.isEmpty() && personalLent.isEmpty() &&
                                    personalRepaid.isEmpty() && shared.isEmpty()) item {
                                    Text("No supported loan or verified personal receivable evidence in this period.")
                                }
                                if (personalLent.isNotEmpty() || personalRepaid.isNotEmpty() ||
                                    shared.isNotEmpty()) item {
                                    val lent = personalLent.fold(0L) { total, row ->
                                        Math.addExact(total, row.amountMinor)
                                    }
                                    val repaid = personalRepaid.fold(0L) { total, row ->
                                        Math.addExact(total, row.amountMinor)
                                    }
                                    val sharedOwed = shared.fold(0L) { total, row ->
                                        Math.addExact(total,
                                            Math.subtractExact(row.amountMinor, row.reportAmountMinor))
                                    }
                                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(17.dp)) {
                                        Column(Modifier.padding(15.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Money owed to you · verified in Activity",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold)
                                            Text("Money lent: " + statementMoney(lent) +
                                                " · Repayments received: " + statementMoney(repaid),
                                                style = MaterialTheme.typography.bodyMedium)
                                            if (sharedOwed > 0L) Text(
                                                "Shared bill amounts owed to you: " +
                                                statementMoney(sharedOwed),
                                                style = MaterialTheme.typography.bodyMedium)
                                            Text("These are historical receipt matches, not a current " +
                                                "outstanding balance or proof of full repayment. " +
                                                "Manage loans and shared bills in Activity.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            (personalLent + personalRepaid + shared).forEach { record ->
                                                HorizontalDivider()
                                                Text(statementDate(record.line.occurredAt) +
                                                    " · " + record.line.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2)
                                            }
                                        }
                                    }
                                }
                                providers.forEach { (provider, events) ->
                                    item(key = provider) {
                                        val borrowed = events.filter { it.debtAction == StatementDebtAction.BORROWED }
                                            .fold(0L) { a,b -> Math.addExact(a,b.amountMinor) }
                                        val repaid = events.filter { it.debtAction == StatementDebtAction.REPAID }
                                            .fold(0L) { a,b -> Math.addExact(a,b.amountMinor) }
                                        val fees = events.filter { it.debtAction == StatementDebtAction.CHARGE }
                                            .fold(0L) { a,b -> Math.addExact(a,b.amountMinor) }
                                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = RoundedCornerShape(18.dp)) {
                                            Column(Modifier.padding(15.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text(provider, style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    StatementDebtTile("Borrowed",borrowed,Modifier.weight(1f))
                                                    StatementDebtTile("Repaid",repaid,Modifier.weight(1f))
                                                    StatementDebtTile("Fees",fees,Modifier.weight(1f))
                                                }
                                                Text(when {
                                                    borrowed > 0L && repaid > 0L ->
                                                        "Repayment observed. Full settlement is not confirmed."
                                                    repaid > 0L ->
                                                        "Repayment observed; borrowing may predate this statement."
                                                    borrowed > 0L ->
                                                        "Borrowing observed. No repayment found in this period."
                                                    else -> "Loan-related records need review."
                                                }, style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                events.sortedBy { it.line.occurredAt }.forEach { event ->
                                                    HorizontalDivider()
                                                    Row(Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Column(Modifier.weight(1f)) {
                                                            Text(event.debtAction?.name?.lowercase()
                                                                ?.replaceFirstChar { it.uppercase() }
                                                                ?: "Needs review")
                                                            Text(statementDate(event.line.occurredAt),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        Text(statementMoney(event.amountMinor),
                                                            fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        StatementPage.BROWSE -> {
                            val matched = months.filter {
                                browseQuery.isBlank() ||
                                    it.usage.label.contains(browseQuery, ignoreCase = true) ||
                                    it.key.contains(browseQuery, ignoreCase = true)
                            }
                            LazyColumn(Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Text("Your statement history",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold)
                                    Text("Browse every imported month, even from years before PesaTrack.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                item {
                                    OutlinedTextField(
                                        value = browseQuery, onValueChange = { browseQuery = it },
                                        label = { Text("Search month or year") }, singleLine = true,
                                        modifier = Modifier.fillMaxWidth())
                                }
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(years) { y ->
                                            FilterChip(selected = browseQuery == y.toString(),
                                                onClick = {
                                                    browseQuery = if (browseQuery == y.toString()) ""
                                                        else y.toString()
                                                }, label = { Text(y.toString()) })
                                        }
                                    }
                                }
                                if (matched.isEmpty()) item { Text("No imported months match.") }
                                matched.groupBy { it.usage.year }
                                    .toSortedMap(compareByDescending { it })
                                    .forEach { (y, group) ->
                                        item(key = "y:$y") {
                                            Text(y.toString(), style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold)
                                        }
                                        items(group, key = { it.key }) { entry ->
                                            Surface(shape = RoundedCornerShape(15.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    monthSelection = entry.key
                                                    yearSelection = entry.usage.year
                                                    dayAnchor = null
                                                    period = StatementFinancialPolicy.Period.MONTH
                                                    page = StatementPage.SUMMARY
                                                }) {
                                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(entry.usage.label,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.SemiBold)
                                                        Text("Money out · " + statementMoney(entry.usage.paidOutMinor) +
                                                            if (entry.sourceCount > 1) " · overlapping PDFs" else "",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                                }
                                            }
                                        }
                                    }
                                item {
                                    Button(onClick = { importing = true },
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
                if (onTabSelected != null) {
                    StatementBottomNavigation(onTabSelected)
                }
            }
        }
    }
    if (showReviewHelp) {
        AlertDialog(
            onDismissRequest = { showReviewHelp = false },
            title = { Text("Review before saving") },
            text = {
                Text("Statements are checked on this device. Review the extracted rows and totals, then confirm before saving them to Statements. They do not change Activity balances.")
            },
            confirmButton = {
                TextButton(onClick = { showReviewHelp = false }) { Text("Got it") }
            }
        )
    }
    if (importing) StatementImportSheet(
        onDismiss = { importing = false; refreshed++ },
        onChanged = { refreshed++ },
        statementOnly = true
    )

    // The preview is an evidence check, never an eager classification write.
    LaunchedEffect(edit?.batchId, edit?.line?.row, editCategory, accountId) {
        bridgePreview = null
        editRememberBoth = false
        val target = edit
        val category = editCategory
        val account = accountId
        if (target == null || category == null || account == null ||
            categoryKinds[category] != "EXPENSE") {
            bridgeScanning = false
            return@LaunchedEffect
        }
        bridgeScanning = true
        try {
            bridgePreview = withContext(Dispatchers.IO) {
                crossCategory.preview(target, account, category)
            }
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { bridgePreview = null }
        finally { bridgeScanning = false }
    }

    val editing = edit
    if (editing != null) {
        val editableCategories = categoryLabels.entries.filter {
            StatementFinancialPolicy.canAssignCategory(editing.line, it.key, categoryKinds)
        }
        val categoryCanSave = editableCategories.any { it.key == editCategory }
        AlertDialog(onDismissRequest = { if (!saving) edit = null },
            title = { Text("Categorize transaction") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 470.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(editing.line.description, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3)
                    Text(
                        statementDate(editing.line.occurredAt) + " · Receipt " +
                            editing.line.reference + " · Page " + editing.line.page,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Choose a category for this statement record",
                        style = MaterialTheme.typography.bodySmall)
                    if (editableCategories.isEmpty()) Text(
                        "This record is informational or contains protected transfer, debt, " +
                            "savings, fee, refund or income evidence. Its financial type " +
                            "cannot be changed from the category selector.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    var expand by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expand = true },
                            enabled = editableCategories.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()) {
                            Text(editCategory?.let { categoryLabels[it] } ?: "Select category",
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                        }
                        DropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                            editableCategories.forEach { (id, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    editCategory = id
                                    editRemember = false
                                    editRememberBoth = false
                                    saveError = null
                                    expand = false
                                })
                            }
                        }
                    }
                    val canRemember = categoryCanSave &&
                        categoryKinds[editCategory] == "EXPENSE" &&
                        StatementFinancialPolicy.merchantKey(editing.line.description) != null
                    if (canRemember && bridgeScanning) Text(
                        "Checking the same M-Pesa receipt in Activity…",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val canBridge = categoryCanSave && bridgePreview != null
                    if (canBridge) {
                        val match = requireNotNull(bridgePreview)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = editRememberBoth, onCheckedChange = {
                                editRememberBoth = it
                                if (it) editRemember = false
                            })
                            Column {
                                Text("Remember in Activity + Statements",
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("Exact receipt verified · " +
                                    "${match.activityMatches} other Activity and " +
                                    "${match.statementMatches} eligible Statement records " +
                                    "(${match.reportMatches} in reports).",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else if (canRemember) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = editRemember, onCheckedChange = {
                                editRemember = it
                                if (it) editRememberBoth = false
                            })
                            Text("Remember for future Statements only",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text("To update Activity too, a matching and eligible original " +
                            "receipt must be verified. A similar name is not enough.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Only this record leaves other transactions unchanged. " +
                        "The shared option requires your confirmation and does not " +
                        "merge the Activity or Statement databases.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (saveError != null) Text(requireNotNull(saveError),
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !saving && !bridgeScanning && categoryCanSave &&
                        (!editRememberBoth || bridgePreview != null),
                    onClick = {
                        val category = editCategory ?: return@TextButton
                        saving = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    if (editRememberBoth) {
                                        val link = requireNotNull(bridgePreview) {
                                            "A verified Activity receipt is required for both."
                                        }
                                        crossCategory.rememberBoth(
                                            editing, requireNotNull(accountId), category,
                                            link.activityTransactionId
                                        )
                                    } else {
                                        store.categorize(editing.batchId, editing.line.page,
                                            editing.line.row, category, editRemember)
                                    }
                                }
                                edit = null
                                labelsChanged++
                            } catch (e: CancellationException) { throw e }
                              catch (e: Exception) { saveError = e.message ?: "Could not save category." }
                            finally { saving = false }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { edit = null }, enabled = !saving) { Text("Cancel") }
            })
    }
}

@Composable
private fun StatementHighlightCard(
    label: String, amount: Long, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Surface(modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(statementMoney(amount), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            Text("View details  ›", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StatementDebtTile(
    title: String, amount: Long, modifier: Modifier = Modifier
) {
    Surface(modifier, shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(statementMoney(amount), style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold)
        }
    }
}
