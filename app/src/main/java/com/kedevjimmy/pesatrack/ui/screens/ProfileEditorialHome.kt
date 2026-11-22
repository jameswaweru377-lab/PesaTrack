package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C

@Composable
internal fun ProfileEditorialHome(
    padding: PaddingValues,
    back: () -> Unit,
    name: String,
    avatar: String,
    moneyHabits: ProfileMoneyHabitsPresentation,
    personal: () -> Unit,
    payCycle: () -> Unit,
    statements: () -> Unit,
    imports: () -> Unit,
    reports: () -> Unit,
    settings: () -> Unit,
    privacy: () -> Unit,
    onMoneyHabits: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileCoverHeader(name, avatar, back, personal)
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ProfileNairobiWellnessFeature(moneyHabits, onMoneyHabits)
            }
            ProfileMoneyWorkspace(statements, imports, reports, payCycle)
            ProfileSettingsEntry(settings)
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.padding(horizontal = 16.dp)) {
            ProfileDevicePrivacy(privacy)
        }
        ProfileEditorialFooter()
    }
}

@Composable
internal fun ProfileNairobiWellnessFeature(
    state: ProfileMoneyHabitsPresentation,
    onClick: () -> Unit
) {
    val accent = PremiumVisualTokens.darkCoffee
    val text = PremiumVisualTokens.darkText
    val secondary = PremiumVisualTokens.darkMuted
    val rated = state.score?.takeIf { !state.isProvisional }
    val status = when {
        rated == null -> "Needs info"
        state.kind == ProfileMoneyHabitsKind.AT_RISK -> "At risk"
        else -> "On track"
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 160.dp)
            .clip(RoundedCornerShape(PesaDesignTokens.cardRadius))
            .background(PremiumVisualTokens.darkSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = if (rated == null) "Money health not rated" else "$status, $rated out of 100" }
            .testTag("profile-money-wellness-card")
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MONEY WELLNESS", color = accent, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("How is your money?", color = text, fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, maxLines = 2)
            Text(state.subtitle, color = secondary, fontSize = 12.sp,
                lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            HorizontalDivider(color = PremiumVisualTokens.leaf.copy(alpha = .55f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View insights", color = accent, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.ArrowForward, contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(start = 4.dp), tint = accent)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (rated ?: 0).coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = accent,
                    trackColor = PremiumVisualTokens.darkBorder,
                    strokeWidth = 8.dp
                )
                Text(status, color = text, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    maxLines = 2, modifier = Modifier.padding(horizontal = 8.dp))
            }
            Text(if (rated == null) "Complete picture" else "Check in",
                color = secondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProfileDevicePrivacy(onClick: () -> Unit) {
    val night = MaterialTheme.colorScheme.background == com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens.darkCanvas
    Surface(
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Box(Modifier.width(1.dp).height(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .28f)))
            Column(Modifier.weight(1f)) {
                Text("Only on this device", fontSize = 16.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Your personal details stay on this phone",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick, shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = if (night) MaterialTheme.colorScheme.primary else C.ink,
                    contentColor = if (night) MaterialTheme.colorScheme.onPrimary else Color.White), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("Privacy", fontSize = 12.sp)
                Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileEditorialFooter(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PesaDesignTokens.faintDividerAlpha))
        Text("PesaTrack", modifier = Modifier.padding(start = 12.dp),
            color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
        Text(" • Your money, your plans", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
            textAlign = TextAlign.Center)
        HorizontalDivider(Modifier.weight(1f).padding(start = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PesaDesignTokens.faintDividerAlpha))
    }
}
