package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterpretedRuleResolverTest {
    @Test
    fun strongTransactionIdentityWinsForGenericPayment() {
        val transactionRule = SmartRule(
            id = "merchant-rule",
            counterpartyContains = "",
            counterpartyExact = "SAMPLE SHOP",
            category = TransactionCategory.SHOPPING
        )
        val broadNotificationRule = SmartRule(
            id = "notification-rule",
            counterpartyContains = "",
            category = TransactionCategory.OTHER,
            notificationTitleContains = "mpesa"
        )

        val match = InterpretedRuleResolver.match(
            counterparty = "SAMPLE SHOP",
            counterpartyPhone = null,
            amountMinor = 85_000L,
            title = "MPESA",
            body = "Ksh 850 paid to SAMPLE SHOP",
            rules = listOf(
                broadNotificationRule,
                transactionRule
            )
        )

        assertEquals("merchant-rule", match?.id)
    }

    @Test
    fun notificationRuleRemainsAvailableAfterGenericParsing() {
        val rule = SmartRule(
            id = "configured-source",
            counterpartyContains = "",
            category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            notificationTitleContains = "sample bank",
            messageContains = "credited"
        )

        val match = InterpretedRuleResolver.match(
            counterparty = null,
            counterpartyPhone = null,
            amountMinor = 200_000L,
            title = "Sample Bank",
            body = "Your account has been credited Ksh 2,000",
            rules = listOf(rule)
        )

        assertEquals("configured-source", match?.id)
    }

    @Test
    fun genericRuleMatchingKeepsAmountCeiling() {
        val rule = SmartRule(
            id = "small-shop-rule",
            counterpartyContains = "sample shop",
            category = TransactionCategory.SHOPPING,
            maximumAmountMinor = 100_000L
        )

        assertNull(
            InterpretedRuleResolver.match(
                counterparty = "SAMPLE SHOP",
                counterpartyPhone = null,
                amountMinor = 150_000L,
                title = "Wallet",
                body = "KES 1,500 paid to SAMPLE SHOP",
                rules = listOf(rule)
            )
        )
    }

    @Test
    fun doesNotInventRuleWhenNothingMatches() {
        val rule = SmartRule(
            id = "unrelated",
            counterpartyContains = "SAMPLE SHOP",
            category = TransactionCategory.SHOPPING
        )

        assertNull(
            InterpretedRuleResolver.match(
                counterparty = "OTHER PERSON",
                counterpartyPhone = null,
                amountMinor = 50_000L,
                title = "MPESA",
                body = "Ksh 500 sent to OTHER PERSON",
                rules = listOf(rule)
            )
        )
    }
}
