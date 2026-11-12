package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

/** Small, offline skyline header reused by the first-run pages. */
@Composable
internal fun OnboardingPremiumHeader() {
    Box(Modifier.fillMaxWidth().height(152.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_default_nairobi_cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = .28f,
            modifier = Modifier.matchParentSize()
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            listOf(
                PremiumVisualTokens.cream.copy(alpha = .18f),
                PremiumVisualTokens.cream.copy(alpha = .76f),
                PremiumVisualTokens.cream
            )
        )))
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PesaLogoMark(size = 40.dp)
            Text(
                "PesaTrack", color = PremiumVisualTokens.ink,
                fontFamily = FontFamily.Serif, fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "YOUR MONEY, YOUR PLANS",
                color = PremiumVisualTokens.coffee,
                fontSize = 9.sp, letterSpacing = 1.5.sp
            )
        }
    }
}
