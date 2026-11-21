package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import android.provider.Telephony
import com.kedevjimmy.pesatrack.domain.model.KenyanBank
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.ReceiptEventFact

data class SalarySmsCandidate(
    val smsId: Long,
    val address: String,
    val receivedAtEpochMs: Long,
    val amountMinor: Long,
    val originator: String?,
    val rawBody: String
)

class SalarySmsCandidateFinder(
    private val context: Context
) {
    fun find(
        bank: KenyanBank,
        sinceEpochMs: Long,
        limit: Int = 12
    ): List<SalarySmsCandidate> {
        if (limit <= 0) return emptyList()
        if (!com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore.canRead(context)) return emptyList()
        val candidates = mutableListOf<SalarySmsCandidate>()
        SelectiveSmsInboxReader(context).scan(
            selection = Telephony.Sms.DATE + " >= ?",
            selectionArgs = arrayOf(sinceEpochMs.toString()),
            sortOrder = Telephony.Sms.DATE + " DESC",
            acceptSender = { sender -> KenyanBankDirectory.matchSender(sender)?.id == bank.id }
        ) { row, _ ->
                val address = row.sender?.trim().orEmpty()
                val body = row.body?.trim().orEmpty()
                if (body.isBlank()) return@scan true
                val receipt =
                    FinancialMessageInterpreter
                        .interpret(
                            title = address,
                            body = body
                        )
                        .events
                        .filterIsInstance<ReceiptEventFact>()
                        .singleOrNull()
                        ?: return@scan true

                candidates += SalarySmsCandidate(
                    smsId = row.id,
                    address = address,
                    receivedAtEpochMs = row.receivedAtEpochMs,
                    amountMinor = receipt.amountMinor,
                    originator = receipt.counterparty,
                    rawBody = body
                )
                candidates.size < limit
        }

        return candidates
    }
}
