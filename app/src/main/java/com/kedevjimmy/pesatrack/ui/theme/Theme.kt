package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    onPrimary = Night,
    primaryContainer = NightMintSurface,
    onPrimaryContainer = NightText,
    secondary = PremiumGold,
    onSecondary = Night,
    secondaryContainer = NightAmberSurface,
    onSecondaryContainer = NightAmberText,
    tertiary = PremiumPurple,
    onTertiary = Night,
    tertiaryContainer = NightVioletSurface,
    onTertiaryContainer = NightVioletText,
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    outline = NightOutline,
    error = PremiumExpense,
    errorContainer = NightRoseSurface
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
