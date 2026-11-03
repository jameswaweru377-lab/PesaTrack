package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.statements.StatementAccountConsentPolicy

/** Shared, read-only ownership decision for admitted bank/wallet Statement evidence. */
internal class StatementAccountConsentReader(context: Context) {
    private val app = context.applicationContext

    fun confirmedAccountIds(): Set<String> = synchronized(CanonicalStoreLock.monitor) {
        val legacy = FinancialUnderstandingStore.get(app).confirmedAccountIds()
        val consent = AccountOwnershipConsentStore(app)
        StatementAccountConsentPolicy.confirmedAccountIds(
            legacy, consent.confirmedRevisions().keys, consent.rejectedAccountIds()
        )
    }
}
