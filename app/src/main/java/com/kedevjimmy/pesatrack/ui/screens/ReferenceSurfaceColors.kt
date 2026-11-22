package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** Colour compatibility for active Statements and Activity components. */
internal object HomeReferencePalette {
    val canvas = HomeMonochromePalette.canvas
    val ink = HomeMonochromePalette.ink
    val muted = HomeMonochromePalette.muted
    val purple = PremiumVisualTokens.coffee
    val violet = PremiumVisualTokens.coffee
    val green = HomeMonochromePalette.green
    val red = Color(0xFFDC1551)
    val blue = Color(0xFF003BCD)
    val line = HomeMonochromePalette.line
}

internal data class HomeReferenceColors(
    val canvas: Color, val ink: Color, val muted: Color,
    val purple: Color, val violet: Color, val green: Color,
    val red: Color, val blue: Color, val line: Color
)

@Composable
internal fun homeReferenceColors(): HomeReferenceColors {
    val mono = homeMonochromeColors()
    val night = MaterialTheme.colorScheme.background == PremiumVisualTokens.darkCanvas
    return HomeReferenceColors(
        canvas = mono.canvas,
        ink = mono.ink,
        muted = mono.muted,
        purple = if (night) PremiumVisualTokens.darkCoffee else HomeReferencePalette.purple,
        violet = if (night) PremiumVisualTokens.darkCoffee else HomeReferencePalette.violet,
        green = if (night) PremiumVisualTokens.darkMoneyIn else HomeReferencePalette.green,
        red = if (night) PremiumVisualTokens.darkMoneyOut else HomeReferencePalette.red,
        blue = if (night) PremiumVisualTokens.darkTransfer else HomeReferencePalette.blue,
        line = mono.line
    )
}

/** Shared upcoming-bill data used by the current coffee Home. */
internal data class ReferenceUpcomingEntry(
    val title: String,
    val whenText: String,
    val amount: String,
    val isSalary: Boolean
)
