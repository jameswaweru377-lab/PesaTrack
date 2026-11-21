package com.kedevjimmy.pesatrack.domain.model

/** Reject ambiguous Unicode and phone-like addresses before normalizing sender IDs. */
object SafeSmsSenderAddress {
    private val permitted = Regex("[A-Za-z0-9][A-Za-z0-9 ._&+\\-]{0,39}")

    fun isValid(address: String?): Boolean =
        address != null && permitted.matches(address.trim())
}
