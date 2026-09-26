package com.kedevjimmy.pesatrack.ui.screens

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
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    scaffoldPadding: PaddingValues,
    onTransactionClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { TransactionStore.get(context) }
    var snapshot by remember { mutableStateOf(store.activitySnapshot()) }
    var selectedFilter by remember { mutableStateOf("All") }

    fun refresh() {
        snapshot = store.activitySnapshot()
    }

    val filtered = snapshot.transactions.filter { item ->
        when (selectedFilter) {
            "Expenses" -> item.direction == MoneyDirection.EXPENSE.name
            "Income" -> item.direction == MoneyDirection.INCOME.name
            "Transfers" -> item.direction == MoneyDirection.TRANSFER.name
            "Needs review" -> item.reviewState == ReviewState.NEEDS_REVIEW.name
            else -> true
        }
    }

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
                        "Your captured money activity.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    IconButton(onClick = ::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Expenses", "Income", "Transfers", "Needs review").forEach { label ->
                    item {
                        FilterChip(
                            selected = selectedFilter == label,
                            onClick = { selectedFilter = label },
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
            ActivitySummaryCard(snapshot)
        }

        if (filtered.isEmpty()) {
            item {
                EmptyActivityState(
                    selectedFilter = selectedFilter,
                    capturedCount = snapshot.transactions.size
                )
            }
        } else {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedFilter == "All") "Recent activity" else selectedFilter,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${filtered.size} transaction${if (filtered.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(filtered.size) { index ->
                ActivityRow(
                    item = filtered[index],
                    onClick = { onTransactionClick(filtered[index].id) }
                )
            }
        }
    }
}

@Composable
private fun ActivitySummaryCard(snapshot: ActivityLedgerSnapshot) {
    val summary = snapshot.summary
    PesaCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        currentMonthLabel(),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "${formatMoney(summary.spentMinor)} spent • ${formatMoney(summary.incomeMinor)} income",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (summary.needsReviewCount > 0) {
                    SmallStatusPill(
                        "${summary.needsReviewCount} need review",
                        "amber"
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            ResponsiveActivityMetrics(snapshot)
        }
    }
}

@Composable
private fun ResponsiveActivityMetrics(snapshot: ActivityLedgerSnapshot) {
    val summary = snapshot.summary
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric(
                        "Spent",
                        formatMoney(summary.spentMinor),
                        "This month",
                        Icons.Rounded.Paid,
                        "red",
                        Modifier.weight(1f)
                    )
                    ActivityMetric(
                        "Income",
                        formatMoney(summary.incomeMinor),
                        "This month",
                        Icons.Rounded.CallReceived,
                        "green",
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric(
                        "Fees",
                        formatMoney(summary.feesMinor),
                        "M-Pesa fees",
                        Icons.Rounded.ReceiptLong,
                        "amber",
                        Modifier.weight(1f)
                    )
                    ActivityMetric(
                        "Net",
                        formatSignedMoney(summary.netMinor),
                        "Cash flow",
                        Icons.Rounded.AccountBalance,
                        "blue",
                        Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActivityMetric("Spent", formatMoney(summary.spentMinor), "This month", Icons.Rounded.Paid, "red", Modifier.weight(1f))
                ActivityMetric("Income", formatMoney(summary.incomeMinor), "This month", Icons.Rounded.CallReceived, "green", Modifier.weight(1f))
                ActivityMetric("Fees", formatMoney(summary.feesMinor), "M-Pesa fees", Icons.Rounded.ReceiptLong, "amber", Modifier.weight(1f))
                ActivityMetric("Net", formatSignedMoney(summary.netMinor), "Cash flow", Icons.Rounded.AccountBalance, "blue", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActivityMetric(
    label: String,
    value: String,
    helper: String,
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
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            helper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyActivityState(
    selectedFilter: String,
    capturedCount: Int
) {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBubble(
                if (selectedFilter == "Needs review") Icons.Rounded.HelpOutline else Icons.Rounded.ReceiptLong,
                size = 52.dp,
                background = pesaToneContainer(if (selectedFilter == "Needs review") "amber" else "green"),
                tint = pesaToneContent(if (selectedFilter == "Needs review") "amber" else "green")
            )
            Text(
                if (capturedCount == 0) {
                    "Waiting for your first captured transaction"
                } else {
                    "Nothing in $selectedFilter"
                },
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (capturedCount == 0) {
                    "Once Notification Access is enabled, new M-Pesa confirmations and configured money alerts will appear here."
                } else if (selectedFilter == "Needs review") {
                    "Good — every captured transaction currently has a classification."
                } else {
                    "Try another filter to see your captured activity."
                },
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityRow(
    item: StoredTransaction,
    onClick: () -> Unit
) {
    val category = runCatching {
        TransactionCategory.valueOf(item.category)
    }.getOrDefault(TransactionCategory.UNCATEGORIZED)
    val needsReview = item.reviewState == ReviewState.NEEDS_REVIEW.name
    val direction = runCatching {
        MoneyDirection.valueOf(item.direction)
    }.getOrDefault(MoneyDirection.NEUTRAL)

    val tone = when {
        needsReview -> "amber"
        direction == MoneyDirection.INCOME -> "green"
        direction == MoneyDirection.TRANSFER -> "blue"
        category == TransactionCategory.SHOPPING -> "violet"
        category == TransactionCategory.ELECTRICITY || category == TransactionCategory.RENT -> "amber"
        else -> "green"
    }

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
                iconFor(category, item.type),
                size = 40.dp,
                background = pesaToneContainer(tone),
                tint = pesaToneContent(tone)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    item.counterparty ?: humanize(item.type),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(item.occurredAtEpochMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (needsReview) "Needs review" else category.prettyName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = pesaToneContent(tone)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amountLabel(item, direction),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (
                        direction == MoneyDirection.INCOME ||
                        item.type == MpesaTransactionType.REVERSAL.name
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
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

private fun iconFor(
    category: TransactionCategory,
    type: String
): ImageVector = when (category) {
    TransactionCategory.RENT -> Icons.Rounded.Home
    TransactionCategory.LUNCH -> Icons.Rounded.Restaurant
    TransactionCategory.ELECTRICITY -> Icons.Rounded.Bolt
    TransactionCategory.INTERNET -> Icons.Rounded.Wifi
    TransactionCategory.TRANSPORT -> Icons.Rounded.DirectionsBus
    TransactionCategory.SHOPPING -> Icons.Rounded.ShoppingCart
    TransactionCategory.FAMILY -> Icons.Rounded.Groups
    TransactionCategory.FEES -> Icons.Rounded.ReceiptLong
    TransactionCategory.PHONE_AND_DATA -> Icons.Rounded.PhoneAndroid
    TransactionCategory.INCOME -> Icons.Rounded.CallReceived
    TransactionCategory.TRANSFER -> Icons.Rounded.SyncAlt
    TransactionCategory.CASH -> Icons.Rounded.LocalAtm
    TransactionCategory.SAVINGS -> Icons.Rounded.Savings
    TransactionCategory.OTHER -> Icons.Rounded.MoreHoriz
    TransactionCategory.UNCATEGORIZED -> if (type == MpesaTransactionType.REVERSAL.name) {
        Icons.Rounded.SyncAlt
    } else {
        Icons.Rounded.HelpOutline
    }
}

private fun amountLabel(
    item: StoredTransaction,
    direction: MoneyDirection
): String {
    val sign = when {
        direction == MoneyDirection.INCOME -> "+"
        item.type == MpesaTransactionType.REVERSAL.name -> "+"
        direction == MoneyDirection.EXPENSE -> "-"
        else -> ""
    }
    return "$sign ${formatMoney(item.amountMinor)}".trim()
}

private fun formatMoney(minor: Long): String =
    "KES ${NumberFormat.getNumberInstance(Locale.US).format(minor / 100.0)}"

private fun formatSignedMoney(minor: Long): String =
    if (minor < 0) "- ${formatMoney(-minor)}" else formatMoney(minor)

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(epochMs))

private fun currentMonthLabel(): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

private fun humanize(value: String): String =
    value.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

private fun TransactionCategory.prettyName(): String =
    name.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
