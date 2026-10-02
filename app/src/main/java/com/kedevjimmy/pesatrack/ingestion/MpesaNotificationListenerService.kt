package com.kedevjimmy.pesatrack.ingestion

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.ingestion.MpesaMessageDetector
import com.kedevjimmy.pesatrack.domain.rules.FinancialMessageEvidence
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.domain.rules.RuleBackedFinancialResolver
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.rules.SmartRuleEngine
import com.kedevjimmy.pesatrack.domain.rules.TransactionEvidenceResolver
import java.util.ArrayDeque
import java.util.concurrent.Executors

class MpesaNotificationListenerService :
    NotificationListenerService() {
    private val executor =
        Executors.newSingleThreadExecutor()
    private val recentEvidence =
        ArrayDeque<FinancialMessageEvidence>()
    private val notificationDeduper =
        RecentFinancialNotificationDeduper()

    override fun onNotificationPosted(
        sbn: StatusBarNotification?
    ) {
        val posted =
            sbn ?: return
        val notification =
            posted.notification
                ?: return

        if (
            notification.flags and
                Notification.FLAG_GROUP_SUMMARY !=
            0
        ) {
            return
        }

        val packageName =
            posted.packageName
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
                ?: return

        if (
            packageName ==
            applicationContext.packageName
        ) {
            return
        }

        val notificationTitle =
            notification.extras
                .getCharSequence(
                    Notification.EXTRA_TITLE
                )
                ?.toString()
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
        val body =
            extractBody(notification)
                ?: return
        val receivedAt =
            posted.postTime
                .takeIf {
                    it > 0L
                }
                ?: System.currentTimeMillis()
        val appLabel =
            applicationLabel(
                packageName
            )
        val interpretationTitle =
            FinancialNotificationCapturePolicy
                .interpretationTitle(
                    notificationTitle =
                        notificationTitle,
                    appLabel = appLabel
                )
        val screening =
            FinancialNotificationCapturePolicy
                .assess(
                    title =
                        interpretationTitle,
                    body = body,
                    sourcePackage =
                        packageName,
                    selfPackage =
                        applicationContext
                            .packageName
                )
        val likelyMpesa =
            MpesaMessageDetector
                .isLikelyMpesa(
                    interpretationTitle,
                    body
                )

        executor.execute {
            val rules =
                RuleStore.get(
                    applicationContext
                ).listRules(
                    enabledOnly = true
                )
            val accounts =
                AccountStore.get(
                    applicationContext
                ).listAccounts()
            val configuredRule =
                configuredRule(
                    originalTitle =
                        notificationTitle,
                    interpretationTitle =
                        interpretationTitle,
                    body = body,
                    accounts = accounts,
                    rules = rules
                )

            if (
                !screening.shouldIngest &&
                !likelyMpesa &&
                configuredRule == null
            ) {
                return@execute
            }

            if (
                !notificationDeduper
                    .shouldProcess(
                        notificationKey =
                            posted.key,
                        sourcePackage =
                            packageName,
                        title =
                            interpretationTitle,
                        body = body,
                        observedAtEpochMs =
                            receivedAt
                    )
            ) {
                return@execute
            }

            LedgerUpdateBus
                .processingStarted()
            var changed =
                screening.shouldIngest

            try {
                val store =
                    TransactionStore.get(
                        applicationContext
                    )
                val interpreted =
                    FinancialMessageIngestionCoordinator
                        .get(
                            applicationContext
                        )
                        .ingest(
                            title =
                                interpretationTitle,
                            body = body,
                            sourcePackage =
                                packageName,
                            sourceKind =
                                SOURCE_APP_NOTIFICATION,
                            receivedAtEpochMs =
                                receivedAt
                        )

                changed =
                    interpreted.changed ||
                        changed

                if (
                    interpreted
                        .consumesLegacyTransactionParsing
                ) {
                    return@execute
                }

                if (likelyMpesa) {
                    // The generic interpreter is authoritative
                    // for M-PESA transaction meaning. If the
                    // wording is still unknown, preserve any
                    // already-recorded balance/diagnostic facts
                    // but do not invent a legacy transaction.
                    return@execute
                }

                val evidenceCandidate =
                    TransactionEvidenceResolver
                        .parseEvidence(
                            title =
                                interpretationTitle,
                            body = body,
                            observedAtEpochMs =
                                receivedAt,
                            sourceFallback =
                                appLabel
                                    ?: packageName
                        )
                if (
                    evidenceCandidate != null
                ) {
                    rememberEvidence(
                        evidenceCandidate
                    )
                    changed =
                        store
                            .attachEvidenceToNearbyUnknown(
                                evidence =
                                    evidenceCandidate,
                                rules = rules
                            ) ||
                            changed
                }

                if (
                    configuredRule ==
                    null
                ) {
                    return@execute
                }

                val parsed =
                    RuleBackedFinancialResolver
                        .resolve(
                            title =
                                interpretationTitle,
                            body = body,
                            rule =
                                configuredRule
                        )
                        ?: return@execute

                store.insertIfNew(
                    parsed = parsed,
                    sourcePackage =
                        packageName,
                    sourceKind =
                        SOURCE_CONFIGURED_NOTIFICATION,
                    receivedAtEpochMs =
                        receivedAt,
                    occurredAtEpochMs =
                        receivedAt,
                    matchedRule =
                        configuredRule
                )
                changed = true
            } finally {
                LedgerUpdateBus
                    .processingFinished(
                        changed
                    )
            }
        }
    }

    private fun configuredRule(
        originalTitle: String?,
        interpretationTitle: String?,
        body: String,
        accounts:
            List<com.kedevjimmy.pesatrack.domain.model.MoneyAccount>,
        rules: List<SmartRule>
    ): SmartRule? {
        val titleCandidates =
            listOfNotNull(
                originalTitle,
                interpretationTitle
            ).distinct()

        titleCandidates.forEach {
            title ->
            OwnAccountTransferDetector
                .matchConfiguredNotification(
                    title = title,
                    body = body,
                    accounts =
                        accounts
                )
                ?.asSmartRule()
                ?.let {
                    return it
                }

            SmartRuleEngine
                .matchNotification(
                    title = title,
                    body = body,
                    rules = rules
                )
                ?.let {
                    return it
                }
        }

        return null
    }

    private fun rememberEvidence(
        evidence:
            FinancialMessageEvidence
    ) {
        recentEvidence.addLast(
            evidence
        )
        pruneEvidence(
            evidence.observedAtEpochMs
        )
        while (
            recentEvidence.size >
            24
        ) {
            recentEvidence
                .removeFirst()
        }
    }

    private fun pruneEvidence(
        nowEpochMs: Long
    ) {
        val cutoff =
            nowEpochMs -
                TransactionEvidenceResolver
                    .MAX_WINDOW_MS
        while (
            recentEvidence
                .firstOrNull()
                ?.observedAtEpochMs
                ?.let {
                    it < cutoff
                } == true
        ) {
            recentEvidence
                .removeFirst()
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun extractBody(
        notification: Notification
    ): String? {
        val extras =
            notification.extras

        val bigText =
            extras.getCharSequence(
                Notification.EXTRA_BIG_TEXT
            )
                ?.toString()
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
        if (bigText != null) {
            return bigText
        }

        val textLines =
            extras.getCharSequenceArray(
                Notification.EXTRA_TEXT_LINES
            )
                ?.map {
                    it.toString()
                        .trim()
                }
                ?.filter(
                    String::isNotBlank
                )
                ?.joinToString(" ")
                ?.takeIf(
                    String::isNotBlank
                )
        if (textLines != null) {
            return textLines
        }

        val text =
            extras.getCharSequence(
                Notification.EXTRA_TEXT
            )
                ?.toString()
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
        if (text != null) {
            return text
        }

        return extras
            .getCharSequence(
                Notification
                    .EXTRA_SUMMARY_TEXT
            )
            ?.toString()
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
    }

    private fun applicationLabel(
        packageName: String
    ): String? =
        runCatching {
            val info =
                packageManager
                    .getApplicationInfo(
                        packageName,
                        PackageManager
                            .GET_META_DATA
                    )
            packageManager
                .getApplicationLabel(
                    info
                )
                .toString()
                .trim()
                .takeIf(
                    String::isNotBlank
                )
        }.getOrNull()

    companion object {
        const val SOURCE_MPESA_NOTIFICATION =
            "mpesa_notification"
        const val SOURCE_CONFIGURED_NOTIFICATION =
            "configured_notification"
        const val SOURCE_FINANCIAL_NOTIFICATION =
            "financial_notification"
        const val SOURCE_APP_NOTIFICATION =
            "financial_app_notification"
    }
}
