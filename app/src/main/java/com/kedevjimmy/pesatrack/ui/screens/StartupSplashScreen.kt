package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.components.PesaLogoMark
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** Launch branding, not another onboarding decision or a 3-D showcase. */
@Composable
fun StartupSplashScreen(darkTheme: Boolean = false) {
    val canvas = if (darkTheme) PremiumVisualTokens.darkCanvas else PremiumVisualTokens.cream
    val ink = if (darkTheme) PremiumVisualTokens.darkText else PremiumVisualTokens.ink
    Column(
        modifier = Modifier.fillMaxSize().background(canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        PesaLogoMark(size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text("PesaTrack", fontSize = 36.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, color = ink)
        Text("YOUR MONEY, YOUR PLANS", fontSize = 12.sp, letterSpacing = 2.sp,
            color = if (darkTheme) PremiumVisualTokens.darkMuted else PremiumVisualTokens.coffee)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(48.dp).height(4.dp)
            .background(PremiumVisualTokens.leaf, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            Image(painterResource(R.drawable.profile_default_nairobi_cover),
                contentDescription = null, contentScale = ContentScale.Crop,
                alpha = if (darkTheme) .18f else .30f, modifier = Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(canvas, canvas.copy(alpha = .45f), canvas))))
        }
        Text("Private. On device.", color = ink.copy(alpha = .72f),
            fontSize = 12.sp, modifier = Modifier.padding(bottom = 32.dp))
    }
}
