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
    val foreground = if (darkTheme) Color.White else Color(0xFF6B381A)
    val secondary = if (darkTheme) Color.White.copy(alpha = .78f) else Color(0xFF785C4C)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (darkTheme) listOf(
                        Color(0xFF6B381A),
                        Color(0xFF4B2919),
                        Color(0xFF2D1D16)
                    ) else listOf(
                        Color(0xFFFAF7F0),
                        Color(0xFFF5EDE1),
                        Color(0xFFFAF7F0)
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
