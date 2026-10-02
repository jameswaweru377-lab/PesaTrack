package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.SmartRule

object SmartRuleDecisionExplanation {
    fun describe(
        rule: SmartRule
    ): String {
        val ruleName =
            rule.name
                .trim()
                .ifBlank {
                    "Saved smart rule"
                }

        val reason =
            when {
                rule.counterpartyExact
                    .isNotBlank() ->
                    "the counterparty matched the saved identity"

                rule.counterpartyPhoneEquals
                    .isNotBlank() ->
                    "the phone number matched the saved identity"

                rule.counterpartyContains
                    .isNotBlank() ->
                    "the counterparty matched a saved name pattern"

                rule.notificationTitleContains
                    .isNotBlank() ->
                    "the notification title matched the saved rule"

                rule.messageContains
                    .isNotBlank() ->
                    "the financial message matched a saved phrase"

                rule.accountId != null ||
                    rule.linkedAccountId !=
                    null ->
                    "the account path matched the saved rule"

                else ->
                    "its saved conditions matched"
            }

        return "Automatically classified by “" +
            ruleName +
            "” because " +
            reason +
            "."
    }
}
