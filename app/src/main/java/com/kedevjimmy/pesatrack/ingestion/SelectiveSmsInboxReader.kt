package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import com.kedevjimmy.pesatrack.data.local.FinancialSmsSenderApprovalStore
import com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore
import com.kedevjimmy.pesatrack.domain.ingestion.FinancialSmsSenderPolicy

/** A single SMS row; `body` is null for an unapproved sender. */
data class SelectiveSmsRow(
    val id: Long,
    val sender: String?,
    val receivedAtEpochMs: Long,
    val body: String?
)

/**
 * Keeps unrelated SMS bodies out of PesaTrack. First query only IDs/senders/dates,
 * then fetches bodies for approved IDs in bounded batches. Nothing is copied to
 * disk by this reader; the caller owns financial interpretation and checkpoints.
 */
class SelectiveSmsInboxReader(private val context: Context) {
    private data class Header(val id: Long, val sender: String?, val receivedAt: Long)

    fun scan(
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        uri: Uri = Telephony.Sms.Inbox.CONTENT_URI,
        acceptSender: ((String?) -> Boolean)? = null,
        isCancelled: () -> Boolean = { false },
        onRow: (row: SelectiveSmsRow, total: Int) -> Boolean
    ): Boolean {
        SmsAccessConsentStore.requireRead(context)
        val cursor = context.contentResolver.query(
            uri, METADATA_PROJECTION, selection, selectionArgs, sortOrder
        ) ?: error("SMS history is temporarily unavailable.")

        cursor.use { headersCursor ->
            val idColumn = headersCursor.getColumnIndexOrThrow(BaseColumns._ID)
            val senderColumn = headersCursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateColumn = headersCursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val total = headersCursor.count.coerceAtLeast(0)
            val batch = ArrayList<Header>(HEADER_BATCH_SIZE)

            val approvals = FinancialSmsSenderApprovalStore(context)

            fun flush(): Boolean {
                if (isCancelled()) return false
                val bodyById = approvals.withApprovedSenders { extraSenders ->
                    val trusted = batch.filter { header ->
                        if (acceptSender == null) {
                            FinancialSmsSenderPolicy.isTrusted(header.sender, extraSenders)
                        } else {
                            acceptSender(header.sender)
                        }
                    }
                    if (isCancelled()) emptyMap() else readTrustedBodies(uri, trusted)
                }
                for (header in batch) {
                    if (isCancelled()) return false
                    SmsAccessConsentStore.requireConsent(context)
                    if (!onRow(
                            SelectiveSmsRow(
                                id = header.id,
                                sender = header.sender,
                                receivedAtEpochMs = header.receivedAt,
                                body = bodyById[header.id]
                            ), total
                        )
                    ) return false
                }
                batch.clear()
                return true
            }

            while (headersCursor.moveToNext()) {
                if (isCancelled()) return false
                batch += Header(
                    id = headersCursor.getLong(idColumn),
                    sender = headersCursor.getString(senderColumn),
                    receivedAt = headersCursor.getLong(dateColumn)
                )
                if (batch.size == HEADER_BATCH_SIZE && !flush()) return false
            }
            return batch.isEmpty() || flush()
        }
    }

    private fun readTrustedBodies(uri: Uri, headers: List<Header>): Map<Long, String> {
        if (headers.isEmpty()) return emptyMap()
        SmsAccessConsentStore.requireRead(context)
        // Binding both metadata values in SQL prevents reading a private SMS body
        // if a provider changes/reuses the ID between the two queries.
        val exactPair = "(" + BaseColumns._ID + " = ? AND " + Telephony.Sms.ADDRESS + " = ?)"
        val selection = List(headers.size) { exactPair }.joinToString(" OR ")
        val approvedHeaders = headers.associateBy { it.id }
        val result = HashMap<Long, String>(headers.size)
        context.contentResolver.query(
            uri, BODY_PROJECTION, selection,
            headers.flatMap { listOf(it.id.toString(), it.sender.orEmpty()) }.toTypedArray(), null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val senderColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val sender = cursor.getString(senderColumn)
                // Recheck against the approved sender from the metadata query.
                val header = approvedHeaders[id]
                if (header != null && header.sender == sender) {
                    SmsAccessConsentStore.requireConsent(context)
                    cursor.getString(bodyColumn)?.let { result[id] = it }
                }
            }
        } ?: error("Approved SMS messages are temporarily unavailable.")
        return result
    }

    private companion object {
        const val HEADER_BATCH_SIZE = 128
        val METADATA_PROJECTION = arrayOf(
            BaseColumns._ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE
        )
        val BODY_PROJECTION = arrayOf(
            BaseColumns._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY
        )
    }
}
