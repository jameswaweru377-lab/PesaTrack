package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Illustrative messages only; these values never enter the user's ledger. */
@Composable
fun FinancialMessagesIllustration(modifier: Modifier = Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(if (compact) 108.dp else 150.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val end = size.width * .64f
                listOf(.3f to .36f, .75f to .64f).forEach { (fromY, toY) ->
                    val start = Offset(size.width * .45f, size.height * fromY)
                    val tip = Offset(end, size.height * toY)
                    drawLine(colors.primary, start, tip, strokeWidth = 2.dp.toPx())
                    drawLine(colors.primary, tip, Offset(tip.x - 10.dp.toPx(), tip.y - 5.dp.toPx()), 2.dp.toPx())
                    drawLine(colors.primary, tip, Offset(tip.x - 6.dp.toPx(), tip.y + 8.dp.toPx()), 2.dp.toPx())
                }
            }
            Column(Modifier.width(maxWidth * .48f).align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IllustrationMessage(Icons.Rounded.PhoneAndroid, "M-Pesa", "KES 1,200.00", "Today 14:21", compact)
                IllustrationMessage(Icons.Rounded.AccountBalance, "Equity Bank", "KES 5,000.00", "Today 09:10", compact)
            }
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).width(92.dp).height(if (compact) 108.dp else 146.dp)
                .border(4.dp, colors.onSurface, RoundedCornerShape(14.dp))
                .background(colors.surface, RoundedCornerShape(14.dp)).padding(10.dp)) {
                Box(Modifier.align(Alignment.TopCenter).width(34.dp).height(6.dp)
                    .background(colors.onSurface, RoundedCornerShape(8.dp)))
                Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(colors.primary, colors.tertiary, colors.error).forEach { color ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(17.dp).background(color, CircleShape))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.width(31.dp).height(5.dp).background(colors.outlineVariant, RoundedCornerShape(4.dp)))
                                Box(Modifier.width(22.dp).height(5.dp).background(colors.outlineVariant, RoundedCornerShape(4.dp)))
                            }
                        }
                    }
                }
            }
        }
        Text("Illustrative messages", Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun IllustrationMessage(icon: ImageVector, name: String, amount: String, time: String, compact: Boolean) {
    val style = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp)
        else MaterialTheme.typography.labelSmall
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .3f))) {
        Row(Modifier.fillMaxWidth().padding(if (compact) 6.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(name, style = style, fontWeight = FontWeight.Bold)
                Text(amount, style = style)
                Text(time, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
