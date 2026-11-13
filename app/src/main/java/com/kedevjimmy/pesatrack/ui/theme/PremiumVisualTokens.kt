package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The approved Profile editorial palette is now the single visual identity
 * for PesaTrack's light, dark and nested screens. Green is decorative unless
 * the darker readable green is explicitly selected for foreground content.
 */
object PremiumVisualTokens {
    val coffee = Color(0xFF6B381A)
    val cream = Color(0xFFFCF0E7)
    val card = Color(0xFFFFF8F3)
    val sand = Color(0xFFF3E3D6)
    val sandBorder = Color(0xFFE3D0C0)
    val leaf = Color(0xFF94B95B)
    val leafReadable = Color(0xFF436225)
    val ink = Color(0xFF161310)
    val muted = Color(0xFF625B55)

    // Semantic movement colors, shared across Home, Activity and Accounts.
    val moneyIn = leafReadable
    val moneyOut = Color(0xFFB4233D)
    val transfer = Color(0xFF285C90)

    val darkCanvas = Color(0xFF1C1714)
    val darkCard = Color(0xFF2A221D)
    val darkSurface = Color(0xFF352B25)
    val darkBorder = Color(0xFF594739)
    val darkText = Color(0xFFF9F0E9)
    val darkMuted = Color(0xFFD4C4B8)
    val darkCoffee = Color(0xFFE8BBA0)
}
