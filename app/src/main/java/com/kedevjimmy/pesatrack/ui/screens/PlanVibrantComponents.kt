package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    period: String, available: String, safeToday: String,
    onCycleClick: (() -> Unit)?
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 170.dp).clip(RoundedCornerShape(22.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF790FFF),
            Color(0xFF3B0FD0), Color(0xFF1157E5))))
        .semantics { contentDescription = "This pay cycle: $period. $available available after commitments." }) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 23.dp, y = (-65).dp).size(148.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF13E5FF),
                Color(0xFF118DFF), Color(0xFF5144FF))), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 86.dp, y = 74.dp).size(208.dp)
            .background(Color(0xFF9C40FF).copy(alpha = .39f), CircleShape))
        PlanCalendarCoinsIllustration(Modifier.align(Alignment.CenterEnd)
            .padding(end = 8.dp, top = 5.dp).size(width = 116.dp, height = 112.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("THIS PAY CYCLE", color = Color.White.copy(alpha = .96f),
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(available, color = Color.White, fontSize = 31.sp, lineHeight = 37.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(.72f))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Available after commitments", color = Color.White, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Rounded.Info, contentDescription = "A planning amount, not a bank balance",
                    modifier = Modifier.size(15.dp), tint = Color.White)
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = Color(0xFFE1FFF0), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp),
                            tint = Color(0xFF009A6F))
                        Text("Safe today · $safeToday", fontSize = 10.sp,
                            color = PlanVibrant.ink, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (onCycleClick != null) {
                    Surface(shape = RoundedCornerShape(19.dp),
                        color = Color.White.copy(alpha = .17f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .45f)),
                        modifier = Modifier.clickable(onClick = onCycleClick)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("View cycle", color = Color.White,
                                fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(17.dp), tint = Color.White)
                        }
                    }
                }
            }
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
internal fun VibrantPlanMeter(label: String, detail: String, progress: Float,
    icon: ImageVector, tone: String) {
    val (bg,color) = planVibrantTone(tone)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(Modifier.size(24.dp), color = bg, shape = RoundedCornerShape(8.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(15.dp), tint = color)
            }
        }
        Text(label, Modifier.widthIn(min = 54.dp), fontSize = 10.sp, color = PlanVibrant.ink,
            fontWeight = FontWeight.Medium)
        LinearProgressIndicator(progress = { progress.coerceIn(0f,1f) },
            modifier = Modifier.weight(1f).height(7.dp),
            color = color, trackColor = Color(0xFFEAE7F8), strokeCap = StrokeCap.Round)
        Text(detail, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF3F457C), maxLines = 1)
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
                subtitle = if (budgetPercent == null) "No budget" else ringLabel
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VibrantPlanMeter("Budgets", budgetPercent?.let { "$it%" } ?: "—",
                    (budgetPercent ?: 0).coerceIn(0, 100) / 100f,
                    Icons.Rounded.Analytics, "violet")
                VibrantPlanMeter("Goals", goalPercent?.let { "$it%" } ?: "—",
                    (goalPercent ?: 0).coerceIn(0, 100) / 100f,
                    Icons.Rounded.CheckCircle, "mint")
                VibrantPlanMeter("Bills", billsPercent?.let { "$it%" } ?: "—",
                    (billsPercent ?: 0).coerceIn(0, 100) / 100f,
                    Icons.Rounded.CalendarMonth, "blue")
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
