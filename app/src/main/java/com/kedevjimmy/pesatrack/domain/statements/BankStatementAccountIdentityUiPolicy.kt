package com.kedevjimmy.pesatrack.domain.statements

/** User-entered bank PDF identity: complete digits, never a masked/last-four shortcut. */
object BankStatementAccountIdentityUiPolicy {
    fun normalizeFullNumber(input: String?): String? {
        val raw = input?.trim().orEmpty()
        if (raw.length > 64) return null
        val digits = raw.filterNot { it.isWhitespace() || it == '-' }
        return digits.takeIf { it.length in 8..20 && it.all { char -> char in '0'..'9' } }
    }

    fun hasFullNumber(input: String?): Boolean = normalizeFullNumber(input) != null

    /** Never interpolate extracted or saved account identifiers into an error. */
    fun safeBlockedMessage(reason: StatementAdmissionResult.Reason): String = when (reason) {
        StatementAdmissionResult.Reason.UNKNOWN_ACCOUNT_IDENTITY ->
            "A complete bank account number is required. Enter the full number in Source account, " +
                "save it on your phone, then verify the PDF again. Nothing was saved."
        StatementAdmissionResult.Reason.WRONG_ACCOUNT ->
            "The selected bank or saved account number does not match this PDF. Choose the correct " +
                "bank account or change its full account number above, then try again. Nothing was saved."
        else -> "This bank statement was blocked: $reason. Nothing was saved."
    }
}
