package com.kedevjimmy.pesatrack.ingestion

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.DebtStore
import com.kedevjimmy.pesatrack.data.local.FulizaStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.MpesaMessageDetector
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionDateResolver
import com.kedevjimmy.pesatrack.domain.parser.ConfiguredMoneyParser
import com.kedevjimmy.pesatrack.domain.parser.FulizaParser
import com.kedevjimmy.pesatrack.domain.parser.MpesaParser
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine
import com.kedevjimmy.pesatrack.domain.rules.FinancialMessageEvidence
import com.kedevjimmy.pesatrack.domain.rules.TransactionEvidenceResolver
import java.util.ArrayDeque
import java.util.concurrent.Executors

class MpesaNotificationListenerService : NotificationListenerService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val recentEvidence = ArrayDeque<FinancialMessageEvidence>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val title = notification.extras
            .getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            ?.trim()

        val body = extractBody(notification) ?: return
        val packageName = sbn.packageName
        val receivedAt = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()

        val likelyMpesa = MpesaMessageDetector.isLikelyMpesa(title, body)
        if (likelyMpesa) {
            LedgerUpdateBus.processingStarted()
        }

        executor.execute {
            var processingStarted = likelyMpesa
            var changed = false

            try {
                val rules = RuleStore.get(applicationContext).listRules(enabledOnly = true)
                val store = TransactionStore.get(applicationContext)

                if (likelyMpesa) {
                val fuliza = FulizaParser.parse(body)
                if (fuliza != null) {
                    val result = FulizaStore.get(applicationContext).insertOrUpdate(
                        parsed = fuliza,
                        sourcePackage = packageName,
                        sourceKind = SOURCE_FULIZA_NOTIFICATION,
                        occurredAtEpochMs = receivedAt
                    )
                    if (
                        result.linkedTransactionId != null ||
                        result.repairedLegacyTransaction
                    ) {
                        PayCycleStore.get(applicationContext).rebuild()
                    }
                    changed = true
                    return@execute
                }

                val fulizaRepayment =
                    FulizaParser.parseRepayment(body)
                if (fulizaRepayment != null) {
                    DebtStore.get(applicationContext)
                        .recordFulizaRepayment(
                            parsed = fulizaRepayment,
                            sourcePackage = packageName,
                            sourceKind = SOURCE_FULIZA_NOTIFICATION,
                            occurredAtEpochMs = receivedAt
                        )
                    PayCycleStore.get(applicationContext).rebuild()
                    changed = true
                    return@execute
                }

                val parsed = MpesaParser.parse(body) ?: return@execute
                pruneEvidence(receivedAt)
                val inlineEvidence = TransactionEvidenceResolver.parseEvidence(
                    title = title,
                    body = body,
                    observedAtEpochMs = receivedAt,
                    sourceFallback = packageName
                )
                val evidence = if (parsed.counterparty.isNullOrBlank()) {
                    TransactionEvidenceResolver.chooseBest(
                        parsed = parsed,
                        transactionObservedAtEpochMs = receivedAt,
                        evidence = buildList {
                            inlineEvidence?.let(::add)
                            addAll(recentEvidence)
                        }
                    )
                } else {
                    null
                }
                val enriched = TransactionEvidenceResolver.enrich(parsed, evidence)
                val matchedRule = SmartRuleEngine.matchTransaction(enriched, rules)

                store.insertIfNew(
                    parsed = enriched,
                    sourcePackage = packageName,
                    sourceKind = SOURCE_MPESA_NOTIFICATION,
                    receivedAtEpochMs = receivedAt,
                    occurredAtEpochMs = TransactionDateResolver.resolve(
                        occurredAtText = parsed.occurredAtText,
                        fallbackEpochMs = receivedAt
                    ),
                    matchedRule = matchedRule,
                    evidence = evidence
                )
                changed = true
                return@execute
            }

            val evidenceCandidate = TransactionEvidenceResolver.parseEvidence(
                title = title,
                body = body,
                observedAtEpochMs = receivedAt,
                sourceFallback = packageName
            )
            if (evidenceCandidate != null) {
                if (!processingStarted) {
                    LedgerUpdateBus.processingStarted()
                    processingStarted = true
                }
                rememberEvidence(evidenceCandidate)
                changed = store.attachEvidenceToNearbyUnknown(
                    evidence = evidenceCandidate,
                    rules = rules
                ) || changed
            }

            val accounts = AccountStore.get(applicationContext).listAccounts()
            val configuredRule =
                OwnAccountTransferDetector.matchConfiguredNotification(
                    title = title,
                    body = body,
                    accounts = accounts
                )?.asSmartRule()
                    ?: SmartRuleEngine.matchNotification(
                        title = title,
                        body = body,
                        rules = rules
                    )

            if (configuredRule == null) {
                return@execute
            }

            if (!processingStarted) {
                LedgerUpdateBus.processingStarted()
                processingStarted = true
            }

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
                occurredAtEpochMs = receivedAt,
                matchedRule = configuredRule
            )
            changed = true
        } finally {
            if (processingStarted) {
                LedgerUpdateBus.processingFinished(changed)
            }
        }
    }
    }

    private fun rememberEvidence(evidence: FinancialMessageEvidence) {
        recentEvidence.addLast(evidence)
        pruneEvidence(evidence.observedAtEpochMs)
        while (recentEvidence.size > 24) {
            recentEvidence.removeFirst()
        }
    }

    private fun pruneEvidence(nowEpochMs: Long) {
        val cutoff = nowEpochMs - TransactionEvidenceResolver.MAX_WINDOW_MS
        while (
            recentEvidence.isNotEmpty() &&
            recentEvidence.peekFirst().observedAtEpochMs < cutoff
        ) {
            recentEvidence.removeFirst()
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
        const val SOURCE_FULIZA_NOTIFICATION = "fuliza_notification"
        const val SOURCE_CONFIGURED_NOTIFICATION = "configured_notification"
    }
}
