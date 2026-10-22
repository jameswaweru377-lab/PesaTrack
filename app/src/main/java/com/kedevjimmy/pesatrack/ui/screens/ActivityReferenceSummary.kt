package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import java.math.BigDecimal
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import java.text.NumberFormat
import java.util.Locale

/** A non-zero share smaller than one percent must never display as zero. */
internal fun activityShareLabel(part: Long, total: Long): String {
    if (total <= 0L || part <= 0L) return "0%"
    val pct = part.toDouble() / total.toDouble() * 100.0
    return if (pct < 1.0) "<1%" else "${kotlin.math.round(pct).toInt()}%"
}

/** Exact KES formatting: show cents whenever any chart segment has fractional shillings. */
internal fun activityKesLabel(minor: Long, showCents: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (showCents || minor % 100L != 0L) 2 else 0
        maximumFractionDigits = 2
    }
    return "KES " + formatter.format(BigDecimal.valueOf(minor, 2))
}

internal data class ActivityReferenceSegment(val label: String, val minor: Long, val color: Color)

private val chartColors = listOf(
    Color(0xFF873AFF), Color(0xFF348BFF), Color(0xFF32CCA2),
    Color(0xFFFFA05F), Color(0xFFFF68AD)
)

internal fun activityReferenceSegments(
    transactions: List<StoredTransaction>,
    selected: String,
    categoryName: (String) -> String,
    accountName: (String?) -> String,
    possibleDuplicateIds: Set<Long>
): List<ActivityReferenceSegment> {
    val eligible = transactions.filter { item ->
        item.reviewState != ReviewState.EXCLUDED.name &&
            when (selected) {
                "Money In" -> item.direction == MoneyDirection.INCOME.name
                "Transfers" -> item.direction == MoneyDirection.TRANSFER.name &&
                    item.relationType != TransactionStore.RELATION_REFUND
                "Needs Review" -> item.reviewState == ReviewState.NEEDS_REVIEW.name ||
                    item.id in possibleDuplicateIds
                else -> item.direction == MoneyDirection.EXPENSE.name &&
                    item.relationType != TransactionStore.RELATION_REFUND
            }
    }
    val grouped = eligible.groupBy { item ->
        when (selected) {
            "Transfers" -> accountName(item.accountId) + " → " + accountName(item.linkedAccountId)
            "Needs Review" -> when {
                item.id in possibleDuplicateIds -> "Possible Duplicate"
                item.direction == MoneyDirection.TRANSFER.name -> "Ambiguous Transfer"
                item.category.isBlank() || categoryName(item.category).contains("uncategor", true) ->
                    "Uncategorized"
                item.accountId == null -> "Needs Account Match"
                else -> "Needs Category"
            }
            else -> categoryName(item.category).ifBlank { "Uncategorized" }
        }
    }
    val values = grouped.map { (label, rows) ->
        val minor = if (selected == "Needs Review") rows.size.toLong()
            else rows.fold(0L) { sum, item ->
                val next = item.amountMinor.coerceAtLeast(0L)
                if (sum > Long.MAX_VALUE - next) Long.MAX_VALUE else sum + next
            }
        label to minor
    }.filter { it.second > 0 }.sortedByDescending { it.second }
    val top = if (values.size <= 4) values else values.take(3) +
        listOf("Others" to values.drop(3).fold(0L) { s, v ->
            if (s > Long.MAX_VALUE - v.second) Long.MAX_VALUE else s + v.second
        })
    return top.mapIndexed { index, (label, minor) ->
        ActivityReferenceSegment(label, minor, chartColors[index % chartColors.size])
    }
}

@Composable
internal fun ActivityReferenceSummary(
    monthLabel: String,
    filter: String,
    transactions: List<StoredTransaction>,
    categoryName: (String) -> String,
    accountName: (String?) -> String,
    possibleDuplicateIds: Set<Long>,
    onMonth: () -> Unit,
    onAnalytics: () -> Unit
) {
    val ink = HomeReferencePalette.ink
    val muted = HomeReferencePalette.muted
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val segments = remember(transactions, filter, categoryName, accountName, possibleDuplicateIds) {
        activityReferenceSegments(transactions, filter, categoryName, accountName, possibleDuplicateIds)
    }
    val totalMinor = segments.fold(0L) { sum, segment -> if (Long.MAX_VALUE - sum < segment.minor) Long.MAX_VALUE else sum + segment.minor }
    val total = totalMinor.toDouble()
    val showCents = filter != "Needs Review" && segments.any { it.minor % 100L != 0L }
    val center = if (filter == "Needs Review") totalMinor.toString()
        else activityKesLabel(totalMinor, showCents)
    val caption = when (filter) {
        "Money In" -> "received"
        "Transfers" -> "moved"
        "Needs Review" -> "to review this month"
        else -> "spent"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color.White)
    ) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFEAF7).copy(alpha = .8f), Color.Transparent),
                        center = Offset(size.width * .17f, size.height * .03f),
                        radius = size.width * .58f
                    ),
                    radius = size.width * .58f,
                    center = Offset(size.width * .17f, size.height * .03f)
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFE0FFF2).copy(alpha = .6f), Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = size.width * .48f
                    ),
                    radius = size.width * .48f, center = Offset(0f, size.height)
                )
            }
            Column(Modifier.padding(start = 15.dp, end = 14.dp, top = 9.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f).clickable(onClick = onMonth),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(monthLabel, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = ink)
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Rounded.KeyboardArrowDown, "Choose month", Modifier.size(22.dp), tint = ink)
                    }
                    Surface(
                        onClick = onAnalytics, shape = RoundedCornerShape(13.dp),
                        color = Color(0xFFF8F5FF),
                        border = BorderStroke(1.dp, Color(0xFFE1D7FF))
                    ) {
                        Icon(Icons.Rounded.BarChart, "Detailed Activity breakdown",
                            modifier = Modifier.padding(6.dp).size(20.dp),
                            tint = HomeReferencePalette.purple)
                    }
                }
                Spacer(Modifier.height(1.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(Modifier.weight(1.04f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        val innerWidth = (maxWidth - 56.dp).coerceAtLeast(42.dp)
                        val innerWidthPx = with(density) { innerWidth.toPx() }
                        fun textFits(value: String, size: androidx.compose.ui.unit.TextUnit): Boolean =
                            textMeasurer.measure(AnnotatedString(value), style = TextStyle(fontSize = size,
                                fontWeight = FontWeight.ExtraBold), maxLines = 1).size.width <= innerWidthPx
                        val oneLineSize = listOf(18.sp, 17.sp, 16.sp).firstOrNull { textFits(center, it) }
                        val splitAmount = filter != "Needs Review" && oneLineSize == null
                        val numericText = if (splitAmount) center.removePrefix("KES ") else center
                        val numericSize = listOf(17.sp, 16.sp, 14.sp, 12.sp, 10.sp)
                            .firstOrNull { textFits(numericText, it) } ?: 10.sp
                        Canvas(Modifier.fillMaxSize()) {
                            val width = 19.dp.toPx()
                            val inset = width / 2f + 3.dp.toPx()
                            val diameter = Size(size.width - inset * 2, size.height - inset * 2)
                            if (total <= 0) {
                                drawArc(Color(0xFFE9E9F5), -90f, 360f, false,
                                    Offset(inset, inset), diameter,
                                    style = Stroke(width = width, cap = StrokeCap.Round))
                            } else {
                                var position = -90f
                                segments.forEach { s ->
                                    val share = s.minor.toDouble() / total
                                    val degrees = 360f * share.toFloat()
                                    if (degrees > 0f) {
                                        drawArc(s.color, position + 1.2f,
                                            (degrees - 2.4f).coerceAtLeast(.2f),
                                            false, Offset(inset, inset), diameter,
                                            style = Stroke(width = width, cap = StrokeCap.Round))
                                    }
                                    position += degrees
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.width(innerWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (splitAmount) {
                                Text("KES", fontSize = 11.sp, lineHeight = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = ink, maxLines = 1)
                            }
                            Text(numericText,
                                fontSize = if (splitAmount) numericSize else (oneLineSize ?: numericSize),
                                lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold, color = ink,
                                maxLines = 1, softWrap = false)
                            Text(caption, fontSize = 11.sp, color = muted,
                                maxLines = 2, lineHeight = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    Column(Modifier.weight(1.46f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (segments.isEmpty()) {
                            Text(if (filter == "Transfers") "No transfers recorded for this period" else "No matching transactions", color = muted, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        segments.forEachIndexed { index, segment ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 26.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(Modifier.size(9.dp).background(segment.color, CircleShape))
                                Text(segment.label, Modifier.weight(1f),
                                    fontSize = 12.sp, color = ink, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                val formatted = if (filter == "Needs Review") segment.minor.toString()
                                    else activityKesLabel(segment.minor, showCents)
                                Text(formatted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = ink, maxLines = 1)
                                Text(activityShareLabel(segment.minor, totalMinor), fontSize = 11.sp, color = muted)
                            }
                            if (index < segments.lastIndex) {
                                HorizontalDivider(color = HomeReferencePalette.line.copy(alpha = .65f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActivityReferenceAttention(
    count: Int,
    transfer: Boolean,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF4E9)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFFFE5CC)) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, Modifier.size(22.dp),
                        tint = Color(0xFFFF7100))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(if (transfer) "Transfers between your accounts"
                    else if (count > 0) "Needs attention" else "Nothing needs attention",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HomeReferencePalette.ink)
                Text(if (transfer) "These are internal transfers and not counted as income or expenses."
                    else "$count review items across all periods",
                    fontSize = 12.sp, lineHeight = 16.sp,
                    color = HomeReferencePalette.muted, maxLines = 2)
            }
            Icon(Icons.Rounded.ChevronRight, "Open details", tint = HomeReferencePalette.muted)
        }
    }
}
