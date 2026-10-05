package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.ConceptBrandLockup
import com.kedevjimmy.pesatrack.ui.components.ConceptWalletIllustration

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF07553F),
                        Color(0xFF063F34),
                        Color(0xFF043127)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConceptBrandLockup(light = true)

        Spacer(Modifier.height(22.dp))

        Text(
            "Your money.\nBetter days.",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            "Understand your money, automatically.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = .78f),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ConceptWalletIllustration(
                modifier = Modifier.padding(top = 8.dp),
                compact = false
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF8F7F1),
                contentColor = Color(0xFF0B503E)
            )
        ) {
            Text("Get started", fontWeight = FontWeight.Bold)
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = .8f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                "Your data stays on your phone",
                modifier = Modifier.padding(start = 7.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = .76f)
            )
        }
    }
}
