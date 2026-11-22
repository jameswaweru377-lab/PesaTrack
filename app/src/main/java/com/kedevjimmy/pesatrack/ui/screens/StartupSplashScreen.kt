package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.components.PesaLogoMark
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** A brief everyday brand reveal; the approved first-run café artwork is unchanged. */
@Composable
fun StartupSplashScreen(darkTheme: Boolean = false, returningUser: Boolean = false) {
    if (returningUser) {
        ReturningStartupSplash()
    } else {
        FirstRunCafeSplash()
    }
}

@Composable
private fun ReturningStartupSplash() {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(520, easing = FastOutSlowInEasing))
    }
    Box(Modifier.fillMaxSize().background(PremiumVisualTokens.darkCanvas)) {
        Column(Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(PesaDesignTokens.dailySplashTraceSize),
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        color = PremiumVisualTokens.darkCoffee,
                        startAngle = 198f,
                        sweepAngle = 105f * reveal.value,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                PesaLogoMark(size = PesaDesignTokens.dailySplashLogoSize)
            }
            Spacer(Modifier.height(8.dp))
            Text("PesaTrack", modifier = Modifier.alpha(reveal.value),
                color = PremiumVisualTokens.darkText, fontFamily = FontFamily.Serif,
                fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("YOUR MONEY, YOUR PLANS", modifier = Modifier.alpha(reveal.value),
                color = PremiumVisualTokens.darkCoffee, fontSize = 10.sp,
                letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun FirstRunCafeSplash() {
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
