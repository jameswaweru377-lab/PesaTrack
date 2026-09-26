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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class GoalUi(
    val title: String,
    val saved: String,
    val progress: Float,
    val estimate: String,
    val icon: ImageVector
)

@Composable
fun GoalsScreen(scaffoldPadding: PaddingValues) {
    val goals = listOf(
        GoalUi("Couch", "KES 28K / 60K", .47f, "Target: December", Icons.Rounded.Chair),
        GoalUi("PS5", "KES 18.5K / 75K", .25f, "Target: March", Icons.Rounded.SportsEsports),
        GoalUi("Emergency fund", "KES 12K / 50K", .24f, "Build resilience", Icons.Rounded.Favorite),
        GoalUi("Land", "KES 4.9K / 200K", .02f, "Long-term goal", Icons.Rounded.Landscape)
    )

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Goals", style = MaterialTheme.typography.headlineMedium)
            Text("Turn future purchases into visible progress.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("Across your goals", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KES 63,400", style = MaterialTheme.typography.displaySmall)
                    Text("Preview savings total", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        items((goals.size + 1) / 2) { rowIndex ->
            val left = goals[rowIndex * 2]
            val right = goals.getOrNull(rowIndex * 2 + 1)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalCard(left, Modifier.weight(1f))
                if (right != null) GoalCard(right, Modifier.weight(1f))
                else Spacer(Modifier.weight(1f))
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Later: Can I afford it?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A purchase simulator will show what a goal or one-off purchase does to commitments, reserves and completion dates—without making the decision for you.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: GoalUi, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Icon(goal.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(goal.title, style = MaterialTheme.typography.titleMedium)
            Text(goal.saved, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(goal.estimate, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
