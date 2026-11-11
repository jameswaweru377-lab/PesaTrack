package com.kedevjimmy.pesatrack.ui.theme

import androidx.compose.runtime.Composable

/**
 * Compatibility wrapper for existing More and Settings routes.
 * The unified app-wide MaterialTheme now owns their controls and palette;
 * nesting another black-only theme would reintroduce visual inconsistency.
 */
@Composable
fun MoreMonochromeTheme(content: @Composable () -> Unit) {
    content()
}
