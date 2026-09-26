package com.kedevjimmy.pesatrack.ingestion

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.MpesaMessageDetector
import com.kedevjimmy.pesatrack.domain.parser.ConfiguredMoneyParser
import com.kedevjimmy.pesatrack.domain.parser.MpesaParser
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine
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
        val packageName = sbn.packageName
        val receivedAt = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()

        executor.execute {
            val rules = RuleStore.get(applicationContext).listRules(enabledOnly = true)
            val store = TransactionStore.get(applicationContext)

            if (MpesaMessageDetector.isLikelyMpesa(title, body)) {
                val parsed = MpesaParser.parse(body) ?: return@execute
                val matchedRule = SmartRuleEngine.matchTransaction(parsed, rules)

                store.insertIfNew(
                    parsed = parsed,
                    sourcePackage = packageName,
                    sourceKind = SOURCE_MPESA_NOTIFICATION,
                    receivedAtEpochMs = receivedAt,
                    matchedRule = matchedRule
                )
                return@execute
            }

            val configuredRule = SmartRuleEngine.matchNotification(
                title = title,
                body = body,
                rules = rules
            ) ?: return@execute

            val parsed = ConfiguredMoneyParser.parse(
                title = title,
                body = body,
                rule = configuredRule
            ) ?: return@execute

            store.insertIfNew(
                parsed = parsed,
                sourcePackage = packageName,
                sourceKind = SOURCE_CONFIGURED_NOTIFICATION,
                receivedAtEpochMs = receivedAt,
                matchedRule = configuredRule
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

        return extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.map { it.toString().trim() }
            ?.filter { it.isNotBlank() }
            ?.joinToString(" ")
            ?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val SOURCE_MPESA_NOTIFICATION = "mpesa_notification"
        const val SOURCE_CONFIGURED_NOTIFICATION = "configured_notification"
    }
}
