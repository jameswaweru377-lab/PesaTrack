package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal data class TransactionDetailAmounts(val feesMinor: Long?, val outflowMinor: Long?)

internal fun transactionDetailIsIncoming(type: String, relationType: String?): Boolean =
    type in setOf("RECEIVED", "DEPOSIT", "REVERSAL") ||
        relationType in setOf("REFUND", "INVESTMENT_SELL")

internal fun transactionDetailAmounts(
    amountMinor: Long,
    feeMinor: Long,
    accessFeeMinor: Long,
    direction: MoneyDirection,
    incomingAdjustment: Boolean = false
): TransactionDetailAmounts {
    val fees = runCatching { Math.addExact(feeMinor, accessFeeMinor) }.getOrNull()
    val outgoing = !incomingAdjustment && direction in listOf(MoneyDirection.EXPENSE, MoneyDirection.TRANSFER)
    val outflow = if (outgoing && fees != null) runCatching { Math.addExact(amountMinor, fees) }.getOrNull() else null
    return TransactionDetailAmounts(fees, outflow)
}

internal fun formatTransactionDetailMoney(minor: Long): String =
    "KES " + DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.ENGLISH))
        .format(BigDecimal.valueOf(minor, 2))

internal fun formatTransactionDetailBalance(minor: Long?): String =
    minor?.let(::formatTransactionDetailMoney) ?: "Balance unknown"
