package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityEvidencePolicy
import com.kedevjimmy.pesatrack.domain.everyday.StatementClassified
import java.util.Locale

/** Looks up a canonical Activity ID only after unique source-and-receipt corroboration. */
class StatementActivityLinkReader(context: Context) {
    private val receipts = StatementReceiptEvidenceReader(context.applicationContext)

    fun linkedTransactionId(row: StatementClassified, accountId: String): Long? {
        if (accountId.isBlank()) return null
        val matches = receipts.read(listOf(row.line))[
            row.line.reference.uppercase(Locale.ROOT)
        ].orEmpty()
        return StatementActivityEvidencePolicy.verified(row.line, accountId, matches)
            ?.transactionId
    }
}
