package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

val PesaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val ColorTokensLightSecondary = Color(0xFF087E73)
private val ColorTokensLightSecondaryDeep = Color(0xFF07564F)
private val ColorTokensLightErrorDeep = Color(0xFF7A2538)

private val LightColors = lightColorScheme(
    primary = AuroraIndigo,
    onPrimary = SurfaceLight,
    primaryContainer = VioletSoft,
    onPrimaryContainer = AuroraIndigoDeep,
    secondary = ColorTokensLightSecondary,
    onSecondary = SurfaceLight,
    secondaryContainer = AquaSoft,
    onSecondaryContainer = ColorTokensLightSecondaryDeep,
    tertiary = Amber,
    onTertiary = Navy,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = Navy,
    background = Porcelain,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = Slate,
    outline = OutlineLight,
    error = Rose,
    onError = SurfaceLight,
    errorContainer = RoseSoft,
    onErrorContainer = ColorTokensLightErrorDeep
)

private val DarkColors = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = Midnight,
    primaryContainer = Color(0xFF2C2850),
    onPrimaryContainer = Color(0xFFE5E1FF),
    secondary = AuroraAquaBright,
    onSecondary = Midnight,
    secondaryContainer = Color(0xFF153D3A),
    onSecondaryContainer = Color(0xFFB7FFF4),
    tertiary = PremiumGold,
    onTertiary = Midnight,
    tertiaryContainer = NightAmberSurface,
    onTertiaryContainer = NightAmberText,
    background = Midnight,
    onBackground = MidnightText,
    surface = MidnightSurface,
    onSurface = MidnightText,
    surfaceVariant = MidnightSurfaceHigh,
    onSurfaceVariant = MidnightMuted,
    outline = MidnightOutline,
    error = PremiumExpense,
    onError = Midnight,
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
        shapes = PesaShapes,
        content = content
    )
}
