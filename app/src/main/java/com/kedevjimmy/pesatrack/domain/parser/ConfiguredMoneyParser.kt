package com.kedevjimmy.pesatrack.domain.parser

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import java.math.RoundingMode

object ConfiguredMoneyParser {
    private val moneyRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        title: String?,
        body: String,
        rule: SmartRule
    ): ParsedMpesaTransaction? {
        val amount = moneyRegex.find(body)?.groupValues?.get(1) ?: return null
        val label = rule.displayLabel
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: rule.name.trim().takeIf { it.isNotBlank() }
            ?: title?.trim()?.takeIf { it.isNotBlank() }

        return ParsedMpesaTransaction(
            transactionCode = null,
            type = MpesaTransactionType.OTHER,
            amountMinor = amountToMinor(amount),
            counterparty = label,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = body
        )
    }

    private fun amountToMinor(raw: String): Long =
        raw.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
}
