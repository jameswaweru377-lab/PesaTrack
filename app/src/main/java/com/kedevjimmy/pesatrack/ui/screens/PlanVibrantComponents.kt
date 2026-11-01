package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Screen-local tokens matching the approved premium Plan reference. */
internal object PlanVibrant {
    val ink = Color(0xFF10113C)
    val muted = Color(0xFF666D95)
    val violet = Color(0xFF6416F5)
    val violetDeep = Color(0xFF3900C3)
    val mint = Color(0xFF10AC82)
    val blue = Color(0xFF187DFF)
    val pink = Color(0xFFEC337D)
    val orange = Color(0xFFFF7137)
    val canvas = Color(0xFFF6F5FF)
    val outline = Color(0xFFE7E4F6)
}
internal fun planVibrantTone(tone: String): Pair<Color, Color> = when (tone) {
    "mint" -> Color(0xFFDEFAEF) to Color(0xFF058B68)
    "blue" -> Color(0xFFE4EFFF) to PlanVibrant.blue
    "pink" -> Color(0xFFFFE5EE) to PlanVibrant.pink
    "orange" -> Color(0xFFFFEDDF) to PlanVibrant.orange
    else -> Color(0xFFEDE6FF) to PlanVibrant.violet
}

@Composable
internal fun VibrantPlanPageTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(title, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold,
            color = PlanVibrant.ink)
        Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = PlanVibrant.muted)
    }
}

@Composable
internal fun VibrantPlanDateRow(
    period: String,
    onCycleClick: (() -> Unit)?,
    onInsightsClick: (() -> Unit)?
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(modifier = Modifier.weight(1f).heightIn(min = 44.dp)
            .then(if (onCycleClick != null) Modifier.clickable(onClick = onCycleClick) else Modifier),
            shape = RoundedCornerShape(15.dp), color = Color.White,
            shadowElevation = 3.dp, border = BorderStroke(1.dp, PlanVibrant.outline)) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null,
                    tint = Color(0xFF34428D), modifier = Modifier.size(20.dp))
                Text(period, Modifier.weight(1f), color = PlanVibrant.ink,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (onCycleClick != null) Icon(Icons.Rounded.ExpandMore, null,
                    Modifier.size(19.dp), tint = PlanVibrant.ink)
            }
        }
        Surface(modifier = Modifier.size(44.dp)
            .then(if (onInsightsClick != null) Modifier.clickable(onClick = onInsightsClick) else Modifier),
            shape = RoundedCornerShape(16.dp), color = Color.White,
            shadowElevation = 3.dp, border = BorderStroke(1.dp, PlanVibrant.outline)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Analytics, contentDescription = "View planning insights",
                    tint = PlanVibrant.violet, modifier = Modifier.size(23.dp))
            }
        }
    }
}

@Composable
internal fun VibrantPlanSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp),
        color = Color.White, border = BorderStroke(1.dp, PlanVibrant.outline),
        shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun VibrantPlanSectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold, color = PlanVibrant.ink)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(action, color = PlanVibrant.violet, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(15.dp), tint = PlanVibrant.violet)
            }
        }
    }
}

@Composable
internal fun VibrantPlanIcon(icon: ImageVector, tone: String, modifier: Modifier = Modifier, size: Int = 43) {
    val (bg, tint) = planVibrantTone(tone)
    Surface(modifier.size(size.dp), shape = RoundedCornerShape((size * .29f).dp), color = bg) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint,
                modifier = Modifier.size((size * .52f).dp))
        }
    }
}

/** Vector illustration: calendar, checks and gold coin stack, drawn locally without remote assets. */
@Composable
private fun PlanCalendarCoinsIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Calendar with checked payments and gold coins" }) {
        val sx = size.width / 120f
        val sy = size.height / 112f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        fun box(x: Float, y: Float, w: Float, h: Float, c: Color, radius: Float) {
            drawRoundRect(color = c, topLeft = point(x, y),
                size = Size(w * sx, h * sy), cornerRadius = CornerRadius(radius * sx))
        }
        // Floating calendar and its pale-blue header.
        box(15f, 12f, 82f, 82f, Color(0xFFDED7FF).copy(alpha = .24f), 14f)
        box(18f, 10f, 79f, 78f, Color(0xFFFCFAFF), 12f)
        box(18f, 10f, 79f, 19f, Color(0xFFB1A5FF), 11f)
        listOf(34f, 79f).forEach { x ->
            drawLine(Color(0xFF2B36AD), point(x, 6f), point(x, 17f),
                strokeWidth = 5f * sx, cap = StrokeCap.Round)
        }
        val cells = listOf(37f to 41f, 65f to 41f, 37f to 65f, 65f to 65f)
        cells.forEachIndexed { index, (x, y) ->
            box(x, y, 15f, 12f, if (index == 1) Color(0xFFE4E0FF)
                else Color(0xFFC7BEFC), 3f)
        }
        listOf(37f to 41f, 65f to 41f, 65f to 65f).forEach { (x,y) ->
            drawLine(Color(0xFF3E53E7), point(x+2f,y+6f), point(x+6f,y+9f),
                2.4f*sx, cap=StrokeCap.Round)
            drawLine(Color(0xFF3E53E7), point(x+6f,y+9f), point(x+14f,y+2f),
                2.4f*sx, cap=StrokeCap.Round)
        }
        // Three glossy coin stacks, separated so they remain legible on small displays.
        for (i in 0..2) {
            val y = 88f - i * 9f
            drawOval(Color(0xFFEF9B04), point(75f,y+6f), Size(37f*sx, 14f*sy))
            box(75f,y,37f,11f, Color(0xFFFFB921), 5f)
            drawOval(Color(0xFFFFDA64), point(76f,y), Size(35f*sx, 8f*sy))
            drawLine(Color(0xFFFFE487), point(81f,y+2f), point(102f,y+2f), 1.2f*sx)
        }
    }
}

@Composable
internal fun VibrantPlanHero(
    period: String,
    available: String,
    safeToday: String?,
    safeTodayPositive: Boolean,
    onCycleClick: (() -> Unit)?
) {
    Box(
        Modifier.fillMaxWidth().height(176.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(
                Color(0xFF790FFF), Color(0xFF3B0FD0), Color(0xFF1157E5)
            )))
            .semantics {
                contentDescription = "This pay cycle: $period. $available available after commitments. " +
                    if (safeToday == null) "Daily guide unavailable." else "Safe today: $safeToday."
            }
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-76).dp).size(155.dp)
                .background(Brush.linearGradient(listOf(
                    Color(0xFF13E5FF), Color(0xFF118DFF), Color(0xFF5144FF)
                )), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 75.dp, y = 90.dp).size(200.dp)
                .background(Color(0xFF9C40FF).copy(alpha = .30f), CircleShape)
        )
        // Illustration lives above the button; no overlaying or clipping the CTA.
        PlanCalendarCoinsIllustration(
            Modifier.align(Alignment.TopEnd)
                .padding(top = 15.dp, end = 12.dp)
                .size(width = 106.dp, height = 98.dp)
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Text(
                "THIS PAY CYCLE", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = .94f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                available, color = Color.White, fontSize = 30.sp, lineHeight = 35.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(.71f)
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Available after commitments", color = Color.White,
                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Rounded.Info,
                    contentDescription = "This is a planning amount, not a bank balance",
                    modifier = Modifier.size(15.dp), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = if (safeTodayPositive) Color(0xFFE1FFF0) else Color(0xFFF3E9FF),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            if (safeTodayPositive) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (safeTodayPositive) Color(0xFF009A6F)
                                else Color(0xFF6422BF)
                        )
                        Text(
                            if (safeToday == null) "Daily guide unavailable"
                            else "Safe today · $safeToday",
                            fontSize = 10.sp, color = PlanVibrant.ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (onCycleClick != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(19.dp),
                        color = Color.White.copy(alpha = .16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .55f)),
                        modifier = Modifier.clickable(onClick = onCycleClick)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("View cycle", color = Color.White, fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Rounded.ChevronRight, null,
                                Modifier.size(16.dp), tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun VibrantPlanSourceWarning(
    message: String,
    onClick: () -> Unit,
    expanded: Boolean = false,
    title: String = "Some spending still needs a source"
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF2D2),
        border = BorderStroke(1.dp, Color(0xFFFFEBBC))
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null,
                modifier = Modifier.size(25.dp), tint = Color(0xFF9C6510))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = PlanVibrant.ink, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message, color = Color(0xFF6E6B77),
                    fontSize = 11.sp, lineHeight = 15.sp
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) "Collapse explanation" else "Explain missing source",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF4D5286)
            )
        }
    }
}

@Composable
internal fun VibrantDestinationTile(
    icon: ImageVector, label: String, status: String, tone: String,
    onClick: (() -> Unit)?, modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.heightIn(min = 62.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Color.White, shape = RoundedCornerShape(17.dp), shadowElevation = 3.dp,
        border = BorderStroke(1.dp, PlanVibrant.outline)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VibrantPlanIcon(icon, tone, size = 39)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = PlanVibrant.ink, maxLines = 1)
                Text(status, fontSize = 10.sp, lineHeight = 13.sp,
                    color = PlanVibrant.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(15.dp),
                tint = Color(0xFF686D9C))
        }
    }
}

@Composable
internal fun VibrantPlanListRow(icon: ImageVector, tone: String, label: String,
    detail: String, amount: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().then(if(onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .heightIn(min = 51.dp).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        VibrantPlanIcon(icon, tone, size = 38)
        Column(Modifier.weight(1f)) {
            Text(label, color = PlanVibrant.ink, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = PlanVibrant.muted,
                fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(amount, color = PlanVibrant.ink,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(17.dp),
            tint = Color(0xFF68729C))
    }
}

@Composable
internal fun VibrantPlanMeter(
    label: String, detail: String, progress: Float,
    icon: ImageVector, tone: String, hasData: Boolean = true
) {
    val (bg, color) = planVibrantTone(tone)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(Modifier.size(24.dp), color = bg, shape = RoundedCornerShape(8.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(15.dp), tint = color)
            }
        }
        Text(
            label, Modifier.widthIn(min = 54.dp), fontSize = 10.sp,
            color = PlanVibrant.ink, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        // An unknown or zero value must not draw a misleading active dot.
        Box(
            Modifier.weight(1f).height(7.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFFEAE7F8))
        ) {
            if (hasData && progress > 0f) {
                Box(
                    Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(7.dp))
                )
            }
        }
        Text(
            detail, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF3F457C), maxLines = 1
        )
    }
}

/** Progress values come from trusted budget, savings and bill evidence, never mock UI percentages. */
@Composable
internal fun VibrantPlanProgressCard(
    periodLabel: String,
    onPeriodClick: () -> Unit,
    budgetPercent: Int?,
    goalPercent: Int?,
    billsPercent: Int?,
    budgetLabel: String = "Budgets",
    goalLabel: String = "Goals",
    billsLabel: String = "Bills",
    ringLabel: String,
    modifier: Modifier = Modifier
) {
    VibrantPlanSurface(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Plan progress", Modifier.weight(1f), fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold, color = PlanVibrant.ink)
            Icon(Icons.Rounded.Info, contentDescription = "Progress reflects recorded data",
                tint = Color(0xFF3A4C9E), modifier = Modifier.size(16.dp))
            Spacer(Modifier.weight(.15f))
            TextButton(onClick = onPeriodClick,
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)) {
                Text(periodLabel, fontSize = 11.sp, color = PlanVibrant.ink,
                    fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.ExpandMore, null, Modifier.size(16.dp), tint = PlanVibrant.ink)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VibrantRing(
                percent = (budgetPercent ?: 0).coerceIn(0, 100) / 100f,
                modifier = Modifier.size(104.dp),
                valueLabel = budgetPercent?.let { "$it%" } ?: "—",
                subtitle = if (budgetPercent == null) "No data" else ringLabel
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VibrantPlanMeter(budgetLabel, budgetPercent?.let { "$it%" } ?: "—",
                    (budgetPercent ?: 0).coerceIn(0, 100) / 100f,
                    Icons.Rounded.Analytics, "violet", hasData = budgetPercent != null)
                VibrantPlanMeter(goalLabel, goalPercent?.let { "$it%" } ?: "—",
                    (goalPercent ?: 0).coerceIn(0, 100) / 100f,
                    Icons.Rounded.CheckCircle, "mint", hasData = goalPercent != null)
                VibrantPlanMeter(billsLabel, billsPercent?.let { "$it%" } ?: "—",
                    (billsPercent ?: 0).coerceIn(0, 100) / 100f,
                    Icons.Rounded.CalendarMonth, "blue", hasData = billsPercent != null)
            }
        }
    }
}

@Composable
internal fun VibrantActionButton(label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().heightIn(min = 50.dp)
        .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), color = PlanVibrant.violet) {
        Box(Modifier.background(Brush.horizontalGradient(listOf(
            Color(0xFF792CFF), Color(0xFF5600DB))))
            .padding(horizontal = 14.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun VibrantRing(percent: Float, modifier: Modifier = Modifier,
    valueLabel: String, subtitle: String) {
    val p = percent.coerceIn(0f,1f)
    Box(modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = 12.dp.toPx()
            val inset = width / 2 + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(Color(0xFFE6DCFC), -90f, 360f, false, Offset(inset,inset),
                arcSize, style=Stroke(width, cap=StrokeCap.Round))
            if (p > 0f) drawArc(Brush.sweepGradient(listOf(
                Color(0xFF6920EF), Color(0xFF2724FF), Color(0xFFA062FF), Color(0xFF6920EF))),
                -90f, 360f*p, false, Offset(inset,inset),
                arcSize, style=Stroke(width, cap=StrokeCap.Round))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text(valueLabel, color=PlanVibrant.ink,
                fontSize=19.sp, fontWeight=FontWeight.ExtraBold)
            Text(subtitle, fontSize=10.sp, color=PlanVibrant.muted, maxLines=1)
        }
    }
}


/**
 * Approved Plan details replacement (2026-11-01).
 *
 * This composable only presents the exact values given by PlanScreen. It deliberately
 * does not calculate balances, infer income, or alter the trusted planning policy.
 * The header stays collapsed by default and is the only expansion affordance.
 */
@Composable
internal fun VibrantPlanMoneyBreakdown(
    expanded: Boolean,
    onToggle: () -> Unit,
    startingFunds: String,
    received: String,
    cashPaid: String,
    fees: String,
    saved: String,
    committed: String,
    available: String
) {
    val ink = Color(0xFF11123F)
    val muted = Color(0xFF686B9C)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFFFDFDFF),
        border = BorderStroke(1.dp, Color(0xFFEAE5F8)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = 17.dp, vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics {
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                        contentDescription = "Your money breakdown. How your available money is calculated"
                    }
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = Color(0xFFE3DFFF),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = null,
                            tint = Color(0xFF5B10E7),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Your money breakdown",
                        color = ink, fontSize = 18.sp, lineHeight = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "How your available money is calculated",
                        color = muted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess
                        else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF594E9B),
                    modifier = Modifier.size(25.dp)
                )
            }
            if (expanded) {
                Spacer(Modifier.height(15.dp))
                VibrantBreakdownRow(
                    Icons.Rounded.Toll, Color(0xFFDEF8ED), Color(0xFF008D63),
                    "Starting funds", startingFunds, Color(0xFF075D41)
                )
                HorizontalDivider(color = Color(0xFFE9E5F8), thickness = 1.dp)
                VibrantBreakdownRow(
                    Icons.Rounded.Download, Color(0xFFDEFAEB), Color(0xFF00A474),
                    "Other money received", received, Color(0xFF075D41)
                )
                HorizontalDivider(color = Color(0xFFE9E5F8), thickness = 1.dp)
                VibrantBreakdownRow(
                    Icons.Rounded.Upload, Color(0xFFFFE3EC), Color(0xFFE8397A),
                    "Cash paid", cashPaid, ink
                )
                HorizontalDivider(color = Color(0xFFE9E5F8), thickness = 1.dp)
                VibrantBreakdownRow(
                    Icons.Rounded.ReceiptLong, Color(0xFFFFEBDD), Color(0xFFF07A43),
                    "Fees", fees, ink
                )
                HorizontalDivider(color = Color(0xFFE9E5F8), thickness = 1.dp)
                VibrantBreakdownRow(
                    Icons.Rounded.Savings, Color(0xFFE1EFFF), Color(0xFF137EDB),
                    "Moved to savings", saved, ink
                )
                HorizontalDivider(color = Color(0xFFE9E5F8), thickness = 1.dp)
                VibrantBreakdownRow(
                    Icons.Rounded.EventAvailable, Color(0xFFEEE5FF), Color(0xFF6D20E9),
                    "Still committed", committed, ink
                )
                Spacer(Modifier.height(11.dp))
                VibrantBreakdownResult(available)
                Spacer(Modifier.height(15.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color(0xFF746BAD),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Based on salary, spending, savings and commitments for this cycle.",
                        color = muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VibrantBreakdownRow(
    icon: ImageVector,
    background: Color,
    tint: Color,
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 61.dp)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Surface(
            modifier = Modifier.size(39.dp),
            color = background,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            label, modifier = Modifier.weight(1f),
            fontSize = 13.sp, lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10123C)
        )
        Text(
            value,
            fontSize = 13.sp, lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2
        )
    }
}

@Composable
private fun VibrantBreakdownResult(available: String) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFF0E9FF),
                        Color(0xFFE5D6FF),
                        Color(0xFFC79CFA)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
                .offset(x = 88.dp, y = 83.dp)
                .size(175.dp)
                .background(Color(0xFFAA75F5).copy(alpha = .22f), CircleShape)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 17.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Available after commitments",
                color = Color(0xFF2A1177),
                fontSize = 14.sp, lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                available, color = Color(0xFF241071),
                fontSize = 31.sp, lineHeight = 37.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Planning amount, not your account balance.",
                color = Color(0xFF585393),
                fontSize = 11.sp, lineHeight = 15.sp
            )
        }
    }
}
