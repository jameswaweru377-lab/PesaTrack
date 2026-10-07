package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = SurfaceLight,
    primaryContainer = MintSoft,
    onPrimaryContainer = EmeraldDeep,
    secondary = Amber,
    onSecondary = Navy,
    secondaryContainer = AmberSoft,
    onSecondaryContainer = Navy,
    background = WarmIvory,
    onBackground = Navy,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = Slate,
    outline = OutlineLight,
    error = Rose,
    errorContainer = RoseSoft
)

private val DarkColors = darkColorScheme(
    primary = NightLime,
    onPrimary = Color(0xFF0A1305),
    primaryContainer = NightMintSurface,
    onPrimaryContainer = NightLimeBright,
    secondary = NightEmerald,
    onSecondary = Color(0xFF03120C),
    secondaryContainer = NightEmeraldSurface,
    onSecondaryContainer = NightEmeraldText,
    tertiary = PremiumPurple,
    onTertiary = Color(0xFF120B1C),
    tertiaryContainer = NightVioletSurface,
    onTertiaryContainer = NightVioletText,
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    surfaceTint = Color.Transparent,
    outline = NightOutline,
    outlineVariant = NightOutline.copy(alpha = .65f),
    error = PremiumExpense,
    onError = Color(0xFF180606),
    errorContainer = NightRoseSurface,
    onErrorContainer = NightRoseText
)

@Composable
fun PesaTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PesaTypography,
        content = content
    )
}
