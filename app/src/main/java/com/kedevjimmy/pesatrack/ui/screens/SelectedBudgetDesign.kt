package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** Selected Budget B: a calm, ring-led overview with real stored limit and spending. */
@Composable
internal fun SelectedBudgetRing(limitMinor: Long, spentMinor: Long, money: (Long) -> String) {
    val p = BrownHomePalette
    val percent = selectedBudgetSpentPercent(limitMinor, spentMinor)
    val remaining = if (limitMinor >= spentMinor) limitMinor - spentMinor else 0L
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { (percent ?: 0) / 100f },
                modifier = Modifier.fillMaxSize(), strokeWidth = 12.dp,
                color = p.coffee, trackColor = p.cream)
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(percent?.let { "$it%" } ?: "—", color = p.coffee,
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(if (percent == null) "No budget set" else money(remaining),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = p.ink, maxLines = 1)
                Text("remaining", color = p.muted,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(if (percent == null) "Create a budget to track your spending" else
            "${money(spentMinor)} of ${money(limitMinor)} spent",
            color = p.muted, style = MaterialTheme.typography.bodyMedium)
        if (limitMinor > 0 && spentMinor > limitMinor) {
            Text("Over budget by ${money(spentMinor - limitMinor)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun SelectedBudgetFilters(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All", "On track", "Over").forEach { filter ->
            val active = filter == selected
            Surface(modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                .clickable { onSelect(filter) },
                shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
                color = if (active) BrownHomePalette.coffee else BrownHomePalette.cream) {
                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                    Text(filter, color = if (active) Color.White else BrownHomePalette.ink,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun SelectedBudgetCategoryRow(
    label: String,
    icon: ImageVector,
    spentMinor: Long,
    limitMinor: Long,
    money: (Long) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    timingContent: @Composable () -> Unit
) {
    val p = BrownHomePalette
    val percent = selectedBudgetSpentPercent(limitMinor, spentMinor)
    val over = spentMinor > limitMinor
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onEdit() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(PesaDesignTokens.iconRadius), color = p.cream) {
                Box(Modifier.size(PesaDesignTokens.iconBubble), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = p.coffee)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(if (over) "Over by ${money(spentMinor - limitMinor)}" else
                    "${money(limitMinor - spentMinor)} left",
                    color = if (over) MaterialTheme.colorScheme.error else p.coffee,
                    style = MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${money(spentMinor)} of ${money(limitMinor)}",
                    color = p.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                LinearProgressIndicator(progress = { (percent ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (over) MaterialTheme.colorScheme.error else p.coffee,
                    trackColor = p.cream)
            }
            Icon(Icons.Rounded.ChevronRight, "Edit $label budget", tint = p.ink)
        }
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide options" else "More options", color = p.coffee,
                style = MaterialTheme.typography.labelSmall)
        }
        if (expanded) {
            Surface(color = p.white,
                border = BorderStroke(1.dp, p.outline),
                shape = RoundedCornerShape(PesaDesignTokens.innerRadius)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timingContent()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                            Text("Edit limit")
                        }
                        TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = p.outline)
    }
}
