package com.kedevjimmy.pesatrack.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size.sp, lineHeight = height.sp)
val PesaTypography = Typography(
    displayLarge = type(30, 36, FontWeight.SemiBold), displayMedium = type(28, 34, FontWeight.SemiBold), displaySmall = type(28, 34, FontWeight.SemiBold),
    headlineLarge = type(24, 30, FontWeight.SemiBold), headlineMedium = type(22, 28, FontWeight.SemiBold), headlineSmall = type(20, 26, FontWeight.SemiBold),
    titleLarge = type(18, 24, FontWeight.SemiBold), titleMedium = type(16, 22, FontWeight.Medium), titleSmall = type(14, 20, FontWeight.Medium),
    bodyLarge = type(16, 24), bodyMedium = type(14, 21), bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.Medium), labelMedium = type(12, 18, FontWeight.Medium), labelSmall = type(12, 16, FontWeight.Medium)
)
