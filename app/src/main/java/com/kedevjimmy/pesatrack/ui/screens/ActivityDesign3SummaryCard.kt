package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** Approved Activity Design 3: four evidence-backed figures, no fake demo amounts. */
@Composable
internal fun ActivityDesign3SummaryCard(
    snapshot: ActivityLedgerSnapshot,
    monthLabel: String,
    onMonth: () -> Unit
) {
    val p = HomeMonochromePalette
    val totals = remember(snapshot) { activityDesign3Totals(snapshot) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = p.softGreen
    ) {
        Column(
            modifier = Modifier.padding(PesaDesignTokens.sectionGap),
            verticalArrangement = Arrangement.spacedBy(PesaDesignTokens.compactGap)
        ) {
            Surface(
                onClick = onMonth,
                modifier = Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget)
                    .semantics { contentDescription = "Choose Activity month: $monthLabel" },
                color = Color.White.copy(alpha = .72f),
                shape = RoundedCornerShape(PesaDesignTokens.controlRadius)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(monthLabel, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium, color = p.ink)
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null,
                        modifier = Modifier.size(20.dp), tint = p.ink)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityDesign3Metric("Income", totals.incomeMinor, Icons.Rounded.AccountBalanceWallet,
                    Modifier.weight(1f), highlighted = true)
                ActivityDesign3Metric("Expenses", totals.expensesMinor, Icons.Rounded.ReceiptLong,
                    Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityDesign3Metric("Transfers", totals.transfersMinor, Icons.Rounded.SwapHoriz,
                    Modifier.weight(1f))
                ActivityDesign3Metric("Invested", totals.investedMinor, Icons.Rounded.Eco,
                    Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActivityDesign3Metric(
    label: String,
    minor: Long?,
    icon: ImageVector,
    modifier: Modifier,
    highlighted: Boolean = false
) {
    val p = HomeMonochromePalette
    val formatted = minor?.let { activityKesLabel(it) } ?: "Unavailable"
    Surface(modifier = modifier,
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius), color = p.surface) {
        Row(modifier = Modifier.heightIn(min = 76.dp).padding(horizontal = 8.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = "$label: $formatted" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp),
                tint = p.green)
            Surface(modifier = Modifier.width(1.dp).heightIn(min = 32.dp), color = p.line) {}
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall,
                    color = p.muted)
                Text(formatted, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) p.green else p.ink,
                    maxLines = 2)
            }
        }
    }
}
