package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.components.PesaLogoMark
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/**
 * Branded splash: the official mark, local Nairobi image and bundled wallet
 * artwork. Keep this screen passive; the first-run buttons are Compose controls.
 */
@Composable
fun StartupSplashScreen(darkTheme: Boolean = false) {
    val canvas = if (darkTheme) PremiumVisualTokens.darkCanvas else PremiumVisualTokens.cream
    val ink = if (darkTheme) PremiumVisualTokens.darkText else PremiumVisualTokens.ink
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val artHeight = (maxHeight * 0.43f).coerceAtMost(380.dp)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(artHeight)) {
            Image(
                painter = painterResource(R.drawable.profile_default_nairobi_cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (darkTheme) 0.15f else 0.28f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(canvas, canvas.copy(alpha = 0.43f), canvas.copy(alpha = 0.90f))
                    )
                )
            )
            Image(
                painter = painterResource(R.drawable.wallet_welcome),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth(0.92f).height(artHeight * 0.90f)
                    .padding(bottom = 32.dp)
            )
        }
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(top = (maxHeight * 0.22f).coerceAtMost(220.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PesaLogoMark(size = 72.dp)
            Text(
                "PesaTrack", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                fontSize = 36.sp, color = ink
            )
            Text(
                "YOUR MONEY, YOUR PLANS",
                fontSize = 12.sp, letterSpacing = 2.sp,
                color = if (darkTheme) PremiumVisualTokens.darkMuted else PremiumVisualTokens.coffee
            )
            Box(
                Modifier.padding(top = 4.dp).width(48.dp).height(4.dp)
                    .background(PremiumVisualTokens.leaf, RoundedCornerShape(4.dp))
            )
        }
    }
}
