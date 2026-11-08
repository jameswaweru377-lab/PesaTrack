package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Neutral Material controls for More and the pages reached through its settings. */
@Composable
fun MoreMonochromeTheme(content: @Composable () -> Unit) {
    val palette = MaterialTheme.colorScheme.copy(
        primary = Color.Black,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF2F2F2),
        onPrimaryContainer = Color.Black,
        secondary = Color.Black,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF2F2F2),
        onSecondaryContainer = Color.Black,
        tertiary = Color.Black,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF2F2F2),
        onTertiaryContainer = Color.Black,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFF6F6F6),
        onSurfaceVariant = Color(0xFF626262),
        inversePrimary = Color.White,
        outline = Color(0xFFBDBDBD),
        outlineVariant = Color(0xFFE0E0E0)
    )
    MaterialTheme(
        colorScheme = palette,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
