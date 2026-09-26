package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.LandscapeBackdrop
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.theme.Amber
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.MintSoft
import com.kedevjimmy.pesatrack.ui.theme.RoseSoft
import com.kedevjimmy.pesatrack.ui.theme.SkySoft

@Composable
fun HomeScreen(scaffoldPadding: PaddingValues) {
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
                        "Good evening, ",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "James",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Your money. A calmer tomorrow.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Shield,
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
                    .height(252.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Available to live",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Rounded.Info,
                            null,
                            modifier = Modifier
                                .padding(start = 5.dp)
                                .size(17.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "KES 12,850",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            " Safe today: KES 428",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CompactMoneyMetric(
                                "Current money",
                                "KES 28,450",
                                Icons.Rounded.AccountBalanceWallet,
                                Modifier.weight(1f)
                            )
                            CompactMoneyMetric(
                                "Committed",
                                "KES 10,200",
                                Icons.Rounded.CalendarMonth,
                                Modifier.weight(1f),
                                "amber"
                            )
                            CompactMoneyMetric(
                                "Saved",
                                "KES 5,400",
                                Icons.Rounded.Savings,
                                Modifier.weight(1f)
                            )
                            CompactMoneyMetric(
                                "Free",
                                "KES 12,850",
                                Icons.Rounded.Paid,
                                Modifier.weight(1f),
                                "blue"
                            )
                        }
                    }
                }
            }
        }

        item {
            PesaCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("September", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "You've spent KES 16,200 so far",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "70% of your normal pace",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    SpendingChart()
                    Row(Modifier.fillMaxWidth()) {
                        listOf("1 Sep", "7 Sep", "14 Sep", "21 Sep", "30 Sep").forEach {
                            Text(
                                it,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AmberSoft)
            ) {
                Row(
                    Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubble(
                        Icons.Rounded.Restaurant,
                        tint = Amber,
                        background = Color(0xFFFFE5AA),
                        size = 50.dp
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "One thing to know",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF9A650D)
                        )
                        Text(
                            "Lunch is running 18% ahead of your normal pace.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "You've spent KES 2,900 on lunch this month.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("This month", "See all")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(
                    "Spent",
                    "KES 16,200",
                    "↑ 12%",
                    Icons.Rounded.Paid,
                    Modifier.weight(1f),
                    RoseSoft
                )
                SummaryCard(
                    "Saved",
                    "KES 5,400",
                    "↑ 8%",
                    Icons.Rounded.Savings,
                    Modifier.weight(1f),
                    MintSoft
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(
                    "Bills",
                    "KES 10,200",
                    "3 upcoming",
                    Icons.Rounded.CalendarMonth,
                    Modifier.weight(1f),
                    SkySoft
                )
                SummaryCard(
                    "Free to spend",
                    "KES 12,850",
                    "Good",
                    Icons.Rounded.Shield,
                    Modifier.weight(1f),
                    MintSoft
                )
            }
        }

        item {
            SectionHeader("Goals", "See all")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalMiniCard(
                    "Couch",
                    "KES 28,000 of 60,000",
                    .47f,
                    "47%",
                    Icons.Rounded.Chair,
                    Modifier.weight(1f)
                )
                GoalMiniCard(
                    "PS5",
                    "KES 18,500 of 75,000",
                    .25f,
                    "25%",
                    Icons.Rounded.SportsEsports,
                    Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeader("Upcoming", "See all")
            Spacer(Modifier.height(8.dp))
            PesaCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    UpcomingRow(Icons.Rounded.CalendarMonth, "Rent", "Due in 5 days · 25 Sep", "KES 6,000", "amber")
                    UpcomingRow(Icons.Rounded.Wifi, "Internet", "Due in 8 days · 28 Sep", "KES 1,500", "blue")
                    UpcomingRow(Icons.Rounded.Bolt, "Electricity", "Due in 12 days · 2 Oct", "KES 1,200", "amber")
                }
            }
        }
    }
}

@Composable
private fun CompactMoneyMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    tone: String = "green"
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IconBubble(
            icon = icon,
            size = 30.dp,
            background = when (tone) {
                "amber" -> AmberSoft
                "blue" -> SkySoft
                else -> MintSoft
            }
        )
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    bubble: Color
) {
    PesaCard(modifier) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(icon, background = bubble, size = 38.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium)
            }
            SmallStatusPill(tag)
        }
    }
}

@Composable
private fun GoalMiniCard(
    title: String,
    value: String,
    progress: Float,
    percent: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    PesaCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(icon, size = 38.dp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    percent,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun UpcomingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    amount: String,
    tone: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            icon,
            size = 38.dp,
            background = if (tone == "blue") SkySoft else AmberSoft
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(amount, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SpendingChart() {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = .45f)
    val fill = primary.copy(alpha = .10f)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(118.dp)
    ) {
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        val points = listOf(
            Offset(0f, size.height * .82f),
            Offset(size.width * .16f, size.height * .73f),
            Offset(size.width * .31f, size.height * .61f),
            Offset(size.width * .47f, size.height * .66f),
            Offset(size.width * .62f, size.height * .56f),
            Offset(size.width * .74f, size.height * .33f)
        )

        val area = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(area, fill)

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, primary, style = Stroke(width = 5f, cap = StrokeCap.Round))
        drawCircle(primary, radius = 7f, center = points.last())

        drawLine(
            color = primary.copy(alpha = .45f),
            start = points.last(),
            end = Offset(size.width, size.height * .18f),
            strokeWidth = 4f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 9f))
        )
    }
}
