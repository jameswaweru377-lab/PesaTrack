package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C

internal enum class ProfileTone { VIOLET, GREEN, BLUE, PINK, AMBER, CORAL }
internal enum class ProfileHeroArt { SHIELD, BELL, PALETTE, QUESTION, WRENCH, GENERIC }

internal fun ProfileTone.ink(): Color = when (this) {
    ProfileTone.VIOLET -> C.violet
    ProfileTone.GREEN -> C.green
    ProfileTone.BLUE -> C.blue
    ProfileTone.PINK -> C.red
    ProfileTone.AMBER -> C.orange
    ProfileTone.CORAL -> C.coral
}

internal fun ProfileTone.wash(): Color = when (this) {
    ProfileTone.VIOLET -> C.lilac
    ProfileTone.GREEN -> C.greenSoft
    ProfileTone.BLUE -> C.blueSoft
    ProfileTone.PINK -> C.redSoft
    ProfileTone.AMBER -> C.orangeSoft
    ProfileTone.CORAL -> C.coralSoft
}

@Composable
internal fun ProfilePremiumPage(
    title: String,
    padding: PaddingValues,
    back: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(C.canvas)) {
        ProfileSoftWaves(Modifier.fillMaxWidth().height(280.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = C.ink)
                    }
                    Text(title, style = MaterialTheme.typography.headlineSmall,
                        color = C.ink, fontWeight = FontWeight.Bold)
                }
            }
            content()
        }
    }
}

@Composable
internal fun ProfileSoftWaves(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val a = Path().apply {
            moveTo(0f, h * .37f)
            cubicTo(w * .25f, h * .1f, w * .42f, h * .94f, w, h * .08f)
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }
        val b = Path().apply {
            moveTo(0f, h * .58f)
            cubicTo(w * .28f, h * .15f, w * .66f, h * .87f, w, h * .28f)
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(a, Brush.linearGradient(listOf(C.lilac.copy(alpha = .55f), C.blueSoft.copy(alpha = .42f))))
        drawPath(b, C.lilac.copy(alpha = .23f))
    }
}

@Composable
internal fun ProfileTile(icon: ImageVector, tone: ProfileTone, size: Dp = 48.dp) {
    Surface(Modifier.size(size), shape = RoundedCornerShape(PesaDesignTokens.iconRadius), color = tone.wash()) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tone.ink(), modifier = Modifier.size(size * .52f))
        }
    }
}

internal data class ProfilePremiumItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tone: ProfileTone,
    val onClick: (() -> Unit)? = null,
    val checked: Boolean? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val onToggle: ((Boolean) -> Unit)? = null
)

@Composable
internal fun ProfilePremiumGroup(items: List<ProfilePremiumItem>) {
    Surface(
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, C.outline.copy(alpha = .35f))
    ) {
        Column {
            items.forEachIndexed { index, item ->
                val click: (() -> Unit)? = if (item.checked != null) ({
                    item.onToggle?.invoke(item.checked != true)
                    Unit
                })
                    else item.onClick
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .then(if (click != null) Modifier.clickable(enabled = item.enabled,
                            role = if (item.checked == null) Role.Button else Role.Switch,
                            onClick = click) else Modifier)
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileTile(item.icon, item.tone)
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 6.dp)) {
                        Text(item.title, color = C.ink, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall)
                        Text(item.subtitle, color = C.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.selected) {
                        Surface(shape = CircleShape, color = C.violet) {
                            Icon(Icons.Rounded.Check, "Selected", tint = Color.White,
                                modifier = Modifier.size(28.dp).padding(4.dp))
                        }
                    } else if (item.checked != null) {
                        Switch(checked = item.checked, onCheckedChange = null, enabled = item.enabled,
                            colors = SwitchDefaults.colors(checkedTrackColor = C.violet,
                                checkedThumbColor = Color.White, uncheckedTrackColor = C.outline,
                                uncheckedThumbColor = Color.White))
                    } else if (item.onClick != null) {
                        Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
                    }
                }
                if (index != items.lastIndex) HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp, end = 12.dp),
                    thickness = 1.dp, color = C.outline)
            }
        }
    }
}

@Composable
internal fun ProfileSection(text: String) {
    Text(text.uppercase(), modifier = Modifier.padding(start = 12.dp, top = 4.dp),
        color = C.muted, fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge)
}

@Composable
internal fun ProfileBanner(
    headline: String,
    description: String,
    illustration: ImageVector,
    dark: Boolean = false,
    art: ProfileHeroArt = ProfileHeroArt.GENERIC,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 156.dp)
            .clip(RoundedCornerShape(PesaDesignTokens.cardRadius))
            .background(if (dark) Brush.linearGradient(listOf(C.violetBright, C.violetDeep))
                else Brush.linearGradient(listOf(C.lilac, C.blueSoft, C.lilac)))
    ) {
        ProfileBannerWavesPublic(Modifier.matchParentSize(), dark)
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(headline, color = if (dark) Color.White else C.ink,
                    fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                Text(description, color = if (dark) Color.White.copy(alpha = .9f) else C.muted,
                    style = MaterialTheme.typography.bodyMedium)
            }
            ProfileHeroArtwork(illustration, dark, art)
        }
    }
}

@Composable
private fun ProfileHeroArtwork(icon: ImageVector, dark: Boolean, art: ProfileHeroArt) {
    Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        if (art == ProfileHeroArt.SHIELD) {
            Canvas(Modifier.size(104.dp)) {
                val path = Path().apply {
                    moveTo(size.width * .5f, size.height * .05f)
                    cubicTo(size.width * .65f, size.height * .16f,
                        size.width * .8f, size.height * .18f, size.width * .92f, size.height * .22f)
                    lineTo(size.width * .9f, size.height * .58f)
                    cubicTo(size.width * .87f, size.height * .78f,
                        size.width * .69f, size.height * .91f, size.width * .5f, size.height * .98f)
                    cubicTo(size.width * .3f, size.height * .89f,
                        size.width * .12f, size.height * .72f, size.width * .1f, size.height * .56f)
                    lineTo(size.width * .08f, size.height * .22f)
                    cubicTo(size.width * .28f, size.height * .17f,
                        size.width * .4f, size.height * .12f, size.width * .5f, size.height * .05f)
                    close()
                }
                drawPath(path, Brush.verticalGradient(listOf(Color.White, C.lilac, C.violetBright.copy(alpha = .68f))))
                drawPath(path, Color.White, style = Stroke(width = 3.dp.toPx()))
            }
            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(50.dp), tint = C.violetDeep)
        } else {
            Surface(Modifier.size(100.dp), CircleShape,
                color = Color.White.copy(alpha = if (dark) .22f else .38f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .65f)),
                shadowElevation = 4.dp) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(72.dp)
                        .graphicsLayer { rotationZ = if (art == ProfileHeroArt.WRENCH) -18f else 0f },
                        tint = if (dark) Color.White else C.violet)
                }
            }
        }
    }
}

@Composable
internal fun ProfileBannerWavesPublic(modifier: Modifier, dark: Boolean = true) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val p = Path().apply {
            moveTo(0f, h * .74f)
            cubicTo(w * .36f, h * 1.18f, w * .63f, h * .42f, w, h * .63f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(p, Brush.linearGradient(if (dark) listOf(Color.White.copy(alpha = .06f), C.blue.copy(alpha = .32f))
            else listOf(Color.White.copy(alpha = .24f), C.violet.copy(alpha = .13f))))
    }
}

@Composable
internal fun ProfileHint(message: String, icon: ImageVector = Icons.Rounded.Lock) {
    Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius), color = C.lilac.copy(alpha = .85f)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = C.muted, modifier = Modifier.size(20.dp))
            Text(message, modifier = Modifier.padding(start = 12.dp),
                color = C.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ProfileGradientButton(text: String, enabled: Boolean = true, action: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clip(RoundedCornerShape(PesaDesignTokens.innerRadius))
            .clickable(enabled = enabled, onClick = action),
        color = Color.Transparent,
        shadowElevation = 3.dp
    ) {
        Box(Modifier.fillMaxWidth().background(
            Brush.horizontalGradient(listOf(C.violetBright, C.violetDeep))),
            contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
        }
    }
}
