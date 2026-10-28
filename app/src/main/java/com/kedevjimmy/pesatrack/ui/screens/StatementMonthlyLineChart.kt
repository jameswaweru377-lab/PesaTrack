package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.domain.everyday.StatementMonthSpend
import java.text.DateFormatSymbols
import java.util.Locale

/** Missing imports interrupt the trend line rather than being plotted as zero spending. */
@Composable
internal fun StatementMonthlyLineChart(
    months: List<StatementMonthSpend>,
    onMonthClick: (Int) -> Unit
) {
    val p = HomeReferencePalette
    val ordered = (1..12).map { month -> months.firstOrNull { it.month == month } }
    val max = ordered.mapNotNull { it?.spentMinor }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val description = ordered.mapIndexed { index, value ->
        val name = DateFormatSymbols(Locale.ENGLISH).shortMonths[index]
        if (value?.imported == true && value.spentMinor != null)
            "$name: ${statementMoney(value.spentMinor)}"
        else "$name: no statement imported"
    }.joinToString(". ")

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(153.dp)
                .pointerInput(months) {
                    detectTapGestures { offset ->
                        val index = ((offset.x / size.width) * 12f).toInt().coerceIn(0, 11)
                        if (ordered[index]?.imported == true) onMonthClick(index + 1)
                    }
                }
                .semantics { contentDescription = "Monthly spending chart. $description" }
        ) {
            val left = 9.dp.toPx()
            val right = size.width - 9.dp.toPx()
            val top = 13.dp.toPx()
            val bottom = size.height - 16.dp.toPx()
            repeat(4) { i ->
                val y = top + (bottom - top) * i / 3f
                drawLine(p.line, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            }
            fun point(i: Int, value: Long): Offset = Offset(
                left + (right - left) * i / 11f,
                bottom - (bottom - top) * (value.toDouble() / max.toDouble()).toFloat()
            )
            var previous: Offset? = null
            ordered.forEachIndexed { index, bar ->
                if (bar?.imported == true && bar.spentMinor != null) {
                    val current = point(index, bar.spentMinor)
                    previous?.let { from ->
                        drawLine(p.purple, from, current, strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round)
                    }
                    drawCircle(p.purple, radius = 4.5.dp.toPx(), center = current)
                    drawCircle(Color.White, radius = 2.dp.toPx(), center = current)
                    previous = current
                } else previous = null
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val names = DateFormatSymbols(Locale.ENGLISH).shortMonths
            ordered.forEachIndexed { index, month ->
                Text(names[index], fontSize = 9.sp,
                    color = if (month?.imported == true) p.muted else p.muted.copy(alpha = .48f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clickable(enabled = month?.imported == true) {
                        onMonthClick(index + 1)
                    })
            }
        }
    }
}
