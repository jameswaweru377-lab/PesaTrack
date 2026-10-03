package com.kedevjimmy.pesatrack.ingestion

data class MessageLabParserDraftField(
    val placeholder: String,
    val occurrence: Int,
    val suggestedField: String,
    val leftContext: String,
    val rightContext: String
)

data class MessageLabParserDraft(
    val draftId: String,
    val shapeId: String,
    val senderIds: List<String>,
    val sourcePackage: String?,
    val sourceKind: String?,
    val semanticLabel:
        MessageLabSemanticLabel,
    val suggestedEventKind: String,
    val redactedExample: String,
    val candidateAnchors: List<String>,
    val fields:
        List<MessageLabParserDraftField>,
    val occurrenceCount: Int,
    val warnings: List<String>
) {
    fun toJson(): String =
        buildString {
            append("{\n")
            appendJsonField(
                "draftVersion",
                1,
                true
            )
            appendJsonField(
                "draftId",
                draftId,
                true
            )
            appendJsonField(
                "shapeId",
                shapeId,
                true
            )
            appendJsonField(
                "country",
                "KE",
                true
            )
            appendJsonArrayField(
                "senderIds",
                senderIds,
                true
            )
            sourcePackage?.let {
                appendJsonField(
                    "sourcePackage",
                    it,
                    true
                )
            }
            sourceKind?.let {
                appendJsonField(
                    "sourceKind",
                    it,
                    true
                )
            }
            appendJsonField(
                "semanticLabel",
                semanticLabel.name,
                true
            )
            appendJsonField(
                "suggestedEventKind",
                suggestedEventKind,
                true
            )
            appendJsonField(
                "active",
                false,
                true
            )
            appendJsonField(
                "requiresManualReview",
                true,
                true
            )
            appendJsonField(
                "occurrenceCount",
                occurrenceCount,
                true
            )
            appendJsonField(
                "redactedExample",
                redactedExample,
                true
            )
            appendJsonArrayField(
                "candidateAnchors",
                candidateAnchors,
                true
            )

            append("  ")
            appendJsonString("fieldCandidates")
            append(": [\n")
            fields.forEachIndexed {
                    index,
                    field ->
                append("    {")
                appendJsonInlineField(
                    "placeholder",
                    field.placeholder,
                    true
                )
                appendJsonInlineField(
                    "occurrence",
                    field.occurrence,
                    true
                )
                appendJsonInlineField(
                    "suggestedField",
                    field.suggestedField,
                    true
                )
                appendJsonInlineField(
                    "leftContext",
                    field.leftContext,
                    true
                )
                appendJsonInlineField(
                    "rightContext",
                    field.rightContext,
                    false
                )
                append("}")
                if (
                    index <
                    fields.lastIndex
                ) {
                    append(',')
                }
                append('\n')
            }
            append("  ],\n")

            appendJsonArrayField(
                "warnings",
                warnings,
                false
            )
            append("}")
        }
}

object MessageLabParserDraftGenerator {
    private val placeholderRegex =
        Regex(
            """<[A-Z_]+>"""
        )

    fun generate(
        shape:
            UnsupportedMessageShape
    ): MessageLabParserDraft? {
        if (
            MessageLabParserReadiness
                .stateFor(shape) !=
            MessageLabParserReadinessState
                .EVIDENCE_READY
        ) {
            return null
        }

        val semanticLabel =
            shape.semanticLabel
                ?: return null

        val fields =
            buildFieldCandidates(
                shape = shape,
                semanticLabel =
                    semanticLabel
            )

        return MessageLabParserDraft(
            draftId =
                buildDraftId(
                    shape
                ),
            shapeId =
                shape.shapeId,
            senderIds =
                shape.sender
                    .trim()
                    .takeIf {
                        it.isNotBlank() &&
                            it != "<SENDER>"
                    }
                    ?.let(::listOf)
                    .orEmpty(),
            sourcePackage =
                shape.sourcePackage,
            sourceKind =
                shape.sourceKind,
            semanticLabel =
                semanticLabel,
            suggestedEventKind =
                eventKindFor(
                    semanticLabel
                ),
            redactedExample =
                shape.redactedBody,
            candidateAnchors =
                candidateAnchors(
                    shape.redactedBody
                ),
            fields =
                fields,
            occurrenceCount =
                shape.occurrenceCount,
            warnings =
                warningsFor(
                    semanticLabel =
                        semanticLabel,
                    fields = fields
                )
        )
    }

    fun bundleJson(
        shapes:
            List<UnsupportedMessageShape>
    ): String? {
        val drafts =
            shapes.mapNotNull(
                ::generate
            )

        if (drafts.isEmpty()) {
            return null
        }

        return buildString {
            append("[\n")
            drafts.forEachIndexed {
                    index,
                    draft ->
                draft.toJson()
                    .lineSequence()
                    .forEach {
                        line ->
                        append("  ")
                        append(line)
                        append('\n')
                    }
                if (
                    index <
                    drafts.lastIndex
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

    private fun buildFieldCandidates(
        shape:
            UnsupportedMessageShape,
        semanticLabel:
            MessageLabSemanticLabel
    ): List<MessageLabParserDraftField> {
        val body =
            shape.redactedBody
        val counters =
            mutableMapOf<
                String,
                Int
            >()

        return placeholderRegex
            .findAll(body)
            .map {
                match ->
                val placeholder =
                    match.value
                val occurrence =
                    (
                        counters[
                            placeholder
                        ] ?: 0
                        ) + 1
                counters[
                    placeholder
                ] = occurrence

                val left =
                    body.substring(
                        maxOf(
                            0,
                            match.range.first -
                                CONTEXT_CHARS
                        ),
                        match.range.first
                    )
                        .trim()
                val right =
                    body.substring(
                        match.range.last +
                            1,
                        minOf(
                            body.length,
                            match.range.last +
                                1 +
                                CONTEXT_CHARS
                        )
                    )
                        .trim()

                MessageLabParserDraftField(
                    placeholder =
                        placeholder,
                    occurrence =
                        occurrence,
                    suggestedField =
                        fieldNameFor(
                            placeholder =
                                placeholder,
                            occurrence =
                                occurrence,
                            leftContext =
                                left,
                            rightContext =
                                right,
                            semanticLabel =
                                semanticLabel
                        ),
                    leftContext =
                        left,
                    rightContext =
                        right
                )
            }
            .toList()
    }

    private fun fieldNameFor(
        placeholder: String,
        occurrence: Int,
        leftContext: String,
        rightContext: String,
        semanticLabel:
            MessageLabSemanticLabel
    ): String {
        val context =
            (
                leftContext +
                    " " +
                    rightContext
                ).lowercase()

        return when (
            placeholder
        ) {
            "<TX_CODE>" ->
                "TRANSACTION_CODE"

            "<DATE>" ->
                "DATE"

            "<TIME>" ->
                "TIME"

            "<PHONE>" ->
                "PHONE"

            "<EMAIL>" ->
                "EMAIL"

            "<PERSON>" ->
                "PERSON"

            "<ENTITY>" ->
                when {
                    semanticLabel ==
                        MessageLabSemanticLabel
                            .TRANSFER &&
                        containsNearby(
                            leftContext,
                            "to"
                        ) ->
                        "DESTINATION"

                    semanticLabel ==
                        MessageLabSemanticLabel
                            .TRANSFER &&
                        containsNearby(
                            leftContext,
                            "from"
                        ) ->
                        "SOURCE"

                    else ->
                        "COUNTERPARTY"
                }

            "<NUMBER>" ->
                "NUMBER_$occurrence"

            "<AMOUNT>" ->
                when {
                    "transaction cost" in
                        context ||
                        "fee" in
                        context ||
                        "charge" in
                        context ->
                        "FEE"

                    (
                        "balance" in
                            context ||
                            "outstanding" in
                            context
                        ) &&
                        (
                            semanticLabel ==
                                MessageLabSemanticLabel
                                    .LOAN_RECEIVED ||
                                semanticLabel ==
                                MessageLabSemanticLabel
                                    .LOAN_REPAYMENT
                            ) ->
                        "OUTSTANDING"

                    "balance" in
                        context ->
                        "BALANCE"

                    occurrence == 1 ->
                        "AMOUNT"

                    else ->
                        "AMOUNT_$occurrence"
                }

            else ->
                placeholder
                    .removePrefix("<")
                    .removeSuffix(">")
        }
    }

    private fun candidateAnchors(
        body: String
    ): List<String> =
        body.split(
            placeholderRegex
        )
            .map {
                it.trim()
                    .trim(
                        ' ',
                        '.',
                        ',',
                        ':',
                        ';',
                        '-'
                    )
                    .replace(
                        Regex(
                            """[ ]{2,}"""
                        ),
                        " "
                    )
            }
            .filter {
                literal ->
                literal.length >=
                    MIN_ANCHOR_LENGTH &&
                    literal.count {
                        it.isLetter()
                    } >=
                    MIN_ANCHOR_LETTERS
            }
            .distinct()
            .take(
                MAX_ANCHORS
            )

    private fun eventKindFor(
        label:
            MessageLabSemanticLabel
    ): String =
        when (label) {
            MessageLabSemanticLabel
                .PAYMENT ->
                "PAYMENT"

            MessageLabSemanticLabel
                .MONEY_RECEIVED ->
                "RECEIPT"

            MessageLabSemanticLabel
                .TRANSFER ->
                "TRANSFER"

            MessageLabSemanticLabel
                .CASH_WITHDRAWAL ->
                "CASH_WITHDRAWAL"

            MessageLabSemanticLabel
                .CASH_DEPOSIT ->
                "CASH_DEPOSIT"

            MessageLabSemanticLabel
                .LOAN_RECEIVED ->
                "DEBT_DRAWDOWN"

            MessageLabSemanticLabel
                .LOAN_REPAYMENT ->
                "DEBT_REPAYMENT"

            MessageLabSemanticLabel
                .BALANCE_UPDATE ->
                "BALANCE_UPDATE"

            MessageLabSemanticLabel
                .SAVINGS_INVESTMENT ->
                "INVESTMENT_REVIEW"

            MessageLabSemanticLabel
                .OTHER_FINANCIAL ->
                "REVIEW_REQUIRED"
        }

    private fun warningsFor(
        semanticLabel:
            MessageLabSemanticLabel,
        fields:
            List<MessageLabParserDraftField>
    ): List<String> =
        buildList {
            add(
                "Draft only: no active Message Pack template or regex is created automatically."
            )
            add(
                "Review product identity, anchors, field roles and extraction regexes against sanitized golden fixtures before activation."
            )
            if (
                fields.none {
                    it.suggestedField ==
                        "AMOUNT"
                } &&
                semanticLabel !in
                setOf(
                    MessageLabSemanticLabel
                        .BALANCE_UPDATE,
                    MessageLabSemanticLabel
                        .OTHER_FINANCIAL
                )
            ) {
                add(
                    "No primary AMOUNT field was inferred from the redacted shape."
                )
            }
            if (
                semanticLabel ==
                MessageLabSemanticLabel
                    .SAVINGS_INVESTMENT
            ) {
                add(
                    "Savings/investment evidence needs a more specific event kind during manual review."
                )
            }
            if (
                semanticLabel ==
                MessageLabSemanticLabel
                    .OTHER_FINANCIAL
            ) {
                add(
                    "Other-financial evidence requires a manual semantic decision before parser implementation."
                )
            }
        }

    private fun buildDraftId(
        shape:
            UnsupportedMessageShape
    ): String {
        val sender =
            shape.sender
                .lowercase()
                .replace(
                    Regex(
                        """[^a-z0-9]+"""
                    ),
                    "-"
                )
                .trim('-')
                .ifBlank {
                    "unknown-source"
                }
                .take(32)

        val meaning =
            shape.semanticLabel
                ?.name
                ?.lowercase()
                ?.replace(
                    "_",
                    "-"
                )
                ?: "unknown"

        return "ke.draft." +
            sender +
            "." +
            meaning +
            "." +
            shape.shapeId
                .take(10)
    }

    private fun containsNearby(
        context: String,
        token: String
    ): Boolean =
        context
            .trim()
            .lowercase()
            .split(
                Regex(
                    """\s+"""
                )
            )
            .takeLast(3)
            .any {
                it.trim(
                    '.',
                    ',',
                    ':',
                    ';'
                ) == token
            }

    private const val CONTEXT_CHARS =
        42
    private const val MIN_ANCHOR_LENGTH =
        5
    private const val MIN_ANCHOR_LETTERS =
        3
    private const val MAX_ANCHORS =
        6
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
    .appendJsonInlineField(
        name: String,
        value: String,
        comma: Boolean
    ) {
        appendJsonString(name)
        append(": ")
        appendJsonString(value)
        if (comma) {
            append(", ")
        }
    }

private fun StringBuilder
    .appendJsonInlineField(
        name: String,
        value: Int,
        comma: Boolean
    ) {
        appendJsonString(name)
        append(": ")
        append(value)
        if (comma) {
            append(", ")
        }
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
