package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.AccountOwnershipConsentStore
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.interpretation.TransferEventFact
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.rules.OwnedTransferRecipientPolicy

/** Supplements the existing provider-neutral interpreter with actual account identity. */
internal class FinancialTransferRecipientRouting(context: Context) {
    private val app = context.applicationContext
    private val accounts = AccountStore.get(app)
    private val consent = AccountOwnershipConsentStore(app)
    private val understanding = FinancialUnderstandingStore.get(app)

    fun route(event: FinancialEventFact): FinancialEventFact {
        if (event !is TransferEventFact || !isBankToWallet(event)) return event
        val destinationId = understanding.accountIdFor(event.destination) ?: return event
        if (verdict(event, destinationId) != OwnedTransferRecipientPolicy.Verdict.EXTERNAL) return event
        // A completed bank transfer to a different full phone is an outgoing payment,
        // never movement between the user's accounts.
        return PaymentEventFact(
            amountMinor = event.amountMinor,
            source = event.source,
            counterparty = event.recipientName ?: "M-Pesa recipient",
            counterpartyPhone = event.recipientPhone,
            routeHint = PaymentRouteHint.PERSON_TO_PERSON,
            feeMinor = event.feeMinor,
            confidence = event.confidence,
            evidence = event.evidence
        )
    }

    fun canApply(event: TransferEventFact, sourceId: String?, destinationId: String?): Boolean {
        if (sourceId == null || destinationId == null || sourceId == destinationId ||
            !understanding.isUserOwned(event.source.entityKey) ||
            !understanding.isUserOwned(event.destination.entityKey)) return false
        if (!isBankToWallet(event)) return true
        val owned = consent.confirmedRevisions().keys
        val source = accounts.listAccounts().firstOrNull { it.id == sourceId }
        if (source?.type != AccountType.BANK || sourceId !in owned) return false
        // A provider title cannot identify which of several confirmed bank accounts sent money.
        val sameProvider = accounts.listAccounts().filter {
            it.type == AccountType.BANK && it.id in owned &&
                com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
                    .resolveAccount(it.name).providerKey == event.source.providerKey
        }
        if (sameProvider.size != 1 || sameProvider.single().id != sourceId) return false
        return verdict(event, destinationId) == OwnedTransferRecipientPolicy.Verdict.OWNED
    }

    private fun verdict(event: TransferEventFact, destinationId: String): OwnedTransferRecipientPolicy.Verdict {
        val owned = consent.confirmedRevisions().keys
        val wallets = accounts.listAccounts().filter { it.id in owned && it.type == AccountType.MOBILE_WALLET }
        val name = app.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
            .getString("profile_name", null)
        return OwnedTransferRecipientPolicy.assess(
            event.recipientPhone, event.recipientName, destinationId, wallets, name
        )
    }

    private fun isBankToWallet(event: TransferEventFact) =
        event.source.accountTypeHint == AccountType.BANK &&
            event.destination.accountTypeHint == AccountType.MOBILE_WALLET
}
