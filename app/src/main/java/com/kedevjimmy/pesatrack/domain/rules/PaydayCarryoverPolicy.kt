package com.kedevjimmy.pesatrack.domain.rules

data class PaydayCarryoverEvidence(
    val knownLiquidMinor: Long,
    val knownLiquidAccountCount: Int,
    val unknownLiquidAccountCount: Int
) {
    val complete: Boolean
        get() =
            knownLiquidAccountCount > 0 &&
                unknownLiquidAccountCount == 0
}

object PaydayCarryoverPolicy {
    fun resolve(
        cycleIndex: Int,
        payday: PaydayCarryoverEvidence?,
        previousTrackedEndingMinor: Long?
    ): Long =
        when {
            payday?.complete == true ->
                payday.knownLiquidMinor

            cycleIndex == 0 &&
                (payday?.knownLiquidAccountCount ?: 0) > 0 ->
                payday!!.knownLiquidMinor

            previousTrackedEndingMinor != null ->
                previousTrackedEndingMinor

            (payday?.knownLiquidAccountCount ?: 0) > 0 ->
                payday!!.knownLiquidMinor

            else -> 0L
        }
}
