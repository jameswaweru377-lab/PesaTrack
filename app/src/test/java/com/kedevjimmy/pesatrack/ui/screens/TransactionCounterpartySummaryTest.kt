package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionCounterpartySummaryTest {
    @Test fun ordinaryPaymentsAndReceiptsKeepTheirKnownDirections() {
        assertEquals("Paid to Amina Mwangi", transactionCounterpartySummary(MoneyDirection.EXPENSE.name, "Amina Mwangi"))
        assertEquals("Received from Amina Mwangi", transactionCounterpartySummary(MoneyDirection.INCOME.name, "Amina Mwangi"))
    }

    @Test fun transfersDoNotGuessWhetherCounterpartyIsSenderOrRecipient() {
        assertEquals("Transfer involving Amina Mwangi",
            transactionCounterpartySummary(MoneyDirection.TRANSFER.name, "Amina Mwangi"))
    }

    @Test fun missingCounterpartyDoesNotInventAnIdentity() {
        assertEquals("Transaction", transactionCounterpartySummary(MoneyDirection.EXPENSE.name, null))
    }
}
