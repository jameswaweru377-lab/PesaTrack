package com.kedevjimmy.pesatrack.domain.parser

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConfiguredMoneyParserTest {
    @Test
    fun parsesConfiguredBankSalaryAlert() {
        val rule = SmartRule(
            id = "salary",
            counterpartyContains = "",
            category = TransactionCategory.INCOME,
            name = "Monthly salary",
            notificationTitleContains = "equity",
            messageContains = "employer",
            direction = MoneyDirection.INCOME,
            planningMonthOffset = 1,
            displayLabel = "Salary"
        )

        val parsed = ConfiguredMoneyParser.parse(
            title = "Equity Mobile",
            body = "KES 60,000.00 credited from EMPLOYER. Available balance KES 75,500.00",
            rule = rule
        )

        assertNotNull(parsed)
        assertEquals(6_000_000L, parsed?.amountMinor)
        assertEquals("Salary", parsed?.counterparty)
    }
}
