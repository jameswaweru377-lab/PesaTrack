package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.CommitmentProgress
import com.kedevjimmy.pesatrack.data.local.PayCycleSnapshot
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun PlanScreen(scaffoldPadding: PaddingValues) {
    val context = LocalContext.current
    val store = remember { PayCycleStore.get(context) }
    val snapshot = remember {
        store.rebuild()
        store.snapshot()
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Plan", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (snapshot.cycle == null) {
                        "Your plan will begin when PesaTrack recognizes a salary."
                    } else {
                        "Protected commitments for your current pay cycle."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            PlanHero(snapshot)
        }

        if (snapshot.cycle != null) {
            item {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 600.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlanMetric(
                                    "Starting funds",
                                    formatPlanMoney(snapshot.startingFundsMinor),
                                    "Salary + carryover",
                                    "green",
                                    Modifier.weight(1f)
                                )
                                PlanMetric(
                                    "Committed",
                                    formatPlanMoney(snapshot.remainingCommitmentsMinor),
                                    "Still protected",
                                    "amber",
                                    Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlanMetric(
                                    "Saved",
                                    formatPlanMoney(snapshot.savingsMinor),
                                    "Moved to savings",
                                    "blue",
                                    Modifier.weight(1f)
                                )
                                PlanMetric(
                                    "Available",
                                    formatPlanMoney(snapshot.availableToLiveMinor),
                                    "Free to live",
                                    "violet",
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PlanMetric("Starting funds", formatPlanMoney(snapshot.startingFundsMinor), "Salary + carryover", "green", Modifier.weight(1f))
                            PlanMetric("Committed", formatPlanMoney(snapshot.remainingCommitmentsMinor), "Still protected", "amber", Modifier.weight(1f))
                            PlanMetric("Saved", formatPlanMoney(snapshot.savingsMinor), "Moved to savings", "blue", Modifier.weight(1f))
                            PlanMetric("Available", formatPlanMoney(snapshot.availableToLiveMinor), "Free to live", "violet", Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                SectionHeader("Committed this cycle")
                Text(
                    "A recurring bill is protected as soon as the pay cycle starts. Its actual payment message reduces the remaining commitment.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (snapshot.commitments.isEmpty()) {
                item {
                    PesaCard {
                        Column(Modifier.padding(18.dp)) {
                            IconBubble(Icons.Rounded.Shield, size = 46.dp)
                            Text(
                                "No recurring commitments yet",
                                modifier = Modifier.padding(top = 10.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Create Rent, Internet, Electricity or another recurring rule. Add an expected amount, or let PesaTrack learn it from a matched payment.",
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(snapshot.commitments.size) { index ->
                    PlanCommitment(snapshot.commitments[index])
                }
            }

            item {
                SectionHeader("Cycle accounting")
                Spacer(Modifier.height(6.dp))
                PesaCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccountingRow("Salary + carryover", snapshot.startingFundsMinor)
                        AccountingRow("Other income", snapshot.additionalIncomeMinor)
                        AccountingRow("Spent", -snapshot.spentMinor)
                        AccountingRow("Fees", -snapshot.feesMinor)
                        AccountingRow("Savings allocations", -snapshot.savingsMinor)
                        AccountingRow("Still committed", -snapshot.remainingCommitmentsMinor)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Available to Live",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    formatPlanMoney(snapshot.availableToLiveMinor),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanHero(snapshot: PayCycleSnapshot) {
    LandscapeBackdrop(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (snapshot.cycle == null) 230.dp else 260.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                if (snapshot.cycle == null) "No active pay cycle" else "Current pay cycle",
                style = MaterialTheme.typography.titleLarge
            )

            if (snapshot.cycle == null) {
                Text(
                    "Create a Salary smart rule and let a matching salary message start the cycle. No fixed payday is required.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@LandscapeBackdrop
            }

            Text(
                formatPlanDate(snapshot.cycle.startEpochMs) + " → next accepted salary",
                modifier = Modifier.padding(top = 4.dp),
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
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 11.dp)
                    ) {
                        Text(
                            "Available to Live",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatPlanMoney(snapshot.availableToLiveMinor),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Safe today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            snapshot.safeTodayMinor?.let(::formatPlanMoney) ?: "Learning",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanMetric(
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
                    "Starting funds" -> Icons.Rounded.AccountBalanceWallet
                    else -> Icons.Rounded.Paid
                },
                size = 36.dp,
                background = pesaToneContainer(tone),
                tint = pesaToneContent(tone)
            )
            Text(
                label,
                modifier = Modifier.padding(top = 9.dp),
                style = MaterialTheme.typography.bodySmall,
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
private fun PlanCommitment(item: CommitmentProgress) {
    val progress = if (item.plannedMinor <= 0L) {
        0f
    } else {
        (item.paidMinor.toFloat() / item.plannedMinor.toFloat()).coerceIn(0f, 1f)
    }

    PesaCard {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    if (item.remainingMinor == 0L) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                    size = 42.dp,
                    background = pesaToneContainer(
                        if (item.remainingMinor == 0L) "green" else "amber"
                    ),
                    tint = pesaToneContent(
                        if (item.remainingMinor == 0L) "green" else "amber"
                    )
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (item.remainingMinor == 0L) {
                            "Paid"
                        } else {
                            formatPlanMoney(item.remainingMinor) + " still reserved"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatPlanMoney(item.plannedMinor),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp)
                    .height(8.dp),
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun AccountingRow(
    label: String,
    value: Long
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            formatSignedPlanMoney(value),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

private fun formatPlanMoney(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return if (minor < 0) {
        "-KES " + formatter.format(-minor / 100.0)
    } else {
        "KES " + formatter.format(minor / 100.0)
    }
}

private fun formatSignedPlanMoney(minor: Long): String =
    if (minor < 0) {
        "- " + formatPlanMoney(-minor)
    } else {
        "+ " + formatPlanMoney(minor)
    }

private fun formatPlanDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))
