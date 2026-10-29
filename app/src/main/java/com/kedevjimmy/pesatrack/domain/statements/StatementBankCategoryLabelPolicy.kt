package com.kedevjimmy.pesatrack.domain.statements

/**
 * A bank's PDF row may carry a local user label, but that label is not proof
 * of income, spending, an owned-account transfer, or an Activity transaction.
 * Only a signed nonzero movement with a compatible category kind is eligible.
 */
object StatementBankCategoryLabelPolicy {
    fun canLabel(movementMinor: Long, categoryKind: String): Boolean = when {
        movementMinor < 0L && movementMinor != Long.MIN_VALUE -> categoryKind == "EXPENSE"
        movementMinor > 0L -> categoryKind == "INCOME"
        else -> false
    }
}
