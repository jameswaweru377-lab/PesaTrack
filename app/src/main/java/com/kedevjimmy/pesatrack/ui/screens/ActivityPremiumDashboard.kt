package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import java.util.Calendar

internal fun monthDaysRemaining(selected: Long, now: Long): Int? {
    val a = Calendar.getInstance().apply { timeInMillis = selected }
    val b = Calendar.getInstance().apply { timeInMillis = now }
    if(a.get(Calendar.YEAR)!=b.get(Calendar.YEAR) ||
       a.get(Calendar.MONTH)!=b.get(Calendar.MONTH)) return null
    return b.getActualMaximum(Calendar.DAY_OF_MONTH) - b.get(Calendar.DAY_OF_MONTH)
}
internal fun periodPercentChange(current: Long, previous: Long): Int? {
    if(previous<=0L || current<0L) return null
    return runCatching {
        java.math.BigDecimal.valueOf(current).subtract(java.math.BigDecimal.valueOf(previous))
            .multiply(java.math.BigDecimal.valueOf(100))
            .divide(java.math.BigDecimal.valueOf(previous),0,java.math.RoundingMode.HALF_UP)
            .intValueExact()
    }.getOrNull()
}

@Composable
internal fun ActivityAttentionHero(count:Int,onReview:()->Unit) {
    if(count<=0)return
    val colors = MaterialTheme.colorScheme
    val brand = colors.primary
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),
        color=colors.surface,
        border=BorderStroke(1.dp,brand.copy(alpha=.7f))) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Surface(shape=CircleShape,color=brand.copy(alpha=.16f)) {
                Icon(Icons.Rounded.PriorityHigh,null,Modifier.padding(9.dp).size(22.dp),tint=brand)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text("WHAT NEEDS MY ATTENTION?",style=MaterialTheme.typography.labelSmall,color=brand)
                Text(count.toString()+" "+(if(count==1)"item needs review" else "items need review"),
                    style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                Text("Review uncertain categories, duplicates and account links.",
                    style=MaterialTheme.typography.labelSmall,maxLines=2,
                    color=colors.onSurfaceVariant)
            }
            Button(onClick=onReview,modifier=Modifier.heightIn(min=48.dp),
                shape=RoundedCornerShape(12.dp),
                contentPadding=PaddingValues(horizontal=8.dp,vertical=7.dp),
                colors=ButtonDefaults.buttonColors(containerColor=brand,contentColor=colors.onPrimary)) {
                Text("Review now",style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
internal fun ActivityPeriodSelectors(
    monthLabel:String,cycleLabel:String,salaryMode:Boolean,
    onMonth:()->Unit,onCycle:()->Unit
) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        PeriodTile(monthLabel,Icons.Rounded.CalendarMonth,onMonth,Modifier.weight(1f))
        PeriodTile(if(salaryMode)cycleLabel else "Salary cycle",
            Icons.Rounded.Autorenew,onCycle,Modifier.weight(1f))
    }
}
@Composable private fun PeriodTile(
    title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,
    onClick:()->Unit,modifier:Modifier
) {
    Surface(onClick=onClick,modifier=modifier.heightIn(min=48.dp),
        shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal=8.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Icon(icon,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary)
            Text(title,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,
                fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Icon(Icons.Rounded.ExpandMore,"Choose period",Modifier.size(17.dp))
        }
    }
}

/**
 * True share of outflow relative to recorded inflow. A value of 1f means that
 * spending meets or exceeds income; never make an overspent period look empty.
 * Null is an entirely empty period and should draw only a neutral track.
 */
internal fun netFlowRingProgress(incomeMinor: Long, spentMinor: Long): Float? {
    if (incomeMinor < 0L || spentMinor < 0L) return null
    if (incomeMinor == 0L && spentMinor == 0L) return null
    if (spentMinor == 0L) return 0f
    if (incomeMinor == 0L || spentMinor >= incomeMinor) return 1f
    return (spentMinor.toDouble() / incomeMinor.toDouble()).toFloat().coerceIn(0f, 1f)
}

/** The approved summary card keeps its compact 2.06:1 phone layout. */
internal const val APPROVED_MONEY_CARD_ASPECT_RATIO = 2.06f

internal fun approvedMoneyCardHeight(widthDp: Float): Float =
    (widthDp / APPROVED_MONEY_CARD_ASPECT_RATIO).coerceIn(166f, 206f)

/** One read-only period label; the clickable selectors remain above this card. */
internal fun approvedMoneyCardPeriodLabel(monthLabel: String, salaryMode: Boolean): String =
    if (salaryMode) "$monthLabel salary cycle" else monthLabel

@Composable
internal fun ActivityPremiumMoneyHero(
    periodLabel: String,
    snapshot: ActivityLedgerSnapshot?,
    previous: ActivityLedgerSnapshot?,
    badge: String,
    waiting: Boolean,
    onExplain: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val brand = colors.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.background,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardHeight = approvedMoneyCardHeight(maxWidth.value).dp
            val compact = maxWidth < 350.dp
            val gaugeSize = (maxWidth * .29f).coerceIn(92.dp, 116.dp)
            val headingTextSize = if (periodLabel.contains("salary cycle", true)) 12.sp
                else if (compact) 13.sp else 15.sp
            val metricSize = if (compact) 14.sp else 15.sp

            Box(Modifier.fillMaxWidth().height(cardHeight)) {
                if (waiting || snapshot == null) {
                    Text(
                        "Updating period…",
                        modifier = Modifier.align(Alignment.CenterStart)
                            .padding(horizontal = 14.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    val income = snapshot.summary.incomeMinor
                    val spent = snapshot.summary.spentMinor
                    val net = CheckedMoney.orNull { CheckedMoney.subtract(income, spent) }
                    val progress = netFlowRingProgress(income, spent)
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 34.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = brand.copy(alpha = .12f)
                            ) {
                                Icon(
                                    Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.padding(7.dp).size(20.dp),
                                    tint = brand
                                )
                            }
                            Text(
                                periodLabel,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = headingTextSize,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = brand.copy(alpha = .12f),
                                border = BorderStroke(1.dp, colors.outlineVariant)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.CalendarMonth, null,
                                        Modifier.size(13.dp), tint = brand)
                                    Text(badge, style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp, maxLines = 1, color = brand)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricColumn(
                                label = "Money in",
                                value = formatActivityMoney(income),
                                change = periodPercentChange(
                                    income, previous?.summary?.incomeMinor ?: 0L
                                ),
                                modifier = Modifier.weight(1f),
                                valueSize = metricSize
                            )
                            MetricColumn(
                                label = "Money out",
                                value = formatActivityMoney(spent),
                                change = periodPercentChange(
                                    spent, previous?.summary?.spentMinor ?: 0L
                                ),
                                modifier = Modifier.weight(1.08f),
                                valueSize = metricSize
                            )

                            Box(
                                Modifier.size(gaugeSize),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(Modifier.matchParentSize()) {
                                    val strokeWidth = 8.dp.toPx()
                                    val pad = strokeWidth * 1.05f
                                    val arcSize = androidx.compose.ui.geometry.Size(
                                        size.width - pad * 2, size.height - pad * 2
                                    )
                                    val top = Offset(pad, pad)
                                    drawArc(
                                        color = colors.outlineVariant,
                                        startAngle = 135f, sweepAngle = 270f,
                                        useCenter = false, topLeft = top, size = arcSize,
                                        style = Stroke(width = strokeWidth,
                                            cap = StrokeCap.Round)
                                    )
                                    if (progress != null && progress > 0f) {
                                        drawArc(
                                            color = brand.copy(alpha = .18f),
                                            startAngle = 135f, sweepAngle = 270f * progress,
                                            useCenter = false, topLeft = top, size = arcSize,
                                            style = Stroke(width = strokeWidth + 5.dp.toPx(),
                                                cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = brand,
                                            startAngle = 135f, sweepAngle = 270f * progress,
                                            useCenter = false, topLeft = top, size = arcSize,
                                            style = Stroke(width = strokeWidth,
                                                cap = StrokeCap.Round)
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                ) {
                                    Text(
                                        "Net flow",
                                        fontSize = 10.sp, lineHeight = 12.sp,
                                        color = colors.onSurfaceVariant, maxLines = 1
                                    )
                                    Text(
                                        net?.let(::formatActivityMoney) ?: "Unavailable",
                                        fontSize = if (compact) 8.sp else 9.sp,
                                        lineHeight = 10.sp,
                                        maxLines = 2,
                                        fontWeight = FontWeight.Bold,
                                        color = if (net != null && net < 0L)
                                            colors.error else colors.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = onExplain,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Info, "Explain net flow",
                                            Modifier.size(15.dp), tint = brand
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
@Composable
private fun MetricColumn(
    label: String,
    value: String,
    change: Int?,
    modifier: Modifier,
    valueSize: androidx.compose.ui.unit.TextUnit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = valueSize,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface
        )
        PercentChange(change)
    }
}

@Composable private fun PercentChange(change:Int?) {
    val brand = MaterialTheme.colorScheme.primary
    if(change==null) {
        Text("No prior comparison",style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
    } else Row(horizontalArrangement=Arrangement.spacedBy(2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Icon(if(change<0)Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
            null,Modifier.size(13.dp),tint=brand)
        Text((if(change>0)"+" else "")+change.toString()+"%",
            style=MaterialTheme.typography.labelSmall,color=brand)
        Text("vs prior",style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
    }
}
@Composable
internal fun ActivityCompactFilters(selected: String, count: Int, onSelect: (String) -> Unit) {
    val tabs = listOf("All", "Money In", "Money Out", "Transfers", "Needs Review")
    val violet = androidx.compose.ui.graphics.Color(0xFF5800F7)
    val deepPurple = androidx.compose.ui.graphics.Color(0xFF3210CB)
    val rowState = rememberLazyListState()
    LaunchedEffect(selected) {
        val index = tabs.indexOf(selected)
        if (index >= 0) rowState.animateScrollToItem(index)
    }
    LazyRow(
        state = rowState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(start = 0.dp, top = 2.dp, end = 20.dp, bottom = 2.dp)
    ) {
        items(tabs.size) { index ->
            val tab = tabs[index]
            val active = selected == tab
            val symbol = when (tab) {
                "Money In" -> Icons.Rounded.ArrowUpward
                "Money Out" -> Icons.Rounded.ArrowDownward
                "Transfers" -> Icons.Rounded.SyncAlt
                "Needs Review" -> Icons.Rounded.PriorityHigh
                else -> null
            }
            val accent = when (tab) {
                "Money In" -> androidx.compose.ui.graphics.Color(0xFF0DBA7F)
                "Money Out" -> androidx.compose.ui.graphics.Color(0xFFFF2775)
                "Needs Review" -> androidx.compose.ui.graphics.Color(0xFFFF7512)
                else -> violet
            }
            Surface(
                onClick = { onSelect(tab) },
                shape = RoundedCornerShape(17.dp),
                color = if (active) violet else androidx.compose.ui.graphics.Color.White.copy(alpha = .85f),
                shadowElevation = if (active) 3.dp else 0.dp,
                border = if (active) null else BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .then(if (active) Modifier.background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(violet, deepPurple))
                        ) else Modifier)
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (symbol != null) {
                        Surface(
                            modifier = Modifier.size(23.dp),
                            shape = CircleShape,
                            color = if (active) androidx.compose.ui.graphics.Color.White.copy(alpha = .17f)
                                else accent.copy(alpha = .12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    symbol, contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = if (active && tab != "Needs Review") androidx.compose.ui.graphics.Color.White else accent
                                )
                            }
                        }
                    }
                    Text(
                        tab,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) androidx.compose.ui.graphics.Color.White
                            else androidx.compose.ui.graphics.Color(0xFF0C104B),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
