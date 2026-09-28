package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleBackedFinancialResolverTest {
    private val salaryRule =
        SmartRule(
            id = "salary",
            name = "Monthly salary",
            counterpartyContains = "",
            notificationTitleContains = "my bank",
            messageContains = "salary",
            category =
                TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            displayLabel = "Salary"
        )

    @Test
    fun choosesTransactionAmountAfterExcludingBalance() {
        val parsed =
            RuleBackedFinancialResolver.resolve(
                title = "My Bank",
                body =
                    "Salary processed. KES 60,000.00. Available balance KES 72,100.00.",
                rule = salaryRule
            )

        assertEquals(
            6_000_000L,
            parsed?.amountMinor
        )
        assertEquals("Salary", parsed?.counterparty)
    }

    @Test
    fun refusesAmbiguousMultipleTransactionAmounts() {
        val parsed =
            RuleBackedFinancialResolver.resolve(
                title = "My Bank",
                body =
                    "Salary processed. KES 60,000.00 plus KES 5,000.00 adjustment. Available balance KES 77,100.00.",
                rule = salaryRule
            )

        assertNull(parsed)
    }

    @Test
    fun excludesRecognizedFeeFromPrimaryAmount() {
        val parsed =
            RuleBackedFinancialResolver.resolve(
                title = "My Bank",
                body =
                    "Salary processed. KES 60,000.00. Transaction fee KES 100.00. Available balance KES 71,900.00.",
                rule = salaryRule
            )

        assertEquals(
            6_000_000L,
            parsed?.amountMinor
        )
        assertEquals(10_000L, parsed?.feeMinor)
    }
}
