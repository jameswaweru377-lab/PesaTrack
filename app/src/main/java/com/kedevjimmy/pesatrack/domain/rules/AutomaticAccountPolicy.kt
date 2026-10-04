package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.model.AccountType

/** A resolved own asset does not need a phone number to exist in the ledger.
 * A missing identifier only prevents identifier-based matching, not accounting.
 * Provider recognition or repetition alone is never evidence of ownership.
 */
object AutomaticAccountPolicy {
    fun canResolve(reference: FinancialAccountReference, explicitlyExternal: Boolean): Boolean =
        !explicitlyExternal &&
            reference.ownershipHint == AccountOwnershipHint.USER_OWNED &&
            reference.nature == FinancialAccountNature.ASSET &&
            reference.displayName.isNotBlank() &&
            reference.accountTypeHint in setOf(
                AccountType.MOBILE_WALLET, AccountType.BANK,
                AccountType.SAVINGS, AccountType.CASH
            )
}
