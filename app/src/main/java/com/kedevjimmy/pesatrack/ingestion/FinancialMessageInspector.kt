package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.CashDepositEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.CashWithdrawalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
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
    val wouldEnterMessageLab: Boolean,
    val redactedSender: String,
    val redactedBody: String
) {
    fun sanitizedExport(): String =
        buildString {
            append("{\n")
            appendJsonField(
                name = "sender",
                value = redactedSender,
                comma = true
            )
            appendJsonField(
                name = "body",
                value = redactedBody,
                comma = true
            )
            appendJsonField(
                name = "recognized",
                value = recognized,
                comma = true
            )
            appendJsonField(
                name =
                    "semanticallyUnderstood",
                value =
                    semanticallyUnderstood,
                comma = true
            )
            appendJsonField(
                name = "relevanceScore",
                value = relevanceScore,
                comma = true
            )
            appendJsonArrayField(
                name = "relevanceSignals",
                values =
                    relevanceSignals
                        .map { it.name }
                        .sorted(),
                comma = true
            )
            appendJsonField(
                name = "parseRoute",
                value = parseRoute.name,
                comma = true
            )
            templateId?.let {
                appendJsonField(
                    name = "templateId",
                    value = it,
                    comma = true
                )
            }
            templateRevision?.let {
                appendJsonField(
                    name =
                        "templateRevision",
                    value = it,
                    comma = true
                )
            }
            appendJsonArrayField(
                name = "eventKinds",
                values =
                    eventSummaries.map {
                        it.substringBefore(
                            " • "
                        )
                    },
                comma = true
            )
            appendJsonArrayField(
                name = "semanticIntents",
                values = semanticIntents,
                comma = true
            )
            appendJsonArrayField(
                name =
                    "extractedFieldNames",
                values =
                    extractedFieldNames,
                comma = true
            )
            appendJsonField(
                name =
                    "wouldEnterMessageLab",
                value =
                    wouldEnterMessageLab,
                comma = true
            )
            appendJsonField(
                name = "notes",
                value =
                    "Sanitized local PesaTrack inspection. Raw message text and extracted values are intentionally omitted.",
                comma = false
            )
            append("\n}")
        }
}


private fun StringBuilder
    .appendJsonField(
        name: String,
        value: String,
        comma: Boolean
    ) {
        append("  ")
        appendJsonString(name)
        append(": ")
        appendJsonString(value)
        if (comma) {
            append(',')
        }
        append('\n')
    }

private fun StringBuilder
    .appendJsonField(
        name: String,
        value: Boolean,
        comma: Boolean
    ) {
        append("  ")
        appendJsonString(name)
        append(": ")
        append(value)
        if (comma) {
            append(',')
        }
        append('\n')
    }

private fun StringBuilder
    .appendJsonField(
        name: String,
        value: Int,
        comma: Boolean
    ) {
        append("  ")
        appendJsonString(name)
        append(": ")
        append(value)
        if (comma) {
            append(',')
        }
        append('\n')
    }

private fun StringBuilder
    .appendJsonArrayField(
        name: String,
        values: List<String>,
        comma: Boolean
    ) {
        append("  ")
        appendJsonString(name)
        append(": [")
        values.forEachIndexed {
                index,
                value ->
            if (index > 0) {
                append(", ")
            }
            appendJsonString(value)
        }
        append(']')
        if (comma) {
            append(',')
        }
        append('\n')
    }

private fun StringBuilder
    .appendJsonString(
        value: String
    ) {
        append('"')
        value.forEach {
            character ->
            when (character) {
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
                    append(character)
            }
        }
        append('"')
    }

object FinancialMessageInspector {
    fun inspect(
        title: String?,
        body: String
    ): FinancialMessageInspection {
        val normalized =
            FinancialMessageNormalizer
                .normalize(
                    title = title,
                    body = body
                )
        val relevance =
            FinancialMessageRelevanceGate
                .assess(
                    normalized
                )
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
        val redacted =
            MessageLabRedactor.redact(
                safeSenderLabel =
                    title,
                body = body
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
            wouldEnterMessageLab =
                heuristicUnsupported &&
                    !interpretation
                        .recognized &&
                    !interpretation
                        .semanticallyUnderstood,
            redactedSender =
                redacted.sender,
            redactedBody =
                redacted.body
        )
    }

    private fun eventSummary(
        event:
            com.kedevjimmy.pesatrack.domain.interpretation
                .FinancialEventFact
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
