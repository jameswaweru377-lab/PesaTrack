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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.CommitmentProgress
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.SalaryCycleCandidate
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.FundingGapCard
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    scaffoldPadding: PaddingValues,
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val cycleStore = remember { PayCycleStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }

    var cycleSnapshot by remember {
        cycleStore.rebuild()
        mutableStateOf(cycleStore.snapshot())
    }
    var ledgerSnapshot by remember {
        mutableStateOf(transactionStore.snapshot())
    }
    var activitySnapshot by remember {
        mutableStateOf(transactionStore.activitySnapshot())
    }

    fun refresh() {
        cycleSnapshot = cycleStore.snapshot()
        ledgerSnapshot = transactionStore.snapshot()
        activitySnapshot = transactionStore.activitySnapshot()
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "PesaTrack",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        if (cycleSnapshot.cycle == null) {
                            "Waiting for your first recognized salary cycle."
                        } else {
                            "Your money between salaries."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onProfileClick),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        cycleSnapshot.pendingSalary?.let { candidate ->
            item {
                PendingSalaryCard(
                    candidate = candidate,
                    onStartCycle = {
                        cycleStore.confirmNewCycle(candidate.transactionId)
                        refresh()
                    },
                    onKeepAsIncome = {
                        cycleStore.keepAsAdditionalIncome(candidate.transactionId)
                        refresh()
                    }
                )
            }
        }

        item {
            PayCycleHero(cycleSnapshot)
        }

        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 600.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MoneyMetric(
                                label = "Starting funds",
                                value = formatMoney(cycleSnapshot.startingFundsMinor),
                                helper = carryoverHelper(cycleSnapshot),
                                tone = "green",
                                modifier = Modifier.weight(1f)
                            )
                            MoneyMetric(
                                label = "Committed",
                                value = formatMoney(cycleSnapshot.remainingCommitmentsMinor),
                                helper = "Reserved bills",
                                tone = "amber",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MoneyMetric(
                                label = "Saved",
                                value = formatMoney(cycleSnapshot.savingsMinor),
                                helper = "Deliberate savings",
                                tone = "blue",
                                modifier = Modifier.weight(1f)
                            )
                            MoneyMetric(
                                label = "Spent",
                                value = formatMoney(cycleSnapshot.spentMinor + cycleSnapshot.feesMinor),
                                helper = "This pay cycle",
                                tone = "violet",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MoneyMetric("Starting funds", formatMoney(cycleSnapshot.startingFundsMinor), carryoverHelper(cycleSnapshot), "green", Modifier.weight(1f))
                        MoneyMetric("Committed", formatMoney(cycleSnapshot.remainingCommitmentsMinor), "Reserved bills", "amber", Modifier.weight(1f))
                        MoneyMetric("Saved", formatMoney(cycleSnapshot.savingsMinor), "Deliberate savings", "blue", Modifier.weight(1f))
                        MoneyMetric("Spent", formatMoney(cycleSnapshot.spentMinor + cycleSnapshot.feesMinor), "This pay cycle", "violet", Modifier.weight(1f))
                    }
                }
            }
        }

        if (cycleSnapshot.unexplainedFundingGapMinor > 0L) {
            item {
                FundingGapCard(cycleSnapshot.unexplainedFundingGapMinor)
            }
        }

        if (ledgerSnapshot.latestBalanceMinor != null) {
            item {
                PesaCard {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBubble(Icons.Rounded.AccountBalanceWallet, size = 44.dp)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 11.dp)
                        ) {
                            Text(
                                "Last known M-Pesa balance",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                formatMoney(ledgerSnapshot.latestBalanceMinor ?: 0L),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            ledgerSnapshot.latestBalanceObservedAtEpochMs?.let { observed ->
                                Text(
                                    "Observed " + formatDateTime(observed) + " • not a live bank balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (cycleSnapshot.commitments.isNotEmpty()) {
            item {
                SectionHeader("Protected this cycle")
            }
            items(cycleSnapshot.commitments.take(4).size) { index ->
                CommitmentRow(cycleSnapshot.commitments[index])
            }
        }

        if (activitySnapshot.summary.needsReviewCount > 0) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = pesaToneContainer("amber")
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                activitySnapshot.summary.needsReviewCount.toString() + " transaction(s) need review",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "They stay out of trusted totals until you classify them.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            PesaCard {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(Icons.Rounded.CalendarMonth, size = 42.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 11.dp)
                    ) {
                        Text(
                            "Calendar month is still available",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            formatMoney(activitySnapshot.summary.spentMinor) +
                                " spent this calendar month. Your pay-cycle plan is tracked separately.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PayCycleHero(snapshot: PayCycleSnapshot) {
    LandscapeBackdrop(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (snapshot.cycle == null) 245.dp else 300.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                if (snapshot.cycle == null) "No active pay cycle yet" else "Available to Live",
                style = MaterialTheme.typography.titleLarge
            )

            if (snapshot.cycle == null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Once a saved Salary rule recognizes an incoming salary, PesaTrack will start the cycle automatically. The next accepted salary closes it.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "No totals are fabricated while PesaTrack is waiting for real data.",
                            modifier = Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@LandscapeBackdrop
            }

            Text(
                formatMoney(snapshot.availableToLiveMinor),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Free after tracked spending, savings and protected commitments.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .96f)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroItem(
                        "Cycle started",
                        formatDate(snapshot.cycle.startEpochMs),
                        Icons.Rounded.CalendarMonth,
                        Modifier.weight(1f)
                    )
                    HeroItem(
                        "Safe today",
                        snapshot.safeTodayMinor?.let(::formatMoney) ?: "Learning",
                        Icons.Rounded.Schedule,
                        Modifier.weight(1f)
                    )
                    HeroItem(
                        "Next salary",
                        snapshot.estimatedDaysRemaining?.let { "≈ " + it + " days" } ?: "Not estimated",
                        Icons.Rounded.Paid,
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingSalaryCard(
    candidate: SalaryCycleCandidate,
    onStartCycle: () -> Unit,
    onKeepAsIncome: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = pesaToneContainer("amber")
    ) {
        Column(Modifier.padding(16.dp)) {
            SmallStatusPill("Salary check", "amber")
            Text(
                candidate.label + " • " + formatMoney(candidate.amountMinor),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                candidate.reason,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Received " + formatDate(candidate.occurredAtEpochMs),
                modifier = Modifier.padding(top = 4.dp),
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
                    onClick = onKeepAsIncome,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Extra income")
                }
                Button(
                    onClick = onStartCycle,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start new cycle")
                }
            }
        }
    }
}

@Composable
private fun MoneyMetric(
    label: String,
    value: String,
    helper: String,
    tone: String,
    modifier: Modifier
) {
    PesaCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            IconBubble(
                when (label) {
                    "Saved" -> Icons.Rounded.Savings
                    "Committed" -> Icons.Rounded.Shield
                    else -> Icons.Rounded.Paid
                },
                size = 36.dp,
                background = pesaToneContainer(tone),
                tint = pesaToneContent(tone)
            )
            Text(
                label,
                modifier = Modifier.padding(top = 9.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
            Text(
                helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CommitmentRow(item: CommitmentProgress) {
    PesaCard {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                Icons.Rounded.Shield,
                size = 40.dp,
                background = pesaToneContainer(
                    if (item.remainingMinor > 0L) "amber" else "green"
                ),
                tint = pesaToneContent(
                    if (item.remainingMinor > 0L) "amber" else "green"
                )
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (item.remainingMinor > 0L) {
                        formatMoney(item.remainingMinor) + " still protected"
                    } else {
                        "Paid this cycle"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatMoney(item.plannedMinor),
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun HeroItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Column(modifier) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2
        )
    }
}

private fun carryoverHelper(snapshot: PayCycleSnapshot): String =
    if (snapshot.cycle == null) {
        "No active cycle"
    } else if (snapshot.cycle.carryoverMinor == 0L) {
        "Salary + other income"
    } else {
        "Includes " + formatMoney(snapshot.cycle.carryoverMinor) + " carryover"
    }

private fun formatMoney(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return if (minor < 0) {
        "-KES " + formatter.format(-minor / 100.0)
    } else {
        "KES " + formatter.format(minor / 100.0)
    }
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))

private fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(epochMs))
