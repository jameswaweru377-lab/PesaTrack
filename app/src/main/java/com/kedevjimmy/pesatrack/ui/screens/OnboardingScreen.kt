package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Legacy first-launch entry shares the same compact welcome as smart setup. */
@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        OnboardingPremiumHeader()
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            PesaTrackWelcomePage(onGetStarted = onContinue, onSkip = onContinue)
        }
    }
}
