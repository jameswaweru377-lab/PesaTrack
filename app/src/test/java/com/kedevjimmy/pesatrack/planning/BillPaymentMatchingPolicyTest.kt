package com.kedevjimmy.pesatrack.planning

import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillPaymentMatchingPolicyTest {
    private val bill = SmartRule(
        id = "rent-rule",
        name = "Rent",
        counterpartyContains = "Example",
        category = TransactionCategory.RENT,
        direction = MoneyDirection.EXPENSE,
        preset = RulePreset.OTHER_RECURRING,
        plannedAmountMinor = 100_000L
    )

    @Test fun paymentAssignedToAnotherRuleCannotPayThisBillByMerchantSimilarity() {
        assertFalse(
            BillPaymentMatchingPolicy.matches(
                transactionRuleId = "school-fees-rule",
                rule = bill,
                counterparty = "Example",
                counterpartyPhone = null,
                amountMinor = 100_000L
            )
        )
    }

    @Test fun exactBillRuleAndUnassignedLegacyPaymentCanMatch() {
        assertTrue(matches(ruleId = bill.id))
        assertTrue(matches(ruleId = null))
    }

    @Test fun unassignedPaymentCannotMatchTwoBillsForTheSamePayee() {
        val schoolBill = bill.copy(
            id = "school-fees-rule",
            name = "School fees",
            category = TransactionCategory.OTHER
        )
        val candidates = listOf(bill, schoolBill)

        assertFalse(matches(ruleId = null, candidates = candidates))
        assertFalse(
            BillPaymentMatchingPolicy.matches(
                transactionRuleId = null,
                rule = schoolBill,
                counterparty = "Example",
                counterpartyPhone = null,
                amountMinor = 100_000L,
                candidateBills = candidates
            )
        )
    }

    private fun matches(
        ruleId: String?,
        candidates: List<SmartRule> = listOf(bill)
    ) =
        BillPaymentMatchingPolicy.matches(
            transactionRuleId = ruleId,
            rule = bill,
            counterparty = "Example",
            counterpartyPhone = null,
            amountMinor = 100_000L,
            candidateBills = candidates
        )
}
