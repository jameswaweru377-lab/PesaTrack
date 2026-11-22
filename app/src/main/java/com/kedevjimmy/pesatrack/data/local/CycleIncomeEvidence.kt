package com.kedevjimmy.pesatrack.data.local

import android.content.Context

/** Cached per rebuild, so closed cycles use one consistent evidence snapshot. */
internal data class CycleIncomeEvidence(
    val ownedAccountIds: Set<String>,
    val receivableShares: Map<Long, ReceivableShare>
) {
    companion object {
        fun read(context: Context) = CycleIncomeEvidence(
            HistoricalIncomeOwnershipReader(context).confirmedAccountIds(),
            PersonalReceivableStore(context).linkedShares()
        )
    }
}
