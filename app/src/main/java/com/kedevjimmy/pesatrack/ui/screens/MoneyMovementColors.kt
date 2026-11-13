package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.graphics.Color
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens

internal fun moneyMovementColor(direction: MoneyDirection, refund: Boolean = false): Color =
    when {
        refund || direction == MoneyDirection.INCOME -> PremiumVisualTokens.moneyIn
        direction == MoneyDirection.EXPENSE -> PremiumVisualTokens.moneyOut
        direction == MoneyDirection.TRANSFER -> PremiumVisualTokens.transfer
        else -> PremiumVisualTokens.muted
    }
