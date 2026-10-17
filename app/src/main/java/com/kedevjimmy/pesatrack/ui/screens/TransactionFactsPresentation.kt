package com.kedevjimmy.pesatrack.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI-only labels for the Facts card. Do not infer merchant identity, location,
 * account balances or transaction provenance from a generic SMS/source kind.
 */
internal fun transactionFactsPayee(counterparty: String?): String =
    counterparty?.trim()?.takeIf(String::isNotBlank) ?: "Not identified"

internal fun transactionFactsKind(type: String, relationType: String?): String {
    if (relationType == "REFUND") return "Refund"
    if (relationType == "OWN_ACCOUNT_TRANSFER") return "Own-account transfer"
    return when (type.uppercase(Locale.ROOT)) {
        "SEND_MONEY" -> "Money sent"
        "BUY_GOODS" -> "Buy goods payment"
        "PAYBILL" -> "Paybill payment"
        "WITHDRAW", "WITHDRAWAL" -> "Cash withdrawal"
        "DEPOSIT" -> "Deposit"
        "RECEIVED" -> "Money received"
        "REVERSAL" -> "Reversal"
        else -> type.replace('_', ' ').trim()
            .takeIf(String::isNotBlank)?.lowercase(Locale.ROOT)
            ?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "Not identified"
    }
}

internal fun transactionFactsAmountTitle(direction: String, incomingAdjustment: Boolean): String =
    when {
        incomingAdjustment || direction == "INCOME" -> "Amount received"
        direction == "TRANSFER" -> "Transfer amount"
        direction == "EXPENSE" -> "Payment amount"
        else -> "Amount"
    }

internal fun transactionFactsChannel(accountLabel: String, sourceKind: String): String =
    accountLabel.takeUnless { it.isBlank() || it == "Not identified" }
        ?: when (sourceKind.uppercase(Locale.ROOT)) {
            "SMS" -> "SMS record"
            "NOTIFICATION" -> "Notification"
            "STATEMENT", "PDF_STATEMENT", "CSV_STATEMENT" -> "Statement"
            "MANUAL" -> "Manual entry"
            else -> "Not identified"
        }

internal fun transactionFactsDate(epochMs: Long, locale: Locale): String =
    if (epochMs <= 0L) "Not provided"
    else SimpleDateFormat("d MMM yyyy, h:mm a", locale).format(Date(epochMs))

internal fun transactionFactsBalanceHelp(balanceMinor: Long?): String =
    if (balanceMinor == null) "No balance was reported with this transaction."
    else "Balance from the recorded message; it may no longer be current."
