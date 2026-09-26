package com.kedevjimmy.pesatrack.ingestion

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.MpesaMessageDetector
import com.kedevjimmy.pesatrack.domain.parser.MpesaParser
import java.util.concurrent.Executors

class MpesaNotificationListenerService : NotificationListenerService() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val title = notification.extras
            .getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            ?.trim()

        val body = extractBody(notification) ?: return
        if (!MpesaMessageDetector.isLikelyMpesa(title, body)) return

        val parsed = MpesaParser.parse(body) ?: return
        val packageName = sbn.packageName
        val receivedAt = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()

        executor.execute {
            TransactionStore.get(applicationContext).insertIfNew(
                parsed = parsed,
                sourcePackage = packageName,
                sourceKind = SOURCE_NOTIFICATION,
                receivedAtEpochMs = receivedAt
            )
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun extractBody(notification: Notification): String? {
        val extras = notification.extras

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (bigText != null) return bigText

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (text != null) return text

        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.map { it.toString().trim() }
            ?.filter { it.isNotBlank() }
            ?.joinToString(" ")
            ?.takeIf { it.isNotBlank() }

        return lines
    }

    companion object {
        const val SOURCE_NOTIFICATION = "notification"
    }
}
