package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.MintSoft
import com.kedevjimmy.pesatrack.ui.theme.SkySoft
import com.kedevjimmy.pesatrack.ui.theme.VioletSoft

private data class GoalCardModel(
    val title: String,
    val saved: String,
    val total: String,
    val percent: String,
    val progress: Float,
    val estimate: String,
    val monthly: String,
    val icon: ImageVector,
    val tone: String
)

@Composable
fun GoalsScreen(
    scaffoldPadding: PaddingValues,
    onGoalClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val goals = listOf(
        GoalCardModel("Couch", "KES 28,000", "60,000", "47%", .47f, "Est. Dec 2024", "KES 2,000", Icons.Rounded.Chair, "green"),
        GoalCardModel("PS5", "KES 18,500", "75,000", "25%", .25f, "Est. Mar 2025", "KES 3,000", Icons.Rounded.SportsEsports, "blue"),
        GoalCardModel("Emergency Fund", "KES 12,000", "50,000", "24%", .24f, "Est. Feb 2025", "KES 2,500", Icons.Rounded.Favorite, "amber"),
        GoalCardModel("Land", "KES 4,900", "200,000", "2%", .02f, "Est. Mar 2026", "KES 1,000", Icons.Rounded.Landscape, "violet")
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
                    Text("Goals", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Turn your dreams into reality.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(onClick = onProfileClick),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            LandscapeBackdrop(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "You've saved",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("KES 33,400", style = MaterialTheme.typography.displaySmall)
                    Text(
                        "across 4 goals",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                IconBubble(Icons.Rounded.Savings, size = 38.dp)
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text("Monthly contributions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("KES 8,400", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                IconBubble(Icons.Rounded.CalendarMonth, size = 38.dp)
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text("Next contribution", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("1 Oct 2024", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = pesaToneContainer("amber")
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(
                        Icons.AutoMirrored.Rounded.TrendingUp,
                        size = 50.dp,
                        background = pesaToneContainer("amber"),
                        tint = pesaToneContent("amber")
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            "You're on track to finish Couch by December.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Keep going! You're 47% there.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Your goals", "See all")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalCard(goals[0], Modifier.weight(1f), onGoalClick)
                GoalCard(goals[1], Modifier.weight(1f), onGoalClick)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalCard(goals[2], Modifier.weight(1f), onGoalClick)
                GoalCard(goals[3], Modifier.weight(1f), onGoalClick)
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Completed goals", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = {}) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.padding(end = 5.dp))
                    Text("New goal")
                }
            }
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Row(
                    Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(Icons.Rounded.CheckCircle, size = 42.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    ) {
                        Text("New Phone", style = MaterialTheme.typography.titleMedium)
                        Text("Completed • Aug 2024", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("KES 45,000", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalCardModel,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
.background(pesaToneContainer(goal.tone)),
                contentAlignment = Alignment.Center
            ) {
                IconBubble(
                    goal.icon,
                    size = 62.dp,
                    background = MaterialTheme.colorScheme.surfaceVariant,
                    tint = pesaToneContent(goal.tone)
                )
                IconButton(
                    onClick = {},
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Rounded.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(goal.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${goal.saved} of ${goal.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { goal.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        goal.percent,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(goal.estimate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(goal.monthly, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("per month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
