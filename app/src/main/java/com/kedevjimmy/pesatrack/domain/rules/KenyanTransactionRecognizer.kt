package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.intelligence.EntityIntelligence
import com.kedevjimmy.pesatrack.domain.intelligence.EntityKind
import com.kedevjimmy.pesatrack.domain.intelligence.EntityMatchConfidence
import com.kedevjimmy.pesatrack.domain.intelligence.EntityResolution
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

data class BuiltInTransactionClassification(
    val direction: MoneyDirection,
    val category: TransactionCategory,
    val entity: EntityResolution? = null,
    val evidenceLabel: String? = null,
    val evidenceSource: String? = null,
    val evidenceConfidence: EvidenceConfidence? = null
)

object KenyanTransactionRecognizer {
    private val phoneDataTerms = listOf(
        "data bundle",
        "data bundles",
        "sms bundle",
        "sms bundles",
        "airtime",
        "tunukiwa",
        "minutes bundle",
        "voice bundle"
    )

    fun classify(
        parsed: ParsedMpesaTransaction
    ): BuiltInTransactionClassification? {
        val searchable =
            buildString {
                append(parsed.counterparty.orEmpty())
                append(' ')
                append(parsed.rawMessage)
            }
                .lowercase()
                .replace(
                    Regex("""\s+"""),
                    " "
                )
                .trim()

        val entity =
            EntityIntelligence.resolve(
                counterparty = parsed.counterparty,
                rawMessage = parsed.rawMessage
            )

        val explicitPhoneData =
            "tunukiwa" in searchable ||
                phoneDataTerms.any(
                    searchable::contains
                )

        val safaricomPhoneData =
            entity?.entity?.kind ==
                EntityKind.TELECOM &&
                (
                    parsed.type ==
                        MpesaTransactionType.AIRTIME ||
                        "bundle" in searchable ||
                        "airtime" in searchable ||
                        "data" in searchable ||
                        "sms" in searchable ||
                        "minutes" in searchable
                    )

        if (
            explicitPhoneData ||
            safaricomPhoneData
        ) {
            return classification(
                category =
                    TransactionCategory
                        .PHONE_AND_DATA,
                entity = entity
            )
        }

        val resolved = entity ?: return null
        val profile = resolved.entity
        val category =
            profile.defaultCategory
                ?: return null

        val outgoingPayment =
            parsed.type ==
                MpesaTransactionType.PAY_BILL ||
                parsed.type ==
                MpesaTransactionType.BUY_GOODS ||
                parsed.type ==
                MpesaTransactionType.SENT_MONEY

        if (
            !outgoingPayment ||
            !profile.autoReviewEligible ||
            resolved.confidence !=
                EntityMatchConfidence.HIGH
        ) {
            return null
        }

        return classification(
            category = category,
            entity = resolved
        )
    }

    private fun classification(
        category: TransactionCategory,
        entity: EntityResolution?
    ): BuiltInTransactionClassification =
        BuiltInTransactionClassification(
            direction = MoneyDirection.EXPENSE,
            category = category,
            entity = entity,
            evidenceLabel =
                entity?.entity?.canonicalName,
            evidenceSource =
                entity?.let {
                    "entity_intelligence:" +
                        it.source
                },
            evidenceConfidence =
                entity?.confidence
                    ?.let {
                        when (it) {
                            EntityMatchConfidence.HIGH ->
                                EvidenceConfidence.HIGH
                            EntityMatchConfidence.LIKELY ->
                                EvidenceConfidence.LIKELY
                        }
                    }
        )

}
