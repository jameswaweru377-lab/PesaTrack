package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import java.util.Locale

private fun selectedGoalIcon(title: String): ImageVector = when {
    "emergency" in title.lowercase(Locale.ROOT) -> Icons.Rounded.HealthAndSafety
    "laptop" in title.lowercase(Locale.ROOT) -> Icons.Rounded.Laptop
    listOf("trip", "holiday", "travel").any { it in title.lowercase(Locale.ROOT) } ->
        Icons.Rounded.Flight
    else -> Icons.Rounded.Savings
}

/** Selected Goals B: featured, actual saved goal and real progress milestones. */
@Composable
internal fun SelectedFeaturedGoal(
    title: String,
    savedMinor: Long,
    targetMinor: Long,
    hasTotalTarget: Boolean,
    money: (Long) -> String,
    onClick: () -> Unit
) {
    val p = HomeMonochromePalette
    val progress = if (hasTotalTarget && targetMinor > 0) (savedMinor.toDouble() / targetMinor)
        .coerceIn(0.0, 1.0).toFloat() else 0f
    val next = if (hasTotalTarget) selectedGoalNextMilestone(targetMinor, savedMinor) else null
    Surface(color = p.softGreen, shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(color = Color.White, shape = RoundedCornerShape(PesaDesignTokens.iconRadius)) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        Icon(selectedGoalIcon(title), contentDescription = null,
                            tint = p.green, modifier = Modifier.size(32.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Featured goal", style = MaterialTheme.typography.labelMedium,
                        color = p.green)
                    Text(title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = p.ink,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${money(savedMinor)} saved", style = MaterialTheme.typography.titleMedium,
                        color = p.ink)
                }
            }
            if (hasTotalTarget) LinearProgressIndicator(progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = p.green, trackColor = Color.White)
            Text(if (hasTotalTarget) "${(progress * 100).toInt()}% of ${money(targetMinor)}"
                else "Monthly savings target: ${money(targetMinor)}",
                color = p.ink, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Flag, contentDescription = null, tint = p.green)
                Text(when {
                    !hasTotalTarget -> "Set a full goal target to see milestones"
                    next == null -> "Goal reached"
                    else -> "Next milestone: ${money(next)}"
                }, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium, color = p.ink)
            }
            if (hasTotalTarget) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    (1..4).forEach { index ->
                        val milestone = (targetMinor.toDouble() * index / 4).toLong()
                        val reached = savedMinor >= milestone && targetMinor > 0L
                        Surface(shape = CircleShape,
                            color = if (reached) p.green else Color.White,
                            border = BorderStroke(1.dp, p.green)) {
                            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                if (reached) Icon(Icons.Rounded.Check, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SelectedOtherGoal(
    title: String,
    savedMinor: Long,
    targetMinor: Long,
    hasTotalTarget: Boolean,
    money: (Long) -> String,
    onClick: () -> Unit
) {
    val p = HomeMonochromePalette
    val progress = if (hasTotalTarget && targetMinor > 0L) (savedMinor.toDouble() / targetMinor)
        .coerceIn(0.0, 1.0).toFloat() else 0f
    Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(PesaDesignTokens.iconRadius), color = p.softGreen) {
            Box(Modifier.size(PesaDesignTokens.iconBubble), contentAlignment = Alignment.Center) {
                Icon(selectedGoalIcon(title), contentDescription = null, tint = p.green)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, color = p.ink,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (hasTotalTarget) Text("${(progress * 100).toInt()}%", color = p.ink,
                    style = MaterialTheme.typography.labelLarge)
            }
            Text(if (hasTotalTarget) "${money(savedMinor)} / ${money(targetMinor)}" else
                "${money(savedMinor)} saved · Monthly target ${money(targetMinor)}", color = p.muted,
                style = MaterialTheme.typography.labelMedium)
            if (hasTotalTarget) LinearProgressIndicator(progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp), color = p.green,
                trackColor = p.softGreen)
        }
    }
}
