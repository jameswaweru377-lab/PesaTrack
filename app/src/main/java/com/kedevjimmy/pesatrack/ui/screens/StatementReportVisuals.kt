package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.everyday.StatementHistoryReport
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

internal val statementPalette = listOf(
    Color(0xFF5B55E7), Color(0xFF786FF0), Color(0xFF9188F4),
    Color(0xFFA69CFF), Color(0xFFB7AEFF), Color(0xFFC9C2FF)
)

internal fun statementMoney(minor: Long): String =
    "KES " + NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(minor, 2))

@Composable
internal fun StatementHeroMetric(label: String, amount: Long, note: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(statementMoney(amount), style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(note, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
internal fun StatementSmallMetric(
    name: String, amount: Long, color: Color,
    modifier: Modifier = Modifier, detail: String? = null, onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(name, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(statementMoney(amount), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun StatementCategoryLine(
    title: String, amount: Long, total: Long, index: Int,
    onClick: () -> Unit
) {
    val color = statementPalette[index % statementPalette.size]
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).background(color, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(9.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f), maxLines = 1)
            Text(statementMoney(amount), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { (amount.toDouble() / total.coerceAtLeast(1L)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
internal fun StatementDonut(
    values: List<Pair<String, Long>>,
    total: Long,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 24.dp.toPx()
            val diameter = minOf(size.width, size.height) - stroke
            val origin = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(
                color = trackColor, startAngle = 0f, sweepAngle = 360f,
                useCenter = false, topLeft = origin, size = Size(diameter, diameter),
                style = Stroke(width = stroke)
            )
            var start = -90f
            values.forEachIndexed { i, pair ->
                val sweep = 360f * (pair.second.toDouble() / total.coerceAtLeast(1L)).toFloat()
                if (sweep > 0f) {
                    drawArc(
                        color = statementPalette[i % statementPalette.size],
                        startAngle = start, sweepAngle = sweep, useCenter = false,
                        topLeft = origin, size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spent", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(statementMoney(total), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun StatementMonthlyChart(
    months: List<StatementHistoryReport.SelectedMonth>,
    onMonth: (String) -> Unit
) {
    val byMonth = months.associateBy { it.usage.month }
    val maximum = maxOf(1L, months.maxOfOrNull { it.usage.paidOutMinor } ?: 0L)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Money out by month", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text("Tap a bar to open that month. Grey bars mean no statement.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val names = listOf("J","F","M","A","M","J","J","A","S","O","N","D")
            (1..12).forEach { month ->
                val entry = byMonth[month]
                Column(Modifier.weight(1f).fillMaxHeight()
                    .then(if (entry == null) Modifier else Modifier.clickable { onMonth(entry.key) }),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    val height = if (entry == null) 9.dp else
                        (10 + 105 * (entry.usage.paidOutMinor.toDouble() / maximum)).dp
                    Box(Modifier.fillMaxWidth().height(height).background(
                        if (entry == null) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                    ))
                    Spacer(Modifier.height(7.dp))
                    Text(names[month - 1], style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}


@Composable
internal fun StatementTrendBars(
    buckets: List<com.kedevjimmy.pesatrack.domain.everyday.StatementTrendBuckets.Bucket>,
    onBar: (Int) -> Unit
) {
    val maximum = maxOf(1L, buckets.maxOfOrNull { it.amountMinor } ?: 0L)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(185.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            buckets.forEach { b ->
                Column(Modifier.weight(1f).fillMaxHeight()
                    .then(if (b.observed) Modifier.clickable { onBar(b.index) } else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    val height = if (!b.observed) 8.dp
                        else (12 + 132 * (b.amountMinor.toDouble() / maximum)).dp
                    Box(Modifier.fillMaxWidth().height(height).background(
                        if (b.observed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                    ))
                    Spacer(Modifier.height(9.dp))
                    Text(b.label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text("Grey means no statement evidence, not zero spending.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
