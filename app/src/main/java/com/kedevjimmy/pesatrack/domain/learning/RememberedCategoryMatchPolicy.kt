package com.kedevjimmy.pesatrack.domain.learning

import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
import java.util.Locale

/** Explicit category consent applies to an exact identity, never a substring of an SMS. */
object RememberedCategoryMatchPolicy {
    fun matches(sourceName: String?, sourcePhone: String?, name: String?, phone: String?): Boolean {
        val sourceNumber = IdentityGraphPolicy.normalizeKenyanPhone(sourcePhone)
        val number = IdentityGraphPolicy.normalizeKenyanPhone(phone)
        if (sourceNumber != null || number != null) {
            return sourceNumber != null && sourceNumber == number
        }
        val source = normalize(sourceName) ?: return false
        return source == normalize(name)
    }

    private fun normalize(value: String?): String? = value
        ?.replace(Regex("[\\s\\p{Zs}]+"), " ")?.trim()
        ?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() }
}
