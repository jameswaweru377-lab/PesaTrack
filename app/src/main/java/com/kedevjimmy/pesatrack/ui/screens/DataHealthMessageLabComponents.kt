package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ingestion.MessageLabImplementationReview
import com.kedevjimmy.pesatrack.ingestion.MessageLabImplementationReviewState
import com.kedevjimmy.pesatrack.ingestion.MessageLabParserReadiness
import com.kedevjimmy.pesatrack.ingestion.MessageLabParserReadinessState
import com.kedevjimmy.pesatrack.ingestion.MessageLabRelevanceExplainer
import com.kedevjimmy.pesatrack.ingestion.MessageLabResolutionKind
import com.kedevjimmy.pesatrack.ingestion.MessageLabSemanticLabel
import com.kedevjimmy.pesatrack.ingestion.MessageLabUserDecision
import com.kedevjimmy.pesatrack.ingestion.ResolvedCoverageCheck
import com.kedevjimmy.pesatrack.ingestion.ResolvedCoverageCheckState
import com.kedevjimmy.pesatrack.ingestion.ResolvedMessageLabShape
import com.kedevjimmy.pesatrack.ingestion.UnsupportedMessageShape
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill

@Composable
internal fun DataHealthUnsupportedShapeCard(
    shape: UnsupportedMessageShape,
    onConfirmFinancial: () -> Unit,
    onSemanticLabelSelected:
        (MessageLabSemanticLabel) -> Unit,
    onSemanticLabelCleared: () -> Unit,
    onImplementationReviewSelected:
        (MessageLabImplementationReviewState) -> Unit,
    onImplementationReviewCleared:
        () -> Unit,
    onIgnoreNonFinancial: () -> Unit,
    onClearDecision: () -> Unit,
    onCopyParserDraft: () -> Unit,
    onCopyFixture: () -> Unit
) {
    PesaCard(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    shape.sender,
                    modifier =
                        Modifier.weight(1f),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
                if (
                    shape.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL
                ) {
                    SmallStatusPill(
                        text =
                            "Financial",
                        tone = "green"
                    )
                }
            }
            val sourceContext =
                listOfNotNull(
                    shape.sourceKind
                        ?.takeIf(
                            String::isNotBlank
                        ),
                    shape.sourcePackage
                        ?.takeIf(
                            String::isNotBlank
                        )
                ).joinToString(" • ")

            if (
                sourceContext.isNotBlank()
            ) {
                Text(
                    sourceContext,
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
            Text(
                shape.redactedBody,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
            dataHealthMessageLabRelevanceExplanation(
                shape = shape
            )
            Text(
                "Seen " +
                    shape.occurrenceCount +
                    if (
                        shape.occurrenceCount ==
                        1
                    ) {
                        " time"
                    } else {
                        " times"
                    },
                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )
            if (
                shape.familyVariantCount >
                    1 ||
                shape.familyOccurrenceCount >
                    shape.occurrenceCount
            ) {
                Text(
                    "Format family: " +
                        shape.familyOccurrenceCount +
                        " observation" +
                        if (
                            shape.familyOccurrenceCount ==
                            1
                        ) {
                            ""
                        } else {
                            "s"
                        } +
                        " across " +
                        shape.familyVariantCount +
                        " wording variant" +
                        if (
                            shape.familyVariantCount ==
                            1
                        ) {
                            ""
                        } else {
                            "s"
                        },
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
            if (
                shape.userDecision ==
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL
            ) {
                val readiness =
                    MessageLabParserReadiness
                        .stateFor(shape)
                val readinessText =
                    when (readiness) {
                        MessageLabParserReadinessState
                            .NEEDS_FINANCIAL_CONFIRMATION ->
                            "Confirm financial"

                        MessageLabParserReadinessState
                            .NEEDS_MEANING_LABEL ->
                            "Needs meaning"

                        MessageLabParserReadinessState
                            .LABELED_SINGLE_OBSERVATION ->
                            "Need another example"

                        MessageLabParserReadinessState
                            .EVIDENCE_READY ->
                            "Evidence ready"
                    }
                SmallStatusPill(
                    text =
                        readinessText,
                    tone =
                        if (
                            readiness ==
                            MessageLabParserReadinessState
                                .EVIDENCE_READY
                        ) {
                            "green"
                        } else {
                            "amber"
                        }
                )
            }
            if (
                shape.userDecision ==
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL
            ) {
                Text(
                    "What does this message mean?",
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )
                Text(
                    "This label is evidence for future parser work only. It does not create or change a transaction.",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
                MessageLabSemanticLabel
                    .values()
                    .toList()
                    .chunked(2)
                    .forEach {
                        rowLabels ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            rowLabels.forEach {
                                label ->
                                FilterChip(
                                    selected =
                                        shape.semanticLabel ==
                                            label,
                                    onClick = {
                                        if (
                                            shape.semanticLabel ==
                                            label
                                        ) {
                                            onSemanticLabelCleared()
                                        } else {
                                            onSemanticLabelSelected(
                                                label
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            label.displayLabel
                                        )
                                    },
                                    modifier =
                                        Modifier.weight(1f)
                                )
                            }
                            if (
                                rowLabels.size == 1
                            ) {
                                Spacer(
                                    modifier =
                                        Modifier.weight(1f)
                                )
                            }
                        }
                    }
            }
            if (
                shape.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL &&
                MessageLabImplementationReview
                    .isReviewable(shape)
            ) {
                Text(
                    "Implementation review",
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )
                Text(
                    "Review the inactive parser draft and the redacted mechanics before deciding whether this shape has enough evidence for implementation.",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    MessageLabImplementationReviewState
                        .values()
                        .forEach {
                            review ->
                            FilterChip(
                                selected =
                                    shape
                                        .implementationReview ==
                                    review,
                                onClick = {
                                    if (
                                        shape
                                            .implementationReview ==
                                        review
                                    ) {
                                        onImplementationReviewCleared()
                                    } else {
                                        onImplementationReviewSelected(
                                            review
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        review.displayLabel
                                    )
                                },
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                }
                shape.implementationReview
                    ?.let {
                        review ->
                        SmallStatusPill(
                            text =
                                review.displayLabel,
                            tone =
                                if (
                                    review ==
                                    MessageLabImplementationReviewState
                                        .READY_FOR_PARSER_WORK
                                ) {
                                    "green"
                                } else {
                                    "amber"
                                }
                        )
                    }
            }
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                if (
                    shape.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL
                ) {
                    Button(
                        onClick =
                            onClearDecision,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("Clear choice")
                    }
                } else {
                    Button(
                        onClick =
                            onConfirmFinancial,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("Financial")
                    }
                    Button(
                        onClick =
                            onIgnoreNonFinancial,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("Not financial")
                    }
                }
            }
            if (
                MessageLabParserReadiness
                    .stateFor(shape) ==
                MessageLabParserReadinessState
                    .EVIDENCE_READY
            ) {
                Button(
                    onClick =
                        onCopyParserDraft,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Copy review-only parser draft"
                    )
                }
                Text(
                    "The draft proposes anchors and field roles only. It stays inactive until a developer reviews real mechanics and writes tested extraction regexes.",
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
            TextButton(
                onClick =
                    onCopyFixture,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "Copy redacted fixture"
                )
            }
        }
    }
}

@Composable
private fun dataHealthMessageLabRelevanceExplanation(
    shape: UnsupportedMessageShape
) {
    val explanation =
        MessageLabRelevanceExplainer
            .explain(
                score =
                    shape.relevanceScore,
                signals =
                    shape.relevanceSignals,
                userDecision =
                    shape.userDecision
            )

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                3.dp
            )
    ) {
        Text(
            explanation.headline,
            style =
                MaterialTheme
                    .typography
                    .labelLarge,
            color =
                MaterialTheme
                    .colorScheme
                    .primary
        )
        explanation.reasons
            .take(4)
            .forEach {
                reason ->
                Text(
                    "• " + reason,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        explanation.caution?.let {
            caution ->
            Text(
                caution,
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun DataHealthResolvedMessageLabCard(
    record: ResolvedMessageLabShape,
    coverageCheck:
        ResolvedCoverageCheck?
) {
    PesaCard(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    record.sender,
                    modifier =
                        Modifier.weight(1f),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
                SmallStatusPill(
                    text =
                        if (
                            record.resolutionKind ==
                            MessageLabResolutionKind
                                .PARSER_COVERAGE
                        ) {
                            "Resolved"
                        } else {
                            "Retired"
                        },
                    tone =
                        if (
                            record.resolutionKind ==
                            MessageLabResolutionKind
                                .PARSER_COVERAGE
                        ) {
                            "green"
                        } else {
                            "blue"
                        }
                )
            }

            Text(
                record.redactedBody,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )

            val routeLabel =
                when {
                    record.templateId != null ->
                        "Template " +
                            record.templateId +
                            (
                                record.templateRevision
                                    ?.let {
                                        " @r$it"
                                    } ?: ""
                                )

                    record.parseRoute != null ->
                        record.parseRoute
                            .lowercase()
                            .replace(
                                "_",
                                " "
                            )
                            .replaceFirstChar {
                                it.titlecase()
                            }

                    else ->
                        record.resolutionKind
                            .displayLabel
                }

            messageLabInspectionDetail(
                label = "Resolved by",
                value = routeLabel
            )

            record.eventKind
                ?.let {
                    messageLabInspectionDetail(
                        label =
                            "Event kind",
                        value = it
                    )
                }

            record.semanticLabel
                ?.let {
                    messageLabInspectionDetail(
                        label =
                            "Previous meaning",
                        value =
                            it.displayLabel
                    )
                }

            if (
                record
                    .resolvedSemanticLabels
                    .isNotEmpty()
            ) {
                messageLabInspectionDetail(
                    label =
                        "Current meaning",
                    value =
                        record
                            .resolvedSemanticLabels
                            .map {
                                it.displayLabel
                            }
                            .sorted()
                            .joinToString()
                )
            }

            if (
                record.resolutionKind ==
                    MessageLabResolutionKind
                        .PARSER_COVERAGE &&
                record.semanticLabel != null
            ) {
                messageLabInspectionDetail(
                    label =
                        "Meaning check",
                    value =
                        record
                            .resolutionAgreement
                            .displayLabel
                )
            }

            Text(
                "Observed " +
                    record.occurrenceCount +
                    " time" +
                    if (
                        record.occurrenceCount ==
                        1
                    ) {
                        ""
                    } else {
                        "s"
                    } +
                    if (
                        record.familyOccurrenceCount >
                        record.occurrenceCount
                    ) {
                        " · " +
                            record.familyOccurrenceCount +
                            " across " +
                            record.familyVariantCount +
                            " format variant" +
                            if (
                                record.familyVariantCount ==
                                1
                            ) {
                                ""
                            } else {
                                "s"
                            }
                    } else {
                        ""
                    },
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            record.provenanceReason
                ?.let {
                    Text(
                        it,
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            coverageCheck?.let {
                check ->
                val tone =
                    when (
                        check.state
                    ) {
                        ResolvedCoverageCheckState
                            .STILL_COVERED ->
                            "green"

                        ResolvedCoverageCheckState
                            .COVERED_DIFFERENT_ROUTE,
                        ResolvedCoverageCheckState
                            .STILL_RETIRED ->
                            "blue"

                        ResolvedCoverageCheckState
                            .POSSIBLE_REGRESSION ->
                            "amber"

                        ResolvedCoverageCheckState
                            .INCONCLUSIVE ->
                            "blue"
                    }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        "Current replay",
                        modifier =
                            Modifier.weight(1f),
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge
                    )
                    SmallStatusPill(
                        text =
                            check.state
                                .displayLabel,
                        tone =
                            tone
                    )
                }

                Text(
                    check.explanation,
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        if (
                            check.state ==
                            ResolvedCoverageCheckState
                                .POSSIBLE_REGRESSION
                        ) {
                            MaterialTheme
                                .colorScheme
                                .error
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        }
                )

                if (
                    check.currentTemplateId !=
                    null &&
                    check.currentTemplateId !=
                    record.templateId
                ) {
                    messageLabInspectionDetail(
                        label =
                            "Current template",
                        value =
                            check.currentTemplateId
                    )
                }
            }
        }
    }
}

@Composable
internal fun DataHealthIgnoredShapeCard(
    shape: UnsupportedMessageShape,
    onUndo: () -> Unit
) {
    PesaCard(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    shape.sender,
                    modifier =
                        Modifier.weight(1f),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
                SmallStatusPill(
                    text =
                        "Not financial",
                    tone = "amber"
                )
            }
            Text(
                shape.redactedBody,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
            dataHealthMessageLabRelevanceExplanation(
                shape = shape
            )
            TextButton(
                onClick = onUndo,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "Undo decision"
                )
            }
        }
    }
}


@Composable
private fun messageLabInspectionDetail(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
