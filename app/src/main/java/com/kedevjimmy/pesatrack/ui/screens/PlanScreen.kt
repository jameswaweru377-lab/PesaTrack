package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class BudgetLine(
    val name: String,
    val spent: String,
    val fraction: Float,
    val type: String,
    val icon: ImageVector
)

@Composable
fun PlanScreen(scaffoldPadding: PaddingValues) {
    val budgets = listOf(
        BudgetLine("Rent", "KES 6,000 / 6,000", 1f, "Fixed", Icons.Rounded.Home),
        BudgetLine("Lunch", "KES 2,900 / 4,000", .725f, "Flexible", Icons.Rounded.Restaurant),
        BudgetLine("Electricity", "KES 1,200 / 2,000", .60f, "Fixed", Icons.Rounded.Bolt),
        BudgetLine("Internet", "KES 1,500 / 2,000", .75f, "Fixed", Icons.Rounded.Wifi),
        BudgetLine("Shopping", "KES 1,400 / 4,000", .35f, "Flexible", Icons.Rounded.ShoppingCart)
    )

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Plan", style = MaterialTheme.typography.headlineMedium)
            Text("Protect commitments before spending the rest.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Monthly plan", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlanMetric("Income", "KES 28K", Modifier.weight(1f))
                        PlanMetric("Planned", "KES 13.8K", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlanMetric("Savings", "KES 12.2K", Modifier.weight(1f))
                        PlanMetric("Unallocated", "KES 2K", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text("Budget categories", style = MaterialTheme.typography.titleLarge)
        }

        items(budgets.size) { index ->
            BudgetRow(budgets[index])
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("On pace", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This card will eventually compare your current pace with the days remaining in your income cycle.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanMetric(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BudgetRow(item: BudgetLine) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(item.type, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(item.spent)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { item.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
