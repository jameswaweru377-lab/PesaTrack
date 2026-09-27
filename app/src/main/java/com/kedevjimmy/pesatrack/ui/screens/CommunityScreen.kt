package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.PesaTrackTopBar
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.SmartInsightCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import com.kedevjimmy.pesatrack.ui.theme.PremiumGold
import com.kedevjimmy.pesatrack.ui.theme.PremiumPurple

@Composable
fun CommunityScreen(
    scaffoldPadding: PaddingValues,
    onProfileClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PesaTrackTopBar(onProfileClick = onProfileClick)
        }

        item {
            CommunityHero()
        }

        item {
            SectionHeader(
                title = "Saving challenges",
                action = "Preview"
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ChallengePreviewCard(
                        title = "KES 100K",
                        subtitle = "End Year Challenge",
                        icon = Icons.Rounded.Savings,
                        tone = "green"
                    )
                }
                item {
                    ChallengePreviewCard(
                        title = "Holiday Fund",
                        subtitle = "Travel savings challenge",
                        icon = Icons.Rounded.FlightTakeoff,
                        tone = "blue"
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Trending discussions",
                action = "Preview"
            )
        }

        item {
            DiscussionPreviewCard(
                author = "Wanjiku M.",
                topic = "Budgeting",
                title = "How do you stick to your budget?",
                body = "A preview of the community format: practical Kenyan money conversations, without exposing your private ledger."
            )
        }

        item {
            DiscussionPreviewCard(
                author = "Kevin O.",
                topic = "Tools & Apps",
                title = "What makes a money app genuinely useful?",
                body = "Community is not live yet. This preview shows the visual direction while the real social layer is still future work."
            )
        }

        item {
            SmartInsightCard(
                title = "Money Tip of the Day",
                body = "Pay yourself first. Even a small amount saved regularly can become meaningful over time."
            )
        }
    }
}

@Composable
private fun CommunityHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF06453E),
                        Color(0xFF0A6A59)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(.60f)
                .align(Alignment.CenterStart)
        ) {
            SmallStatusPill("Community preview", "amber")
            Text(
                "A richer you,\ntogether.",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Learn, share, save and grow with other Kenyans—without sharing private transaction data.",
                modifier = Modifier.padding(top = 7.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = .80f)
            )
        }

        CommunityPeopleIllustration(
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun CommunityPeopleIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(
            width = 132.dp,
            height = 150.dp
        )
    ) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = PremiumGold.copy(alpha = .18f),
            radius = w * .47f,
            center = Offset(w * .62f, h * .32f)
        )

        fun person(
            x: Float,
            headY: Float,
            skin: Color,
            shirt: Color,
            scale: Float
        ) {
            val headRadius = w * .09f * scale
            drawCircle(
                color = skin,
                radius = headRadius,
                center = Offset(x, headY)
            )
            drawRoundRect(
                color = shirt,
                topLeft = Offset(
                    x - headRadius * 1.45f,
                    headY + headRadius * 1.05f
                ),
                size = Size(
                    headRadius * 2.9f,
                    headRadius * 4.0f
                ),
                cornerRadius = CornerRadius(
                    headRadius * .8f,
                    headRadius * .8f
                )
            )
        }

        person(
            x = w * .26f,
            headY = h * .33f,
            skin = Color(0xFF8A4E2E),
            shirt = Color(0xFFF1B94F),
            scale = .92f
        )
        person(
            x = w * .55f,
            headY = h * .25f,
            skin = Color(0xFF5D321F),
            shirt = Color(0xFF152E55),
            scale = 1.08f
        )
        person(
            x = w * .80f,
            headY = h * .38f,
            skin = Color(0xFFA9633B),
            shirt = PremiumPurple,
            scale = .88f
        )
    }
}

@Composable
private fun ChallengePreviewCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tone: String
) {
    PesaCard(
        modifier = Modifier.size(
            width = 190.dp,
            height = 158.dp
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            IconBubble(
                icon = icon,
                size = 42.dp,
                background = pesaToneContainer(tone),
                tint = pesaToneContent(tone)
            )
            Text(
                title,
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Preview challenge",
                modifier = Modifier.padding(top = 9.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DiscussionPreviewCard(
    author: String,
    topic: String,
    title: String,
    body: String
) {
    PesaCard {
        Column(Modifier.padding(15.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            author.take(1),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        author,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        "Preview discussion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = pesaToneContainer("green")
                ) {
                    Text(
                        topic,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = pesaToneContent("green")
                    )
                }
            }

            Text(
                title,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                body,
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
