package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType

/** Old pending rows omitted the destination phone; only a new SMS read can prove ownership. */
object LegacyPendingTransferReplayPolicy {
    fun canApply(sourceType: String, destinationType: String): Boolean =
        !(destinationType == AccountType.MOBILE_WALLET.name &&
            sourceType in setOf(AccountType.BANK.name, AccountType.OTHER.name))
}
