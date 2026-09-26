package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = SurfaceLight,
    primaryContainer = MintSoft,
    onPrimaryContainer = EmeraldDeep,
    secondary = Amber,
    onSecondary = SurfaceLight,
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
    primaryContainer = EmeraldDarker,
    onPrimaryContainer = NightText,
    secondary = androidx.compose.ui.graphics.Color(0xFFFFC764),
    onSecondary = Night,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF4D3712),
    onSecondaryContainer = NightText,
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    outline = NightOutline,
    error = androidx.compose.ui.graphics.Color(0xFFFF9097),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF4A2025)
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
