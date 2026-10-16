package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionRedesignFinancialPolicyTest {
    @Test fun moneyLentAndSharedBillRequireUnlinkedConfirmedExpenses() {
        assertTrue(canMakePersonalDebtLink("EXPENSE", null, null, "AUTO_CLASSIFIED"))
        assertTrue(canMakePersonalDebtLink("EXPENSE", null, null, "USER_CLASSIFIED"))
        assertFalse(canMakePersonalDebtLink("INCOME", null, null, "USER_CLASSIFIED"))
        assertFalse(canMakePersonalDebtLink("TRANSFER", null, null, "USER_CLASSIFIED"))
        assertFalse(canMakePersonalDebtLink("EXPENSE", "REFUND", null, "USER_CLASSIFIED"))
        assertFalse(canMakePersonalDebtLink("EXPENSE", null, "REVERSALCODE", "USER_CLASSIFIED"))
        assertFalse(canMakePersonalDebtLink("EXPENSE", null, null, "EXCLUDED"))
        assertFalse(canMakePersonalDebtLink("EXPENSE", null, null, "NEEDS_REVIEW"))
    }
}
