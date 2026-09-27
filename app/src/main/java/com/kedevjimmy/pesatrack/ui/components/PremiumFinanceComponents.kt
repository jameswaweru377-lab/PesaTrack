package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PremiumCyan
import com.kedevjimmy.pesatrack.ui.theme.PremiumGold
import com.kedevjimmy.pesatrack.ui.theme.PremiumPurple
import com.kedevjimmy.pesatrack.ui.theme.PremiumTeal

data class SpendingSlice(
    val label: String,
    val amountMinor: Long,
    val color: Color
)

@Composable
fun PesaTrackTopBar(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandWordmark(
            modifier = Modifier.weight(1f),
            compact = true
        )
        Icon(
            Icons.Rounded.NotificationsNone,
            contentDescription = "Notifications",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(23.dp)
        )
        Surface(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(40.dp)
                .clickable(onClick = onProfileClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
fun AvailableToLiveHero(
    amount: String,
    subtitle: String,
    footer: String?,
    modifier: Modifier = Modifier
) {
    val dark = isPesaDarkTheme()
    val start = if (dark) Color(0xFF06453E) else Color(0xFF087A5B)
    val end = if (dark) Color(0xFF0A6A59) else Color(0xFF0A9B73)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(start, end)))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = .06f),
                radius = size.minDimension * .38f,
                center = Offset(size.width * .92f, size.height * .16f)
            )
            drawCircle(
                color = PremiumGold.copy(alpha = .13f),
                radius = size.minDimension * .19f,
                center = Offset(size.width * .86f, size.height * .77f)
            )
            drawLine(
                color = Color.White.copy(alpha = .16f),
                start = Offset(size.width * .68f, size.height * .84f),
                end = Offset(size.width * .88f, size.height * .48f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                "Available to live",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = .88f)
            )
            Text(
                amount,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                subtitle,
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = .82f)
            )
            Spacer(Modifier.weight(1f))
            if (!footer.isNullOrBlank()) {
                Text(
                    footer,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = .72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MoneySummaryTile(
    label: String,
    amount: String,
    incoming: Boolean,
    modifier: Modifier = Modifier
) {
    val tone = if (incoming) "green" else "red"
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = pesaToneContainer(tone)
    ) {
        Column(Modifier.padding(14.dp)) {
            IconBubble(
                if (incoming) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                size = 34.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = .68f),
                tint = pesaToneContent(tone)
            )
            Text(
                label,
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                amount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "This month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickAction(
    label: String,
    icon: ImageVector,
    tone: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBubble(
            icon = icon,
            size = 48.dp,
            background = pesaToneContainer(tone),
            tint = pesaToneContent(tone)
        )
        Text(
            label,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun SpendingDonutCard(
    slices: List<SpendingSlice>,
    totalLabel: String,
    modifier: Modifier = Modifier
) {
    val filtered = slices.filter { it.amountMinor > 0L }
    val total = filtered.sumOf { it.amountMinor }.coerceAtLeast(1L)

    PesaCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text("Spending this month", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(126.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(126.dp)) {
                        var startAngle = -90f
                        filtered.forEach { slice ->
                            val sweep =
                                360f * slice.amountMinor.toFloat() / total.toFloat()
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(
                                    width = 20.dp.toPx(),
                                    cap = StrokeCap.Butt
                                )
                            )
                            startAngle += sweep
                        }
                        if (filtered.isEmpty()) {
                            drawArc(
                                color = Color.Gray.copy(alpha = .3f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx())
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "KES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            totalLabel.removePrefix("KES "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filtered.take(5).forEach { slice ->
                        val pct = ((slice.amountMinor * 100L) / total).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = CircleShape,
                                color = slice.color
                            ) {}
                            Text(
                                slice.label,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                pct.toString() + "%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartInsightCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    val dark = isPesaDarkTheme()
    val colors = if (dark) {
        listOf(Color(0xFF2C2516), Color(0xFF3A2D18))
    } else {
        listOf(Color(0xFFFFF6DA), Color(0xFFFFEDB8))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(colors))
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                icon = Icons.Rounded.Lightbulb,
                size = 42.dp,
                background = PremiumGold.copy(alpha = .18f),
                tint = PremiumGold
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun premiumCategoryColor(index: Int): Color =
    listOf(
        PremiumTeal,
        PremiumCyan,
        PremiumGold,
        PremiumPurple,
        Color(0xFFFF7E67),
        Color(0xFF6FA8FF)
    )[index % 6]
