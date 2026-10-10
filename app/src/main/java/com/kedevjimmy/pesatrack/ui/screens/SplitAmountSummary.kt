package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.everyday.EverydayMoneyPolicy
import com.kedevjimmy.pesatrack.domain.everyday.MoneyPart
import java.math.BigInteger

internal data class SplitAmountSummary(
    val allocatedMinor: BigInteger,
    val remainingMinor: BigInteger,
    val values: List<MoneyPart>,
    val canSave: Boolean
) {
    companion object {
        fun from(totalMinor: Long, inputs: List<Pair<String, String>>): SplitAmountSummary {
            val values = inputs.mapNotNull { (category, text) ->
                EverydayMoneyPolicy.money(text)?.let { MoneyPart(category, it) }
            }
            val allocated = values.fold(BigInteger.ZERO) { sum, part -> sum + BigInteger.valueOf(part.minor) }
            return SplitAmountSummary(allocated, BigInteger.valueOf(totalMinor) - allocated, values,
                values.size == inputs.size && EverydayMoneyPolicy.validSplit(totalMinor, values))
        }
    }
}
