package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.CashDepositEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.CashWithdrawalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageNormalizer
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageRelevanceGate
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialRelevanceSignal
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.ReceiptEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.ReversalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.TransferEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.UnresolvedFinancialEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.UnsupportedFinancialCandidatePolicy
import com.kedevjimmy.pesatrack.domain.messagepack.MessageLabRedactor
import com.kedevjimmy.pesatrack.domain.messagepack.MessagePackFieldValue
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute
import java.util.Locale

data class FinancialMessageInspection(
    val recognized: Boolean,
    val semanticallyUnderstood: Boolean,
    val relevanceScore: Int,
    val relevanceSignals:
        Set<FinancialRelevanceSignal>,
    val parseRoute: MessageParseRoute,
    val templateId: String?,
    val templateRevision: Int?,
    val provenanceReason: String,
    val transactionCode: String?,
    val occurredAtText: String?,
    val eventSummaries: List<String>,
    val balanceSummaries: List<String>,
    val semanticIntents: List<String>,
    val extractedFieldNames: List<String>,
    val heuristicUnsupported: Boolean,
    val wouldEnterMessageLab: Boolean,
    val savedUserDecision:
        MessageLabUserDecision?,
    val savedSemanticLabel:
        MessageLabSemanticLabel?,
    val redactedBody: String
) {
    fun sanitizedExport(): String {
        val entries =
            mutableListOf<
                Pair<String, String>
            >()

        entries +=
            "sender" to
                jsonString("<SENDER>")
        entries +=
            "body" to
                jsonString(
                    redactedBody
                )
        entries +=
            "recognized" to
                recognized.toString()
        entries +=
            "semanticallyUnderstood" to
                semanticallyUnderstood
                    .toString()
        entries +=
            "relevanceScore" to
                relevanceScore
                    .toString()
        entries +=
            "relevanceSignals" to
                jsonArray(
                    relevanceSignals
                        .map { it.name }
                        .sorted()
                )
        entries +=
            "parseRoute" to
                jsonString(
                    parseRoute.name
                )
        templateId?.let {
            entries +=
                "templateId" to
                    jsonString(it)
        }
        templateRevision?.let {
            entries +=
                "templateRevision" to
                    it.toString()
        }
        entries +=
            "eventKinds" to
                jsonArray(
                    eventSummaries.map {
                        it.substringBefore(
                            " • "
                        )
                    }
                )
        entries +=
            "semanticIntents" to
                jsonArray(
                    semanticIntents
                )
        entries +=
            "extractedFieldNames" to
                jsonArray(
                    extractedFieldNames
                )
        entries +=
            "heuristicUnsupported" to
                heuristicUnsupported
                    .toString()
        entries +=
            "wouldEnterMessageLab" to
                wouldEnterMessageLab
                    .toString()
        savedUserDecision?.let {
            entries +=
                "savedUserDecision" to
                    jsonString(
                        it.name
                    )
        }
        savedSemanticLabel?.let {
            entries +=
                "savedSemanticLabel" to
                    jsonString(
                        it.name
                    )
            entries +=
                "savedSemanticLabelDisplay" to
                    jsonString(
                        it.displayLabel
                    )
        }
        entries +=
            "notes" to
                jsonString(
                    "Sanitized local PesaTrack inspection. Sender, transaction values and extracted raw fields are intentionally omitted."
                )

        return entries.joinToString(
            separator = ",\n",
            prefix = "{\n",
            postfix = "\n}"
        ) {
            (key, value) ->
            "  " +
                jsonString(key) +
                ": " +
                value
        }
    }
}

private fun jsonArray(
    values: List<String>
): String =
    values.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]"
    ) {
        jsonString(it)
    }

private fun jsonString(
    value: String
): String =
    "\"" +
        buildString {
            value.forEach {
                char ->
                when (char) {
                    '\\' ->
                        append("\\\\")
                    '"' ->
                        append("\\\"")
                    '\n' ->
                        append("\\n")
                    '\r' ->
                        append("\\r")
                    '\t' ->
                        append("\\t")
                    else ->
                        if (
                            char.code <
                            0x20
                        ) {
                            append(
                                "\\u" +
                                    char.code
                                        .toString(16)
                                        .padStart(
                                            4,
                                            '0'
                                        )
                            )
                        } else {
                            append(char)
                        }
                }
            }
        } +
        "\""


object FinancialMessageInspector {
    fun inspect(
        title: String?,
        body: String,
        savedUserDecision:
            MessageLabUserDecision? = null,
        savedSemanticLabel:
            MessageLabSemanticLabel? = null
    ): FinancialMessageInspection {
        val normalized =
            FinancialMessageNormalizer
                .normalize(
                    title = title,
                    body = body
                )
        val relevance =
            FinancialMessageRelevanceGate
                .assess(normalized)
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = title,
                    body = body
                )
        val heuristicUnsupported =
            UnsupportedFinancialCandidatePolicy
                .shouldTrack(
                    relevance
                )
        val retainedUnsupported =
            MessageLabDecisionPolicy
                .retainUnsupported(
                    heuristicUnsupported =
                        heuristicUnsupported,
                    userDecision =
                        savedUserDecision
                )
        val wouldEnterMessageLab =
            retainedUnsupported &&
                !interpretation.recognized &&
                !interpretation
                    .semanticallyUnderstood
        val redactedBody =
            sanitizeBody(
                body = body,
                fields =
                    interpretation
                        .parseProvenance
                        .extractedFields
            )

        return FinancialMessageInspection(
            recognized =
                interpretation.recognized,
            semanticallyUnderstood =
                interpretation
                    .semanticallyUnderstood,
            relevanceScore =
                relevance.score,
            relevanceSignals =
                relevance.signals,
            parseRoute =
                interpretation
                    .parseProvenance
                    .route,
            templateId =
                interpretation
                    .parseProvenance
                    .templateId,
            templateRevision =
                interpretation
                    .parseProvenance
                    .templateRevision,
            provenanceReason =
                interpretation
                    .parseProvenance
                    .reason,
            transactionCode =
                interpretation
                    .transactionCode,
            occurredAtText =
                interpretation
                    .occurredAtText,
            eventSummaries =
                interpretation
                    .events
                    .map(
                        ::eventSummary
                    ),
            balanceSummaries =
                interpretation
                    .balances
                    .map {
                        fact ->
                        fact.account
                            .displayName +
                            " • " +
                            formatKes(
                                fact.balanceMinor
                            )
                    },
            semanticIntents =
                interpretation
                    .semanticAssessment
                    ?.intents
                    ?.map { it.name }
                    ?.sorted()
                    .orEmpty(),
            extractedFieldNames =
                interpretation
                    .parseProvenance
                    .extractedFields
                    .map { it.field }
                    .distinct()
                    .sorted(),
            heuristicUnsupported =
                heuristicUnsupported,
            wouldEnterMessageLab =
                wouldEnterMessageLab,
            savedUserDecision =
                savedUserDecision,
            savedSemanticLabel =
                savedSemanticLabel,
            redactedBody =
                redactedBody
        )
    }

    private fun sanitizeBody(
        body: String,
        fields:
            List<MessagePackFieldValue>
    ): String {
        var scrubbed =
            body

        fields
            .filter {
                it.startIndex >= 0 &&
                    it.endIndexExclusive <=
                    body.length &&
                    it.startIndex <
                    it.endIndexExclusive
            }
            .sortedByDescending {
                it.startIndex
            }
            .forEach {
                field ->
                scrubbed =
                    scrubbed.replaceRange(
                        field.startIndex,
                        field.endIndexExclusive,
                        placeholderFor(
                            field.field
                        )
                    )
            }

        return MessageLabRedactor
            .redact(
                safeSenderLabel = null,
                body = scrubbed
            )
            .body
    }

    private fun placeholderFor(
        field: String
    ): String {
        val normalized =
            field.uppercase()

        return when {
            normalized.contains(
                "AMOUNT"
            ) ||
                normalized.contains(
                    "BALANCE"
                ) ||
                normalized.contains(
                    "FEE"
                ) ||
                normalized.contains(
                    "OUTSTANDING"
                ) ||
                normalized.contains(
                    "LIMIT"
                ) ->
                "<AMOUNT>"

            normalized.contains(
                "TRANSACTION_CODE"
            ) ->
                "<TX_CODE>"

            normalized.contains(
                "PHONE"
            ) ||
                normalized.contains(
                    "MSISDN"
                ) ->
                "<PHONE>"

            normalized.contains(
                "COUNTERPARTY"
            ) ||
                normalized.contains(
                    "MERCHANT"
                ) ||
                normalized.contains(
                    "BENEFICIARY"
                ) ||
                normalized.contains(
                    "PAYER"
                ) ->
                "<ENTITY>"

            normalized.contains(
                "DATE"
            ) ||
                normalized.contains(
                    "TIME"
                ) ->
                "<DATE_TIME>"

            normalized.contains(
                "ACCOUNT"
            ) ||
                normalized.contains(
                    "REFERENCE"
                ) ||
                normalized.contains(
                    "REF"
                ) ||
                normalized.contains(
                    "DESTINATION"
                ) ||
                normalized.contains(
                    "SOURCE"
                ) ->
                "<REFERENCE>"

            else ->
                "<VALUE>"
        }
    }

    private fun eventSummary(
        event: FinancialEventFact
    ): String =
        when (event) {
            is PaymentEventFact ->
                "PAYMENT • " +
                    formatKes(
                        event.amountMinor
                    ) +
                    " • " +
                    event.counterparty

            is ReceiptEventFact ->
                "RECEIPT • " +
                    formatKes(
                        event.amountMinor
                    ) +
                    " • from " +
                    event.counterparty

            is TransferEventFact ->
                "TRANSFER • " +
                    formatKes(
                        event.amountMinor
                    ) +
                    " • " +
                    event.source.displayName +
                    " → " +
                    event.destination
                        .displayName

            is CashWithdrawalEventFact ->
                "CASH_WITHDRAWAL • " +
                    formatKes(
                        event.amountMinor
                    )

            is CashDepositEventFact ->
                "CASH_DEPOSIT • " +
                    formatKes(
                        event.amountMinor
                    )

            is DebtDrawdownEventFact ->
                "DEBT_DRAWDOWN • " +
                    formatKes(
                        event.amountMinor
                    ) +
                    " • " +
                    event.liability
                        .displayName

            is DebtRepaymentEventFact ->
                "DEBT_REPAYMENT • " +
                    formatKes(
                        event.amountMinor
                    ) +
                    " • " +
                    event.liability
                        .displayName +
                    (
                        event
                            .outstandingAfterMinor
                            ?.let {
                                " • remaining " +
                                    formatKes(it)
                            }
                            ?: ""
                        )

            is ReversalEventFact ->
                "REVERSAL • " +
                    formatKes(
                        event.amountMinor
                    )

            is UnresolvedFinancialEventFact ->
                "UNRESOLVED • " +
                    formatKes(
                        event.amountMinor
                    )

            else ->
                event.kind.name
        }

    private fun formatKes(
        amountMinor: Long
    ): String =
        String.format(
            Locale.US,
            "KES %,.2f",
            amountMinor / 100.0
        )
}
