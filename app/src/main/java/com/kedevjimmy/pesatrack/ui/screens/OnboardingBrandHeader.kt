package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.ui.components.PesaLogoMark
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

@Composable
internal fun OnboardingBrandHeader() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(70.dp).background(PremiumVisualTokens.darkSurface, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center) {
            PesaLogoMark(size = 62.dp)
        }
        Spacer(Modifier.height(5.dp))
        Text("PesaTrack", fontFamily = FontFamily.Serif, fontSize = 30.sp,
            fontWeight = FontWeight.Bold, color = PremiumVisualTokens.darkText)
        Text("YOUR MONEY, YOUR PLANS", color = PremiumVisualTokens.darkCoffee,
            fontSize = 10.sp, letterSpacing = 1.7.sp)
    }
}
