package com.kedevjimmy.pesatrack.ingestion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.interpretation.*

data class TransactionMessageEvidenceResult(
    val message: TransactionMessageEvidence? = null,
    val unavailableReason: String? = null
)

/** Reads only on demand. Original messages are never copied into the ledger. */
class TransactionMessageEvidenceReader(private val context: Context) {
    fun read(transaction: StoredTransaction): TransactionMessageEvidenceResult {
        val code = transaction.transactionCode?.trim()?.takeIf { it.isNotEmpty() }
            ?: return TransactionMessageEvidenceResult(unavailableReason = "No message reference is recorded for this transaction.")
        if (!com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore.canRead(context)) {
            return TransactionMessageEvidenceResult(unavailableReason = "SMS access is off. Open More → Import history, accept the disclosure and allow Android SMS access to view the original message.")
        }
        return try {
            val candidates = mutableListOf<TransactionMessageEvidence>()
            val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
            // Escape LIKE wildcards even though provider references are normally alphanumeric.
            val escaped = code.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI, projection,
                "${Telephony.Sms.BODY} LIKE ? ESCAPE '\\'", arrayOf("%$escaped%"),
                "${Telephony.Sms.DATE} DESC"
            ) ?: return TransactionMessageEvidenceResult(unavailableReason = "Messages are temporarily unavailable. Try refreshing.")
            cursor.use {
                val senderIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (it.moveToNext()) {
                    com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore.requireConsent(context)
                    val sender = it.getString(senderIndex).orEmpty()
                    val body = it.getString(bodyIndex).orEmpty()
                    val interpretation = FinancialMessageInterpreter.interpret(title = sender, body = body)
                    val amounts = interpretation.events.map { event ->
                        when (event) {
                            is PaymentEventFact -> event.amountMinor
                            is ReceiptEventFact -> event.amountMinor
                            is TransferEventFact -> event.amountMinor
                            is CashWithdrawalEventFact -> event.amountMinor
                            is CashDepositEventFact -> event.amountMinor
                            is UnresolvedFinancialEventFact -> event.amountMinor
                            is FeeEventFact -> event.amountMinor
                            is ReversalEventFact -> event.amountMinor
                            is DebtDrawdownEventFact -> event.amountMinor
                            is DebtRepaymentEventFact -> event.amountMinor
                        }
                    }
                    candidates += TransactionMessageEvidence(sender, body, it.getLong(dateIndex), interpretation.transactionCode, amounts)
                }
            }
            val match = matchingTransactionMessage(code, transaction.amountMinor, candidates)
            TransactionMessageEvidenceResult(match, if (match == null) "The original message could not be uniquely matched on this device. It may have been deleted or received as a notification." else null)
        } catch (_: SecurityException) {
            TransactionMessageEvidenceResult(unavailableReason = "SMS access is unavailable. Check message permissions in Connect messages.")
        } catch (_: Exception) {
            TransactionMessageEvidenceResult(unavailableReason = "Messages are temporarily unavailable. Try refreshing.")
        }
    }
}
