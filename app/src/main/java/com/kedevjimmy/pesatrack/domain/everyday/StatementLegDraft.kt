package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction

/**
 * Statement receipt codes are event references, not unique cash-leg IDs.
 * Keep the receipt in the evidence journal; a canonical statement draft must
 * use the individually checked account/row key for its fingerprint.
 *
 * The source movement remains unclassified until the user reviews it.
 */
object StatementLegDraft {
    fun create(row: StatementRow, importKey: String): ParsedMpesaTransaction {
        require(importKey.matches(Regex("[a-f0-9]{64}"))) {
            "A deterministic statement-leg identity is required."
        }
        require(row.signedMinor != 0L && row.signedMinor != Long.MIN_VALUE)
        return ParsedMpesaTransaction(
            transactionCode = null,
            type = MpesaTransactionType.OTHER,
            amountMinor = kotlin.math.abs(row.signedMinor),
            counterparty = row.description,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = "statement-entry|$importKey",
            reversalOfCode = null
        )
    }
}
