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

/** Passive launch splash; the approved current logo stays centered over café artwork. */
@Composable
fun StartupSplashScreen(darkTheme: Boolean = false) {
    Box(Modifier.fillMaxSize().background(PremiumVisualTokens.darkCanvas)) {
        Image(painter = painterResource(R.drawable.onboarding_cafe_clean),
            contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = .25f), Color.Transparent, Color.Black.copy(alpha = .7f)
        ))))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).background(PremiumVisualTokens.darkSurface, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center) {
                PesaLogoMark(size = 88.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text("PesaTrack", color = Color.White, fontFamily = FontFamily.Serif,
                fontSize = 37.sp, fontWeight = FontWeight.Bold)
            Text("YOUR MONEY, YOUR PLANS", color = PremiumVisualTokens.darkCoffee,
                fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}
