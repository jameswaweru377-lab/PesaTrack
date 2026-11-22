package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction

/** Repairs bank-origin "Send Money" SMS only with explicit two-account identity evidence. */
object ImportedOwnedPaymentPolicy {
    fun match(
        parsed: ParsedMpesaTransaction,
        observedSourceId: String?,
        accounts: List<MoneyAccount>,
        confirmedAccountIds: Set<String>,
        profileName: String?,
        uniqueSourceAtProvider: Boolean,
        trustedInterpretedSource: Boolean
    ): OwnAccountTransferMatch? {
        if (!trustedInterpretedSource || !uniqueSourceAtProvider ||
            parsed.type != MpesaTransactionType.SENT_MONEY ||
            parsed.counterpartyPhone.isNullOrBlank()) return null

        val source = accounts.singleOrNull {
            it.id == observedSourceId && it.enabled && it.id in confirmedAccountIds &&
                it.type in setOf(AccountType.BANK, AccountType.OTHER)
        } ?: return null
        val wallets = accounts.filter {
            it.id in confirmedAccountIds && it.enabled && it.type == AccountType.MOBILE_WALLET
        }
        val destination = wallets.singleOrNull { wallet ->
            wallet.id != source.id &&
                OwnedTransferRecipientPolicy.assess(
                    parsed.counterpartyPhone, parsed.counterparty, wallet.id, wallets, profileName
                ) == OwnedTransferRecipientPolicy.Verdict.OWNED
        } ?: return null

        return OwnAccountTransferMatch(
            source.id, source.name, destination.id, destination.name,
            OwnTransferEvidence.EXACT_IDENTIFIER
        )
    }
}
