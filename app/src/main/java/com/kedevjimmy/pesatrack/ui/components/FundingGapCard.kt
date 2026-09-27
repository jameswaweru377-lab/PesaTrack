package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FundingGapCard(
    unexplainedMinor: Long,
    modifier: Modifier = Modifier
) {
    if (unexplainedMinor <= 0L) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = pesaToneContainer("amber")
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.HelpOutline,
                contentDescription = null,
                tint = pesaToneContent("amber")
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(
                    "Some spending needs a source",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    formatGapMoney(unexplainedMinor) +
                        " of actual spending and fees is above the salary, carryover and other income PesaTrack can currently explain.",
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Was it previous savings or balance, a loan or debt, another account, or income we missed? PesaTrack will not assume it was debt.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatGapMoney(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return "KES " + formatter.format(minor / 100.0)
}
