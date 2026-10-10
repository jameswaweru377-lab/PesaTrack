package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class RuleSetupPreviewTest {
    @Test fun salaryAndOtherRulesDoNotPromiseAPlanReservation() {
        for (rule in listOf(
            SmartRule("salary", "employer", TransactionCategory.INCOME, plannedAmountMinor = 10000, startsPayCycle = true),
            SmartRule("other", "sample", TransactionCategory.OTHER, plannedAmountMinor = 10000)
        )) {
            assertFalse(RuleSetupPreview.describe(rule, 0).contains("will reserve"))
        }
    }
    @Test fun notificationPreviewDoesNotPromiseHistoricalReplay() {
        val rule = SmartRule("draft", "", TransactionCategory.INCOME,
            messageContains = "salary", startsPayCycle = true)
        val text = RuleSetupPreview.describe(rule, 0)
        assertTrue(text.contains("future notifications"))
        assertTrue(text.contains("cannot be checked"))
        assertTrue(text.contains("pay cycle"))
    }

    @Test fun counterpartyPreviewStatesExplicitHistoricalImpactAndReservation() {
        val rule = SmartRule("draft", "planet internet", TransactionCategory.INTERNET,
            plannedAmountMinor = 300000)
        val text = RuleSetupPreview.describe(rule, 2)
        assertTrue(text.contains("2 eligible"))
        assertTrue(text.contains("expected amount is saved"))
        assertTrue(text.contains("Nothing changes"))
    }
}
