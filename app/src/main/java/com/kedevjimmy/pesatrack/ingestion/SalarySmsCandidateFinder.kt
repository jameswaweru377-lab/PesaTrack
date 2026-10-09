package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import android.provider.Telephony
import com.kedevjimmy.pesatrack.domain.model.KenyanBank
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
        if (!com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore.canRead(context)) return emptyList()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val senderHints = bank.senderHints.map(::normalizeSender)
        val candidates = mutableListOf<SalarySmsCandidate>()

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            Telephony.Sms.DATE + " >= ?",
            arrayOf(sinceEpochMs.toString()),
            Telephony.Sms.DATE + " DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext() && candidates.size < limit) {
                if (!com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore.hasConsent(context)) break
                val address = cursor.getString(addressIndex)?.trim().orEmpty()
                val normalizedAddress = normalizeSender(address)
                if (normalizedAddress.isBlank()) continue
                if (senderHints.none { hint ->
                        normalizedAddress.contains(hint) || hint.contains(normalizedAddress)
                    }
                ) {
                    continue
                }

                val body =
                    cursor.getString(bodyIndex)
                        ?.trim()
                        .orEmpty()
                val receipt =
                    FinancialMessageInterpreter
                        .interpret(
                            title = address,
                            body = body
                        )
                        .events
                        .filterIsInstance<ReceiptEventFact>()
                        .singleOrNull()
                        ?: continue

                candidates += SalarySmsCandidate(
                    smsId = cursor.getLong(idIndex),
                    address = address,
                    receivedAtEpochMs =
                        cursor.getLong(dateIndex),
                    amountMinor = receipt.amountMinor,
                    originator = receipt.counterparty,
                    rawBody = body
                )
            }
        }

        return candidates
    }

    private fun normalizeSender(value: String): String =
        value.uppercase()
            .replace(Regex("""[^A-Z0-9]+"""), "")
            .trim()
}
