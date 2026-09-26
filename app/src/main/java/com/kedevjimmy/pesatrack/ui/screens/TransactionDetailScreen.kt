package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.theme.Amber
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.MintSoft
import com.kedevjimmy.pesatrack.ui.theme.SkySoft
import com.kedevjimmy.pesatrack.ui.theme.VioletSoft

@Composable
fun TransactionDetailScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    var autoRule by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text("Transaction details", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            LandscapeBackdrop(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp),
                dark = MaterialTheme.colorScheme.background.luminance() < .2f
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.AccountCircle, size = 56.dp, background = MaterialTheme.colorScheme.primaryContainer)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text("Elizabeth W.", style = MaterialTheme.typography.titleLarge)
                            Text("Sent money via M-Pesa", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("KES 450", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailTopMetric("Date & time", "21 Sep 2024\n1:24 PM", Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                            DetailTopMetric("M-Pesa code", "OE4G7K3L9P", Icons.Rounded.ReceiptLong, Modifier.weight(1f))
                            DetailTopMetric("Transaction type", "Sent money", Icons.Rounded.SyncAlt, Modifier.weight(1f))
                            DetailTopMetric("Fee", "KES 0", Icons.Rounded.Paid, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Category")
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { },
                shape = RoundedCornerShape(20.dp),
                color = AmberSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = .65f))
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(Icons.Rounded.Restaurant, tint = Amber, background = Color(0xFFFFE1A3), size = 46.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text("Current category", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9A650D))
                        Text("Lunch", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("Change", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            PesaCard {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(Icons.Rounded.Spa, size = 46.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text("Always classify Elizabeth as Lunch", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "This helps future payments to Elizabeth be categorized automatically.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoRule, onCheckedChange = { autoRule = it })
                }
            }
        }

        item {
            SectionHeader("Transaction information")
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    InformationRow(Icons.Rounded.AccountCircle, "Recipient", "Elizabeth W.")
                    InformationRow(Icons.Rounded.SyncAlt, "Transaction type", "Sent money")
                    InformationRow(Icons.Rounded.ReceiptLong, "M-Pesa code", "OE4G7K3L9P", trailingIcon = Icons.Rounded.ContentCopy)
                    InformationRow(Icons.Rounded.AccountBalanceWallet, "From account", "M-Pesa (Personal)", trailingIcon = Icons.Rounded.ChevronRight)
                    InformationRow(Icons.Rounded.CalendarMonth, "Date & time", "21 Sep 2024, 1:24 PM")
                    InformationRow(Icons.Rounded.Notes, "Notes", "No notes yet", trailingIcon = Icons.Rounded.ChevronRight)
                }
            }
        }

        item {
            SectionHeader("Actions")
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    ActionRow(Icons.Rounded.Edit, "Re-categorize")
                    ActionRow(Icons.Rounded.SwapHoriz, "Mark as transfer")
                    ActionRow(Icons.Rounded.Notes, "Add note")
                    ActionRow(Icons.Rounded.Block, "Exclude from budget", MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            SectionHeader("Category suggestions")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip("Lunch", Icons.Rounded.LocalDining, "amber", Modifier.weight(1f))
                    SuggestionChip("Shopping", Icons.Rounded.ShoppingBag, "violet", Modifier.weight(1f))
                    SuggestionChip("Family", Icons.Rounded.Groups, "green", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip("Transport", Icons.Rounded.DirectionsBus, "blue", Modifier.weight(1f))
                    SuggestionChip("Bills", Icons.Rounded.ReceiptLong, "blue", Modifier.weight(1f))
                    SuggestionChip("Other", Icons.Rounded.MoreHoriz, "green", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailTopMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Column(modifier) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun InformationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
        if (trailingIcon != null) {
            Icon(
                trailingIcon,
                null,
                modifier = Modifier
                    .padding(start = 7.dp)
                    .height(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint)
        Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp), color = tint)
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tone: String,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.clickable { },
        shape = RoundedCornerShape(50),
        color = when (tone) {
            "amber" -> AmberSoft
            "blue" -> SkySoft
            "violet" -> VioletSoft
            else -> MintSoft
        }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.height(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun Color.luminance(): Float {
    fun channel(v: Float): Float = if (v <= .03928f) v / 12.92f else Math.pow(((v + .055f) / 1.055f).toDouble(), 2.4).toFloat()
    return .2126f * channel(red) + .7152f * channel(green) + .0722f * channel(blue)
}
