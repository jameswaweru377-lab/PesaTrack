package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.SmartRule

/** Describes local preview limits without changing matching or saving a rule. */
object RuleSetupPreview {
    fun describe(rule: SmartRule, matchingCount: Int): String = buildString {
        if (rule.counterpartyContains.isBlank()) {
            append("This rule will recognize future notifications. Existing notification messages cannot be checked or reapplied here.")
        } else {
            append("$matchingCount eligible saved transactions match this name. Saving applies this rule to those matches and future matches. Previously reviewed or protected entries keep their protections.")
        }
        if (rule.plannedAmountMinor != null) {
            append(" The expected amount is saved with this rule. Eligible bills and savings commitments can use it in your plan.")
        }
        if (rule.startsPayCycle) {
            append(" Recognized salary receipts can start a pay cycle.")
        }
        append(" Nothing changes until you choose Save and apply.")
    }
}
