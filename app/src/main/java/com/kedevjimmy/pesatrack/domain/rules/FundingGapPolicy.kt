package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.max
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney

object FundingGapPolicy {
    fun unexplainedGap(
        startingFundsMinor: Long,
        additionalIncomeMinor: Long,
        spentMinor: Long,
        feesMinor: Long
    ): Long {
        val recognizedFunding = max(
            0L,
            CheckedMoney.add(startingFundsMinor, additionalIncomeMinor)
        )
        val actualOutflow =
            CheckedMoney.add(max(0L, spentMinor), max(0L, feesMinor))

        return max(0L, CheckedMoney.subtract(actualOutflow, recognizedFunding))
    }
}
