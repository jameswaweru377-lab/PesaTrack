package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.identity.SelfNameEvidenceLevel
import com.kedevjimmy.pesatrack.domain.identity.SelfNameEvidencePolicy
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount

/** Full recipient identity is required before posting an owned bank-to-wallet transfer. */
object OwnedTransferRecipientPolicy {
    enum class Verdict { OWNED, EXTERNAL, UNCERTAIN }

    fun assess(
        recipientPhone: String?,
        recipientName: String?,
        destinationAccountId: String,
        confirmedWallets: List<MoneyAccount>,
        profileName: String?
    ): Verdict {
        val wallet = confirmedWallets.singleOrNull { account ->
            account.id == destinationAccountId && account.enabled &&
                account.type == AccountType.MOBILE_WALLET
        } ?: return Verdict.UNCERTAIN
        val registeredPhone = wallet.identifier?.let(OwnAccountTransferDetector::normalizeKenyanPhone)
            ?: return Verdict.UNCERTAIN
        val observedPhone = recipientPhone?.let(OwnAccountTransferDetector::normalizeKenyanPhone)
            ?: return Verdict.UNCERTAIN

        val matchingWallets = confirmedWallets.filter { account ->
            account.enabled && account.type == AccountType.MOBILE_WALLET &&
                account.identifier?.let(OwnAccountTransferDetector::normalizeKenyanPhone) == observedPhone
        }
        if (matchingWallets.isEmpty()) return Verdict.EXTERNAL
        if (matchingWallets.size != 1 || matchingWallets.single().id != wallet.id ||
            registeredPhone != observedPhone) return Verdict.UNCERTAIN

        if (!recipientName.isNullOrBlank() && !profileName.isNullOrBlank() &&
            SelfNameEvidencePolicy.compare(profileName, recipientName) ==
            SelfNameEvidenceLevel.NO_MATCH) return Verdict.UNCERTAIN
        return Verdict.OWNED
    }
}
