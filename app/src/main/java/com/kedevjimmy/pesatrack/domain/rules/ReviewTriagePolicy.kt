package com.kedevjimmy.pesatrack.domain.rules

object ReviewTriagePolicy {
    fun identityKey(
        counterparty: String?,
        counterpartyPhone: String?
    ): String? {
        val phone = normalizePhone(counterpartyPhone.orEmpty())
        if (phone.isNotBlank()) return "phone:$phone"

        val name = normalizeName(counterparty.orEmpty())
        if (name.isBlank()) return null
        return "name:$name"
    }

    fun shouldDefaultToOther(monthlyOccurrenceCount: Int): Boolean =
        monthlyOccurrenceCount == 1

    fun shouldRequestTeaching(monthlyOccurrenceCount: Int): Boolean =
        monthlyOccurrenceCount >= 2

    fun normalizeName(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return when {
            digits.startsWith("254") && digits.length == 12 -> "+$digits"
            digits.startsWith("0") && digits.length == 10 -> "+254" + digits.drop(1)
            else -> digits
        }
    }
}
