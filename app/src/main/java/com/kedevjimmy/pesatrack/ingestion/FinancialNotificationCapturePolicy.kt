package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageRelevance
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageRelevanceGate

data class FinancialNotificationScreening(
    val shouldIngest: Boolean,
    val relevance: FinancialMessageRelevance
)

object FinancialNotificationCapturePolicy {
    fun assess(
        title: String?,
        body: String,
        sourcePackage: String?,
        selfPackage: String,
        sourceApproved: Boolean = false
    ): FinancialNotificationScreening {
        if (!sourceApproved || !NotificationSourceTrustPolicy.isEligible(sourcePackage, selfPackage)) {
            return FinancialNotificationScreening(
                shouldIngest = false,
                relevance = FinancialMessageRelevance(false, 0, emptySet())
            )
        }
        val relevanceBody =
            listOfNotNull(
                title
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    ),
                body.trim()
            ).joinToString(" ")

        val normalized =
            FinancialMessageNormalizer
                .normalize(
                    title = title,
                    body = relevanceBody
                )
        val relevance =
            FinancialMessageRelevanceGate
                .assess(normalized)

        val fromSelf =
            sourcePackage
                ?.trim()
                ?.equals(
                    selfPackage,
                    ignoreCase = false
                ) == true

        return FinancialNotificationScreening(
            shouldIngest =
                !fromSelf &&
                    normalized.body
                        .isNotBlank() &&
                    relevance.isRelevant,
            relevance = relevance
        )
    }

    fun interpretationTitle(
        notificationTitle: String?,
        appLabel: String?
    ): String? {
        val title =
            notificationTitle
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
        val label =
            appLabel
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )

        if (title == null) {
            return label
        }

        if (
            label != null &&
            GENERIC_NOTIFICATION_TITLE
                .containsMatchIn(title)
        ) {
            return label
        }

        return title
    }

    private val GENERIC_NOTIFICATION_TITLE =
        Regex(
            """^(?:transaction|transaction alert|payment|payment alert|account alert|notification|banking alert)$""",
            RegexOption.IGNORE_CASE
        )
}
