package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialRelevanceSignal

data class MessageLabImplementationPacketVariant(
    val shapeId: String,
    val redactedBody: String,
    val occurrenceCount: Int,
    val userDecision:
        MessageLabUserDecision?,
    val semanticLabel:
        MessageLabSemanticLabel?,
    val relevanceScore: Int?,
    val relevanceSignals:
        Set<FinancialRelevanceSignal>
)

data class MessageLabImplementationPacket(
    val packetId: String,
    val familyId: String,
    val representativeShapeId: String,
    val semanticLabel:
        MessageLabSemanticLabel,
    val familyOccurrenceCount: Int,
    val familyVariantCount: Int,
    val variants:
        List<MessageLabImplementationPacketVariant>,
    val parserDraft:
        MessageLabParserDraft,
    val implementationChecklist:
        List<String>,
    val validationChecklist:
        List<String>,
    val guardrails:
        List<String>
) {
    fun toJson(): String =
        buildString {
            append("{\n")
            appendJsonField(
                "packetVersion",
                1,
                true
            )
            appendJsonField(
                "packetId",
                packetId,
                true
            )
            appendJsonField(
                "status",
                "READY_FOR_PARSER_WORK",
                true
            )
            appendJsonField(
                "requiresManualImplementation",
                true,
                true
            )
            appendJsonField(
                "familyId",
                familyId,
                true
            )
            appendJsonField(
                "representativeShapeId",
                representativeShapeId,
                true
            )
            appendJsonField(
                "semanticLabel",
                semanticLabel.name,
                true
            )
            appendJsonField(
                "semanticLabelDisplay",
                semanticLabel.displayLabel,
                true
            )
            appendJsonField(
                "familyOccurrenceCount",
                familyOccurrenceCount,
                true
            )
            appendJsonField(
                "familyVariantCount",
                familyVariantCount,
                true
            )

            append("  ")
            appendJsonString(
                "representativeParserDraft"
            )
            append(": ")
            appendIndentedJson(
                parserDraft.toJson(),
                spaces = 2
            )
            append(",\n")

            append("  ")
            appendJsonString(
                "familyVariants"
            )
            append(": [\n")
            variants.forEachIndexed {
                    index,
                    variant ->
                append("    {\n")
                appendJsonFieldAtIndent(
                    name = "shapeId",
                    value =
                        variant.shapeId,
                    indent = 6,
                    comma = true
                )
                appendJsonFieldAtIndent(
                    name = "redactedBody",
                    value =
                        variant.redactedBody,
                    indent = 6,
                    comma = true
                )
                appendJsonFieldAtIndent(
                    name =
                        "occurrenceCount",
                    value =
                        variant
                            .occurrenceCount,
                    indent = 6,
                    comma = true
                )
                appendJsonFieldAtIndent(
                    name = "userDecision",
                    value =
                        variant.userDecision
                            ?.name
                            ?: "UNDECIDED",
                    indent = 6,
                    comma = true
                )
                appendJsonFieldAtIndent(
                    name = "semanticLabel",
                    value =
                        variant.semanticLabel
                            ?.name
                            ?: "UNLABELED",
                    indent = 6,
                    comma = true
                )
                variant.relevanceScore
                    ?.let {
                        appendJsonFieldAtIndent(
                            name =
                                "relevanceScore",
                            value = it,
                            indent = 6,
                            comma = true
                        )
                    }
                appendJsonArrayFieldAtIndent(
                    name =
                        "relevanceSignals",
                    values =
                        variant
                            .relevanceSignals
                            .map {
                                it.name
                            }
                            .sorted(),
                    indent = 6,
                    comma = false
                )
                append("    }")
                if (
                    index <
                    variants.lastIndex
                ) {
                    append(',')
                }
                append('\n')
            }
            append("  ],\n")

            appendJsonArrayField(
                name =
                    "implementationChecklist",
                values =
                    implementationChecklist,
                comma = true
            )
            appendJsonArrayField(
                name =
                    "validationChecklist",
                values =
                    validationChecklist,
                comma = true
            )
            appendJsonArrayField(
                name = "guardrails",
                values = guardrails,
                comma = false
            )
            append("}")
        }
}

object MessageLabImplementationPacketGenerator {
    fun generate(
        representative:
            UnsupportedMessageShape,
        allShapes:
            List<UnsupportedMessageShape>
    ): MessageLabImplementationPacket? {
        if (
            representative
                .implementationReview !=
            MessageLabImplementationReviewState
                .READY_FOR_PARSER_WORK
        ) {
            return null
        }

        if (
            !MessageLabImplementationReview
                .isReviewable(
                    representative
                )
        ) {
            return null
        }

        val semanticLabel =
            representative.semanticLabel
                ?: return null
        val parserDraft =
            MessageLabParserDraftGenerator
                .generate(
                    representative
                )
                ?: return null

        val familyVariants =
            allShapes
                .filter {
                    it.familyId ==
                        representative
                            .familyId &&
                        it.userDecision !=
                        MessageLabUserDecision
                            .NOT_FINANCIAL
                }
                .sortedWith(
                    compareByDescending<
                        UnsupportedMessageShape
                    > {
                        it.shapeId ==
                            representative
                                .shapeId
                    }.thenByDescending {
                        it.occurrenceCount
                    }.thenByDescending {
                        it.lastSeenAtEpochMs
                    }
                )
                .map {
                    MessageLabImplementationPacketVariant(
                        shapeId =
                            it.shapeId,
                        redactedBody =
                            it.redactedBody,
                        occurrenceCount =
                            it.occurrenceCount,
                        userDecision =
                            it.userDecision,
                        semanticLabel =
                            it.semanticLabel,
                        relevanceScore =
                            it.relevanceScore,
                        relevanceSignals =
                            it.relevanceSignals
                    )
                }

        return MessageLabImplementationPacket(
            packetId =
                "implementation." +
                    representative
                        .familyId
                        .take(16) +
                    "." +
                    semanticLabel
                        .name
                        .lowercase(),
            familyId =
                representative.familyId,
            representativeShapeId =
                representative.shapeId,
            semanticLabel =
                semanticLabel,
            familyOccurrenceCount =
                maxOf(
                    representative
                        .occurrenceCount,
                    representative
                        .familyOccurrenceCount
                ),
            familyVariantCount =
                maxOf(
                    representative
                        .familyVariantCount,
                    familyVariants.size
                ),
            variants =
                familyVariants,
            parserDraft =
                parserDraft,
            implementationChecklist =
                implementationChecklist(
                    semanticLabel
                ),
            validationChecklist =
                validationChecklist(
                    semanticLabel
                ),
            guardrails =
                guardrails()
        )
    }

    fun bundleJson(
        allShapes:
            List<UnsupportedMessageShape>,
        limit: Int = 50
    ): String? {
        val representatives =
            MessageLabImplementationReview
                .readyForParserWork(
                    allShapes
                )
                .take(
                    limit.coerceAtLeast(1)
                )
        val packets =
            representatives
                .mapNotNull {
                    generate(
                        representative = it,
                        allShapes = allShapes
                    )
                }

        if (packets.isEmpty()) {
            return null
        }

        return buildString {
            append("[\n")
            packets.forEachIndexed {
                    index,
                    packet ->
                packet.toJson()
                    .lineSequence()
                    .forEach {
                        line ->
                        append("  ")
                        append(line)
                        append('\n')
                    }
                if (
                    index <
                    packets.lastIndex
                ) {
                    val lastNewline =
                        lastIndexOf(
                            "\n"
                        )
                    if (
                        lastNewline >= 0
                    ) {
                        insert(
                            lastNewline,
                            ","
                        )
                    }
                }
            }
            append("]")
        }
    }

    private fun implementationChecklist(
        semanticLabel:
            MessageLabSemanticLabel
    ): List<String> =
        buildList {
            add(
                "Review every sanitized family variant and confirm they describe the same financial mechanics."
            )
            add(
                "Confirm the proposed Message Pack event kind and field roles; do not trust the draft blindly."
            )
            add(
                "Choose provider/product/account identity only when the message evidence actually supports it."
            )
            add(
                "Author deterministic extraction regexes or reusable generic grammar; do not activate the review-only draft directly."
            )
            add(
                "Add at least three sanitized golden fixtures covering realistic value/date/wording variation."
            )
            add(
                "Prefer provider-neutral mechanics when the same meaning can be expressed across institutions."
            )

            when (semanticLabel) {
                MessageLabSemanticLabel
                    .LOAN_RECEIVED,
                MessageLabSemanticLabel
                    .LOAN_REPAYMENT ->
                    add(
                        "Verify principal, repayment amount, resulting outstanding balance, fees and funding/destination accounts independently; do not infer missing debt mechanics."
                    )

                MessageLabSemanticLabel
                    .TRANSFER ->
                    add(
                        "Resolve ownership/self-transfer semantics and mirrored bank/M-Pesa copies before allowing expense or income classification."
                    )

                MessageLabSemanticLabel
                    .PAYMENT ->
                    add(
                        "Separate the primary payment from fees and balances; do not fabricate merchant/category identity from weak text."
                    )

                MessageLabSemanticLabel
                    .MONEY_RECEIVED ->
                    add(
                        "Do not assume salary/income when the evidence could be a self-transfer, loan credit, refund or reversal."
                    )

                MessageLabSemanticLabel
                    .BALANCE_UPDATE ->
                    add(
                        "Keep balance observation separate from money-movement creation unless the message independently proves a posted movement."
                    )

                MessageLabSemanticLabel
                    .SAVINGS_INVESTMENT ->
                    add(
                        "Distinguish submitted/pending orders from settled investment or savings movements before ledger persistence."
                    )

                MessageLabSemanticLabel
                    .CASH_WITHDRAWAL,
                MessageLabSemanticLabel
                    .CASH_DEPOSIT ->
                    add(
                        "Confirm owned-account and cash-side semantics without inventing a merchant or counterparty."
                    )

                MessageLabSemanticLabel
                    .OTHER_FINANCIAL ->
                    add(
                        "Refine the financial meaning before production implementation; Other financial is not specific enough to define accounting behavior."
                    )
            }
        }

    private fun validationChecklist(
        semanticLabel:
            MessageLabSemanticLabel
    ): List<String> =
        buildList {
            add(
                "Positive tests: all supplied sanitized family variants match the intended parser route."
            )
            add(
                "Extraction tests: declared amount/date/account/balance fields meet the normal structured-field gate."
            )
            add(
                "Negative test: similar promotional, informational or incomplete wording does not create a ledger movement."
            )
            add(
                "Non-posted test: pending, declined, failed or rejected wording never becomes a posted movement."
            )
            add(
                "Duplicate-accounting test: mirrored or replayed evidence cannot create a second economic movement."
            )
            add(
                "Reprocessing test: an older stored message can be reinterpreted without cloning ledger state."
            )
            add(
                "Message Inspector test: the representative sample reports the new route/provenance after implementation."
            )

            when (semanticLabel) {
                MessageLabSemanticLabel
                    .LOAN_REPAYMENT ->
                    add(
                        "Debt reconciliation tests: zero observed outstanding closes the liability; non-zero observed outstanding remains authoritative over estimated subtraction."
                    )

                MessageLabSemanticLabel
                    .LOAN_RECEIVED ->
                    add(
                        "Debt drawdown tests: loan principal and any fee/excise amount are not double-counted as ordinary income."
                    )

                MessageLabSemanticLabel
                    .TRANSFER ->
                    add(
                        "Transfer tests: owned-account transfers remain transfers and mirrored channel copies reconcile to one movement."
                    )

                MessageLabSemanticLabel
                    .BALANCE_UPDATE ->
                    add(
                        "Balance-only test: the observation updates account state without inventing an expense, income or transfer."
                    )

                else ->
                    Unit
            }
        }

    private fun guardrails(): List<String> =
        listOf(
            "This packet is development evidence only and must never be loaded as an active parser.",
            "Raw financial SMS must remain local; only sanitized/redacted evidence belongs in fixtures or developer handoff.",
            "User labels and implementation review are evidence, not accounting authority.",
            "Do not add provider-specific if/else parsing when a reusable financial grammar can express the same mechanics.",
            "Production activation still requires the existing relevance, extraction, duplicate-accounting and reconciliation gates."
        )
}

private fun StringBuilder
    .appendIndentedJson(
        json: String,
        spaces: Int
    ) {
        val indent =
            " ".repeat(spaces)
        val lines =
            json.lineSequence()
                .toList()
        lines.forEachIndexed {
                index,
                line ->
            if (index > 0) {
                append(indent)
            }
            append(line)
            if (
                index <
                lines.lastIndex
            ) {
                append('\n')
            }
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
    .appendJsonFieldAtIndent(
        name: String,
        value: String,
        indent: Int,
        comma: Boolean
    ) {
        append(
            " ".repeat(indent)
        )
        appendJsonString(name)
        append(": ")
        appendJsonString(value)
        if (comma) {
            append(',')
        }
        append('\n')
    }

private fun StringBuilder
    .appendJsonFieldAtIndent(
        name: String,
        value: Int,
        indent: Int,
        comma: Boolean
    ) {
        append(
            " ".repeat(indent)
        )
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
        appendJsonArrayFieldAtIndent(
            name = name,
            values = values,
            indent = 2,
            comma = comma
        )
    }

private fun StringBuilder
    .appendJsonArrayFieldAtIndent(
        name: String,
        values: List<String>,
        indent: Int,
        comma: Boolean
    ) {
        append(
            " ".repeat(indent)
        )
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
