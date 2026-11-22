package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** Shared palette retained by current Activity, Plan, Pay cycle and Statements. */
internal object HomeMonochromePalette {
    val canvas = PremiumVisualTokens.cream
    val surface = PremiumVisualTokens.card
    val ink = PremiumVisualTokens.ink
    val muted = PremiumVisualTokens.muted
    val line = PremiumVisualTokens.sandBorder
    val green = PremiumVisualTokens.leafReadable
    val softGreen = Color(0xFFE7EFD9)
    val dark = PremiumVisualTokens.coffee
}

internal data class HomeMonochromeColors(
    val canvas: Color, val surface: Color, val ink: Color, val muted: Color,
    val line: Color, val green: Color, val softGreen: Color, val dark: Color
)

@Composable
internal fun homeMonochromeColors(): HomeMonochromeColors {
    val night = MaterialTheme.colorScheme.background == PremiumVisualTokens.darkCanvas
    return if (night) HomeMonochromeColors(
        canvas = PremiumVisualTokens.darkCanvas,
        surface = PremiumVisualTokens.darkCard,
        ink = PremiumVisualTokens.darkText,
        muted = PremiumVisualTokens.darkMuted,
        line = PremiumVisualTokens.darkBorder,
        green = PremiumVisualTokens.darkMoneyIn,
        softGreen = PremiumVisualTokens.darkSurface,
        dark = PremiumVisualTokens.darkSurface
    ) else HomeMonochromeColors(
        canvas = HomeMonochromePalette.canvas,
        surface = HomeMonochromePalette.surface,
        ink = HomeMonochromePalette.ink,
        muted = HomeMonochromePalette.muted,
        line = HomeMonochromePalette.line,
        green = HomeMonochromePalette.green,
        softGreen = HomeMonochromePalette.softGreen,
        dark = HomeMonochromePalette.dark
    )
}
