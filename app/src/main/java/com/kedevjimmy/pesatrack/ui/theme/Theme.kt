package com.kedevjimmy.pesatrack.ui.theme

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
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val LightColors = lightColorScheme(
    primary = PremiumVisualTokens.coffee,
    onPrimary = PremiumVisualTokens.card,
    primaryContainer = PremiumVisualTokens.sand,
    onPrimaryContainer = PremiumVisualTokens.ink,
    secondary = PremiumVisualTokens.leafReadable,
    onSecondary = PremiumVisualTokens.card,
    secondaryContainer = AquaSoft,
    onSecondaryContainer = PremiumVisualTokens.leafReadable,
    tertiary = Amber,
    onTertiary = PremiumVisualTokens.ink,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = PremiumVisualTokens.ink,
    background = PremiumVisualTokens.cream,
    onBackground = PremiumVisualTokens.ink,
    surface = PremiumVisualTokens.card,
    onSurface = PremiumVisualTokens.ink,
    surfaceVariant = PremiumVisualTokens.sand,
    onSurfaceVariant = PremiumVisualTokens.muted,
    surfaceContainerLowest = PremiumVisualTokens.card,
    surfaceContainerLow = PremiumVisualTokens.card,
    surfaceContainer = PremiumVisualTokens.cream,
    surfaceContainerHigh = PremiumVisualTokens.sand,
    surfaceContainerHighest = PremiumVisualTokens.sandBorder,
    surfaceDim = PremiumVisualTokens.sand,
    surfaceBright = PremiumVisualTokens.card,
    surfaceTint = PremiumVisualTokens.coffee,
    outlineVariant = PremiumVisualTokens.sandBorder,
    outline = PremiumVisualTokens.sandBorder,
    error = Color(0xFFB33B49),
    onError = PremiumVisualTokens.card,
    errorContainer = RoseSoft,
    onErrorContainer = Color(0xFF7A2538)
)

private val DarkColors = darkColorScheme(
    primary = PremiumVisualTokens.darkCoffee,
    onPrimary = PremiumVisualTokens.darkCanvas,
    primaryContainer = PremiumVisualTokens.darkSurface,
    onPrimaryContainer = PremiumVisualTokens.darkText,
    secondary = Color(0xFFC0DCA0),
    onSecondary = PremiumVisualTokens.darkCanvas,
    secondaryContainer = Color(0xFF2F3926),
    onSecondaryContainer = Color(0xFFC0DCA0),
    tertiary = PremiumGold,
    onTertiary = PremiumVisualTokens.darkCanvas,
    tertiaryContainer = NightAmberSurface,
    onTertiaryContainer = NightAmberText,
    background = PremiumVisualTokens.darkCanvas,
    onBackground = PremiumVisualTokens.darkText,
    surface = PremiumVisualTokens.darkCard,
    onSurface = PremiumVisualTokens.darkText,
    surfaceVariant = PremiumVisualTokens.darkSurface,
    onSurfaceVariant = PremiumVisualTokens.darkMuted,
    surfaceContainerLowest = PremiumVisualTokens.darkCanvas,
    surfaceContainerLow = PremiumVisualTokens.darkCard,
    surfaceContainer = PremiumVisualTokens.darkSurface,
    surfaceContainerHigh = PremiumVisualTokens.darkSurface,
    surfaceContainerHighest = PremiumVisualTokens.darkBorder,
    surfaceDim = PremiumVisualTokens.darkCanvas,
    surfaceBright = PremiumVisualTokens.darkSurface,
    surfaceTint = PremiumVisualTokens.darkCoffee,
    outlineVariant = PremiumVisualTokens.darkBorder,
    outline = PremiumVisualTokens.darkBorder,
    error = PremiumExpense,
    onError = PremiumVisualTokens.darkCanvas,
    errorContainer = NightRoseSurface,
    onErrorContainer = NightRoseText
)

@Composable
fun PesaTrackTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PesaTypography,
        shapes = PesaShapes,
        content = content
    )
}
