package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Presentation-only PesaTrack Plan tokens. Values and navigation are supplied by each owner screen. */
internal object PlanVibrant {
    val ink = Color(0xFF10113C)
    val muted = Color(0xFF666D95)
    val violet = Color(0xFF6416F5)
    val violetDeep = Color(0xFF3900C3)
    val mint = Color(0xFF1EBC93)
    val blue = Color(0xFF267BFF)
    val pink = Color(0xFFEC337D)
    val orange = Color(0xFFFF7137)
    val canvas = Color(0xFFF4F2FF)
}
internal fun planVibrantTone(tone: String): Pair<Color,Color> = when (tone) {
    "mint" -> Color(0xFFDEFAEF) to Color(0xFF058B68)
    "blue" -> Color(0xFFE4EFFF) to PlanVibrant.blue
    "pink" -> Color(0xFFFFE5EE) to PlanVibrant.pink
    "orange" -> Color(0xFFFFEDDF) to PlanVibrant.orange
    else -> Color(0xFFEDE6FF) to PlanVibrant.violet
}

@Composable
internal fun VibrantPlanPageTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 27.sp, lineHeight = 32.sp,
            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, fontSize = 13.sp, lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun VibrantPlanSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)),
        shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun VibrantPlanSectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 17.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(action, color = PlanVibrant.violet, fontSize = 13.sp)
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
                modifier = Modifier.size((size * .50f).dp))
        }
    }
}

@Composable
internal fun VibrantPlanHero(
    period: String, available: String, safeToday: String,
    onCycleClick: (() -> Unit)?
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 207.dp)
        .background(Brush.linearGradient(listOf(Color(0xFF721AFF),
            Color(0xFF4C0ACA), Color(0xFF0E49C6))),
            RoundedCornerShape(24.dp))) {
        // Translucent decorative circles reproduce the approved Home/Plan ambient glow.
        Box(Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-33).dp)
            .size(154.dp).background(Color(0xFF02D5F0).copy(alpha = .36f), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 65.dp, y = 55.dp)
            .size(200.dp).background(Color(0xFF8837FF).copy(alpha = .39f), CircleShape))
        Column(Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("This pay cycle", color = Color.White.copy(alpha = .94f),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(period, color = Color.White.copy(alpha = .86f), fontSize = 12.sp)
            Text(available, color = Color.White, fontSize = 33.sp,
                lineHeight = 39.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Available after commitments", color = Color.White.copy(alpha = .90f),
                fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = .94f), modifier = Modifier.weight(1f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Rounded.Shield, null, Modifier.size(17.dp),
                            tint = Color(0xFF058B68))
                        Text("Safe today", fontSize = 11.sp, color = PlanVibrant.ink)
                        Text(safeToday, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = PlanVibrant.ink, maxLines = 1)
                    }
                }
                if (onCycleClick != null) {
                    Surface(shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = .17f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .35f)),
                        modifier = Modifier.clickable(onClick = onCycleClick)) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("View cycle", color = Color.White,
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(17.dp), tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun VibrantDestinationTile(icon: ImageVector, label: String,
    status: String, tone: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.heightIn(min = 91.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .43f))) {
        Row(Modifier.fillMaxWidth().padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VibrantPlanIcon(icon, tone, size = 42)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(status, fontSize = 11.sp, lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun VibrantPlanListRow(icon: ImageVector, tone: String, label: String,
    detail: String, amount: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().then(if(onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .heightIn(min = 55.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VibrantPlanIcon(icon, tone, size = 43)
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(amount, color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun VibrantPlanMeter(label: String, detail: String, progress: Float,
    icon: ImageVector, tone: String) {
    val (bg,color) = planVibrantTone(tone)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(Modifier.size(29.dp), color = bg, shape = RoundedCornerShape(9.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(17.dp), tint = color)
            }
        }
        Text(label, Modifier.widthIn(min = 79.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        LinearProgressIndicator(progress = { progress.coerceIn(0f,1f) },
            modifier = Modifier.weight(1f).height(7.dp),
            color = color, trackColor = Color(0xFFEAE7F8), strokeCap = StrokeCap.Round)
        Text(detail, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
            val width = 13.dp.toPx()
            val inset = width / 2 + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(Color(0xFFE7E0F8),-90f,360f,false,Offset(inset,inset),
                arcSize,style=Stroke(width, cap=StrokeCap.Round))
            if (p > 0) drawArc(Brush.sweepGradient(listOf(
                PlanVibrant.violet,Color(0xFFAD65FF), PlanVibrant.violet)),
                -90f,360f*p,false,Offset(inset,inset),
                arcSize,style=Stroke(width, cap=StrokeCap.Round))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text(valueLabel,color=MaterialTheme.colorScheme.onSurface,
                fontSize=19.sp,fontWeight=FontWeight.ExtraBold)
            Text(subtitle,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
