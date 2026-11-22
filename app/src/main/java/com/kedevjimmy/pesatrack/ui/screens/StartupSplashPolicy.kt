package com.kedevjimmy.pesatrack.ui.screens

/** The first-run café welcome stays intact; returning launches use a brief dark brand reveal. */
internal object StartupSplashPolicy {
    fun isVisibleAtColdStart(setupComplete: Boolean): Boolean = true

    fun durationMs(setupComplete: Boolean): Long = if (setupComplete) 700L else 650L
}
