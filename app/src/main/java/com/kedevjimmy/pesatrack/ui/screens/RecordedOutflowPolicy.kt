package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.model.CheckedMoney

/** Activity spentMinor is already net of trusted linked refunds; add fees only once. */
internal object RecordedOutflowPolicy {
    fun total(netSpentMinor: Long, feesMinor: Long): Long =
        maxOf(0L, CheckedMoney.add(netSpentMinor, feesMinor))

    fun available(netSpentMinor: Long, feesMinor: Long): Long? =
        CheckedMoney.orNull { total(netSpentMinor, feesMinor) }
}
