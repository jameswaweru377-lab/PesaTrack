package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens

/** One offline progress treatment for SMS scanning and user-initiated backup restoration. */
@Composable
internal fun OnboardingProgressPanel(
    title: String,
    description: String,
    status: String,
    icon: ImageVector,
    finished: Boolean = false
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .45f),
            shape = RoundedCornerShape(PesaDesignTokens.cardRadius)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(Modifier.size(72.dp), shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text(title, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(PesaDesignTokens.cardRadius)) {
            Column(Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Progress", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                if (!finished) {
                    // Indeterminate by design: SMS and backup APIs do not expose trustworthy percentages.
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().heightIn(min = 4.dp))
                }
                Row(
                    Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (finished) Icon(Icons.Rounded.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    else CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(status, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
