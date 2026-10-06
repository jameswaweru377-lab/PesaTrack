package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.*

/** A payer match enriches a credit; it cannot turn a purchase into a salary. */
object SalaryEvidencePolicy {
    fun isSalary(rule: SmartRule)=rule.startsPayCycle || rule.preset==RulePreset.SALARY
    fun accepts(transaction: ParsedMpesaTransaction): Boolean = when(transaction.type) {
        MpesaTransactionType.RECEIVED -> true
        MpesaTransactionType.OTHER -> hasCreditEvidence(transaction.rawMessage)
        else -> false
    }
    fun hasCreditEvidence(body: String): Boolean {
        val text=body.lowercase()
        if(Regex("\\b(paid to|sent to|debited|purchased|bought|withdrawn|withdrawal|you paid)\\b").containsMatchIn(text))return false
        return Regex("\\b(received from|you have received|you received|credited|salary received|salary payment received)\\b").containsMatchIn(text)
    }
}
