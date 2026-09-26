package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    onSurface = Navy,
    surfaceVariant = Color(0xFFF1F4F2),
    onSurfaceVariant = Color(0xFF66736F),
    outline = OutlineLight,
    error = Danger
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Night,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = NightText,
    secondary = Color(0xFFFFC25C),
    onSecondary = Night,
    secondaryContainer = Color(0xFF4A3310),
    onSecondaryContainer = NightText,
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    outline = NightOutline,
    error = Color(0xFFFF8B96)
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
