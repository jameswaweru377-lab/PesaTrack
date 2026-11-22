package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.rules.ImportedOwnedPaymentPolicy
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferMatch

/** Keep source-account context when the generic interpreter persists a legacy SMS shape. */
internal class ImportedOwnTransferRouting(context: Context) {
    private val app = context.applicationContext
    private val consents = AccountOwnershipConsentStore(app)

    fun match(
        parsed: ParsedMpesaTransaction,
        sourceKind: String,
        observedSourceId: String?,
        accounts: List<MoneyAccount>
    ): OwnAccountTransferMatch? {
        if (observedSourceId == null || observedSourceId == AccountStore.DEFAULT_MPESA_ID) {
            return OwnAccountTransferDetector.matchMpesa(
                parsed, accounts, AccountStore.DEFAULT_MPESA_ID
            )
        }
        if (!sourceKind.startsWith("interpreted_") ||
            parsed.type != MpesaTransactionType.SENT_MONEY ||
            parsed.counterpartyPhone.isNullOrBlank()) return null
        val confirmed = consents.confirmedRevisions().keys
        val source = accounts.firstOrNull { it.id == observedSourceId } ?: return null
        val providerKey = FinancialProviderRegistry.resolveAccount(source.name).providerKey
        val sameProvider = accounts.filter { account ->
            account.enabled && account.id in confirmed &&
                (if (providerKey != null) {
                    FinancialProviderRegistry.resolveAccount(account.name).providerKey == providerKey
                } else {
                    account.name.equals(source.name, ignoreCase = true)
                })
        }
        val name = app.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
            .getString("profile_name", null)
        return ImportedOwnedPaymentPolicy.match(
            parsed = parsed, observedSourceId = observedSourceId, accounts = accounts,
            confirmedAccountIds = confirmed, profileName = name,
            uniqueSourceAtProvider = sameProvider.size == 1 && sameProvider.single().id == source.id,
            trustedInterpretedSource = true
        )
    }
}
