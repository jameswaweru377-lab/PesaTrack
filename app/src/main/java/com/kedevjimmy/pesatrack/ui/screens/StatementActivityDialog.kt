package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.runtime.Composable
import com.kedevjimmy.pesatrack.ui.components.MainTab

/**
 * Original Activity and More entry points are preserved.
 * Reports now use the approved independent statement intelligence workspace.
 */
@Composable
internal fun StatementActivityDialog(
    onDismiss: () -> Unit,
    onTabSelected: ((MainTab) -> Unit)? = null
) {
    StatementIntelligenceDashboard(onDismiss, onTabSelected)
}
