package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.NightAmberSurface
import com.kedevjimmy.pesatrack.ui.theme.NightAmberText
import com.kedevjimmy.pesatrack.ui.theme.NightBlueSurface
import com.kedevjimmy.pesatrack.ui.theme.NightBlueText
import com.kedevjimmy.pesatrack.ui.theme.NightMintSurface
import com.kedevjimmy.pesatrack.ui.theme.NightMintText
import com.kedevjimmy.pesatrack.ui.theme.NightRoseSurface
import com.kedevjimmy.pesatrack.ui.theme.NightRoseText
import com.kedevjimmy.pesatrack.ui.theme.NightVioletSurface
import com.kedevjimmy.pesatrack.ui.theme.NightVioletText
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.MintSoft
import com.kedevjimmy.pesatrack.ui.theme.SkySoft
import com.kedevjimmy.pesatrack.ui.theme.VioletSoft

@Composable
fun PesaLogoMark(modifier: Modifier = Modifier, size: Dp = PesaDesignTokens.iconBubble) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.28f),
        color = Color.Transparent
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_pesatrack_logo),
            contentDescription = "PesaTrack",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PesaDesignTokens.wordmarkGap)
    ) {
        PesaLogoMark(
            size = if (compact) PesaDesignTokens.compactWordmarkLogoSize
            else PesaDesignTokens.regularWordmarkLogoSize
        )
        Column {
            Text(
                "PesaTrack",
                style = if (compact) MaterialTheme.typography.titleLarge
                        else MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            if (!compact) {
                Text(
                    "Your money. A calmer tomorrow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PesaCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            PesaDesignTokens.cardOutlineWidth,
            MaterialTheme.colorScheme.outline.copy(
                alpha = if (isPesaDarkTheme()) .34f else .58f
            )
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPesaDarkTheme()) 0.dp else PesaDesignTokens.lightCardElevation
        )
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(PesaDesignTokens.sectionActionIconSize)
                )
            }
        }
    }
}

@Composable
fun IconBubble(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    size: Dp = PesaDesignTokens.iconBubble
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = background
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.48f))
        }
    }
}

@Composable
fun SmallStatusPill(
    text: String,
    tone: String = "green"
) {
    val bg = pesaToneContainer(tone)
    val fg = pesaToneContent(tone)
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text,
            modifier = Modifier.padding(
                horizontal = PesaDesignTokens.statusPillHorizontalPadding,
                vertical = PesaDesignTokens.statusPillVerticalPadding
            ),
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

@Composable
fun LandscapeBackdrop(
    modifier: Modifier = Modifier,
    dark: Boolean? = null,
    showSun: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDark = dark ?: isPesaDarkTheme()
    val base = if (useDark) Color(0xFF17162C) else Color(0xFFF0EFFF)
    val hill1 = if (useDark) Color(0xFF25214B) else Color(0xFFDED9FF)
    val hill2 = if (useDark) Color(0xFF332B68) else Color(0xFFC8C0FF)
    val hill3 = if (useDark) Color(0xFF1A3150) else Color(0xFFA9DDF2)
    val sun = if (useDark) Color(0xFF55E6D1).copy(alpha = .18f) else Color(0xFFFFE6B0)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(base)
    ) {
        Canvas(Modifier.matchParentSize()) {
            if (showSun) {
                drawCircle(
                    color = sun,
                    radius = size.minDimension * .17f,
                    center = Offset(size.width * .80f, size.height * .20f)
                )
            }
            val p1 = Path().apply {
                moveTo(0f, size.height * .70f)
                cubicTo(size.width * .22f, size.height * .42f, size.width * .44f, size.height * .62f, size.width * .62f, size.height * .44f)
                cubicTo(size.width * .78f, size.height * .28f, size.width * .90f, size.height * .42f, size.width, size.height * .34f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            val p2 = Path().apply {
                moveTo(0f, size.height * .78f)
                cubicTo(size.width * .18f, size.height * .55f, size.width * .36f, size.height * .73f, size.width * .52f, size.height * .57f)
                cubicTo(size.width * .70f, size.height * .39f, size.width * .85f, size.height * .63f, size.width, size.height * .47f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            val p3 = Path().apply {
                moveTo(size.width * .56f, size.height)
                cubicTo(size.width * .72f, size.height * .68f, size.width * .84f, size.height * .61f, size.width, size.height * .44f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(p1, hill1)
            drawPath(p2, hill2)
            drawPath(p3, hill3)
            drawCircle(hill3, size.minDimension * .08f, Offset(size.width * .91f, size.height * .69f))
            drawCircle(hill2, size.minDimension * .055f, Offset(size.width * .84f, size.height * .79f))
        }
        content()
    }
}

@Composable
fun MetricRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun MetricCell(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: String = "green"
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        IconBubble(
            icon = icon,
            size = 34.dp,
            background = when (tone) {
                "amber" -> AmberSoft
                "blue" -> SkySoft
                "violet" -> VioletSoft
                else -> MintSoft
            }
        )
        Spacer(Modifier.height(7.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun isPesaDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.35f

@Composable
fun pesaToneContainer(tone: String): Color {
    val dark = isPesaDarkTheme()
    return if (dark) {
        when (tone) {
            "amber" -> NightAmberSurface
            "blue" -> NightBlueSurface
            "violet" -> NightVioletSurface
            "red" -> NightRoseSurface
            else -> NightMintSurface
        }
    } else {
        when (tone) {
            "amber" -> AmberSoft
            "blue" -> SkySoft
            "violet" -> VioletSoft
            "red" -> com.kedevjimmy.pesatrack.ui.theme.RoseSoft
            else -> MintSoft
        }
    }
}

@Composable
fun pesaToneContent(tone: String): Color {
    val dark = isPesaDarkTheme()
    return if (dark) {
        when (tone) {
            "amber" -> NightAmberText
            "blue" -> NightBlueText
            "violet" -> NightVioletText
            "red" -> NightRoseText
            else -> NightMintText
        }
    } else {
        when (tone) {
            "amber" -> Color(0xFF8B590B)
            "blue" -> Color(0xFF176A99)
            "violet" -> Color(0xFF60459F)
            "red" -> Color(0xFFB33B49)
            else -> MaterialTheme.colorScheme.primary
        }
    }
}
