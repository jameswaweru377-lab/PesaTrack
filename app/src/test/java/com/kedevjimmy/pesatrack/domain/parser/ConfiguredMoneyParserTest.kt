package com.kedevjimmy.pesatrack.domain.parser

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConfiguredMoneyParserTest {
    @Test
    fun parsesConfiguredBankIncomeWithoutHardcodingBankOrEmployer() {
        val rule = SmartRule(
            id = "salary",
            name = "Monthly salary",
            counterpartyContains = "",
            notificationTitleContains = "my bank",
            messageContains = "my employer",
            category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            planningMonthOffset = 1,
            displayLabel = "Salary"
        )

        val parsed = ConfiguredMoneyParser.parse(
            title = "My Bank",
            body = "KES 60,000.00 received from My Employer. Available balance KES 72,100.00.",
            rule = rule
        )

        assertNotNull(parsed)
        assertEquals(6_000_000L, parsed?.amountMinor)
        assertEquals("Salary", parsed?.counterparty)
    }

    @Test
    fun requiresAnAmount() {
        val rule = SmartRule(
            id = "salary",
            counterpartyContains = "",
            category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            notificationTitleContains = "bank",
            messageContains = "salary"
        )

        assertEquals(
            null,
            ConfiguredMoneyParser.parse(
                title = "Bank",
                body = "Salary notification with no monetary amount.",
                rule = rule
            )
        )
    }
}
