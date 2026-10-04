package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.IdentityGraphStore
import com.kedevjimmy.pesatrack.data.local.PersonalLearningStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.health.ReviewBurdenAnalysis
import com.kedevjimmy.pesatrack.domain.health.ReviewBurdenSnapshot
import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphSnapshot
import com.kedevjimmy.pesatrack.domain.messagepack.KenyaMessagePack
import com.kedevjimmy.pesatrack.ingestion.FinancialCoverageAnalysis
import com.kedevjimmy.pesatrack.ingestion.FinancialHistoryReprocessFreshness
import com.kedevjimmy.pesatrack.ingestion.FinancialHistoryReprocessReport
import com.kedevjimmy.pesatrack.ingestion.FinancialHistoryReprocessStateStore
import com.kedevjimmy.pesatrack.ingestion.FinancialHistoryReprocessor
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageCoverageSnapshot
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageInspection
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageInspector
import com.kedevjimmy.pesatrack.ingestion.FinancialSourceCoverageSnapshot
import com.kedevjimmy.pesatrack.ingestion.MessageLabStore
import com.kedevjimmy.pesatrack.ingestion.MessageLabImplementationReview
import com.kedevjimmy.pesatrack.ingestion.MessageLabImplementationReviewState
import com.kedevjimmy.pesatrack.ingestion.MessageInspectorParserHandoffPolicy
import com.kedevjimmy.pesatrack.ingestion.MessageLabParserReadiness
import com.kedevjimmy.pesatrack.ingestion.MessageLabParserReadinessState
import com.kedevjimmy.pesatrack.ingestion.MessageLabRelevanceExplainer
import com.kedevjimmy.pesatrack.ingestion.MessageLabResolutionKind
import com.kedevjimmy.pesatrack.ingestion.MessageLabResolutionRegressionChecker
import com.kedevjimmy.pesatrack.ingestion.ResolvedCoverageCheck
import com.kedevjimmy.pesatrack.ingestion.ResolvedCoverageCheckState
import com.kedevjimmy.pesatrack.ingestion.ResolvedMessageLabShape
import com.kedevjimmy.pesatrack.ingestion.MessageLabSemanticLabel
import com.kedevjimmy.pesatrack.ingestion.MessageLabUserDecision
import com.kedevjimmy.pesatrack.ingestion.UnsupportedMessageShape
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun MessageInspectorCard(
    title: String,
    body: String,
    inspection:
        FinancialMessageInspection?,
    evidenceShape:
        UnsupportedMessageShape?,
    financialEvidenceSaved: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onClear: () -> Unit,
    onAddFinancialEvidence: () -> Unit,
    onSemanticLabelSelected:
        (MessageLabSemanticLabel) -> Unit,
    onSemanticLabelCleared: () -> Unit,
    onImplementationReviewSelected:
        (MessageLabImplementationReviewState) ->
        Unit,
    onImplementationReviewCleared:
        () -> Unit,
    onCopyParserDraft: () -> Unit,
    onCopyImplementationPacket: () -> Unit,
    onCopySanitized: () -> Unit
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
                    10.dp
                )
        ) {
            Text(
                "Test the current PesaTrack brain",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )
            Text(
                "Paste one problematic SMS here. PesaTrack analyzes it locally with the current Message Pack and Financial Intelligence. The raw text is not saved by this tool.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            OutlinedTextField(
                value = title,
                onValueChange =
                    onTitleChange,
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "Sender / title (optional)"
                    )
                },
                singleLine = true
            )

            OutlinedTextField(
                value = body,
                onValueChange =
                    onBodyChange,
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("Message")
                },
                minLines = 4,
                maxLines = 8
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                Button(
                    onClick =
                        onAnalyze,
                    enabled =
                        body.isNotBlank(),
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Analyze")
                }
                TextButton(
                    onClick =
                        onClear,
                    enabled =
                        body.isNotBlank() ||
                            title.isNotBlank() ||
                            inspection != null,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }

            inspection?.let {
                result ->
                val statusText =
                    when {
                        result.recognized ->
                            "Recognized"

                        result
                            .semanticallyUnderstood ->
                            "Understood"

                        result
                            .wouldEnterMessageLab ->
                            "Not understood"

                        else ->
                            "Not financial / ignored"
                    }
                val statusTone =
                    when {
                        result.recognized ->
                            "green"

                        result
                            .semanticallyUnderstood ->
                            "blue"

                        result
                            .wouldEnterMessageLab ->
                            "amber"

                        else ->
                            "blue"
                    }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        "Inspection result",
                        modifier =
                            Modifier.weight(1f),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                    SmallStatusPill(
                        text =
                            statusText,
                        tone =
                            statusTone
                    )
                }

                InspectionDetail(
                    label = "Parse route",
                    value =
                        result.parseRoute.name
                )
                InspectionDetail(
                    label = "Template",
                    value =
                        result.templateId
                            ?.let {
                                id ->
                                id +
                                    (
                                        result
                                            .templateRevision
                                            ?.let {
                                                " @r$it"
                                            }
                                            ?: ""
                                        )
                            }
                            ?: "No Message Pack template"
                )
                InspectionDetail(
                    label =
                        "Relevance score",
                    value =
                        result.relevanceScore
                            .toString()
                )

                if (
                    result
                        .relevanceSignals
                        .isNotEmpty()
                ) {
                    InspectionDetail(
                        label =
                            "Relevance signals",
                        value =
                            result
                                .relevanceSignals
                                .map {
                                    it.name
                                        .lowercase()
                                        .replace(
                                            "_",
                                            " "
                                        )
                                }
                                .sorted()
                                .joinToString()
                    )
                }

                if (
                    result
                        .extractedFieldNames
                        .isNotEmpty()
                ) {
                    InspectionDetail(
                        label =
                            "Extracted fields",
                        value =
                            result
                                .extractedFieldNames
                                .joinToString()
                    )
                }

                result.transactionCode
                    ?.let {
                        InspectionDetail(
                            label =
                                "Transaction code",
                            value = it
                        )
                    }
                result.occurredAtText
                    ?.let {
                        InspectionDetail(
                            label =
                                "Message date/time",
                            value = it
                        )
                    }

                if (
                    result.eventSummaries
                        .isNotEmpty()
                ) {
                    Text(
                        "Events",
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                    result.eventSummaries
                        .forEach {
                            event ->
                            Text(
                                "• $event",
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
                }

                if (
                    result.balanceSummaries
                        .isNotEmpty()
                ) {
                    Text(
                        "Balances",
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                    result.balanceSummaries
                        .forEach {
                            balance ->
                            Text(
                                "• $balance",
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
                }

                if (
                    result.semanticIntents
                        .isNotEmpty()
                ) {
                    InspectionDetail(
                        label =
                            "Semantic hints",
                        value =
                            result
                                .semanticIntents
                                .map {
                                    it.lowercase()
                                        .replace(
                                            "_",
                                            " "
                                        )
                                }
                                .joinToString()
                    )
                }

                result.savedUserDecision
                    ?.let {
                        decision ->
                        InspectionDetail(
                            label =
                                "Saved Message Lab choice",
                            value =
                                when (decision) {
                                    MessageLabUserDecision
                                        .CONFIRMED_FINANCIAL ->
                                        "Financial"

                                    MessageLabUserDecision
                                        .NOT_FINANCIAL ->
                                        "Not financial"
                                }
                        )
                    }

                result.savedSemanticLabel
                    ?.let {
                        label ->
                        InspectionDetail(
                            label =
                                "Saved meaning",
                            value =
                                label.displayLabel
                        )
                    }

                if (
                    result
                        .savedDecisionAmbiguous
                ) {
                    Text(
                        "This wording exists in more than one Message Lab source channel, so no saved Financial / Not financial choice was applied to this pasted message.",
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

                Text(
                    result.provenanceReason,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    when {
                        result
                            .wouldEnterMessageLab ->
                            "With the current parser and any unambiguous saved Message Lab choice, this message would still enter Message Lab as unsupported financial evidence."

                        result.recognized ||
                            result
                                .semanticallyUnderstood ->
                            "This message should not remain in Message Lab after reconciliation."

                        result.savedUserDecision ==
                            MessageLabUserDecision
                                .NOT_FINANCIAL ->
                            "Your saved Not financial choice suppresses this shape from Message Lab."

                        result
                            .savedDecisionAmbiguous ->
                            "No source-specific saved choice was applied because the same wording exists in multiple source channels."

                        else ->
                            "This message does not currently qualify as unsupported financial evidence."
                    },
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                if (
                    result.wouldEnterMessageLab
                ) {
                    if (
                        financialEvidenceSaved ||
                        result.savedUserDecision ==
                            MessageLabUserDecision
                                .CONFIRMED_FINANCIAL
                    ) {
                        Text(
                            "This unsupported wording is already saved as financial evidence. Manual inspector evidence stays at one observation; repeated taps cannot make it parser-ready.",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                        Text(
                            "What does this message mean?",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleSmall
                        )
                        Text(
                            "This meaning is evidence for future parser work only. It does not create, edit, or categorize a ledger transaction.",
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
                                        Modifier
                                            .fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement
                                            .spacedBy(
                                                8.dp
                                            )
                                ) {
                                    rowLabels.forEach {
                                        label ->
                                        FilterChip(
                                            selected =
                                                result
                                                    .savedSemanticLabel ==
                                                label,
                                            onClick = {
                                                if (
                                                    result
                                                        .savedSemanticLabel ==
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
                                                    label
                                                        .displayLabel
                                                )
                                            },
                                            modifier =
                                                Modifier
                                                    .weight(1f)
                                        )
                                    }
                                    if (
                                        rowLabels.size ==
                                        1
                                    ) {
                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .weight(1f)
                                        )
                                    }
                                }
                            }
                        evidenceShape
                            ?.takeIf {
                                result
                                    .savedSemanticLabel !=
                                    null
                            }
                            ?.let {
                                shape ->
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
                                            "Need another real example"

                                        MessageLabParserReadinessState
                                            .EVIDENCE_READY ->
                                            "Evidence ready"
                                    }

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    verticalAlignment =
                                        Alignment
                                            .CenterVertically
                                ) {
                                    Text(
                                        "Parser evidence",
                                        modifier =
                                            Modifier
                                                .weight(1f),
                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleSmall
                                    )
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

                                Text(
                                    "Observed " +
                                        shape.occurrenceCount +
                                        " time" +
                                        if (
                                            shape.occurrenceCount ==
                                            1
                                        ) {
                                            ""
                                        } else {
                                            "s"
                                        } +
                                        if (
                                            shape.familyOccurrenceCount >
                                            shape.occurrenceCount
                                        ) {
                                            " · " +
                                                shape.familyOccurrenceCount +
                                                " across this format family"
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

                                when (readiness) {
                                    MessageLabParserReadinessState
                                        .LABELED_SINGLE_OBSERVATION ->
                                        Text(
                                            "One real observation is not enough for parser work. A manually pasted sample stays at one observation; wait for another actual occurrence or existing family evidence.",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodySmall,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                        )

                                    MessageLabParserReadinessState
                                        .EVIDENCE_READY -> {
                                        val handoff =
                                            MessageInspectorParserHandoffPolicy
                                                .availabilityFor(
                                                    shape
                                                )
                                        Text(
                                            "Implementation review",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .titleSmall
                                        )
                                        Text(
                                            "Reviewing evidence still does not activate a parser. It only decides whether development should proceed or wait for more examples.",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodySmall,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                        )
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
                                                            review
                                                                .displayLabel
                                                        )
                                                    },
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                )
                                            }
                                        if (
                                            handoff
                                                .parserDraftAvailable
                                        ) {
                                            Button(
                                                onClick =
                                                    onCopyParserDraft,
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                            ) {
                                                Text(
                                                    "Copy review-only parser draft"
                                                )
                                            }
                                        }
                                        if (
                                            handoff
                                                .implementationPacketAvailable
                                        ) {
                                            Button(
                                                onClick =
                                                    onCopyImplementationPacket,
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                            ) {
                                                Text(
                                                    "Copy implementation work packet"
                                                )
                                            }
                                            Text(
                                                "The work packet includes this approved family, all active sanitized variants, the parser draft, and the implementation/test guardrails. It still does not activate a parser.",
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

                                    else ->
                                        Unit
                                }
                            }
                    } else {
                        Button(
                            onClick =
                                onAddFinancialEvidence,
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Add as financial evidence"
                            )
                        }
                        Text(
                            "Only the locally redacted shape is stored. This manual sample counts as one observation and does not create a transaction, balance, parser, or repeated-evidence credit.",
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

                Text(
                    "The copied diagnostic replaces the sender and interpreter-extracted values with placeholders. It does not copy event amounts, counterparties or account references.",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                TextButton(
                    onClick =
                        onCopySanitized,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Copy sanitized diagnostic"
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectionDetail(
    label: String,
    value: String
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                2.dp
            )
    ) {
        Text(
            label,
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
        Text(
            value,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}

@Composable
internal fun HistoryReprocessCard(
    permissionGranted: Boolean,
    permissionDenied: Boolean,
    selectedMonths: Int,
    freshness:
        FinancialHistoryReprocessFreshness,
    reprocessing: Boolean,
    report:
        FinancialHistoryReprocessReport?,
    error: String?,
    onMonthsSelected: (Int) -> Unit,
    onRequestPermission: () -> Unit,
    onReprocess: () -> Unit
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
                    10.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    "Reprocess financial history",
                    modifier =
                        Modifier.weight(1f),
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )
                SmallStatusPill(
                    text =
                        if (
                            freshness
                                .reprocessRecommended
                        ) {
                            "Recommended"
                        } else {
                            "Up to date"
                        },
                    tone =
                        if (
                            freshness
                                .reprocessRecommended
                        ) {
                            "amber"
                        } else {
                            "green"
                        }
                )
            }
            Text(
                when {
                    freshness
                        .lastSuccessfulAtEpochMs ==
                        null ->
                        "No successful history reconciliation has been recorded on this installation yet."

                    freshness
                        .reprocessRecommended ->
                        "PesaTrack was updated after the last successful history reconciliation. Run it again so older SMS can benefit from the newer intelligence."

                    else ->
                        "History has been reconciled after the current app update" +
                            (
                                freshness
                                    .lastMonths
                                    ?.let {
                                        months ->
                                        " using the last " +
                                            months +
                                            if (
                                                months == 1
                                            ) {
                                                " month."
                                            } else {
                                                " months."
                                            }
                                    }
                                    ?: "."
                                )
                },
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    if (
                        freshness
                            .reprocessRecommended
                    ) {
                        MaterialTheme
                            .colorScheme
                            .tertiary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    }
            )
            Text(
                "Run the latest PesaTrack intelligence over SMS already on this phone. Existing ledger movements are deduplicated, while newer debt and account understanding can refresh old state.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
            Text(
                "This reconciliation pass does not add duplicate Data Health observations or inflate existing Message Lab occurrence counts.",
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            if (!permissionGranted) {
                Button(
                    onClick =
                        onRequestPermission,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Allow SMS history access"
                    )
                }
                if (permissionDenied) {
                    Text(
                        "SMS access was not granted. Real-time notification capture can still work, but existing SMS cannot be reconciled.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            } else {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    (1..3).forEach {
                        months ->
                        FilterChip(
                            selected =
                                selectedMonths ==
                                    months,
                            onClick = {
                                onMonthsSelected(
                                    months
                                )
                            },
                            label = {
                                Text(
                                    months.toString() +
                                        if (
                                            months == 1
                                        ) {
                                            " month"
                                        } else {
                                            " months"
                                        }
                                )
                            },
                            enabled =
                                !reprocessing,
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }

                Button(
                    onClick =
                        onReprocess,
                    enabled =
                        !reprocessing,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    if (reprocessing) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.height(
                                    20.dp
                                ),
                            strokeWidth = 2.dp
                        )
                        Text(
                            " Reprocessing…"
                        )
                    } else {
                        Text(
                            "Reprocess last " +
                                selectedMonths +
                                if (
                                    selectedMonths == 1
                                ) {
                                    " month"
                                } else {
                                    " months"
                                }
                        )
                    }
                }
            }

            error?.let {
                message ->
                Text(
                    message,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }

            report?.let {
                result ->
                Text(
                    "Reconciliation report",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
                HealthMetric(
                    label = "SMS scanned",
                    value =
                        result
                            .importResult
                            .scanned
                )
                HealthMetric(
                    label =
                        "Understood by current brain",
                    value =
                        result
                            .importResult
                            .understood
                )
                HealthMetric(
                    label =
                        "New ledger/events added",
                    value =
                        result
                            .importResult
                            .inserted
                )
                HealthMetric(
                    label =
                        "Existing duplicates preserved",
                    value =
                        result
                            .importResult
                            .duplicates
                )
                HealthMetric(
                    label =
                        "Existing state refreshed",
                    value =
                        result
                            .importResult
                            .stateRefreshed
                )
                HealthMetric(
                    label =
                        "Pending review events",
                    value =
                        result
                            .importResult
                            .pendingReview
                )
                HealthMetric(
                    label =
                        "Not applied",
                    value =
                        result
                            .importResult
                            .ignored
                )
                HealthMetric(
                    label =
                        "Old unsupported occurrences cleared",
                    value =
                        result
                            .retiredUnsupportedOccurrences
                )

                Text(
                    "Debt outstanding: " +
                        formatKesMinor(
                            result
                                .totalOutstandingBeforeMinor
                        ) +
                        " → " +
                        formatKesMinor(
                            result
                                .totalOutstandingAfterMinor
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )

                if (
                    result
                        .debtBalanceChanges
                        .isEmpty()
                ) {
                    Text(
                        "No debt balance changed during this pass.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                } else {
                    result
                        .debtBalanceChanges
                        .take(6)
                        .forEach {
                            change ->
                            Text(
                                change.name +
                                    ": " +
                                    (
                                        change
                                            .beforeOutstandingMinor
                                            ?.let(
                                                ::formatKesMinor
                                            )
                                            ?: "new"
                                        ) +
                                    " → " +
                                    formatKesMinor(
                                        change
                                            .afterOutstandingMinor
                                    ),
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
                }
            }
        }
    }
}

private fun formatKesMinor(
    amountMinor: Long
): String =
    String.format(
        Locale.US,
        "KES %,.2f",
        amountMinor / 100.0
    )

@Composable
internal fun ReviewBurdenCard(
    snapshot:
        ReviewBurdenSnapshot
) {
    val rate =
        snapshot.reviewRatePercent
    val targetMet =
        snapshot.mature &&
            rate != null &&
            rate <
            ReviewBurdenAnalysis
                .TARGET_PERCENT
    val status =
        when {
            !snapshot.mature ->
                "Learning " +
                    snapshot
                        .observedHistoryDays +
                    "/" +
                    snapshot.windowDays +
                    " days"

            rate == null ->
                "No ordinary spending"

            targetMet ->
                "Target met"

            else ->
                "Keep learning"
        }

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
                    10.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    "30-day manual review burden",
                    modifier =
                        Modifier.weight(1f),
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )
                SmallStatusPill(
                    text = status,
                    tone =
                        if (targetMet) {
                            "green"
                        } else {
                            "amber"
                        }
                )
            }

            Text(
                if (rate == null) {
                    "No ordinary spending is available in the current outcome window yet."
                } else {
                    String.format(
                        Locale.US,
                        "%.1f%% required manual category review",
                        rate
                    )
                },
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            HealthMetric(
                label =
                    "Ordinary spending transactions",
                value =
                    snapshot
                        .eligibleTransactionCount
            )
            HealthMetric(
                label =
                    "Needed manual category review",
                value =
                    snapshot
                        .manualReviewTransactionCount
            )
            HealthMetric(
                label =
                    "Eligible history days",
                value =
                    snapshot
                        .observedHistoryDays
            )

            Text(
                "This outcome counts ordinary spending that is still in Needs Review or was manually classified. Transfers, income, debt, savings, investments, reversals, excluded items and unresolved duplicate pairs are excluded.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                if (snapshot.mature) {
                    "Blueprint target: fewer than roughly 5% after a month of eligible history."
                } else {
                    "The blueprint target is not graded yet. PesaTrack will wait for " +
                        snapshot.windowDays +
                        " days of eligible history instead of treating a short sample as success."
                },
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SourceCoverageCard(
    source:
        FinancialSourceCoverageSnapshot,
    selected: Boolean,
    onInspect: () -> Unit
) {
    PesaCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled =
                        source
                            .unsupportedFinancial >
                        0,
                    onClick =
                        onInspect
                )
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
                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        source.displayName,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                    Text(
                        sourceChannelLabel(
                            source
                        ),
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
                SmallStatusPill(
                    source
                        .understoodPercent
                        .toString() +
                        "% understood",
                    if (
                        source
                            .unsupportedFinancial ==
                        0
                    ) {
                        "green"
                    } else {
                        "amber"
                    }
                )
            }
            Text(
                source.totalObservations
                    .toString() +
                    " observation" +
                    if (
                        source
                            .totalObservations ==
                        1
                    ) {
                        ""
                    } else {
                        "s"
                    } +
                    " • " +
                    source.templateParsed +
                    " template • " +
                    source.unsupportedFinancial +
                    " unsupported",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
            if (
                source.unsupportedFinancial >
                0
            ) {
                Text(
                    if (selected) {
                        "Filtering Message Lab below"
                    } else {
                        "Tap to inspect unsupported shapes"
                    },
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }
    }
}

private fun sourceChannelLabel(
    source:
        FinancialSourceCoverageSnapshot
): String {
    val channel =
        when (
            source.sourceKind
                .trim()
                .lowercase()
        ) {
            "financial_notification" ->
                "App notification"

            "historical_financial_message" ->
                "SMS / imported message"

            "mpesa_notification" ->
                "M-Pesa notification"

            "configured_notification" ->
                "Configured notification"

            else ->
                source.sourceKind
                    .replace(
                        "_",
                        " "
                    )
                    .ifBlank {
                        "Financial source"
                    }
        }

    return listOfNotNull(
        channel,
        source.sourcePackage
            ?.takeIf(
                String::isNotBlank
            )
    ).joinToString(" • ")
}

@Composable
internal fun HealthMetric(
    label: String,
    value: Int
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
        Text(
            value.toString(),
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )
    }
}
