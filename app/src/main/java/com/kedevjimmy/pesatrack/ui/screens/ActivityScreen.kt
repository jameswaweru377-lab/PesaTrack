package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.MintSoft
import com.kedevjimmy.pesatrack.ui.theme.RoseSoft
import com.kedevjimmy.pesatrack.ui.theme.SkySoft
import com.kedevjimmy.pesatrack.ui.theme.VioletSoft

private data class ActivityItem(
    val name: String,
    val time: String,
    val category: String,
    val amount: String,
    val positive: Boolean,
    val icon: ImageVector,
    val tone: String
)

@Composable
fun ActivityScreen(
    scaffoldPadding: PaddingValues,
    onTransactionClick: () -> Unit = {}
) {
    val today = listOf(
        ActivityItem("Family Wings", "18:24", "Rent", "- KES 6,000", false, Icons.Rounded.AccountBalance, "amber"),
        ActivityItem("Elizabeth", "13:15", "Food & Dining", "- KES 2,900", false, Icons.Rounded.Restaurant, "green"),
        ActivityItem("Planet Internet", "09:32", "Internet", "- KES 1,500", false, Icons.Rounded.Wifi, "blue"),
        ActivityItem("M-Pesa Transaction Fee", "08:17", "Fees", "- KES 28", false, Icons.Rounded.ReceiptLong, "neutral")
    )
    val yesterday = listOf(
        ActivityItem("Joseph", "19:46", "Shopping", "- KES 3,200", false, Icons.Rounded.ShoppingCart, "violet"),
        ActivityItem("KPLC", "14:20", "Electricity", "- KES 1,200", false, Icons.Rounded.Bolt, "amber"),
        ActivityItem("Safaricom Airtime", "11:08", "Phone & Data", "- KES 500", false, Icons.Rounded.PhoneAndroid, "blue"),
        ActivityItem("Salary - ABC Ltd", "08:05", "Income", "+ KES 45,000", true, Icons.Rounded.CallReceived, "green")
    )
    val earlier = listOf(
        ActivityItem("Transfer to KCB", "18 Sep, 21:14", "Transfer", "- KES 10,000", false, Icons.Rounded.AccountBalance, "neutral"),
        ActivityItem("Cash Withdrawal", "18 Sep, 16:30", "Cash", "- KES 5,000", false, Icons.Rounded.LocalAtm, "green"),
        ActivityItem("M-Pesa Reversal", "18 Sep, 11:12", "Other", "+ KES 428", true, Icons.Rounded.SyncAlt, "green")
    )

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Activity", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "All your money in one place.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Expenses", "Income", "Transfers", "Fees", "Uncategorized").forEachIndexed { index, label ->
                    item {
                        FilterChip(
                            selected = index == 0,
                            onClick = {},
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        item {
            PesaCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("September", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "KES 16,200 spent • KES 45,000 income",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MiniTrend()
                        Column(horizontalAlignment = Alignment.End) {
                            Text("+12%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("vs Aug", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    ResponsiveActivityMetrics()
                }
            }
        }

        item { ActivityGroupHeader("Today", "21 September 2024", "KES 4,800") }
        items(today.size) { index ->
            ActivityRow(today[index], onTransactionClick)
        }

        item { ActivityGroupHeader("Yesterday", "20 September 2024", "KES 7,350") }
        items(yesterday.size) { index ->
            ActivityRow(yesterday[index], onTransactionClick)
        }

        item { ActivityGroupHeader("Earlier this week", "18 September 2024", "KES 5,428") }
        items(earlier.size) { index ->
            ActivityRow(earlier[index], onTransactionClick)
        }
    }
}

@Composable
private fun ResponsiveActivityMetrics() {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric("Spent", "KES 16,200", "↑ 12%", Icons.Rounded.Paid, "red", Modifier.weight(1f))
                    ActivityMetric("Income", "KES 45,000", "↑ 8%", Icons.Rounded.CallReceived, "green", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric("Fees", "KES 428", "↓ 20%", Icons.Rounded.ReceiptLong, "amber", Modifier.weight(1f))
                    ActivityMetric("Net", "KES 28,372", "↑ 15%", Icons.Rounded.Paid, "blue", Modifier.weight(1f))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActivityMetric("Spent", "KES 16,200", "↑ 12%", Icons.Rounded.Paid, "red", Modifier.weight(1f))
                ActivityMetric("Income", "KES 45,000", "↑ 8%", Icons.Rounded.CallReceived, "green", Modifier.weight(1f))
                ActivityMetric("Fees", "KES 428", "↓ 20%", Icons.Rounded.ReceiptLong, "amber", Modifier.weight(1f))
                ActivityMetric("Net", "KES 28,372", "↑ 15%", Icons.Rounded.Paid, "blue", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActivityMetric(
    label: String,
    value: String,
    trend: String,
    icon: ImageVector,
    tone: String,
    modifier: Modifier
) {
    Column(modifier) {
        IconBubble(
            icon,
            size = 34.dp,
            background = pesaToneContainer(tone),
            tint = pesaToneContent(tone)
        )
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        SmallStatusPill(trend)
    }
}

@Composable
private fun ActivityGroupHeader(title: String, date: String, total: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            "  •  $date",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(total, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActivityRow(item: ActivityItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                item.icon,
                size = 40.dp,
                background = if (item.tone == "neutral") {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    pesaToneContainer(item.tone)
                },
                tint = if (item.tone == "neutral") {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    pesaToneContent(item.tone)
                }
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = pesaToneContent(
                            when (item.tone) {
                                "neutral" -> "blue"
                                else -> item.tone
                            }
                        )
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    item.amount,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniTrend() {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier
            .size(width = 96.dp, height = 45.dp)
            .padding(horizontal = 6.dp)
    ) {
        val points = listOf(
            Offset(0f, size.height * .75f),
            Offset(size.width * .22f, size.height * .58f),
            Offset(size.width * .43f, size.height * .62f),
            Offset(size.width * .64f, size.height * .40f),
            Offset(size.width * .82f, size.height * .30f),
            Offset(size.width, size.height * .12f)
        )
        points.zipWithNext().forEach { (a, b) ->
            drawLine(primary, a, b, strokeWidth = 4f, cap = StrokeCap.Round)
        }
        drawCircle(primary, radius = 5f, center = points.last())
    }
}
