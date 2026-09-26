package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartRuleEngineTest {

    @Test
    fun appliesRecipientRuleCaseInsensitively() {
        val transaction = ParsedMpesaTransaction(
            transactionCode = "TQK12ABC34",
            type = MpesaTransactionType.SENT_MONEY,
            amountMinor = 45_000,
            counterparty = "Elizabeth W.",
            feeMinor = 0,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = "test"
        )

        val result = SmartRuleEngine.categorize(
            transaction,
            listOf(
                SmartRule(
                    id = "lunch-rule",
                    counterpartyContains = "elizabeth",
                    category = TransactionCategory.LUNCH
                )
            )
        )

        assertEquals(TransactionCategory.LUNCH, result)
    }

    @Test
    fun matchesConfiguredSalaryNotification() {
        val rule = SmartRule(
            id = "salary",
            counterpartyContains = "",
            category = TransactionCategory.INCOME,
            name = "Monthly salary",
            notificationTitleContains = "equity",
            messageContains = "braeburn schools",
            direction = MoneyDirection.INCOME,
            planningMonthOffset = 1
        )

        val matched = SmartRuleEngine.matchNotification(
            title = "Equity Mobile",
            body = "KES 60,000 credited from BRAEBURN SCHOOLS",
            rules = listOf(rule)
        )

        assertEquals(rule, matched)
    }

    @Test
    fun configuredNotificationRequiresAllProvidedMatchers() {
        val rule = SmartRule(
            id = "salary",
            counterpartyContains = "",
            category = TransactionCategory.INCOME,
            notificationTitleContains = "equity",
            messageContains = "braeburn schools",
            direction = MoneyDirection.INCOME
        )

        val matched = SmartRuleEngine.matchNotification(
            title = "Equity Mobile",
            body = "KES 60,000 credited from another source",
            rules = listOf(rule)
        )

        assertEquals(null, matched)
    }

    @Test
    fun respectsOptionalMaximumAmount() {
        val transaction = ParsedMpesaTransaction(
            transactionCode = null,
            type = MpesaTransactionType.SENT_MONEY,
            amountMinor = 250_000,
            counterparty = "Sample Merchant",
            feeMinor = 0,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = "test"
        )

        val result = SmartRuleEngine.categorize(
            transaction,
            listOf(
                SmartRule(
                    id = "small-purchases",
                    counterpartyContains = "sample merchant",
                    category = TransactionCategory.LUNCH,
                    maximumAmountMinor = 100_000
                )
            )
        )

        assertEquals(TransactionCategory.UNCATEGORIZED, result)
    }
}
