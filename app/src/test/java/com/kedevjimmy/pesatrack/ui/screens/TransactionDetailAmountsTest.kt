package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import org.junit.Assert.*
import org.junit.Test

class TransactionDetailAmountsTest {
    @Test fun paymentOutflowIncludesBothFeesOnce() {
        val result = transactionDetailAmounts(200000, 2300, 500, MoneyDirection.EXPENSE)
        assertEquals(2800L, result.feesMinor)
        assertEquals(202800L, result.outflowMinor)
    }

    @Test fun incomeAndRefundsDoNotBecomeOutflows() {
        assertNull(transactionDetailAmounts(200000, 0, 0, MoneyDirection.INCOME).outflowMinor)
        assertNull(transactionDetailAmounts(200000, 0, 0, MoneyDirection.EXPENSE, incomingAdjustment = true).outflowMinor)
    }

    @Test fun transferOutflowIncludesFeeWithoutCallingItSpending() {
        assertEquals(201000L, transactionDetailAmounts(200000, 1000, 0, MoneyDirection.TRANSFER).outflowMinor)
    }

    @Test fun overflowNeverWrapsIntoAValidAmount() {
        val result = transactionDetailAmounts(Long.MAX_VALUE, 1, 0, MoneyDirection.EXPENSE)
        assertNull(result.outflowMinor)
        assertNull(transactionDetailAmounts(1, Long.MAX_VALUE, 1, MoneyDirection.EXPENSE).feesMinor)
    }

    @Test fun moneyKeepsCentsAndNeverUsesFloatingPoint() {
        assertEquals("KES 2,000.05", formatTransactionDetailMoney(200005))
        assertEquals("KES 92,233,720,368,547,758.07", formatTransactionDetailMoney(Long.MAX_VALUE))
        assertEquals("Balance unknown", formatTransactionDetailBalance(null))
    }
}
