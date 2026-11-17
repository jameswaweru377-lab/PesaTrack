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

/** Readable movement colours on the active background, without changing ledger meaning. */
@androidx.compose.runtime.Composable
internal fun themeMoneyMovementColor(
    direction: MoneyDirection,
    refund: Boolean = false
): Color {
    val night = androidx.compose.material3.MaterialTheme.colorScheme.background ==
        PremiumVisualTokens.darkCanvas
    if (!night) return moneyMovementColor(direction, refund)
    return when {
        refund || direction == MoneyDirection.INCOME -> PremiumVisualTokens.darkMoneyIn
        direction == MoneyDirection.EXPENSE -> PremiumVisualTokens.darkMoneyOut
        direction == MoneyDirection.TRANSFER -> PremiumVisualTokens.darkTransfer
        else -> PremiumVisualTokens.darkMuted
    }
}
