package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared visual tokens for the core PesaTrack component system.
 * Screens should prefer these values over introducing one-off radii/gaps.
 */
object PesaDesignTokens {
    val screenHorizontalPadding =
        18.dp
    val screenVerticalPadding =
        16.dp

    val sectionGap =
        14.dp
    val cardContentPadding =
        16.dp
    val compactGap =
        8.dp

    val cardRadius =
        22.dp
    val elevatedRadius =
        26.dp
    val innerRadius =
        16.dp
    val controlRadius =
        16.dp
    val iconRadius =
        13.dp

    val iconBubble =
        42.dp

    // Shared component details. These preserve the approved dimensions
    // while keeping future reuse independent of one-off literals.
    val compactWordmarkLogoSize = 36.dp
    val regularWordmarkLogoSize = 44.dp
    val wordmarkGap = 10.dp
    val cardOutlineWidth = 1.dp
    val lightCardElevation = 2.dp
    val sectionActionIconSize = 19.dp
    val statusPillHorizontalPadding = 10.dp
    val statusPillVerticalPadding = 5.dp
}
