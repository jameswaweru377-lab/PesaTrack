package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import java.util.Calendar

private val aqua = Color(0xFF52E5D3)
private val violet = Color(0xFFC78CFA)

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
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,violet.copy(alpha=.7f))) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.horizontalGradient(listOf(
                    violet.copy(alpha=.19f),Color.Transparent,aqua.copy(alpha=.11f))))
            }
            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                Surface(shape=CircleShape,color=violet.copy(alpha=.20f)) {
                    Icon(Icons.Rounded.PriorityHigh,null,Modifier.padding(9.dp).size(22.dp),tint=violet)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text("WHAT NEEDS MY ATTENTION?",style=MaterialTheme.typography.labelSmall,color=violet)
                    Text(count.toString()+" "+(if(count==1)"item needs review" else "items need review"),
                        style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                    Text("Review uncertain categories, duplicates and account links.",
                        style=MaterialTheme.typography.labelSmall,maxLines=2,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick=onReview,shape=RoundedCornerShape(12.dp),
                    contentPadding=PaddingValues(horizontal=8.dp,vertical=7.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=violet,contentColor=Color(0xFF19102B))) {
                    Text("Review now",style=MaterialTheme.typography.labelSmall)
                }
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
        PeriodTile(monthLabel,Icons.Rounded.CalendarMonth,!salaryMode,onMonth,Modifier.weight(1f))
        PeriodTile(if(salaryMode)cycleLabel else "Salary cycle",
            Icons.Rounded.Autorenew,salaryMode,onCycle,Modifier.weight(1f))
    }
}
@Composable private fun PeriodTile(
    title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,
    selected:Boolean,onClick:()->Unit,modifier:Modifier
) {
    Surface(onClick=onClick,modifier=modifier.heightIn(min=48.dp),
        shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,if(selected)aqua.copy(alpha=.5f)
            else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal=8.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Icon(icon,null,Modifier.size(18.dp),tint=if(selected)aqua else violet)
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

/**
 * The approved reference image is 1,277 x 620 pixels, so the visible card
 * keeps a 2.06:1 landscape ratio on a phone. These coordinates are normalized
 * to the content canvas to keep the same wave on other Android densities.
 */
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
    val dark = MaterialTheme.colorScheme.background.red < 0.4f
    val foreground = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFF36D9D1).copy(alpha = .90f))
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardHeight = approvedMoneyCardHeight(maxWidth.value).dp
            val compact = maxWidth < 350.dp
            // Correct placement rather than oversized drawings: left 66% of
            // the card holds figures, right 34% holds the ring.
            val gaugeSize = (maxWidth * .305f).coerceIn(100.dp, 120.dp)
            val headingTextSize = if (periodLabel.contains("salary cycle", true)) 12.sp
                else if (compact) 13.sp else 15.sp
            val metricSize = if (compact) 14.sp else 15.sp

            Box(Modifier.fillMaxWidth().height(cardHeight)) {
                Canvas(Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val dim = if (dark) 1f else .64f
                    // Reference: deep indigo interior; brighter upper-left
                    // teal atmosphere; cooler upper-right and a thin aqua rim.
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF04766E).copy(alpha = .30f * dim),
                                Color(0xFF063B4B).copy(alpha = .13f * dim),
                                Color.Transparent
                            ),
                            start = Offset(w * .01f, 0f),
                            end = Offset(w * .96f, h * .77f)
                        )
                    )
                    // Background waves are very soft and stay behind the true
                    // bright wave, as on the approved reference.
                    val rearWave = Path().apply {
                        moveTo(0f, h * .70f)
                        cubicTo(w * .13f, h * .65f, w * .16f, h * .63f, w * .23f, h * .70f)
                        cubicTo(w * .35f, h * .79f, w * .39f, h * .67f, w * .49f, h * .70f)
                        cubicTo(w * .59f, h * .65f, w * .68f, h * .69f, w * .77f, h * .83f)
                        cubicTo(w * .88f, h * .91f, w * .94f, h * .82f, w, h * .69f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = rearWave,
                        brush = Brush.verticalGradient(
                            colors = listOf(aqua.copy(alpha = .06f * dim),
                                aqua.copy(alpha = .12f * dim),
                                aqua.copy(alpha = .015f)),
                            startY = h * .61f, endY = h
                        )
                    )
                    // Coordinates taken from the APPROVED card rather than
                    // the earlier generic wavy line:
                    // peak 1 ~ (0.16, 0.69), valley ~ (0.35, 0.83),
                    // peak 2 ~ (0.60, 0.70), deep valley next to ring
                    // ~ (0.82, 0.88), rising toward the bottom-right edge.
                    val wave = Path().apply {
                        moveTo(0f, h * .80f)
                        cubicTo(w * .07f, h * .77f, w * .11f, h * .65f, w * .17f, h * .69f)
                        cubicTo(w * .25f, h * .68f, w * .27f, h * .83f, w * .35f, h * .83f)
                        cubicTo(w * .45f, h * .85f, w * .51f, h * .68f, w * .60f, h * .71f)
                        cubicTo(w * .69f, h * .73f, w * .72f, h * .90f, w * .83f, h * .88f)
                        cubicTo(w * .90f, h * .88f, w * .95f, h * .78f, w, h * .73f)
                    }
                    val filled = Path().apply {
                        addPath(wave)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        filled,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                aqua.copy(alpha = .40f * dim),
                                Color(0xFF068F84).copy(alpha = .22f * dim),
                                Color(0xFF03766B).copy(alpha = .05f)
                            ),
                            startY = h * .67f,
                            endY = h
                        )
                    )
                    // Broad + narrow strokes simulate the approved luminous
                    // line while remaining GPU-friendly vector primitives.
                    drawPath(wave, color = aqua.copy(alpha = .085f * dim),
                        style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(wave, color = aqua.copy(alpha = .26f * dim),
                        style = Stroke(width = 6.5.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(wave, color = Color(0xFF36F5E2),
                        style = Stroke(width = 2.3.dp.toPx(), cap = StrokeCap.Round))
                    // Subtle reference-image vertical dotted guides and the
                    // highlighted second crest; decorative, not a trend chart.
                    val guideStroke = Stroke(
                        width = 0.7.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(2.5.dp.toPx(), 5.dp.toPx())
                        )
                    )
                    listOf(
                        .17f to .69f, .35f to .83f,
                        .60f to .71f, .83f to .88f
                    ).forEach { (x, y) ->
                        drawLine(
                            color = aqua.copy(alpha = .18f * dim),
                            start = Offset(w * x, h * y),
                            end = Offset(w * x, h * .97f),
                            strokeWidth = guideStroke.width,
                            pathEffect = guideStroke.pathEffect
                        )
                    }
                    drawCircle(
                        aqua.copy(alpha = .16f), radius = 14.dp.toPx(),
                        center = Offset(w * .60f, h * .71f)
                    )
                    drawCircle(
                        aqua.copy(alpha = .48f), radius = 8.dp.toPx(),
                        center = Offset(w * .60f, h * .71f)
                    )
                    drawCircle(
                        Color(0xFF71FFF1), radius = 3.dp.toPx(),
                        center = Offset(w * .60f, h * .71f)
                    )
                }

                // The month belongs to the upper period selector. It is
                // deliberately NOT clickable or decorated with a dropdown.
                Row(
                    modifier = Modifier.align(Alignment.TopStart)
                        .fillMaxWidth().padding(start = 12.dp, end = 11.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = aqua.copy(alpha = .11f),
                        border = BorderStroke(1.dp, aqua.copy(alpha = .30f))
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(20.dp),
                            tint = aqua
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
                        color = foreground,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = aqua.copy(alpha = .12f),
                        border = BorderStroke(1.dp, aqua.copy(alpha = .38f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, null,
                                Modifier.size(13.dp), tint = aqua)
                            Text(badge, style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp, maxLines = 1, color = aqua)
                        }
                    }
                }

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
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                            .padding(start = 13.dp, end = 11.dp, top = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("Money in", style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp, color = secondary)
                            Text(
                                formatActivityMoney(income),
                                fontSize = metricSize,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                color = foreground
                            )
                            PercentChange(periodPercentChange(
                                income, previous?.summary?.incomeMinor ?: 0L))
                        }
                        Column(
                            modifier = Modifier.weight(1.20f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("Money out", style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp, color = secondary)
                            Text(
                                formatActivityMoney(spent),
                                fontSize = metricSize,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                color = foreground
                            )
                            PercentChange(periodPercentChange(
                                spent, previous?.summary?.spentMinor ?: 0L))
                        }

                        // The ring is 30.5% of card width, matching the source.
                        // It is drawn ABOVE the wave; its center never clips.
                        Box(
                            Modifier.size(gaugeSize).offset(y = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.matchParentSize()) {
                                val strokeWidth = 8.5.dp.toPx()
                                val pad = strokeWidth * .95f
                                val arcSize = androidx.compose.ui.geometry.Size(
                                    size.width - pad * 2, size.height - pad * 2
                                )
                                val top = Offset(pad, pad)
                                drawCircle(
                                    color = aqua.copy(alpha = .035f),
                                    radius = size.minDimension * .485f
                                )
                                drawArc(
                                    color = Color(0xFF304355),
                                    startAngle = 135f, sweepAngle = 270f,
                                    useCenter = false, topLeft = top, size = arcSize,
                                    style = Stroke(width = strokeWidth,
                                        cap = StrokeCap.Round)
                                )
                                if (progress != null && progress > 0f) {
                                    drawArc(
                                        color = aqua.copy(alpha = .18f),
                                        startAngle = 135f, sweepAngle = 270f * progress,
                                        useCenter = false, topLeft = top, size = arcSize,
                                        style = Stroke(width = strokeWidth + 5.dp.toPx(),
                                            cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            listOf(aqua.copy(alpha = .70f),
                                                Color(0xFF32F6E5), aqua)
                                        ),
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
                                modifier = Modifier.padding(horizontal = 9.dp)
                            ) {
                                Text(
                                    "Net flow",
                                    fontSize = 10.sp, lineHeight = 12.sp,
                                    color = secondary, maxLines = 1
                                )
                                Text(
                                    net?.let(::formatActivityMoney) ?: "Unavailable",
                                    fontSize = if (compact) 8.sp else 9.sp,
                                    lineHeight = 10.sp,
                                    maxLines = 2,
                                    fontWeight = FontWeight.Bold,
                                    color = if (net != null && net < 0L)
                                        MaterialTheme.colorScheme.error else foreground,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                IconButton(
                                    onClick = onExplain,
                                    modifier = Modifier.size(25.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Info, "Explain net flow",
                                        Modifier.size(15.dp), tint = aqua
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
@Composable private fun PercentChange(change:Int?) {
    if(change==null) {
        Text("No prior comparison",style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
    } else Row(horizontalArrangement=Arrangement.spacedBy(2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Icon(if(change<0)Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
            null,Modifier.size(13.dp),tint=if(change<0)violet else aqua)
        Text((if(change>0)"+" else "")+change.toString()+"%",
            style=MaterialTheme.typography.labelSmall,color=if(change<0)violet else aqua)
        Text("vs prior",style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
    }
}
@Composable
internal fun ActivityCompactFilters(selected:String,count:Int,onSelect:(String)->Unit) {
    Surface(shape=RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            listOf("All","Reviews","Pending","Transfers").forEach { tab ->
                val active=selected==tab
                Surface(onClick={onSelect(tab)},modifier=Modifier.weight(1f).heightIn(min=42.dp),
                    shape=RoundedCornerShape(15.dp),
                    color=if(active)aqua.copy(alpha=.17f) else Color.Transparent,
                    border=if(active)BorderStroke(1.dp,aqua) else null) {
                    Box(contentAlignment=Alignment.Center) {
                        Row(verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(tab,style=MaterialTheme.typography.labelMedium,maxLines=1,
                                fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)
                            if(tab=="Reviews" && count>0) {
                                Surface(shape=CircleShape,color=violet) {
                                    Text(count.toString(),Modifier.padding(horizontal=5.dp,vertical=1.dp),
                                        style=MaterialTheme.typography.labelSmall,color=Color(0xFF200E2F))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
