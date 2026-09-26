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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import com.kedevjimmy.pesatrack.ui.theme.Amber
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.MintSoft

@Composable
fun GoalDetailScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text("Couch", style = MaterialTheme.typography.headlineMedium)
                    Text("Goal details", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                }
            }
        }

        item {
            LandscapeBackdrop(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                dark = MaterialTheme.colorScheme.background.luminance() < .2f
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.Chair, size = 54.dp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Couch", style = MaterialTheme.typography.titleLarge)
                            Text("Target amount", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("KES 40,000", style = MaterialTheme.typography.displaySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column {
                            Text("KES 28,000", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            Text("Saved so far", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("KES 12,000", style = MaterialTheme.typography.titleLarge)
                            Text("Remaining", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { .70f },
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                        Text("70%", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            PesaCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.CalendarMonth, size = 44.dp)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 11.dp)
                        ) {
                            Text("On track for December", style = MaterialTheme.typography.titleMedium)
                            Text("You'll reach your goal in 3 months", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    GoalTimeline()
                }
            }
        }

        item {
            PesaCard {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(Icons.Rounded.Savings, size = 44.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 11.dp)
                    ) {
                        Text("Planned monthly contribution", style = MaterialTheme.typography.titleMedium)
                        Text("KES 5,000 per month", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {}) {
                        Text("Edit")
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = pesaToneContainer("amber"),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, pesaToneContent("amber").copy(alpha = .55f))
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(
                        Icons.Rounded.TrendingUp,
                        tint = pesaToneContent("amber"),
                        background = pesaToneContainer("amber"),
                        size = 48.dp
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            "Save faster",
                            style = MaterialTheme.typography.titleMedium,
                            color = pesaToneContent("amber")
                        )
                        Text(
                            "If you add KES 5,000 this month, you'll finish 3 weeks earlier.",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Contribution history", "See all")
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    ContributionRow("Manual deposit", "21 Sep 2024, 10:24 AM", "+ KES 5,000")
                    ContributionRow("Auto-savings", "1 Sep 2024, 09:00 AM", "+ KES 5,000")
                    ContributionRow("Manual deposit", "14 Aug 2024, 02:18 PM", "+ KES 5,000")
                    ContributionRow("Auto-savings", "1 Aug 2024, 09:00 AM", "+ KES 5,000")
                }
            }
        }

        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 600.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, null, modifier = Modifier.padding(end = 5.dp))
                                Text("Edit goal")
                            }
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Pause, null, modifier = Modifier.padding(end = 5.dp))
                                Text("Pause")
                            }
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.padding(end = 5.dp))
                            Text("Mark complete")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.padding(end = 5.dp))
                            Text("Edit goal")
                        }
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Rounded.Pause, null, modifier = Modifier.padding(end = 5.dp))
                            Text("Pause")
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1.25f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.padding(end = 5.dp))
                            Text("Complete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributionRow(title: String, date: String, amount: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(Icons.Rounded.Add, size = 40.dp)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 11.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(amount, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun GoalTimeline() {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("May", "Jul", "Sep", "Dec").forEachIndexed { index, label ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (index < 2) Icons.Rounded.CheckCircle else Icons.Rounded.CalendarMonth,
                        null,
                        tint = if (index <= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        listOf("10K", "20K", "28K", "40K")[index],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

