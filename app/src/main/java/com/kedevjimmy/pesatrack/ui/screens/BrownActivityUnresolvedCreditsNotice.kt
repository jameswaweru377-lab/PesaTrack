package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** An honest, quiet second tier. No unverified cash is promoted to earnings. */
@Composable
internal fun BrownActivityUnresolvedCreditsNotice(amountMinor: Long, onShowAll: () -> Unit) {
    val p = homeColors()
    Surface(
        onClick = onShowAll,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .heightIn(min = PesaDesignTokens.minimumTouchTarget),
        shape = RoundedCornerShape(PesaDesignTokens.iconRadius),
        color = p.white, border = BorderStroke(1.dp, p.outline)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Unresolved credits · ${activityKesLabel(amountMinor)}",
                    color = p.ink, fontWeight = FontWeight.SemiBold)
                Text("Received money with an unverified origin. Not included in Money in. Review in All.",
                    color = p.muted)
            }
            Icon(Icons.Rounded.ChevronRight, "Show all transactions",
                tint = p.coffee)
        }
    }
}
