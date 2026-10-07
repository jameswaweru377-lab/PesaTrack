package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaLogoMark

@Composable
fun AppLockScreen(
    available: Boolean,
    errorMessage: String?,
    onUnlock: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF063F34),
                            Color(0xFF04352C),
                            Color(0xFF02241F)
                        )
                    )
                )
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                )
    ) {
        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.Center
                    )
                    .padding(
                        horizontal = 30.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            PesaLogoMark(
                size = 64.dp
            )

            Spacer(
                Modifier.size(26.dp)
            )

            Surface(
                modifier =
                    Modifier.size(86.dp),
                shape =
                    CircleShape,
                color =
                    Color.White.copy(
                        alpha = .08f
                    ),
                border =
                    androidx.compose.foundation
                        .BorderStroke(
                            1.dp,
                            Color.White.copy(
                                alpha = .14f
                            )
                        )
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                38.dp
                            ),
                        tint =
                            Color(0xFF86E8BE)
                    )
                }
            }

            Text(
                "PesaTrack is locked",
                modifier =
                    Modifier.padding(
                        top = 22.dp
                    ),
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                color = Color.White,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                if (available) {
                    "Unlock with your fingerprint, face, or device screen lock."
                } else {
                    "App lock needs a secure screen lock or supported biometric on this device."
                },
                modifier =
                    Modifier.padding(
                        top = 8.dp
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    Color.White.copy(
                        alpha = .72f
                    ),
                textAlign =
                    TextAlign.Center
            )

            errorMessage
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    Text(
                        it,
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            Color(0xFFFFC9C9),
                        textAlign =
                            TextAlign.Center
                    )
                }

            Button(
                onClick = onUnlock,
                enabled = available,
                modifier =
                    Modifier.padding(
                        top = 24.dp
                    ),
                shape =
                    RoundedCornerShape(
                        18.dp
                    )
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription =
                        null
                )
                Text(
                    "Unlock PesaTrack",
                    modifier =
                        Modifier.padding(
                            start = 8.dp
                        )
                )
            }
        }
    }
}
