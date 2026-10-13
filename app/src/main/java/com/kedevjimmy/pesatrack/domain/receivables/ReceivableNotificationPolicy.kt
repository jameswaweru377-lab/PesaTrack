package com.kedevjimmy.pesatrack.domain.receivables

data class NoticePerson(val id: String, val phone: String?, val hasHistory: Boolean, val outstanding: Long?, val knownAt: Long = 0)
object ReceivableNotificationPolicy {
    fun match(people: List<NoticePerson>, phone: String?, direction: String, amount: Long,
        occurred: Long, received: Long, source: String, enabledAt: Long, now: Long,
        linkEligible: Boolean, ambiguous: Boolean): NoticePerson? {
        if (!linkEligible || ambiguous || amount <= 0 || enabledAt <= 0 ||
            direction !in setOf("EXPENSE","INCOME") ||
            source !in setOf("sms","mpesa_sms","financial_sms","mpesa_notification",
                "financial_notification","configured_notification") ||
            occurred <= enabledAt || received <= enabledAt || occurred > now || received > now ||
            now - occurred > 24 * 60 * 60_000L) return null
        val normalized = ReceivableHistoryPolicy.normalizedPhone(phone) ?: return null
        // Count all phone collisions, including people with invalid/empty history.
        val person = people.filter { ReceivableHistoryPolicy.normalizedPhone(it.phone) == normalized }.singleOrNull()
            ?: return null
        return person.takeIf { it.hasHistory && it.outstanding != null && it.outstanding >= 0 && it.knownAt <= occurred }
    }
    fun canRepay(person: NoticePerson, amount: Long): Boolean =
        person.hasHistory && amount > 0 && person.outstanding?.let { amount <= it && it > 0 } == true
}
