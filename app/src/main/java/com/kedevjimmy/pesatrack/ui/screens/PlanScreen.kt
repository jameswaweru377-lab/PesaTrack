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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.MintSoft
import com.kedevjimmy.pesatrack.ui.theme.RoseSoft
import com.kedevjimmy.pesatrack.ui.theme.SkySoft

private data class PlanBudget(
    val name: String,
    val type: String,
    val amount: String,
    val percent: String,
    val progress: Float,
    val icon: ImageVector,
    val tone: String
)

private data class RecurringBill(
    val title: String,
    val due: String,
    val amount: String,
    val icon: ImageVector,
    val tone: String
)

@Composable
fun PlanScreen(scaffoldPadding: PaddingValues) {
    val budgets = listOf(
        PlanBudget("Rent", "Fixed", "KES 6,000 / 6,000", "100%", 1f, Icons.Rounded.Home, "amber"),
        PlanBudget("Lunch", "Flexible", "KES 2,900 / 4,000", "73%", .73f, Icons.Rounded.Restaurant, "green"),
        PlanBudget("Electricity", "Fixed", "KES 1,200 / 2,000", "60%", .60f, Icons.Rounded.Bolt, "amber"),
        PlanBudget("Internet", "Fixed", "KES 1,500 / 2,000", "75%", .75f, Icons.Rounded.Wifi, "blue"),
        PlanBudget("Transport", "Flexible", "KES 1,800 / 3,000", "60%", .60f, Icons.Rounded.AccountBalanceWallet, "green"),
        PlanBudget("Shopping", "Flexible", "KES 1,400 / 4,000", "35%", .35f, Icons.Rounded.ShoppingBag, "red"),
        PlanBudget("Family", "Flexible", "KES 1,000 / 2,000", "50%", .50f, Icons.Rounded.Groups, "blue")
    )

    val bills = listOf(
        RecurringBill("Rent", "Due in 5 days · 25 Sep", "KES 6,000", Icons.Rounded.Home, "amber"),
        RecurringBill("Planet Internet", "Due in 8 days · 28 Sep", "KES 1,500", Icons.Rounded.Wifi, "blue"),
        RecurringBill("KPLC Electricity", "Due in 12 days · 2 Oct", "KES 1,200", Icons.Rounded.Bolt, "amber")
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
                    Text("Plan", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Budget. Bills. A brighter tomorrow.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.padding(end = 6.dp))
                    Text("September")
                }
            }
        }

        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 600.dp
                LandscapeBackdrop(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 340.dp else 230.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Monthly plan",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .96f)
                        ) {
                            if (compact) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        PlanTopMetric("Monthly income", "KES 28,000", "↑ 0%", Icons.Rounded.AccountBalanceWallet, Modifier.weight(1f), "green")
                                        PlanTopMetric("Planned spending", "KES 13,800", "↑ 5%", Icons.Rounded.ShoppingBag, Modifier.weight(1f), "red")
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        PlanTopMetric("Planned savings", "KES 12,200", "↑ 8%", Icons.Rounded.Savings, Modifier.weight(1f), "green")
                                        PlanTopMetric("Unallocated", "KES 2,000", "7% of income", Icons.Rounded.TrendingUp, Modifier.weight(1f), "blue")
                                    }
                                }
                            } else {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PlanTopMetric("Monthly income", "KES 28,000", "↑ 0%", Icons.Rounded.AccountBalanceWallet, Modifier.weight(1f), "green")
                                    PlanTopMetric("Planned spending", "KES 13,800", "↑ 5%", Icons.Rounded.ShoppingBag, Modifier.weight(1f), "red")
                                    PlanTopMetric("Planned savings", "KES 12,200", "↑ 8%", Icons.Rounded.Savings, Modifier.weight(1f), "green")
                                    PlanTopMetric("Unallocated", "KES 2,000", "7% of income", Icons.Rounded.TrendingUp, Modifier.weight(1f), "blue")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            PesaCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Budget categories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = {},
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.padding(end = 4.dp))
                            Text("Add category")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    budgets.forEach { BudgetRow(it) }
                }
            }
        }

        item {
            SectionHeader("Recurring bills", "See all")
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    bills.forEach { bill ->
                        RecurringBillRow(bill)
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MintSoft,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(Icons.Rounded.TrendingUp, size = 40.dp, background = MintSoft)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    ) {
                        Text(
                            "You are on pace to save KES 12,200 this month.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "You're 8% ahead of your savings goal.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Edit, null, modifier = Modifier.padding(end = 8.dp))
                Text("Edit plan")
            }
        }
    }
}

@Composable
private fun PlanTopMetric(
    label: String,
    value: String,
    trend: String,
    icon: ImageVector,
    modifier: Modifier,
    tone: String
) {
    Column(modifier) {
        IconBubble(
            icon,
            size = 34.dp,
            background = pesaToneContainer(tone),
            tint = pesaToneContent(tone)
        )
        Spacer(Modifier.height(7.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        SmallStatusPill(trend, if (tone == "red") "amber" else if (tone == "blue") "blue" else "green")
    }
}

@Composable
private fun BudgetRow(item: PlanBudget) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                item.icon,
                size = 40.dp,
                background = pesaToneContainer(item.tone),
                tint = pesaToneContent(item.tone)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                SmallStatusPill(item.type, if (item.type == "Fixed") "blue" else "green")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.amount, style = MaterialTheme.typography.bodyMedium)
                Text(
                    item.percent,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LinearProgressIndicator(
            progress = { item.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 51.dp, top = 9.dp)
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun RecurringBillRow(item: RecurringBill) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            item.icon,
            size = 40.dp,
            background = pesaToneContainer(item.tone),
            tint = pesaToneContent(item.tone)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(item.due, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.amount, style = MaterialTheme.typography.titleMedium)
            SmallStatusPill("Monthly", "blue")
        }
    }
}
