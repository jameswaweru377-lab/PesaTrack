package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.statements.StatementAccountConsentPolicy

/** Past income retains explicit ownership after archiving; statement admission does not. */
internal class HistoricalIncomeOwnershipReader(context: Context) {
    private val app = context.applicationContext

    fun confirmedAccountIds(): Set<String> = synchronized(CanonicalStoreLock.monitor) {
        val legacy = FinancialUnderstandingStore.get(app).confirmedAccountIds()
        val consent = AccountOwnershipConsentStore(app)
        StatementAccountConsentPolicy.confirmedAccountIds(
            legacy, consent.confirmedHistoricalRevisions().keys, consent.rejectedAccountIds()
        )
    }
}
