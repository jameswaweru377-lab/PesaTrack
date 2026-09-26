package com.kedevjimmy.pesatrack.domain.parser

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import java.math.RoundingMode

object MpesaParser {
    private val codeRegex = Regex("""\b([A-Z0-9]{10})\s+Confirmed\b""", RegexOption.IGNORE_CASE)
    private val moneyRegex = Regex("""(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val feeRegex = Regex(
        """(?:Transaction\s+(?:cost|fee)|charge)\s*[,.:]?\s*(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val balanceRegex = Regex(
        """(?:New\s+M-PESA\s+balance\s+is|M-PESA\s+balance\s+is)\s*(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val sentToRegex = Regex(
        """sent\s+to\s+(.+?)(?=\s+(?:\+?254|0)\d{8,9}\b|\s+on\s+\d|\.\s*New\s+M-PESA|$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val paidToRegex = Regex(
        """paid\s+to\s+(.+?)(?=\s+for\s+account|\s+on\s+\d|\.\s*New\s+M-PESA|$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val receivedFromRegex = Regex(
        """received\s+(?:Ksh|KES)\s*[0-9,]+(?:\.\d{1,2})?\s+from\s+(.+?)(?=\s+(?:\+?254|0)\d{8,9}\b|\s+on\s+\d|\.\s*New\s+M-PESA|$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val occurredAtRegex = Regex(
        """\bon\s+(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\s+at\s+\d{1,2}:\d{2}\s*(?:AM|PM)?)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(message: String): ParsedMpesaTransaction? {
        val normalized = message.replace(Regex("""\s+"""), " ").trim()
        if (!normalized.contains("M-PESA", ignoreCase = true) &&
            !normalized.contains("Confirmed", ignoreCase = true)
        ) {
            return null
        }

        val amountMatch = moneyRegex.find(normalized) ?: return null
        val type = detectType(normalized)

        return ParsedMpesaTransaction(
            transactionCode = codeRegex.find(normalized)?.groupValues?.get(1)?.uppercase(),
            type = type,
            amountMinor = amountToMinor(amountMatch.groupValues[1]),
            counterparty = extractCounterparty(normalized, type),
            feeMinor = feeRegex.find(normalized)?.groupValues?.get(1)?.let(::amountToMinor) ?: 0L,
            balanceMinor = balanceRegex.find(normalized)?.groupValues?.get(1)?.let(::amountToMinor),
            occurredAtText = occurredAtRegex.find(normalized)?.groupValues?.get(1),
            rawMessage = message
        )
    }

    private fun detectType(message: String): MpesaTransactionType {
        val lower = message.lowercase()
        return when {
            "reversal" in lower || "reversed" in lower -> MpesaTransactionType.REVERSAL
            "withdraw" in lower -> MpesaTransactionType.WITHDRAWAL
            "received" in lower && "from" in lower -> MpesaTransactionType.RECEIVED
            "paid to" in lower && ("paybill" in lower || "account" in lower) -> MpesaTransactionType.PAY_BILL
            "paid to" in lower -> MpesaTransactionType.BUY_GOODS
            "sent to" in lower -> MpesaTransactionType.SENT_MONEY
            "airtime" in lower -> MpesaTransactionType.AIRTIME
            "deposit" in lower -> MpesaTransactionType.DEPOSIT
            else -> MpesaTransactionType.OTHER
        }
    }

    private fun extractCounterparty(message: String, type: MpesaTransactionType): String? {
        val raw = when (type) {
            MpesaTransactionType.SENT_MONEY -> sentToRegex.find(message)?.groupValues?.get(1)
            MpesaTransactionType.PAY_BILL,
            MpesaTransactionType.BUY_GOODS -> paidToRegex.find(message)?.groupValues?.get(1)
            MpesaTransactionType.RECEIVED -> receivedFromRegex.find(message)?.groupValues?.get(1)
            else -> null
        }
        return raw?.trim()?.trimEnd('.', ',')?.takeIf { it.isNotBlank() }
    }

    private fun amountToMinor(raw: String): Long =
        raw.replace(",", "")
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
}
