package com.kedevjimmy.pesatrack.ui.screens

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.domain.payroll.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

// Paycheck dashboard visuals are derived only from saved, user-reviewed payroll records.
// Never use synthetic chart points or count a payslip as cash received.
private val DashPurple = Color(0xFF9D72FF)
private val DashLilac = Color(0xFFC5ADFF)
private val DashPink = Color(0xFFFF6D99)
private val DashTeal = Color(0xFF48DCC3)
private val DashBlue = Color(0xFF508FFF)
private val DashGold = Color(0xFFFFBD66)

private fun paycheckMoney(amount: Long): String {
    val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true
    }.format(BigDecimal.valueOf(amount, 2))
    return "KES " + formatted
}

private fun paycheckPct(part: Long, whole: Long): String =
    if (whole <= 0L) "—"
    else String.format(Locale.US, "%.1f%%", part.toDouble() / whole.toDouble() * 100.0)

private data class DeductionSlice(val name: String, val amount: Long, val color: Color)

private fun breakdown(summary: PayrollSummary): List<DeductionSlice> {
    val parts = summary.employeeByType
    val tax = parts[PayrollLineType.TAX] ?: 0L
    val nssf = parts[PayrollLineType.NSSF] ?: 0L
    val health = parts[PayrollLineType.HEALTH] ?: 0L
    val other = summary.employeeDeductionMinor - tax - nssf - health
    return listOf(
        DeductionSlice("PAYE", tax, DashPurple),
        DeductionSlice("NSSF", nssf, DashBlue),
        DeductionSlice("Health", health, DashTeal),
        DeductionSlice("Other", other, DashGold)
    ).filter { it.amount > 0 }
}

@Composable
private fun PaycheckPanel(modifier: Modifier = Modifier, tint: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val shape = RoundedCornerShape(22.dp)
    val panelBackground = if (tint == null) Modifier.background(background)
        else Modifier.background(Brush.linearGradient(listOf(
            tint.copy(alpha = if (MaterialTheme.colorScheme.background.luminance() < .5f) .25f else .11f),
            background
        )))
    Column(
        modifier
            .clip(shape)
            .then(panelBackground)
            .border(1.dp, outline, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun IconDisc(icon: ImageVector, color: Color, modifier: Modifier = Modifier, diameter: Dp = 46.dp) {
    Box(modifier.size(diameter).background(color.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size((diameter.value * .5f).dp))
    }
}

@Composable
private fun PaycheckMetric(
    title: String, amount: String, caption: String, icon: ImageVector,
    accent: Color, modifier: Modifier = Modifier, compact: Boolean = false
) {
    PaycheckPanel(modifier = modifier.heightIn(min = if (compact) 122.dp else 132.dp), tint = accent) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconDisc(icon, accent, diameter = if (compact) 31.dp else 36.dp)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(amount, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
            fontSize = if (compact) 16.sp else 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(caption, color = accent, style = MaterialTheme.typography.labelSmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PaycheckMetricGrid(record: PayrollRecord?, summary: PayrollSummary?) {
    val metrics = listOf(
        Triple("Gross Pay", record?.draft?.grossMinor?.let(::paycheckMoney) ?: "—", DashPurple),
        Triple("Net Pay", record?.draft?.netMinor?.let(::paycheckMoney) ?: "—", DashLilac),
        Triple("Total Deductions", summary?.employeeDeductionMinor?.let(::paycheckMoney) ?: "—", DashPink),
        Triple("Employer Contributions", summary?.employerContributionMinor?.let(::paycheckMoney) ?: "—", DashGold)
    )
    val icons = listOf(Icons.Rounded.Paid, Icons.Rounded.AccountBalanceWallet,
        Icons.Rounded.ReceiptLong, Icons.Rounded.Groups)
    val captions = listOf(
        "Total earnings", "After deductions",
        if (record == null) "Awaiting payslip" else paycheckPct(summary?.employeeDeductionMinor ?: 0L, record.draft.grossMinor) + " of gross",
        "Reported only"
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val available = maxWidth
        val columns = when {
            available < 340.dp -> 1
            available >= 760.dp -> 4
            else -> 2
        }
        val compact = columns == 2 && available < 410.dp
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.chunked(columns).forEachIndexed {row, group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    group.forEachIndexed {index,metric ->
                        val i = row * columns + index
                        PaycheckMetric(metric.first, metric.second, captions[i], icons[i],
                            metric.third, Modifier.weight(1f), compact)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaycheckMonthlyTrend(history: List<PayrollRecord>) {
    PaycheckPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.BarChart, contentDescription = null, tint = DashPurple)
            Spacer(Modifier.width(8.dp))
            Text("Monthly Trend", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Last 6 payslips", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("● Gross Pay", color = DashPurple, style = MaterialTheme.typography.labelMedium)
            Text("● Net Pay", color = DashLilac, style = MaterialTheme.typography.labelMedium)
        }
        if (history.size < 2) {
            Box(Modifier.fillMaxWidth().height(138.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.ShowChart, contentDescription = null, tint = DashLilac)
                    Text("A trend needs at least two saved months",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val maxMinor = max(1L, history.maxOf { it.draft.grossMinor }).toDouble()
            val grid = MaterialTheme.colorScheme.outlineVariant
            Canvas(
                modifier = Modifier.fillMaxWidth().height(156.dp).semantics {
                    contentDescription = "Gross and net salary trend across " + history.size +
                        " saved payroll months, from " + history.first().draft.period +
                        " to " + history.last().draft.period
                }
            ) {
                val inset = 9.dp.toPx()
                val usableH = size.height - inset * 2f
                val usableW = size.width - inset * 2f
                repeat(4) { index ->
                    val y = inset + usableH * index / 3f
                    drawLine(grid.copy(alpha = .55f), Offset(inset,y),
                        Offset(size.width-inset,y), strokeWidth = 1.dp.toPx())
                }
                fun plot(value: (PayrollRecord)->Long, color:Color) {
                    val points = history.mapIndexed {i, r ->
                        Offset(inset + usableW * i / (history.size-1).toFloat(),
                            inset + usableH * (1.0 - value(r).toDouble() / maxMinor).toFloat())
                    }
                    val path = Path().apply {
                        points.forEachIndexed {i,p -> if(i==0) moveTo(p.x,p.y) else lineTo(p.x,p.y)}
                    }
                    drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                    points.forEach {drawCircle(color, radius = 4.dp.toPx(), center = it)}
                }
                plot({it.draft.grossMinor}, DashPurple)
                plot({it.draft.netMinor}, DashLilac)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                history.forEach { item ->
                    val period = item.draft.period
                    Text(period.substring(5) + "/" + period.substring(2,4),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("Only saved months from the selected employer are plotted.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaycheckDonut(slices: List<DeductionSlice>, total: Long, modifier: Modifier = Modifier) {
    Box(modifier.size(152.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics {
            contentDescription = "Deduction composition: " + slices.joinToString { it.name +
                " " + paycheckPct(it.amount,total) }
        }) {
            val arc = 22.dp.toPx()
            var angle = -90f
            if (total == 0L) {
                drawArc(Color.Gray.copy(alpha=.18f), -90f, 360f, false, style=Stroke(arc))
            } else {
                slices.forEach { entry ->
                    val sweep = (entry.amount.toDouble() / total.toDouble() * 360.0).toFloat()
                    drawArc(entry.color, angle, sweep, false, style=Stroke(arc, cap=StrokeCap.Butt))
                    angle += sweep
                }
            }
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text("KES",style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(NumberFormat.getNumberInstance(Locale.US).format(BigDecimal.valueOf(total,2)
                .setScale(0,java.math.RoundingMode.HALF_UP)), fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium)
            Text("deductions",style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeductionLegend(slices: List<DeductionSlice>, total:Long, modifier:Modifier=Modifier) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        slices.forEach { entry ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(entry.color))
                Text(entry.name,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,
                    maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(paycheckPct(entry.amount,total),style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(paycheckMoney(entry.amount),style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PaycheckDeductions(record: PayrollRecord, summary: PayrollSummary) {
    val slices = remember(record.id,record.revision) { breakdown(summary) }
    var showLines by rememberSaveable(record.id) {mutableStateOf(false)}
    PaycheckPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Rounded.DonutLarge,null,tint=DashPurple)
            Spacer(Modifier.width(8.dp))
            Text("Deductions Breakdown",style=MaterialTheme.typography.titleMedium,
                fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if(maxWidth>=540.dp) {
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    PaycheckDonut(slices,summary.employeeDeductionMinor)
                    DeductionLegend(slices,summary.employeeDeductionMinor,Modifier.weight(1f))
                }
            } else {
                Column(horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    PaycheckDonut(slices,summary.employeeDeductionMinor)
                    DeductionLegend(slices,summary.employeeDeductionMinor,Modifier.fillMaxWidth())
                }
            }
        }
        TextButton(onClick={showLines=!showLines},modifier=Modifier.align(Alignment.End)) {
            Text(if(showLines)"Hide original deductions" else "View original deduction lines")
            Icon(if(showLines)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription=null)
        }
        if(showLines) {
            HorizontalDivider()
            record.draft.lines.filter {it.role==PayrollLineRole.EMPLOYEE_DEDUCTION}.forEach {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(it.label,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                    Text(paycheckMoney(it.amountMinor),style=MaterialTheme.typography.bodySmall)
                }
            }
            Text("Other groups additional types for the chart; original lines remain separate.",
                style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaycheckInsights(record:PayrollRecord, summary:PayrollSummary) {
    val biggest = summary.employeeByType.maxByOrNull { it.value }
    val ratio = paycheckPct(record.draft.netMinor,record.draft.grossMinor)
    PaycheckPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb,null,tint=DashGold)
            Spacer(Modifier.width(8.dp))
            Text("Key Insights",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sideBySide = maxWidth >= 410.dp
            if(sideBySide) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                InsightItem("Highest deduction",biggest?.key?.title ?: "None reported",
                    biggest?.value?.let {paycheckMoney(it)+" · "+paycheckPct(it,summary.employeeDeductionMinor)}
                        ?: "No deductions",DashPurple,Modifier.weight(1f))
                VerticalDivider(modifier=Modifier.height(68.dp),color=MaterialTheme.colorScheme.outlineVariant)
                InsightItem("Net pay ratio",ratio,
                    paycheckMoney(record.draft.netMinor)+" of "+paycheckMoney(record.draft.grossMinor),
                    DashLilac,Modifier.weight(1f))
            } else Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                InsightItem("Highest deduction",biggest?.key?.title ?: "None reported",
                    biggest?.value?.let {paycheckMoney(it)+" · "+paycheckPct(it,summary.employeeDeductionMinor)}
                        ?: "No deductions",DashPurple)
                HorizontalDivider()
                InsightItem("Net pay ratio",ratio,
                    paycheckMoney(record.draft.netMinor)+" of "+paycheckMoney(record.draft.grossMinor),
                    DashLilac)
            }
        }
        Text("Payslip figures are employer-reported. They are not confirmed deposits or remittances.",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InsightItem(label:String,headline:String,detail:String,accent:Color,modifier:Modifier=Modifier) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(label,style=MaterialTheme.typography.labelMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(headline,fontWeight=FontWeight.Bold,color=accent,
            style=MaterialTheme.typography.titleMedium,maxLines=2)
        Text(detail,style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PaycheckImportAction(
    enabled:Boolean, onClick:()->Unit, manual:Boolean = false
) {
    val shape=RoundedCornerShape(20.dp)
    val onDark=Color.White
    val darkTheme=MaterialTheme.colorScheme.background.luminance()<.5f
    val foreground=if(manual) MaterialTheme.colorScheme.primary else onDark
    val background=if(manual) MaterialTheme.colorScheme.surfaceContainerLow else DashPurple
    Row(
        Modifier.fillMaxWidth().heightIn(min=68.dp)
            .clip(shape)
            .background(if(manual) Brush.linearGradient(listOf(background,background))
            else Brush.horizontalGradient(listOf(Color(0xFF6833DB), Color(0xFF4B319C))))
            .border(1.dp,if(manual) MaterialTheme.colorScheme.outlineVariant
                else DashPurple.copy(alpha=.7f),shape)
            .clickable(enabled=enabled,onClick=onClick)
            .padding(horizontal=16.dp,vertical=15.dp)
            .testTag(if(manual)"paycheck-manual" else "paycheck-import"),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        Icon(if(manual)Icons.Rounded.Edit else Icons.Rounded.PictureAsPdf,
            contentDescription=null,tint=foreground,modifier=Modifier.size(29.dp))
        Column(Modifier.weight(1f)) {
            Text(if(manual)"Enter paycheck manually" else "Import payslip PDF",
                fontWeight=FontWeight.SemiBold,color=foreground,
                style=MaterialTheme.typography.titleSmall)
            if(!manual) Text(if(Build.VERSION.SDK_INT>=35)
                "Read locally · private payroll review" else "Requires Android 15+",
                style=MaterialTheme.typography.labelSmall,
                color=if(darkTheme)Color(0xFFEEE5FF) else Color.White.copy(alpha=.9f))
        }
        Icon(Icons.Rounded.ChevronRight,null,tint=foreground)
    }
}

@Composable
private fun SavedPaycheckCard(record:PayrollRecord, onClick:()->Unit) {
    val summary=remember(record.id,record.revision) {PayrollJournalPolicy.validate(record.draft)}
    PaycheckPanel(Modifier.fillMaxWidth().clickable(onClick=onClick)) {
        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            IconDisc(Icons.Rounded.Business,DashPurple,diameter=42.dp)
            Column(Modifier.weight(1f)) {
                Text(record.draft.employer+" · "+record.draft.period,
                    fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(if(record.draft.source==PayrollSource.PDF_TEXT)
                    "Reviewed PDF record" else "Manual payroll record",
                    style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.65f))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow=maxWidth<480.dp
            if(narrow) {
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        SmallRecordAmount("Gross",record.draft.grossMinor,DashPurple,Modifier.weight(1f))
                        SmallRecordAmount("Net",record.draft.netMinor,DashLilac,Modifier.weight(1f))
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        SmallRecordAmount("Deductions",summary.employeeDeductionMinor,DashPink,Modifier.weight(1f))
                        SmallRecordAmount("Employer contrib.",summary.employerContributionMinor,DashGold,Modifier.weight(1f))
                    }
                }
            } else Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                SmallRecordAmount("Gross",record.draft.grossMinor,DashPurple,Modifier.weight(1f))
                SmallRecordAmount("Net",record.draft.netMinor,DashLilac,Modifier.weight(1f))
                SmallRecordAmount("Deductions",summary.employeeDeductionMinor,DashPink,Modifier.weight(1f))
                SmallRecordAmount("Employer contrib.",summary.employerContributionMinor,DashGold,Modifier.weight(1f))
            }
        }
        Text("Tap to review or edit · no cash transaction was created",
            style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SmallRecordAmount(label:String,amount:Long,color:Color,modifier:Modifier=Modifier) {
    Column(modifier) {
        Text(label,style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,
            overflow=TextOverflow.Ellipsis)
        Text(paycheckMoney(amount),style=MaterialTheme.typography.labelLarge,
            fontWeight=FontWeight.SemiBold,color=color,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

/** Real saved payroll only; the reference mockup contains sample months not inserted here. */
@Composable
internal fun PaycheckDashboard(
    records:List<PayrollRecord>, busy:Boolean, pdfError:String?,
    error:String?, message:String?, onBack:()->Unit, onImport:()->Unit,
    onManual:()->Unit, onEdit:(PayrollRecord)->Unit, padding:PaddingValues
) {
    var selectedId by rememberSaveable {mutableStateOf<String?>(null)}
    var monthMenu by remember {mutableStateOf(false)}
    var showAll by rememberSaveable {mutableStateOf(false)}
    val selected=records.firstOrNull{it.id==selectedId} ?: records.firstOrNull()
    val summary=selected?.let{PayrollJournalPolicy.validate(it.draft)}
    val history=selected?.let { current ->
        records.filter {
            PayrollJournalPolicy.normalizeEmployer(it.draft.employer) ==
                PayrollJournalPolicy.normalizeEmployer(current.draft.employer)
        }.distinctBy {it.draft.period}.sortedBy{it.draft.period}.takeLast(6)
    }.orEmpty()
    val contentMaxWidth=900.dp
    LazyColumn(
        modifier=Modifier.fillMaxSize().padding(padding).testTag("paycheck-analysis-page"),
        contentPadding=PaddingValues(horizontal=16.dp,vertical=16.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        item {
            Column(Modifier.widthIn(max=contentMaxWidth).fillMaxWidth(),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Paycheck Analysis",style=MaterialTheme.typography.headlineMedium,
                            fontWeight=FontWeight.Bold,maxLines=2)
                        Text("Privately reviewed payslip insights",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        OutlinedButton(onClick={monthMenu=true},
                            enabled=records.isNotEmpty(),contentPadding=PaddingValues(horizontal=8.dp)) {
                            Icon(Icons.Rounded.CalendarMonth,null,modifier=Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(selected?.draft?.period ?: "Month",
                                style=MaterialTheme.typography.labelMedium)
                            Icon(Icons.Rounded.ExpandMore,null,modifier=Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded=monthMenu,onDismissRequest={monthMenu=false}) {
                            records.forEach {item ->
                                DropdownMenuItem(
                                    text={Text(item.draft.period+" · "+item.draft.employer,
                                        maxLines=1,overflow=TextOverflow.Ellipsis)},
                                    onClick={selectedId=item.id;monthMenu=false})
                            }
                        }
                    }
                }
                TextButton(onClick=onBack) {
                    Icon(Icons.Rounded.ArrowBack,null,Modifier.size(16.dp))
                    Text("Back to Activity")
                }
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(error!=null) Text(error,color=MaterialTheme.colorScheme.error)
                if(pdfError!=null) Text(pdfError,color=MaterialTheme.colorScheme.error)
                if(message!=null) Text(message,color=MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Box(Modifier.widthIn(max=contentMaxWidth).fillMaxWidth()) {
                PaycheckMetricGrid(selected,summary)
            }
        }
        if(selected!=null && summary!=null) {
            item {
                BoxWithConstraints(Modifier.widthIn(max=contentMaxWidth).fillMaxWidth()) {
                    if(maxWidth>=730.dp) Row(horizontalArrangement=Arrangement.spacedBy(14.dp),
                        verticalAlignment=Alignment.Top) {
                        Box(Modifier.weight(1.1f)){PaycheckMonthlyTrend(history)}
                        Box(Modifier.weight(.9f)){PaycheckDeductions(selected,summary)}
                    } else Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        PaycheckMonthlyTrend(history)
                        PaycheckDeductions(selected,summary)
                    }
                }
            }
            item {
                Box(Modifier.widthIn(max=contentMaxWidth).fillMaxWidth()) {
                    PaycheckInsights(selected,summary)
                }
            }
        } else item {
            PaycheckPanel(Modifier.widthIn(max=contentMaxWidth).fillMaxWidth()) {
                Text("No saved payslip yet",style=MaterialTheme.typography.titleMedium)
                Text("Import a PDF or enter a paycheck manually to build your private insights. Trends appear when you have at least two saved months.",
                    style=MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Column(Modifier.widthIn(max=contentMaxWidth).fillMaxWidth(),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                PaycheckImportAction(!busy&&Build.VERSION.SDK_INT>=35,onImport)
                PaycheckImportAction(!busy,onManual,manual=true)
            }
        }
        if(records.isNotEmpty()) {
            item {
                Row(Modifier.widthIn(max=contentMaxWidth).fillMaxWidth(),
                    verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History,null,tint=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Saved payroll",style=MaterialTheme.typography.titleMedium,
                        fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                    TextButton(onClick={showAll=!showAll}) {
                        Text(if(showAll)"Show less" else "View all ("+records.size+")")
                    }
                }
            }
            items(if(showAll)records else listOfNotNull(selected),key={it.id}) {item ->
                Box(Modifier.widthIn(max=contentMaxWidth).fillMaxWidth()) {
                    SavedPaycheckCard(item){onEdit(item)}
                }
            }
        }
        item {
            Text("Payroll figures are reported, not verified as received, deducted by the bank, or remitted. Your cash totals are unchanged.",
                modifier=Modifier.widthIn(max=contentMaxWidth).fillMaxWidth(),
                style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
