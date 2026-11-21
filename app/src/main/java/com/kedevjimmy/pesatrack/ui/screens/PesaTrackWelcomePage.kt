package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/** Approved photographic welcome, with the message and button unobstructed. */
@Composable
internal fun PesaTrackWelcomePage(onGetStarted: () -> Unit) {
    Box(Modifier.fillMaxSize().background(PremiumVisualTokens.darkCanvas)) {
        Image(
            painter = painterResource(R.drawable.onboarding_cafe_clean),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = .48f), Color.Transparent, Color.Black.copy(alpha = .1f),
            Color.Black.copy(alpha = .84f)
        ))))
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            Text("YOUR MONEY.\nYOUR PLANS.", color = Color.White, fontSize = 40.sp,
                lineHeight = 43.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text("Understand M-PESA, banks and transfers — privately.",
                Modifier.padding(top = 12.dp, bottom = 25.dp), color = Color.White,
                fontSize = 16.sp, textAlign = TextAlign.Center)
            Button(onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumVisualTokens.cream,
                    contentColor = PremiumVisualTokens.coffee)) {
                Text("Get started", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}
