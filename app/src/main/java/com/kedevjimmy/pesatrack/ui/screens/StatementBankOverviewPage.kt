package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.StatementBankArchiveStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.StatementEvidenceReadModel
import com.kedevjimmy.pesatrack.domain.statements.StatementBankHistoryPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementBankCategoryLabelPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementSourceCoveragePolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** Bank source view reusing Phase 1 palette/cards. No guessed spending or merged accounts. */
@Composable
internal fun StatementBankOverviewPage(
    accountId: String,
    accountLabel: String,
    sourceLabel: String,
    months: List<StatementBankHistoryPolicy.Month>,
    availableAccounts: List<Pair<String, String>>,
    onSelectAccount: (String) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    initialYear: Int? = null
) {
    val context = LocalContext.current
    val p = HomeReferencePalette
    val scope = rememberCoroutineScope()
    val bankCategories = remember(context) { CategoryStore.get(context).all() }
    val bankCategoryLabels = remember(bankCategories) { bankCategories.associate { it.id to it.label } }
    var limitedSources by remember(accountId) {
        mutableStateOf<List<StatementBankArchiveStore.Source>>(emptyList())
    }
    var limitedSelected by remember(accountId) {
        mutableStateOf<StatementBankArchiveStore.Source?>(null)
    }
    LaunchedEffect(accountId, months) {
        limitedSources = withContext(Dispatchers.IO) {
            StatementBankArchiveStore(context).limitedSourcesFor(accountId)
        }
    }
    limitedSelected?.let { selected ->
        StatementLimitedSourcePage(selected) { limitedSelected = null }
        return
    }
    val years = remember(months) { months.map { it.key.substringBefore('-').toInt() }.distinct().sortedDescending() }
    var year by remember(accountId, initialYear) {
        mutableStateOf<Int?>(initialYear?.takeIf { it in years } ?: years.firstOrNull())
    }
    var accountMenu by remember { mutableStateOf(false) }
    var chosenMonth by remember(accountId) { mutableStateOf<StatementBankHistoryPolicy.Month?>(null) }
    BackHandler(enabled=chosenMonth!=null) { chosenMonth=null }
    var evidence by remember { mutableStateOf<List<StatementBankArchiveStore.Leg>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var editingLeg by remember(accountId) { mutableStateOf<StatementBankArchiveStore.Leg?>(null) }
    var selectedLabel by remember(accountId) { mutableStateOf<String?>(null) }
    var labelError by remember { mutableStateOf<String?>(null) }
    var savingLabel by remember { mutableStateOf(false) }
    var overlapByMonth by remember(accountId) {
        mutableStateOf<Map<String, StatementSourceCoveragePolicy.MonthCoverage>>(emptyMap())
    }
    var overlapUnavailable by remember(accountId) { mutableStateOf(false) }
    LaunchedEffect(accountId, year, months) {
        overlapByMonth = emptyMap()
        overlapUnavailable = false
        val selectedYears = year?.let { listOf(it) } ?: years
        try {
            overlapByMonth = withContext(Dispatchers.IO) {
                selectedYears.flatMap { selectedYear ->
                    val dates = java.util.Calendar.getInstance(
                        java.util.TimeZone.getTimeZone("Africa/Nairobi")
                    ).apply { clear(); set(selectedYear, java.util.Calendar.JANUARY, 1) }
                    val from = dates.timeInMillis
                    dates.add(java.util.Calendar.YEAR, 1)
                    StatementEvidenceReadModel(context).read(from, dates.timeInMillis, accountId)
                        .coverage
                }.associateBy { it.monthKey }
            }
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { overlapUnavailable = true }
    }
    LaunchedEffect(accountId, chosenMonth) {
        evidence = emptyList();failed = false
        val selected = chosenMonth ?: return@LaunchedEffect
        try {
            val monthPrefix = selected.key
            evidence = withContext(Dispatchers.IO) {
                val formatter = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.ROOT).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Africa/Nairobi")
                }
                StatementBankArchiveStore(context).legsFor(selected.sourceId).filter {
                    formatter.format(java.util.Date(it.occurredAt)) == monthPrefix
                }
            }
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { failed = true }
    }
    val visible = remember(months, year) {
        if (year == null) months else months.filter { it.key.startsWith("$year-") }
    }
    val moneyIn = remember(visible) { visible.fold(0L) { sum, m -> Math.addExact(sum, m.moneyInMinor) } }
    val moneyOut = remember(visible) { visible.fold(0L) { sum, m -> Math.addExact(sum, m.moneyOutMinor) } }

    LazyColumn(Modifier.fillMaxSize().background(p.canvas),
        contentPadding = PaddingValues(start=16.dp,end=16.dp,top=6.dp,bottom=32.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(verticalAlignment=Alignment.Top) {
                Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFFE9E7FE)) {
                    IconButton(onClick=if(chosenMonth==null) onBack else ({chosenMonth=null})) {
                        Icon(Icons.Rounded.ArrowBack,"Back",tint=p.ink)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sourceLabel,fontSize=27.sp,fontWeight=FontWeight.ExtraBold,color=p.ink)
                    Text(if(chosenMonth==null) "Statement insights" else "Source details",
                        style=MaterialTheme.typography.bodyMedium,color=p.muted)
                    Box {
                        Surface(shape=RoundedCornerShape(13.dp),color=Color.White,
                            modifier=Modifier.clickable(enabled=availableAccounts.size>1){accountMenu=true}) {
                            Row(Modifier.padding(horizontal=10.dp,vertical=7.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AccountBalance,null,tint=p.green,modifier=Modifier.size(18.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(accountLabel,color=p.ink,style=MaterialTheme.typography.labelLarge,
                                    maxLines=1,overflow=TextOverflow.Ellipsis)
                                if(availableAccounts.size>1)Icon(Icons.Rounded.ExpandMore,"Choose account",tint=p.muted)
                            }
                        }
                        DropdownMenu(expanded=accountMenu,onDismissRequest={accountMenu=false}) {
                            availableAccounts.forEach { (id,name) ->
                                DropdownMenuItem(text={Text(name)},onClick={accountMenu=false;onSelectAccount(id)})
                            }
                        }
                    }
                }
                IconButton(onClick=onImport){Icon(Icons.Rounded.Add,"Import statement",tint=p.ink)}
            }
        }
        if(chosenMonth==null) {
            item {
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected=year==null,onClick={year=null},label={Text("All")})
                    years.forEach { y -> FilterChip(selected=year==y,onClick={year=y},label={Text(y.toString())}) }
                }
            }
            item { StatementInsightCard {
                Text("Bank cash flow",style=MaterialTheme.typography.titleLarge,
                    fontWeight=FontWeight.Bold,color=p.ink)
                Text("Verified movements in this bank account. Transfers, loans and withdrawals are not automatically classified as spending or income.",
                    style=MaterialTheme.typography.bodySmall,color=p.muted)
                Text("Each imported month currently shows one selected document; multiple documents are checked for repeated evidence separately.",
                    style=MaterialTheme.typography.bodySmall,color=p.muted)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("MONEY IN",color=p.green,style=MaterialTheme.typography.labelSmall)
                        Text(statementMoney(moneyIn),fontWeight=FontWeight.Bold,color=p.ink)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("MONEY OUT",color=p.red,style=MaterialTheme.typography.labelSmall)
                        Text(statementMoney(moneyOut),fontWeight=FontWeight.Bold,color=p.ink)
                    }
                }
            } }
            item { StatementInsightCard {
                Text("Money out by imported month",style=MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.Bold,color=p.ink)
                val maximum = maxOf(1L, visible.maxOfOrNull{it.moneyOutMinor} ?: 0L)
                Row(Modifier.fillMaxWidth().height(145.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    visible.take(12).asReversed().forEach { entry ->
                        Column(Modifier.weight(1f).fillMaxHeight().clickable{chosenMonth=entry},
                            horizontalAlignment=Alignment.CenterHorizontally) {
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.fillMaxWidth().height((12+95*entry.moneyOutMinor.toDouble()/maximum).dp)
                                .background(p.purple,RoundedCornerShape(topStart=5.dp,topEnd=5.dp)))
                            Text(entry.key.substring(5),style=MaterialTheme.typography.labelSmall,color=p.muted)
                        }
                    }
                }
                Text("Only imported periods are shown; missing months are unknown, not zero.",
                    style=MaterialTheme.typography.bodySmall,color=p.muted)
            } }
            item { Text("Imported months",fontWeight=FontWeight.Bold,color=p.ink,
                style=MaterialTheme.typography.titleLarge) }
            items(visible,key={it.key}) { entry ->
                StatementInsightCard {
                    Row(Modifier.fillMaxWidth().clickable{chosenMonth=entry},verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.key,fontWeight=FontWeight.Bold,color=p.ink)
                            Text("${entry.rowCount} verified movements",style=MaterialTheme.typography.bodySmall,color=p.muted)
                            if(entry.containsOverlappingImports)Text("Alternative imports exist; counted once",
                                style=MaterialTheme.typography.bodySmall,color=p.red)
                            val overlap = overlapByMonth[entry.key]
                            if(overlap != null && overlap.sourceCount > 1) {
                                Text("${overlap.confirmedRepeatCopies} exact repeated entries verified",
                                    style=MaterialTheme.typography.bodySmall,color=p.muted)
                                if(overlap.status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW)
                                    Text("${overlap.unresolvedCandidateCount} entries need evidence review",
                                        style=MaterialTheme.typography.bodySmall,color=p.red)
                                else Text("Coverage checked; this card still reports one selected PDF",
                                    style=MaterialTheme.typography.bodySmall,color=p.muted)
                            }
                            if(overlapUnavailable && entry.containsOverlappingImports)
                                Text("Could not verify repeated entries yet",
                                    style=MaterialTheme.typography.bodySmall,color=p.red)
                        }
                        Text(statementMoney(entry.moneyOutMinor),fontWeight=FontWeight.SemiBold,color=p.ink)
                        Icon(Icons.Rounded.ChevronRight,null,tint=p.muted)
                    }
                }
            }
            if(limitedSources.isNotEmpty()) {
                item {
                    Text("Limited assurance documents",fontWeight=FontWeight.Bold,color=p.ink,
                        style=MaterialTheme.typography.titleLarge)
                    Text("Reconciled source rows only. These documents are excluded from verified cash-flow charts and Activity.",
                        color=p.muted,style=MaterialTheme.typography.bodySmall)
                }
                items(limitedSources,key={"limited:${it.id}"}) { source ->
                    StatementInsightCard {
                        Row(Modifier.fillMaxWidth().clickable { limitedSelected = source },
                            verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Limited assurance · ${source.rows} rows",
                                    fontWeight=FontWeight.SemiBold,color=p.ink)
                                Text("${statementDate(source.firstAt)} – ${statementDate(source.lastAt)}",
                                    color=p.muted,style=MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Rounded.ChevronRight,"View source rows",tint=p.muted)
                        }
                    }
                }
            }
            item { Text("Categories and transfers stay unclassified until verified. No bank statement changes your Activity balance.",
                style=MaterialTheme.typography.bodySmall,color=p.muted) }
        } else {
            item { StatementInsightCard {
                Text("${chosenMonth!!.key} · Source transactions",fontWeight=FontWeight.Bold,color=p.ink)
                Text("Every row is from your saved document. No inferred merchants, purchases or income.",
                    style=MaterialTheme.typography.bodySmall,color=p.muted)
                if(chosenMonth!!.containsOverlappingImports) Text("Other documents cover this month; only one source is shown.",
                    style=MaterialTheme.typography.bodySmall,color=p.red)
                overlapByMonth[chosenMonth!!.key]?.let { overlap ->
                    if(overlap.status == StatementSourceCoveragePolicy.Status.NEEDS_REVIEW)
                        Text("${overlap.unresolvedCandidateCount} entries require overlap or source verification. A combined bank total is not available.",
                            style=MaterialTheme.typography.bodySmall,color=p.red)
                }
            } }
            if(failed) item {Text("Could not read bank evidence. Your saved records were not changed.",color=p.red)}
            items(evidence,key={it.sourceId+":"+it.number}) { row -> StatementInsightCard {
                Text(row.description,fontWeight=FontWeight.SemiBold,color=p.ink)
                Text(statementDate(row.occurredAt),style=MaterialTheme.typography.bodySmall,color=p.muted)
                Text((if(row.movementMinor<0)"Money out · " else "Money in · ")+
                    statementMoney(kotlin.math.abs(row.movementMinor)),color=p.ink)
                Text("Running balance · "+statementMoney(row.runningBalanceMinor),
                    style=MaterialTheme.typography.bodySmall,color=p.muted)
                Text("Original source · Page ${row.page}, row ${row.row}",
                    style=MaterialTheme.typography.labelSmall,color=p.muted)
                TextButton(onClick = {
                    editingLeg = row
                    selectedLabel = row.categoryId
                    labelError = null
                }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(row.categoryId?.let { bankCategoryLabels[it] } ?: "Add category label")
                }
            } }
        }
    }

    val editing = editingLeg
    if (editing != null) {
        val allowed = bankCategories.filter {
            StatementBankCategoryLabelPolicy.canLabel(editing.movementMinor, it.kind.name)
        }
        var categoryMenu by remember(editing.sourceId, editing.number) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!savingLabel) editingLeg = null },
            title = { Text("Label bank statement record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(editing.description, maxLines = 3,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text("Your label stays on this exact bank statement row. It does not change " +
                        "Activity, spending totals, transfers, or other imported files. " +
                        "A matching name or amount is not enough to link the records.",
                        style = MaterialTheme.typography.bodySmall, color = p.muted)
                    Box {
                        OutlinedButton(onClick = { categoryMenu = true },
                            enabled = !savingLabel && allowed.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()) {
                            Text(selectedLabel?.let { bankCategoryLabels[it] } ?: "No category label",
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ExpandMore, null)
                        }
                        DropdownMenu(expanded = categoryMenu,
                            onDismissRequest = { categoryMenu = false }) {
                            allowed.forEach { category ->
                                DropdownMenuItem(text = { Text(category.label) }, onClick = {
                                    selectedLabel = category.id
                                    labelError = null
                                    categoryMenu = false
                                })
                            }
                        }
                    }
                    if (editing.categoryId != null || selectedLabel != null) {
                        TextButton(onClick = { selectedLabel = null; labelError = null },
                            enabled = !savingLabel) { Text("Remove category label") }
                    }
                    if (labelError != null) Text(requireNotNull(labelError),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = !savingLabel &&
                    (selectedLabel == null || allowed.any { it.id == selectedLabel }),
                    onClick = {
                        val chosen = selectedLabel
                        savingLabel = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    StatementBankArchiveStore(context).labelLeg(
                                        editing.sourceId, editing.number, chosen)
                                }
                                evidence = evidence.map { leg ->
                                    if (leg.sourceId == editing.sourceId && leg.number == editing.number)
                                        leg.copy(categoryId = chosen) else leg
                                }
                                editingLeg = null
                            } catch (e: CancellationException) { throw e }
                              catch (_: Exception) {
                                  labelError = "Could not save this bank label. Please try again."
                              } finally { savingLabel = false }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingLeg = null },
                enabled = !savingLabel) { Text("Cancel") } }
        )
    }
}
