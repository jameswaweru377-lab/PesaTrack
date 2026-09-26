package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartRuleEngineTest {
    @Test
    fun categorizesMatchingCounterparty() {
        val transaction = sample(counterparty = "PLANET INTERNET LTD")
        val rule = SmartRule(
            id = "internet",
            counterpartyContains = "planet internet",
            category = TransactionCategory.INTERNET
        )

        assertEquals(
            TransactionCategory.INTERNET,
            SmartRuleEngine.categorize(transaction, listOf(rule))
        )
    }

    @Test
    fun amountCapPreventsOverBroadPersonRule() {
        val transaction = sample(
            counterparty = "ELIZABETH W",
            amountMinor = 250_000L
        )
        val rule = SmartRule(
            id = "lunch",
            counterpartyContains = "elizabeth",
            category = TransactionCategory.LUNCH,
            maximumAmountMinor = 100_000L
        )

        assertEquals(
            TransactionCategory.UNCATEGORIZED,
            SmartRuleEngine.categorize(transaction, listOf(rule))
        )
    }

    @Test
    fun salaryNotificationRequiresConfiguredSenderAndPhrase() {
        val salaryRule = SmartRule(
            id = "salary",
            name = "Monthly salary",
            counterpartyContains = "",
            notificationTitleContains = "equity",
            messageContains = "braeburn schools",
            category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            planningMonthOffset = 1
        )

        val match = SmartRuleEngine.matchNotification(
            title = "Equity Mobile",
            body = "KES 60,000 received from Braeburn Schools.",
            rules = listOf(salaryRule)
        )

        assertEquals("salary", match?.id)

        val wrongMessage = SmartRuleEngine.matchNotification(
            title = "Equity Mobile",
            body = "Your card payment was successful.",
            rules = listOf(salaryRule)
        )
        assertNull(wrongMessage)
    }

    @Test
    fun disabledNotificationRuleDoesNotMatch() {
        val rule = SmartRule(
            id = "disabled",
            counterpartyContains = "",
            notificationTitleContains = "equity",
            messageContains = "salary",
            category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            enabled = false
        )

        assertNull(
            SmartRuleEngine.matchNotification(
                title = "Equity",
                body = "Salary received KES 50,000",
                rules = listOf(rule)
            )
        )
    }


    @Test
    fun exactFullNameRuleDoesNotMatchFirstNameOnly() {
        val rule = SmartRule(
            id = "shopping-joseph",
            counterpartyContains = "",
            counterpartyExact = "Joseph Mwango",
            category = TransactionCategory.SHOPPING
        )

        assertEquals(
            TransactionCategory.SHOPPING,
            SmartRuleEngine.categorize(
                sample(counterparty = "JOSEPH   MWANGO"),
                listOf(rule)
            )
        )

        assertEquals(
            TransactionCategory.UNCATEGORIZED,
            SmartRuleEngine.categorize(
                sample(counterparty = "Joseph Kasongo"),
                listOf(rule)
            )
        )
    }

    @Test
    fun phoneIdentityWinsWhenAvailable() {
        val rule = SmartRule(
            id = "phone-shopping",
            counterpartyContains = "",
            counterpartyPhoneEquals = "+254712345678",
            category = TransactionCategory.SHOPPING
        )

        val transaction = sample(
            counterparty = "Joseph Mwango",
            counterpartyPhone = "0712345678"
        )

        assertEquals(
            TransactionCategory.SHOPPING,
            SmartRuleEngine.categorize(transaction, listOf(rule))
        )
    }

    private fun sample(
        counterparty: String,
        amountMinor: Long = 150_000L,
        counterpartyPhone: String? = null
    ) = ParsedMpesaTransaction(
        transactionCode = "TQK12ABC34",
        type = MpesaTransactionType.BUY_GOODS,
        amountMinor = amountMinor,
        counterparty = counterparty,
        counterpartyPhone = counterpartyPhone,
        feeMinor = 0L,
        balanceMinor = null,
        occurredAtText = null,
        rawMessage = "sample"
    )
}
