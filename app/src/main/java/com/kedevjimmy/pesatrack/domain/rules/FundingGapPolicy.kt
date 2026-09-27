package com.kedevjimmy.pesatrack.domain.rules

import kotlin.math.max

object FundingGapPolicy {
    fun unexplainedGap(
        startingFundsMinor: Long,
        additionalIncomeMinor: Long,
        spentMinor: Long,
        feesMinor: Long
    ): Long {
        val recognizedFunding = max(
            0L,
            startingFundsMinor + additionalIncomeMinor
        )
        val actualOutflow =
            max(0L, spentMinor) + max(0L, feesMinor)

        return max(0L, actualOutflow - recognizedFunding)
    }
}
