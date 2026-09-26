package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class DemoTransaction(
    val name: String,
    val category: String,
    val amount: String,
    val time: String,
    val icon: ImageVector
)

@Composable
fun ActivityScreen(scaffoldPadding: PaddingValues) {
    val items = listOf(
        DemoTransaction("Family Wings", "Rent", "- KES 6,000", "18:24", Icons.Rounded.Payments),
        DemoTransaction("Elizabeth", "Lunch", "- KES 450", "13:15", Icons.Rounded.Restaurant),
        DemoTransaction("Planet Internet", "Internet", "- KES 1,500", "09:32", Icons.Rounded.Wifi),
        DemoTransaction("KPLC", "Electricity", "- KES 1,200", "Yesterday", Icons.Rounded.Bolt),
        DemoTransaction("Joseph", "Shopping", "- KES 3,200", "Yesterday", Icons.Rounded.ShoppingCart)
    )

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Activity", style = MaterialTheme.typography.headlineMedium)
                    Text("Every money movement, understandable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = true, onClick = {}, label = { Text("All") }) }
                item { FilterChip(selected = false, onClick = {}, label = { Text("Expenses") }) }
                item { FilterChip(selected = false, onClick = {}, label = { Text("Income") }) }
                item { FilterChip(selected = false, onClick = {}, label = { Text("Transfers") }) }
                item { FilterChip(selected = false, onClick = {}, label = { Text("Fees") }) }
                item { FilterChip(selected = false, onClick = {}, label = { Text("Needs review") }) }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Row(
                    Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric("Spent", "KES 12.3K", Modifier.weight(1f))
                    SummaryMetric("Income", "KES 45K", Modifier.weight(1f))
                    SummaryMetric("Fees", "KES 428", Modifier.weight(1f))
                }
            }
        }

        item {
            Text("Recent", style = MaterialTheme.typography.titleLarge)
        }

        items(items.size) { index ->
            TransactionRow(items[index])
        }

        item {
            Text(
                "Preview transactions only — real Activity data will come from the local transaction store.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(end = 8.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TransactionRow(item: DemoTransaction) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text("${item.category} • ${item.time}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.amount, style = MaterialTheme.typography.titleMedium)
        }
    }
}
