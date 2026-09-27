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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivityMonthBucket
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PossibleDuplicateGroup
import com.kedevjimmy.pesatrack.data.local.ReviewGroup
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    scaffoldPadding: PaddingValues,
    onTransactionClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { TransactionStore.get(context) }
    val ledgerUpdateState by LedgerUpdateBus.state.collectAsState()
    val currentMonthEpochMs = remember { monthStart(System.currentTimeMillis()) }

    fun loadMonthBuckets(): List<ActivityMonthBucket> {
        val existing = store.activityMonths().toMutableList()
        if (existing.none { it.monthStartEpochMs == currentMonthEpochMs }) {
            existing.add(
                0,
                ActivityMonthBucket(
                    monthStartEpochMs = currentMonthEpochMs,
                    transactionCount = 0
                )
            )
        }
        return existing.sortedByDescending { it.monthStartEpochMs }
    }

    var monthBuckets by remember {
        mutableStateOf(loadMonthBuckets())
    }
    var selectedMonthEpochMs by rememberSaveable {
        mutableStateOf(
            monthBuckets.firstOrNull()?.monthStartEpochMs
                ?: currentMonthEpochMs
        )
    }
    var expandedMonthEpochMs by rememberSaveable {
        mutableStateOf<Long?>(selectedMonthEpochMs)
    }
    var snapshot by remember {
        mutableStateOf(
            store.activitySnapshot(nowEpochMs = selectedMonthEpochMs)
        )
    }
    var selectedFilter by remember { mutableStateOf("All") }

    fun selectMonth(epochMs: Long) {
        val normalizedMonth = monthStart(epochMs)
        selectedMonthEpochMs = normalizedMonth
        expandedMonthEpochMs = normalizedMonth
        selectedFilter = "All"
        snapshot = store.activitySnapshot(nowEpochMs = normalizedMonth)
    }

    fun refresh() {
        monthBuckets = loadMonthBuckets()
        snapshot = store.activitySnapshot(nowEpochMs = selectedMonthEpochMs)
    }

    LaunchedEffect(ledgerUpdateState.revision) {
        if (ledgerUpdateState.revision > 0L) {
            refresh()
        }
    }

    val selectedMonthLabel = monthLabel(selectedMonthEpochMs)
    val possibleDuplicateIds = snapshot.possibleDuplicates
        .flatMap { listOf(it.first.id, it.second.id) }
        .toSet()
    val filtered = snapshot.transactions.filter { item ->
        if (item.reviewState == ReviewState.EXCLUDED.name) {
            return@filter false
        }
        if (item.id in possibleDuplicateIds) {
            return@filter false
        }

        when (selectedFilter) {
            "Expenses" -> item.direction == MoneyDirection.EXPENSE.name
            "Income" -> item.direction == MoneyDirection.INCOME.name
            "Transfers" -> item.direction == MoneyDirection.TRANSFER.name
            "Review" -> item.reviewState == ReviewState.NEEDS_REVIEW.name
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

        if (ledgerUpdateState.isProcessing) {
            item {
                LiveActivityUpdateStatus()
            }
        }

        item {
            ActivitySummaryCard(snapshot, selectedMonthLabel)
        }

        monthBuckets.forEach { bucket ->
            val isExpanded =
                expandedMonthEpochMs == bucket.monthStartEpochMs

            item(key = "month-" + bucket.monthStartEpochMs) {
                ActivityMonthSectionHeader(
                    monthLabel = monthLabel(bucket.monthStartEpochMs),
                    transactionCount = bucket.transactionCount,
                    expanded = isExpanded,
                    onClick = {
                        if (isExpanded) {
                            expandedMonthEpochMs = null
                        } else {
                            selectMonth(bucket.monthStartEpochMs)
                        }
                    }
                )
            }

            if (isExpanded) {
                item(key = "filters-" + bucket.monthStartEpochMs) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "All",
                            "Review",
                            "Duplicates",
                            "Expenses",
                            "Income",
                            "Transfers"
                        ).forEach { label ->
                            item {
                                FilterChip(
                                    selected = selectedFilter == label,
                                    onClick = { selectedFilter = label },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor =
                                            MaterialTheme.colorScheme.primary,
                                        selectedLabelColor =
                                            MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                if (
                    selectedFilter == "All" &&
                    snapshot.possibleDuplicates.isNotEmpty()
                ) {
                    item(
                        key = "duplicate-callout-" +
                            bucket.monthStartEpochMs
                    ) {
                        PossibleDuplicateCallout(
                            pairCount = snapshot.possibleDuplicates.size,
                            onClick = {
                                selectedFilter = "Duplicates"
                            }
                        )
                    }
                }

                if (
                    selectedFilter == "All" &&
                    snapshot.reviewGroups.isNotEmpty()
                ) {
                    item(key = "review-callout-" + bucket.monthStartEpochMs) {
                        ReviewQueueCallout(
                            groupCount = snapshot.reviewGroups.size,
                            transactionCount =
                                snapshot.summary.needsReviewCount,
                            onClick = { selectedFilter = "Review" }
                        )
                    }
                }

                if (selectedFilter == "Duplicates") {
                    if (snapshot.possibleDuplicates.isEmpty()) {
                        item(
                            key = "duplicate-empty-" +
                                bucket.monthStartEpochMs
                        ) {
                            EmptyDuplicateState(selectedMonthLabel)
                        }
                    } else {
                        item(
                            key = "duplicate-title-" +
                                bucket.monthStartEpochMs
                        ) {
                            Column {
                                Text(
                                    "Possible duplicates",
                                    style =
                                        MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    "These are reconciliation problems, not categorization problems. Compare the two observations and decide whether they are the same money movement.",
                                    modifier = Modifier.padding(top = 3.dp),
                                    style =
                                        MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(
                            count = snapshot.possibleDuplicates.size,
                            key = { index ->
                                "duplicate-" +
                                    snapshot.possibleDuplicates[index].key
                            }
                        ) { index ->
                            val group =
                                snapshot.possibleDuplicates[index]
                            PossibleDuplicateRow(
                                group = group,
                                onSameTransaction = {
                                    store.resolvePossibleDuplicate(
                                        pairKey = group.key,
                                        sameTransaction = true
                                    )
                                    refresh()
                                },
                                onKeepBoth = {
                                    store.resolvePossibleDuplicate(
                                        pairKey = group.key,
                                        sameTransaction = false
                                    )
                                    refresh()
                                }
                            )
                        }
                    }
                } else if (selectedFilter == "Review") {
                    if (snapshot.reviewGroups.isEmpty()) {
                        item(key = "review-empty-" + bucket.monthStartEpochMs) {
                            EmptyActivityState(
                                selectedFilter = "Review",
                                capturedCount = snapshot.transactions.size,
                                monthLabel = selectedMonthLabel
                            )
                        }
                    } else {
                        item(key = "review-title-" + bucket.monthStartEpochMs) {
                            Column {
                                Text(
                                    "Review by identity",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    "Teach one repeated name or number once. Matching pending transactions update together.",
                                    modifier = Modifier.padding(top = 3.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(
                            count = snapshot.reviewGroups.size,
                            key = { index ->
                                "review-" +
                                    snapshot.reviewGroups[index].key
                            }
                        ) { index ->
                            ReviewGroupRow(
                                group = snapshot.reviewGroups[index],
                                onClick = {
                                    onTransactionClick(
                                        snapshot.reviewGroups[index]
                                            .representativeTransactionId
                                    )
                                }
                            )
                        }
                    }
                } else if (filtered.isEmpty()) {
                    item(key = "empty-" + bucket.monthStartEpochMs) {
                        EmptyActivityState(
                            selectedFilter = selectedFilter,
                            capturedCount = snapshot.transactions.size,
                            monthLabel = selectedMonthLabel
                        )
                    }
                } else {
                    items(
                        count = filtered.size,
                        key = { index -> filtered[index].id }
                    ) { index ->
                        ActivityRow(
                            item = filtered[index],
                            onClick = {
                                onTransactionClick(filtered[index].id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityMonthSectionHeader(
    monthLabel: String,
    transactionCount: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (expanded) 2.dp else 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    monthLabel + " activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    transactionCount.toString() +
                        " transaction" +
                        if (transactionCount == 1) "" else "s",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) {
                    Icons.Rounded.ExpandLess
                } else {
                    Icons.Rounded.ExpandMore
                },
                contentDescription = if (expanded) {
                    "Collapse month"
                } else {
                    "Expand month"
                }
            )
        }
    }
}

@Composable
private fun LiveActivityUpdateStatus() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                "Updating activity…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "A financial notification is being checked and reconciled.",
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ActivitySummaryCard(
    snapshot: ActivityLedgerSnapshot,
    monthLabel: String
) {
    val summary = snapshot.summary
    PesaCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        monthLabel,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "${formatMoney(summary.spentMinor)} spent • ${formatMoney(summary.incomeMinor)} income",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (
                    snapshot.possibleDuplicates.isNotEmpty() ||
                    snapshot.reviewGroups.isNotEmpty()
                ) {
                    val duplicateText =
                        if (snapshot.possibleDuplicates.isNotEmpty()) {
                            snapshot.possibleDuplicates.size.toString() +
                                " duplicate" +
                                if (
                                    snapshot.possibleDuplicates.size == 1
                                ) {
                                    ""
                                } else {
                                    "s"
                                }
                        } else {
                            ""
                        }
                    val reviewText =
                        if (snapshot.reviewGroups.isNotEmpty()) {
                            snapshot.reviewGroups.size.toString() +
                                " review"
                        } else {
                            ""
                        }
                    SmallStatusPill(
                        listOf(duplicateText, reviewText)
                            .filter(String::isNotBlank)
                            .joinToString(" • "),
                        "amber"
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            ResponsiveActivityMetrics(snapshot, monthLabel)
        }
    }
}

@Composable
private fun ResponsiveActivityMetrics(
    snapshot: ActivityLedgerSnapshot,
    monthLabel: String
) {
    val summary = snapshot.summary
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric(
                        "Spent",
                        formatMoney(summary.spentMinor),
                        "$monthLabel",
                        Icons.Rounded.Paid,
                        "red",
                        Modifier.weight(1f)
                    )
                    ActivityMetric(
                        "Income",
                        formatMoney(summary.incomeMinor),
                        "$monthLabel",
                        Icons.Rounded.CallReceived,
                        "green",
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActivityMetric(
                        "Fees",
                        formatMoney(summary.feesMinor),
                        "M-Pesa + Fuliza",
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
                ActivityMetric("Spent", formatMoney(summary.spentMinor), "$monthLabel", Icons.Rounded.Paid, "red", Modifier.weight(1f))
                ActivityMetric("Income", formatMoney(summary.incomeMinor), "$monthLabel", Icons.Rounded.CallReceived, "green", Modifier.weight(1f))
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
    capturedCount: Int,
    monthLabel: String
) {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBubble(
                if (selectedFilter == "Review") Icons.Rounded.HelpOutline else Icons.Rounded.ReceiptLong,
                size = 52.dp,
                background = pesaToneContainer(if (selectedFilter == "Review") "amber" else "green"),
                tint = pesaToneContent(if (selectedFilter == "Review") "amber" else "green")
            )
            Text(
                if (selectedFilter == "Review") {
                    "Nothing to review in $monthLabel"
                } else if (capturedCount == 0) {
                    "No activity in $monthLabel"
                } else {
                    "Nothing in $selectedFilter for $monthLabel"
                },
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (selectedFilter == "Review") {
                    "Good — this month's captured transactions currently have no pending review groups."
                } else if (capturedCount == 0) {
                    "Choose another month or refresh after importing older M-Pesa and bank activity."
                } else {
                    "Try another filter to see more activity from $monthLabel."
                },
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PossibleDuplicateCallout(
    pairCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.Rounded.WarningAmber,
                size = 42.dp,
                background =
                    MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                tint = MaterialTheme.colorScheme.error
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    pairCount.toString() +
                        " possible duplicate pair" +
                        if (pairCount == 1) "" else "s",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "These observations are close enough to deserve reconciliation before they are treated as separate transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Review possible duplicates"
            )
        }
    }
}

@Composable
private fun PossibleDuplicateRow(
    group: PossibleDuplicateGroup,
    onSameTransaction: () -> Unit,
    onKeepBoth: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    Icons.Rounded.WarningAmber,
                    size = 40.dp,
                    background = MaterialTheme.colorScheme.errorContainer,
                    tint = MaterialTheme.colorScheme.error
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        formatMoney(group.amountMinor),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Two observations • " +
                            (group.timeDeltaMs / 1000L).toString() +
                            "s apart",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DuplicateObservationLine(
                label = "Observation 1",
                item = group.first
            )
            DuplicateObservationLine(
                label = "Observation 2",
                item = group.second
            )

            Text(
                "PesaTrack will not remove either record unless you confirm they describe the same transaction.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onKeepBoth,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Keep both")
                }
                Button(
                    onClick = onSameTransaction,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Same transaction")
                }
            }
        }
    }
}

@Composable
private fun DuplicateObservationLine(
    label: String,
    item: StoredTransaction
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            duplicateSourceLabel(item) +
                " • " +
                formatPreciseTime(item.receivedAtEpochMs),
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            buildString {
                append(humanize(item.direction))
                if (!item.transactionCode.isNullOrBlank()) {
                    append(" • code ")
                    append(item.transactionCode)
                } else {
                    append(" • no transaction code")
                }
            },
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyDuplicateState(
    monthLabel: String
) {
    PesaCard {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBubble(
                Icons.Rounded.WarningAmber,
                size = 52.dp,
                background = pesaToneContainer("green"),
                tint = pesaToneContent("green")
            )
            Text(
                "No possible duplicates in " + monthLabel,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Any previously reviewed pairs marked Keep both are also kept out of this queue.",
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewQueueCallout(
    groupCount: Int,
    transactionCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = pesaToneContainer("amber")
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.Rounded.HelpOutline,
                size = 42.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                tint = pesaToneContent("amber")
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    "$groupCount group" +
                        if (groupCount == 1) " to review" else "s to review",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$transactionCount pending transaction" +
                        if (transactionCount == 1) "" else "s" +
                        " collapsed by full name or phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewGroupRow(
    group: ReviewGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.Rounded.HelpOutline,
                size = 42.dp,
                background = pesaToneContainer("amber"),
                tint = pesaToneContent("amber")
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    group.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${group.transactionCount} transaction" +
                        if (group.transactionCount == 1) "" else "s" +
                        " • ${formatMoney(group.totalMinor)} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap once to classify and optionally remember this identity.",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = pesaToneContent("amber")
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                if (
                    item.counterparty.isNullOrBlank() &&
                    !item.evidenceLabel.isNullOrBlank()
                ) {
                    Text(
                        "Possible match: " + item.evidenceLabel,
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.fulizaFinancedMinor > 0L) {
                    Text(
                        "Fuliza financed " + formatMoney(item.fulizaFinancedMinor) +
                            if (item.fulizaAccessFeeMinor > 0L) {
                                " • access fee " + formatMoney(item.fulizaAccessFeeMinor)
                            } else {
                                ""
                            },
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amountLabel(item, direction),
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        direction == MoneyDirection.INCOME ||
                            item.type == MpesaTransactionType.REVERSAL.name ->
                            MaterialTheme.colorScheme.primary
                        direction == MoneyDirection.EXPENSE ->
                            MaterialTheme.colorScheme.error
                        needsReview || direction == MoneyDirection.NEUTRAL ->
                            pesaToneContent("amber")
                        else -> MaterialTheme.colorScheme.onSurface
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

private fun monthLabel(epochMs: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(epochMs))

private fun monthStart(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun monthStart(year: Int, month: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis


private fun duplicateSourceLabel(
    item: StoredTransaction
): String =
    item.counterparty
        ?.takeIf(String::isNotBlank)
        ?: item.evidenceSource
            ?.takeIf(String::isNotBlank)
        ?: item.sourcePackage
            ?.substringAfterLast('.')
            ?.takeIf(String::isNotBlank)
        ?: humanize(item.sourceKind)

private fun formatPreciseTime(epochMs: Long): String =
    SimpleDateFormat(
        "d MMM, HH:mm:ss",
        Locale.getDefault()
    ).format(Date(epochMs))

private fun humanize(value: String): String =
    value.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

private fun TransactionCategory.prettyName(): String =
    name.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
