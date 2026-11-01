package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kedevjimmy.pesatrack.security.AppLockManager
import com.kedevjimmy.pesatrack.security.ScreenCapturePolicy

/** Dialogs own windows and do not automatically inherit later activity flag changes. */
@Composable
internal fun rememberCaptureAwareDialogPolicy(): SecureFlagPolicy {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> foreground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> foreground = false
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val appLock = remember(context) { AppLockManager(context) }
    return if (ScreenCapturePolicy.shouldBlockCapture(
            appLockEnabled = appLock.isEnabled(),
            appLocked = false,
            foreground = foreground,
            backupRecoveryBlocked = false
        )) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
}
