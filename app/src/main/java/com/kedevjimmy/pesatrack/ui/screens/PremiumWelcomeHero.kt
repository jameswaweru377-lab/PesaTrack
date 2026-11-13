package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

/**
 * Decorative finance illustration built from locally bundled images and Compose.
 * No invented transaction values or network-loaded pictures.
 */
@Composable
internal fun PremiumWelcomeHero() {
    Box(
        modifier = Modifier.fillMaxWidth().height(188.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(PremiumVisualTokens.sand)
    ) {
        Image(
            painter = painterResource(R.drawable.profile_default_nairobi_cover),
            contentDescription = null, contentScale = ContentScale.Crop, alpha = 0.21f,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(PremiumVisualTokens.card.copy(alpha = 0.40f),
                    PremiumVisualTokens.cream.copy(alpha = 0.76f))
            )
        ))
        Image(
            painter = painterResource(R.drawable.wallet_welcome),
            contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.BottomStart)
                .width(192.dp).height(170.dp).offset(x = (-8).dp)
        )
        PreviewPhone(
            modifier = Modifier.align(Alignment.CenterEnd)
                .padding(end = 28.dp).rotate(5f)
        )
    }
}

@Composable
private fun PreviewPhone(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.width(130.dp).height(174.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .background(PremiumVisualTokens.ink, RoundedCornerShape(22.dp))
            .padding(4.dp)
            .background(PremiumVisualTokens.card, RoundedCornerShape(18.dp))
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).width(32.dp).height(4.dp)
                    .background(PremiumVisualTokens.ink, RoundedCornerShape(4.dp))
            )
            Text(
                "YOUR MONEY", fontWeight = FontWeight.Bold, fontSize = 10.sp,
                color = PremiumVisualTokens.coffee
            )
            Text(
                "All in one place", fontSize = 10.sp,
                color = PremiumVisualTokens.ink
            )
            Box(
                Modifier.fillMaxWidth().height(4.dp).background(
                    PremiumVisualTokens.leaf,
                    RoundedCornerShape(4.dp)
                )
            )
            PreviewAccountLine("M-Pesa", true)
            PreviewAccountLine("Bank", false)
        }
    }
}

@Composable
private fun PreviewAccountLine(label: String, wallet: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (wallet) Icons.Rounded.AccountBalanceWallet else Icons.Rounded.AccountBalance,
            contentDescription = null, tint = PremiumVisualTokens.coffee,
            modifier = Modifier.size(16.dp)
        )
        Text(
            label, fontSize = 10.sp, color = PremiumVisualTokens.ink,
            fontWeight = FontWeight.SemiBold
        )
    }
}
