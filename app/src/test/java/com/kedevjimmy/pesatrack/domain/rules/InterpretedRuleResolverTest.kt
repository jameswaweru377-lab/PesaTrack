package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterpretedRuleResolverTest {
    @Test
    fun strongTransactionIdentityWinsForGenericPayment() {
        val parsed = ParsedMpesaTransaction(
            transactionCode = "TST12RULE1",
            type = MpesaTransactionType.BUY_GOODS,
            amountMinor = 85_000L,
            counterparty = "SAMPLE SHOP",
            counterpartyPhone = null,
            feeMinor = 0L,
            balanceMinor = 100_000L,
            occurredAtText = null,
            rawMessage = "synthetic"
        )
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
            transaction = parsed,
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
        val parsed = ParsedMpesaTransaction(
            transactionCode = "TST12RULE2",
            type = MpesaTransactionType.RECEIVED,
            amountMinor = 200_000L,
            counterparty = null,
            counterpartyPhone = null,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = "synthetic"
        )
        val rule = SmartRule(
            id = "configured-source",
            counterpartyContains = "",
            category = TransactionCategory.INCOME,
            direction = MoneyDirection.INCOME,
            notificationTitleContains = "sample bank",
            messageContains = "credited"
        )

        val match = InterpretedRuleResolver.match(
            transaction = parsed,
            title = "Sample Bank",
            body = "Your account has been credited Ksh 2,000",
            rules = listOf(rule)
        )

        assertEquals("configured-source", match?.id)
    }

    @Test
    fun doesNotInventRuleWhenNothingMatches() {
        val parsed = ParsedMpesaTransaction(
            transactionCode = "TST12RULE3",
            type = MpesaTransactionType.SENT_MONEY,
            amountMinor = 50_000L,
            counterparty = "OTHER PERSON",
            counterpartyPhone = null,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = "synthetic"
        )
        val rule = SmartRule(
            id = "unrelated",
            counterpartyContains = "SAMPLE SHOP",
            category = TransactionCategory.SHOPPING
        )

        assertNull(
            InterpretedRuleResolver.match(
                transaction = parsed,
                title = "MPESA",
                body = "Ksh 500 sent to OTHER PERSON",
                rules = listOf(rule)
            )
        )
    }
}
