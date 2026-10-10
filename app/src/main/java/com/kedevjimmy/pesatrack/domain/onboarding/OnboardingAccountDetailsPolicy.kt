package com.kedevjimmy.pesatrack.domain.onboarding

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import java.util.Locale

data class AccountDetailsValidation(val entries: Map<String, String> = emptyMap(), val error: String? = null)

/** Four digits alone must never be treated as a verified bank account number. */
object OnboardingAccountDetailsPolicy {
    fun validate(accounts: List<MoneyAccount>, proposed: Map<String, String>): AccountDetailsValidation {
        val banks = accounts.filter { it.type == AccountType.BANK }.associateBy { it.id }
        val cleaned = linkedMapOf<String, String>()
        for ((id, value) in proposed) {
            if (id !in banks) return AccountDetailsValidation(error = "The account list changed. Check your accounts and try again.")
            val raw = value.trim()
            if (raw.isEmpty()) continue
            if (raw.count(Char::isLetterOrDigit) < 6)
                return AccountDetailsValidation(error = "Enter the complete account number, not just its last four digits, or leave blank.")
            if (raw.length > 60) return AccountDetailsValidation(error = "The account reference is too long.")
            cleaned[id] = raw
        }
        val normalized = banks.mapValues { (id, account) ->
            (cleaned[id] ?: account.identifier.orEmpty()).filter(Char::isLetterOrDigit).uppercase(Locale.ROOT)
        }
        for (id in cleaned.keys) {
            if (normalized.any { (otherId, value) -> otherId != id && value.isNotEmpty() && value == normalized[id] })
                return AccountDetailsValidation(error = "Two accounts have the same reference. Check the account numbers.")
        }
        return AccountDetailsValidation(entries = cleaned)
    }
}
