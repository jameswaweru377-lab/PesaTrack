package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRuleDecisionExplanationTest {
    @Test
    fun exactCounterpartyExplanationNamesRuleWithoutEchoingSensitiveValue() {
        val explanation =
            SmartRuleDecisionExplanation
                .describe(
                    rule(
                        name =
                            "Remember Sample Merchant",
                        counterpartyExact =
                            "Sample Merchant"
                    )
                )

        assertTrue(
            explanation.contains(
                "Remember Sample Merchant"
            )
        )
        assertTrue(
            explanation.contains(
                "counterparty matched the saved identity"
            )
        )
    }

    @Test
    fun phoneExplanationDoesNotEchoStoredPhoneNumber() {
        val phone =
            "+254700123456"
        val explanation =
            SmartRuleDecisionExplanation
                .describe(
                    rule(
                        name =
                            "Remember mobile recipient",
                        counterpartyPhoneEquals =
                            phone
                    )
                )

        assertTrue(
            explanation.contains(
                "phone number matched the saved identity"
            )
        )
        assertFalse(
            explanation.contains(phone)
        )
    }

    @Test
    fun messageConditionDoesNotEchoSavedMessagePhrase() {
        val sensitivePhrase =
            "private reference phrase"
        val explanation =
            SmartRuleDecisionExplanation
                .describe(
                    rule(
                        name =
                            "Monthly service",
                        messageContains =
                            sensitivePhrase
                    )
                )

        assertTrue(
            explanation.contains(
                "financial message matched a saved phrase"
            )
        )
        assertFalse(
            explanation.contains(
                sensitivePhrase
            )
        )
    }

    private fun rule(
        name: String,
        counterpartyExact: String = "",
        counterpartyPhoneEquals: String = "",
        counterpartyContains: String = "",
        messageContains: String = ""
    ) =
        SmartRule(
            id = "rule-1",
            counterpartyContains =
                counterpartyContains,
            category =
                TransactionCategory
                    .FOOD_DINING,
            name = name,
            notificationTitleContains = "",
            messageContains =
                messageContains,
            direction =
                MoneyDirection.EXPENSE,
            counterpartyExact =
                counterpartyExact,
            counterpartyPhoneEquals =
                counterpartyPhoneEquals
        )
}
