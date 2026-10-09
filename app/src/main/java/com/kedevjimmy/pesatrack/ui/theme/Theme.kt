package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode { SYSTEM, LIGHT, DARK }
val PesaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)
)
private val LightColors = lightColorScheme(
    primary = Ink, onPrimary = SurfaceLight, primaryContainer = SurfaceSoft, onPrimaryContainer = Ink,
    secondary = Color(0xFF287A5B), onSecondary = SurfaceLight, secondaryContainer = AquaSoft, onSecondaryContainer = Color(0xFF205C45),
    tertiary = Amber, onTertiary = SurfaceLight, tertiaryContainer = AmberSoft, onTertiaryContainer = Color(0xFF704710),
    background = Porcelain, onBackground = Ink, surface = SurfaceLight, onSurface = Ink,
    surfaceVariant = SurfaceSoft, onSurfaceVariant = Slate, outline = Color(0xFF777777), outlineVariant = OutlineLight,
    error = Rose, onError = SurfaceLight, errorContainer = RoseSoft, onErrorContainer = Color(0xFF7A2D30),
    inverseSurface = MidnightSurface, inverseOnSurface = MidnightText, inversePrimary = MidnightText,
    surfaceTint = Ink, scrim = Color.Black,
    surfaceDim = Color(0xFFE7E7E7), surfaceBright = SurfaceLight,
    surfaceContainerLowest = SurfaceLight, surfaceContainerLow = SurfaceRaised, surfaceContainer = Porcelain,
    surfaceContainerHigh = SurfaceSoft, surfaceContainerHighest = Color(0xFFE9E9E9)
)
private val DarkColors = darkColorScheme(
    primary = MidnightText, onPrimary = Midnight, primaryContainer = MidnightSurfaceHigh, onPrimaryContainer = MidnightText,
    secondary = AuroraAquaBright, onSecondary = Midnight, secondaryContainer = NightMintSurface, onSecondaryContainer = AuroraAquaBright,
    tertiary = PremiumGold, onTertiary = Midnight, tertiaryContainer = NightAmberSurface, onTertiaryContainer = NightAmberText,
    background = Midnight, onBackground = MidnightText, surface = MidnightSurface, onSurface = MidnightText,
    surfaceVariant = MidnightSurfaceHigh, onSurfaceVariant = MidnightMuted, outline = Color(0xFF8A8A8A), outlineVariant = MidnightOutline,
    error = PremiumExpense, onError = Midnight, errorContainer = NightRoseSurface, onErrorContainer = NightRoseText,
    inverseSurface = SurfaceSoft, inverseOnSurface = Ink, inversePrimary = Ink,
    surfaceTint = MidnightText, scrim = Color.Black,
    surfaceDim = Midnight, surfaceBright = MidnightSurfaceRaised,
    surfaceContainerLowest = Color(0xFF0B0B0B), surfaceContainerLow = Color(0xFF151515), surfaceContainer = MidnightSurface,
    surfaceContainerHigh = MidnightSurfaceHigh, surfaceContainerHighest = MidnightSurfaceRaised
)
@Composable
fun PesaTrackTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = PesaTypography, shapes = PesaShapes, content = content)
}
