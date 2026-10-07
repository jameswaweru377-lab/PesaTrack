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
    primary = NightGreen,
    onPrimary = Color(0xFF101700),
    primaryContainer = NightMintSurface,
    onPrimaryContainer = NightMint,
    secondary = PremiumGold,
    onSecondary = Color(0xFF181300),
    secondaryContainer = NightAmberSurface,
    onSecondaryContainer = NightAmberText,
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
    outlineVariant = NightOutline.copy(alpha = .62f),
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
