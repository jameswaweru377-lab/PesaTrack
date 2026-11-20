package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy

/**
 * A separate secure Android window is placed above any already-open Compose dialogs
 * or sheets. An activity-only overlay would sit beneath those windows.
 */
@Composable
internal fun SecureAppLockDialog(content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = { /* Authentication is required; never dismiss. */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOn
        )
    ) {
        BackHandler { /* Do not pass navigation to an underlying dialog. */ }
        val blockerInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(interactionSource = blockerInteraction, indication = null) {
                    /* Block touches outside authentication controls. */
                }
        ) { content() }
    }
}
