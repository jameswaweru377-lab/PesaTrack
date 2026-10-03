package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.CashDepositEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.CashWithdrawalEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FieldEvidence
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialField
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpretation
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
        MessageLabUserDecision? = null,
    val savedSemanticLabel:
        MessageLabSemanticLabel? = null,
    val savedDecisionAmbiguous:
        Boolean = false,
    val redactedSender: String,
    val redactedBody: String
) {
    fun sanitizedExport(): String =
        buildString {
            append("{\n")
            appendJsonField(
                name = "sender",
                value = "<SENDER>",
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
                    "heuristicUnsupported",
                value =
                    heuristicUnsupported,
                comma = true
            )
            appendJsonField(
                name =
                    "wouldEnterMessageLab",
                value =
                    wouldEnterMessageLab,
                comma = true
            )
            savedUserDecision?.let {
                appendJsonField(
                    name =
                        "savedUserDecision",
                    value = it.name,
                    comma = true
                )
            }
            savedSemanticLabel?.let {
                appendJsonField(
                    name =
                        "savedSemanticLabel",
                    value = it.name,
                    comma = true
                )
            }
            appendJsonField(
                name =
                    "savedDecisionAmbiguous",
                value =
                    savedDecisionAmbiguous,
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
        body: String,
        savedUserDecision:
            MessageLabUserDecision? = null,
        savedSemanticLabel:
            MessageLabSemanticLabel? = null,
        savedDecisionAmbiguous:
            Boolean = false
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
        val effectiveDecision =
            if (
                savedDecisionAmbiguous
            ) {
                null
            } else {
                savedUserDecision
            }
        val retainedUnsupported =
            MessageLabDecisionPolicy
                .retainUnsupported(
                    heuristicUnsupported =
                        heuristicUnsupported,
                    userDecision =
                        effectiveDecision
                )
        val redactedBody =
            sanitizeBody(
                originalBody = body,
                normalizedBody =
                    normalized.body,
                interpretation =
                    interpretation
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
                retainedUnsupported &&
                    !interpretation
                        .recognized &&
                    !interpretation
                        .semanticallyUnderstood,
            savedUserDecision =
                if (
                    savedDecisionAmbiguous
                ) {
                    null
                } else {
                    savedUserDecision
                },
            savedSemanticLabel =
                if (
                    savedDecisionAmbiguous
                ) {
                    null
                } else {
                    savedSemanticLabel
                },
            savedDecisionAmbiguous =
                savedDecisionAmbiguous,
            redactedSender =
                "<SENDER>",
            redactedBody =
                redactedBody
        )
    }

    private data class RedactionSpan(
        val startOffset: Int,
        val endOffsetExclusive: Int,
        val placeholder: String
    )

    private fun sanitizeBody(
        originalBody: String,
        normalizedBody: String,
        interpretation:
            FinancialMessageInterpretation
    ): String {
        val evidence =
            buildList {
                addAll(
                    interpretation
                        .metadataEvidence
                )
                interpretation
                    .balances
                    .forEach {
                        addAll(it.evidence)
                    }
                interpretation
                    .events
                    .forEach {
                        addAll(
                            eventEvidence(it)
                        )
                    }
            }

        var scrubbed =
            redactEvidenceSpans(
                body = normalizedBody,
                evidence = evidence
            )

        interpretation
            .parseProvenance
            .extractedFields
            .forEach {
                field ->
                scrubbed =
                    redactExtractedValue(
                        body = scrubbed,
                        field = field
                    )
            }

        // If normalization changed currency typography or spacing,
        // the generic/interpreter spans refer to normalizedBody.
        // The raw body is deliberately not used after this point.
        @Suppress("UNUSED_VARIABLE")
        val rawBodyWasNormalized =
            originalBody !=
                normalizedBody

        return MessageLabRedactor
            .redact(
                safeSenderLabel = null,
                body = scrubbed
            )
            .body
    }

    private fun redactEvidenceSpans(
        body: String,
        evidence:
            List<FieldEvidence>
    ): String {
        val spans =
            evidence.mapNotNull {
                item ->
                item.span?.let {
                    span ->
                    if (
                        span.startOffset >= 0 &&
                        span.endOffsetExclusive <=
                        body.length &&
                        span.startOffset <
                        span.endOffsetExclusive
                    ) {
                        RedactionSpan(
                            startOffset =
                                span.startOffset,
                            endOffsetExclusive =
                                span.endOffsetExclusive,
                            placeholder =
                                placeholderFor(
                                    item.field.name
                                )
                        )
                    } else {
                        null
                    }
                }
            }

        if (spans.isEmpty()) {
            return body
        }

        val merged =
            mergeSpans(spans)
        var result =
            body

        merged
            .asReversed()
            .forEach {
                span ->
                result =
                    result.replaceRange(
                        span.startOffset,
                        span.endOffsetExclusive,
                        span.placeholder
                    )
            }

        return result
    }

    private fun mergeSpans(
        spans: List<RedactionSpan>
    ): List<RedactionSpan> {
        val sorted =
            spans.sortedWith(
                compareBy<
                    RedactionSpan
                > {
                    it.startOffset
                }.thenBy {
                    it.endOffsetExclusive
                }
            )
        val merged =
            mutableListOf<
                RedactionSpan
            >()

        sorted.forEach {
            next ->
            val current =
                merged.lastOrNull()
            if (
                current == null ||
                next.startOffset >=
                    current
                        .endOffsetExclusive
            ) {
                merged += next
            } else {
                merged[
                    merged.lastIndex
                ] =
                    RedactionSpan(
                        startOffset =
                            current.startOffset,
                        endOffsetExclusive =
                            maxOf(
                                current
                                    .endOffsetExclusive,
                                next
                                    .endOffsetExclusive
                            ),
                        placeholder =
                            if (
                                current
                                    .startOffset ==
                                next.startOffset &&
                                current
                                    .endOffsetExclusive ==
                                next
                                    .endOffsetExclusive &&
                                current
                                    .placeholder ==
                                next.placeholder
                            ) {
                                current.placeholder
                            } else {
                                "<VALUE>"
                            }
                    )
            }
        }

        return merged
    }

    private fun redactExtractedValue(
        body: String,
        field:
            MessagePackFieldValue
    ): String {
        val raw =
            field.value.trim()
        if (raw.isEmpty()) {
            return body
        }

        val variants =
            listOf(
                raw,
                FinancialMessageNormalizer
                    .normalize(
                        title = null,
                        body = raw
                    )
                    .body
            )
                .filter(
                    String::isNotBlank
                )
                .distinct()

        var result =
            body
        variants.forEach {
            value ->
            val pattern =
                Regex(
                    "(?<![A-Za-z0-9])" +
                        Regex.escape(
                            value
                        ) +
                        "(?![A-Za-z0-9])",
                    RegexOption
                        .IGNORE_CASE
                )
            result =
                result.replace(
                    pattern,
                    placeholderFor(
                        field.field
                    )
                )
        }

        return result
    }

    private fun placeholderFor(
        fieldName: String
    ): String =
        when (
            runCatching {
                FinancialField.valueOf(
                    fieldName.uppercase()
                )
            }.getOrNull()
        ) {
            FinancialField.AMOUNT,
            FinancialField.FEE,
            FinancialField.BALANCE,
            FinancialField.OUTSTANDING_BALANCE,
            FinancialField.AVAILABLE_CREDIT ->
                "<AMOUNT>"

            FinancialField.TRANSACTION_CODE ->
                "<TX_CODE>"

            FinancialField.RELATED_TRANSACTION_CODE,
            FinancialField.ACCOUNT_REFERENCE ->
                "<REFERENCE>"

            FinancialField.OCCURRED_AT ->
                "<DATE_TIME>"

            FinancialField.COUNTERPARTY ->
                "<ENTITY>"

            FinancialField.ACCOUNT,
            FinancialField.SOURCE_ACCOUNT,
            FinancialField.DESTINATION_ACCOUNT ->
                "<ACCOUNT>"

            FinancialField.LIABILITY ->
                "<LIABILITY>"

            FinancialField.COMPLETION,
            FinancialField.RELATIONSHIP,
            null ->
                when {
                    fieldName.contains(
                        "AMOUNT",
                        ignoreCase = true
                    ) ||
                        fieldName.contains(
                            "BALANCE",
                            ignoreCase = true
                        ) ||
                        fieldName.contains(
                            "FEE",
                            ignoreCase = true
                        ) ||
                        fieldName.contains(
                            "OUTSTANDING",
                            ignoreCase = true
                        ) ||
                        fieldName.contains(
                            "LIMIT",
                            ignoreCase = true
                        ) ->
                        "<AMOUNT>"

                    fieldName.contains(
                        "TRANSACTION_CODE",
                        ignoreCase = true
                    ) ->
                        "<TX_CODE>"

                    fieldName.contains(
                        "COUNTERPARTY",
                        ignoreCase = true
                    ) ||
                        fieldName.contains(
                            "MERCHANT",
                            ignoreCase = true
                        ) ||
                        fieldName.contains(
                            "BENEFICIARY",
                            ignoreCase = true
                        ) ||
                        fieldName.contains(
                            "PAYER",
                            ignoreCase = true
                        ) ->
                        "<ENTITY>"

                    fieldName.contains(
                        "DATE",
                        ignoreCase = true
                    ) ||
                        fieldName.contains(
                            "TIME",
                            ignoreCase = true
                        ) ->
                        "<DATE_TIME>"

                    fieldName.contains(
                        "ACCOUNT",
                        ignoreCase = true
                    ) ||
                        fieldName.contains(
                            "REFERENCE",
                            ignoreCase = true
                        ) ||
                        fieldName.contains(
                            "REF",
                            ignoreCase = true
                        ) ||
                        fieldName.contains(
                            "DESTINATION",
                            ignoreCase = true
                        ) ||
                        fieldName.contains(
                            "SOURCE",
                            ignoreCase = true
                        ) ->
                        "<REFERENCE>"

                    else ->
                        "<VALUE>"
                }
        }

    private fun eventEvidence(
        event:
            com.kedevjimmy.pesatrack.domain.interpretation
                .FinancialEventFact
    ): List<FieldEvidence> =
        when (event) {
            is PaymentEventFact ->
                event.evidence
            is ReceiptEventFact ->
                event.evidence
            is TransferEventFact ->
                event.evidence
            is CashWithdrawalEventFact ->
                event.evidence
            is CashDepositEventFact ->
                event.evidence
            is DebtDrawdownEventFact ->
                event.evidence
            is DebtRepaymentEventFact ->
                event.evidence
            is ReversalEventFact ->
                event.evidence
            is UnresolvedFinancialEventFact ->
                event.evidence
            else ->
                emptyList()
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
