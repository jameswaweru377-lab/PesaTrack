package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class TransactionFactsPresentationTest {
    @Test fun payeeIsNeverInferredWhenMissing() {
        assertEquals("Not identified", transactionFactsPayee(null))
        assertEquals("Not identified", transactionFactsPayee("  "))
        assertEquals("Erick Odhiambo", transactionFactsPayee(" Erick Odhiambo "))
    }

    @Test fun financialMeaningIsNotFlattenedIntoEverydayExpense() {
        assertEquals("Payment amount", transactionFactsAmountTitle("EXPENSE", false))
        assertEquals("Transfer amount", transactionFactsAmountTitle("TRANSFER", false))
        assertEquals("Amount received", transactionFactsAmountTitle("INCOME", false))
        assertEquals("Amount received", transactionFactsAmountTitle("EXPENSE", true))
        assertEquals("Refund", transactionFactsKind("BUY_GOODS", "REFUND"))
        assertEquals("Money sent", transactionFactsKind("SEND_MONEY", null))
    }

    @Test fun sourceNeverClaimsAnUnknownProvider() {
        assertEquals("M-Pesa", transactionFactsChannel("M-Pesa", "SMS"))
        assertEquals("SMS record", transactionFactsChannel("Not identified", "SMS"))
        assertEquals("Notification", transactionFactsChannel("Not identified", "NOTIFICATION"))
        assertEquals("Not identified", transactionFactsChannel("", "UNKNOWN"))
    }

    @Test fun balanceIsHistoricalAndMayBeMissing() {
        assertEquals("Balance unknown", formatTransactionDetailBalance(null))
        assertTrue(transactionFactsBalanceHelp(null).contains("No balance"))
        assertTrue(transactionFactsBalanceHelp(351322L).contains("may no longer be current"))
    }

    @Test fun dateNeverSubstitutesCurrentTimeForMissingTimestamp() {
        assertEquals("Not provided", transactionFactsDate(0L, Locale.ENGLISH))
        assertEquals("Not provided", transactionFactsDate(-1L, Locale.ENGLISH))
        assertTrue(transactionFactsDate(1760000000000L, Locale.ENGLISH).contains("2025"))
    }
}
