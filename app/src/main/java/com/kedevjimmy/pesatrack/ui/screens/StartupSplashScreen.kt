package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kedevjimmy.pesatrack.ui.components.ConceptBrandLockup
import com.kedevjimmy.pesatrack.ui.components.ConceptWalletIllustration

@Composable
fun StartupSplashScreen(darkTheme: Boolean = false) {
    val foreground = if (darkTheme) Color.White else Color(0xFF0B103A)
    val secondary = if (darkTheme) Color.White.copy(alpha = .78f) else Color(0xFF626B98)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (darkTheme) listOf(
                        Color(0xFF3933A8),
                        Color(0xFF24213F),
                        Color(0xFF111827)
                    ) else listOf(
                        Color(0xFFF8F7FF),
                        Color(0xFFEAF3FF),
                        Color(0xFFF7F7FF)
                    )
                )
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
            )
            .padding(
                horizontal = 20.dp,
                vertical = 22.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        ConceptBrandLockup(
            light = darkTheme
        )

        Spacer(
            Modifier.height(24.dp)
        )

        Text(
            "Your money.\nBetter days.",
            style =
                MaterialTheme
                    .typography
                    .displaySmall,
            color = foreground,
            fontWeight =
                FontWeight.ExtraBold,
            textAlign =
                TextAlign.Center
        )

        Text(
            "Understand your money, automatically.",
            modifier =
                Modifier.padding(
                    top = 8.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            color = secondary,
            textAlign =
                TextAlign.Center
        )

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment =
                Alignment.Center
        ) {
            ConceptWalletIllustration(
                compact = false
            )
        }
    }
}
