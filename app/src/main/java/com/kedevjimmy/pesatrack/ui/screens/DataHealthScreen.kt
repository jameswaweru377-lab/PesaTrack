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
fun DataHealthScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onIdentityReviewClick: () -> Unit
) {
    val context =
        LocalContext.current
    val diagnosticsStore =
        remember {
            FinancialIngestionDiagnosticsStore
                .get(context)
        }
    val messageLabStore =
        remember {
            MessageLabStore.get(context)
        }
    val identityGraphStore =
        remember {
            IdentityGraphStore.get(context)
        }
    val personalLearningStore =
        remember {
            PersonalLearningStore.get(context)
        }

    val transactionStore =
        remember {
            TransactionStore.get(context)
        }

    val historyReprocessor =
        remember {
            FinancialHistoryReprocessor(
                context
            )
        }
    val reprocessStateStore =
        remember {
            FinancialHistoryReprocessStateStore(
                context
            )
        }
    val scope =
        rememberCoroutineScope()

    var coverage by remember {
        mutableStateOf(
            FinancialMessageCoverageSnapshot(
                totalFinancialObservations = 0,
                templateParsed = 0,
                genericFallbackParsed = 0,
                understoodNoLedgerEffect = 0,
                unsupportedFinancial = 0
            )
        )
    }
    var sourceCoverage by remember {
        mutableStateOf<
            List<FinancialSourceCoverageSnapshot>
        >(emptyList())
    }
    var unsupported by remember {
        mutableStateOf<
            List<UnsupportedMessageShape>
        >(emptyList())
    }
    var ignoredUnsupported by remember {
        mutableStateOf<
            List<UnsupportedMessageShape>
        >(emptyList())
    }
    var selectedCoverageSource by
        remember {
            mutableStateOf<
                FinancialSourceCoverageSnapshot?
            >(null)
        }
    var identitySnapshot by remember {
        mutableStateOf(
            IdentityGraphSnapshot(
                nodeCount = 0,
                aliasCount = 0,
                selfAliasCount = 0,
                pendingPromptCount = 0,
                unclassifiedIdentityCount = 0
            )
        )
    }
    var identityBackfillCount by remember {
        mutableStateOf(0)
    }
    var learningPreferenceCount by remember {
        mutableStateOf(0)
    }
    var learningApplicationCount by remember {
        mutableStateOf(0)
    }
    var retiredMessageLabCount by remember {
        mutableStateOf(0)
    }

    var reviewBurden by remember {
        mutableStateOf(
            ReviewBurdenSnapshot
                .empty()
        )
    }

    var selectedReprocessMonths by
        remember {
            mutableStateOf(1)
        }
    var reprocessingHistory by remember {
        mutableStateOf(false)
    }
    var reprocessReport by remember {
        mutableStateOf<
            FinancialHistoryReprocessReport?
        >(null)
    }
    var reprocessFreshness by remember {
        mutableStateOf(
            reprocessStateStore
                .snapshot()
        )
    }
    var reprocessError by remember {
        mutableStateOf<String?>(null)
    }
    var smsPermissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(
                Manifest.permission.READ_SMS
            ) ==
                PackageManager
                    .PERMISSION_GRANTED
        )
    }
    var smsPermissionDenied by remember {
        mutableStateOf(false)
    }

    var inspectorTitle by remember {
        mutableStateOf("")
    }
    var inspectorBody by remember {
        mutableStateOf("")
    }
    var messageInspection by remember {
        mutableStateOf<
            FinancialMessageInspection?
        >(null)
    }

    var inspectorFinancialEvidenceSaved by
        remember {
            mutableStateOf(false)
        }

    var inspectorSavedShapeId by
        remember {
            mutableStateOf<String?>(null)
        }

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) { granted ->
            smsPermissionGranted =
                granted
            smsPermissionDenied =
                !granted
        }

    suspend fun loadDataHealth():
        DataHealthLoadResult =
        withContext(
            Dispatchers.IO
        ) {
            val backfilled =
                identityGraphStore
                    .synchronize(
                        transactionLimit =
                            5_000
                    )
            DataHealthLoadResult(
                coverage =
                    diagnosticsStore
                        .coverageSnapshot(),
                sourceCoverage =
                    diagnosticsStore
                        .sourceCoverage(
                            sourceLimit = 8
                        ),
                unsupported =
                    messageLabStore
                        .prioritizedUnsupported(100),
                ignoredUnsupported =
                    messageLabStore
                        .recentIgnored(100),
                identitySnapshot =
                    identityGraphStore
                        .snapshot(),
                identityBackfillCount =
                    backfilled,
                learningPreferenceCount =
                    personalLearningStore
                        .preferenceCount(),
                learningApplicationCount =
                    personalLearningStore
                        .applicationCount(),
                retiredMessageLabCount =
                    messageLabStore
                        .retiredOccurrenceCount(),
                reviewBurden =
                    transactionStore
                        .reviewBurdenSnapshot(),
                reprocessFreshness =
                    reprocessStateStore
                        .snapshot()
            )
        }

    fun applyDataHealth(
        loaded: DataHealthLoadResult
    ) {
        coverage = loaded.coverage
        sourceCoverage =
            loaded.sourceCoverage
        unsupported = loaded.unsupported
        ignoredUnsupported =
            loaded.ignoredUnsupported
        identitySnapshot =
            loaded.identitySnapshot
        identityBackfillCount =
            loaded.identityBackfillCount
        learningPreferenceCount =
            loaded.learningPreferenceCount
        learningApplicationCount =
            loaded.learningApplicationCount
        retiredMessageLabCount =
            loaded.retiredMessageLabCount
        reviewBurden =
            loaded.reviewBurden
        reprocessFreshness =
            loaded.reprocessFreshness
    }

    fun runHistoryReprocess() {
        if (
            !smsPermissionGranted ||
            reprocessingHistory
        ) {
            return
        }

        reprocessingHistory = true
        reprocessError = null
        reprocessReport = null

        scope.launch {
            val outcome =
                runCatching {
                    withContext(
                        Dispatchers.IO
                    ) {
                        historyReprocessor
                            .reprocess(
                                selectedReprocessMonths
                            )
                    }
                }

            outcome.onSuccess {
                report ->
                reprocessReport =
                    report
                runCatching {
                    loadDataHealth()
                }.onSuccess(
                    ::applyDataHealth
                )
            }.onFailure {
                error ->
                reprocessError =
                    when (error) {
                        is SecurityException ->
                            "Android did not allow PesaTrack to read SMS history on this installation."

                        else ->
                            error.message
                                ?: "Financial history could not be reprocessed."
                    }
            }

            reprocessingHistory = false
        }
    }

    LaunchedEffect(Unit) {
        applyDataHealth(
            loadDataHealth()
        )
    }

    LazyColumn(
        modifier =
            Modifier
                .padding(scaffoldPadding)
                .background(
                    MaterialTheme
                        .colorScheme
                        .background
                ),
        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        Icons.AutoMirrored
                            .Rounded
                            .ArrowBack,
                        contentDescription =
                            "Back"
                    )
                }
                Column {
                    Text(
                        "Data Health",
                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )
                    Text(
                        "What PesaTrack understands, falls back on, or still needs to learn.",
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

        item {
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
                        "Message coverage",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )
                    Text(
                        coverage
                            .templateCoveragePercent
                            .toString() +
                            "% matched a Message Pack template",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )
                    HealthMetric(
                        label =
                            "Financial observations",
                        value =
                            coverage
                                .totalFinancialObservations
                    )
                    HealthMetric(
                        label =
                            "Template parsed",
                        value =
                            coverage
                                .templateParsed
                    )
                    HealthMetric(
                        label =
                            "V2 generic fallback",
                        value =
                            coverage
                                .genericFallbackParsed
                    )
                    HealthMetric(
                        label =
                            "Understood, no ledger effect",
                        value =
                            coverage
                                .understoodNoLedgerEffect
                    )
                    HealthMetric(
                        label =
                            "Resolved by current coverage",
                        value =
                            coverage
                                .resolvedByCurrentCoverage
                    )
                    HealthMetric(
                        label =
                            "Unsupported financial shapes",
                        value =
                            coverage
                                .unsupportedFinancial
                    )
                    HealthMetric(
                        label =
                            "Resolved Message Lab occurrences",
                        value =
                            retiredMessageLabCount
                    )
                    HealthMetric(
                        label =
                            "Ignored as non-financial",
                        value =
                            coverage
                                .ignoredNonFinancial
                    )
                }
            }
        }

        item {
            SectionHeader(
                "Reconcile existing history"
            )
        }

        item {
            HistoryReprocessCard(
                permissionGranted =
                    smsPermissionGranted,
                permissionDenied =
                    smsPermissionDenied,
                selectedMonths =
                    selectedReprocessMonths,
                freshness =
                    reprocessFreshness,
                reprocessing =
                    reprocessingHistory,
                report =
                    reprocessReport,
                error =
                    reprocessError,
                onMonthsSelected = {
                    selectedReprocessMonths =
                        it
                },
                onRequestPermission = {
                    smsPermissionDenied =
                        false
                    smsPermissionLauncher
                        .launch(
                            Manifest.permission
                                .READ_SMS
                        )
                },
                onReprocess =
                    ::runHistoryReprocess
            )
        }

        item {
            SectionHeader(
                "Inspect one message"
            )
        }

        item {
            val inspectorEvidenceShape =
                inspectorSavedShapeId
                    ?.let {
                        shapeId ->
                        unsupported
                            .firstOrNull {
                                it.shapeId ==
                                    shapeId
                            }
                    }

            MessageInspectorCard(
                title = inspectorTitle,
                body = inspectorBody,
                inspection =
                    messageInspection,
                evidenceShape =
                    inspectorEvidenceShape,
                onTitleChange = {
                    inspectorTitle = it
                    messageInspection = null
                    inspectorFinancialEvidenceSaved =
                        false
                    inspectorSavedShapeId =
                        null
                },
                onBodyChange = {
                    inspectorBody = it
                    messageInspection = null
                    inspectorFinancialEvidenceSaved =
                        false
                    inspectorSavedShapeId =
                        null
                },
                onAnalyze = {
                    if (
                        inspectorBody
                            .isNotBlank()
                    ) {
                        val sender =
                            inspectorTitle
                                .trim()
                                .takeIf(
                                    String::isNotBlank
                                )
                        val storedMatch =
                            messageLabStore
                                .inspectionMatch(
                                    safeSenderLabel =
                                        sender,
                                    body =
                                        inspectorBody
                                )
                        messageInspection =
                            FinancialMessageInspector
                                .inspect(
                                    title =
                                        sender,
                                    body =
                                        inspectorBody,
                                    savedUserDecision =
                                        storedMatch
                                            .shape
                                            ?.userDecision,
                                    savedSemanticLabel =
                                        storedMatch
                                            .shape
                                            ?.semanticLabel,
                                    savedDecisionAmbiguous =
                                        storedMatch
                                            .ambiguousSourceMatch
                                )
                        inspectorFinancialEvidenceSaved =
                            !storedMatch
                                .ambiguousSourceMatch &&
                                storedMatch
                                    .shape
                                    ?.userDecision ==
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                        inspectorSavedShapeId =
                            storedMatch
                                .shape
                                ?.takeIf {
                                    !storedMatch
                                        .ambiguousSourceMatch &&
                                        it.userDecision ==
                                        MessageLabUserDecision
                                            .CONFIRMED_FINANCIAL
                                }
                                ?.shapeId
                    }
                },
                onClear = {
                    inspectorTitle = ""
                    inspectorBody = ""
                    messageInspection = null
                    inspectorFinancialEvidenceSaved =
                        false
                    inspectorSavedShapeId =
                        null
                },
                financialEvidenceSaved =
                    inspectorFinancialEvidenceSaved,
                onAddFinancialEvidence = {
                    val inspection =
                        messageInspection
                    if (
                        inspection != null &&
                        inspection
                            .wouldEnterMessageLab &&
                        inspectorBody
                            .isNotBlank()
                    ) {
                        val result =
                            messageLabStore
                                .addManualConfirmedFinancialEvidence(
                                    safeSenderLabel =
                                        inspectorTitle
                                            .trim()
                                            .takeIf(
                                                String::isNotBlank
                                            ),
                                    body =
                                        inspectorBody,
                                    relevanceScore =
                                        inspection
                                            .relevanceScore,
                                    relevanceSignals =
                                        inspection
                                            .relevanceSignals
                                )
                        if (result != null) {
                            inspectorFinancialEvidenceSaved =
                                true
                            inspectorSavedShapeId =
                                result.shapeId
                            diagnosticsStore
                                .restoreShapeUnsupported(
                                    result.shapeId
                                )
                            unsupported =
                                messageLabStore
                                    .prioritizedUnsupported(
                                        100
                                    )
                            ignoredUnsupported =
                                messageLabStore
                                    .recentIgnored(100)
                            coverage =
                                diagnosticsStore
                                    .coverageSnapshot()
                            sourceCoverage =
                                diagnosticsStore
                                    .sourceCoverage(
                                        sourceLimit = 8
                                    )
                            messageInspection =
                                inspection.copy(
                                    savedUserDecision =
                                        MessageLabUserDecision
                                            .CONFIRMED_FINANCIAL,
                                    savedSemanticLabel =
                                        messageLabStore
                                            .semanticLabel(
                                                result.shapeId
                                            ),
                                    savedDecisionAmbiguous =
                                        false
                                )
                            Toast.makeText(
                                context,
                                if (
                                    result
                                        .reusedExistingShape
                                ) {
                                    "Existing Message Lab shape confirmed as financial"
                                } else {
                                    "Sanitized financial evidence saved"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onSemanticLabelSelected = {
                        label ->
                    inspectorSavedShapeId
                        ?.let {
                            shapeId ->
                            if (
                                messageLabStore
                                    .setSemanticLabel(
                                        shapeId,
                                        label
                                    )
                            ) {
                                unsupported =
                                    messageLabStore
                                        .prioritizedUnsupported(
                                            100
                                        )
                                messageInspection =
                                    messageInspection
                                        ?.copy(
                                            savedUserDecision =
                                                MessageLabUserDecision
                                                    .CONFIRMED_FINANCIAL,
                                            savedSemanticLabel =
                                                label,
                                            savedDecisionAmbiguous =
                                                false
                                        )
                                Toast.makeText(
                                    context,
                                    "Meaning saved: " +
                                        label.displayLabel,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                },
                onSemanticLabelCleared = {
                    inspectorSavedShapeId
                        ?.let {
                            shapeId ->
                            if (
                                messageLabStore
                                    .clearSemanticLabel(
                                        shapeId
                                    )
                            ) {
                                unsupported =
                                    messageLabStore
                                        .prioritizedUnsupported(
                                            100
                                        )
                                messageInspection =
                                    messageInspection
                                        ?.copy(
                                            savedSemanticLabel =
                                                null
                                        )
                            }
                        }
                },
                onImplementationReviewSelected = {
                        review ->
                    inspectorSavedShapeId
                        ?.let {
                            shapeId ->
                            if (
                                messageLabStore
                                    .setImplementationReview(
                                        shapeId,
                                        review
                                    )
                            ) {
                                unsupported =
                                    messageLabStore
                                        .prioritizedUnsupported(
                                            100
                                        )
                                Toast.makeText(
                                    context,
                                    review.displayLabel,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                },
                onImplementationReviewCleared = {
                    inspectorSavedShapeId
                        ?.let {
                            shapeId ->
                            if (
                                messageLabStore
                                    .clearImplementationReview(
                                        shapeId
                                    )
                            ) {
                                unsupported =
                                    messageLabStore
                                        .prioritizedUnsupported(
                                            100
                                        )
                            }
                        }
                },
                onCopyParserDraft = {
                    inspectorSavedShapeId
                        ?.let {
                            shapeId ->
                            messageLabStore
                                .exportParserDraft(
                                    shapeId
                                )
                                ?.let {
                                    draft ->
                                    val clipboard =
                                        context
                                            .getSystemService(
                                                Context
                                                    .CLIPBOARD_SERVICE
                                            ) as
                                            ClipboardManager
                                    clipboard
                                        .setPrimaryClip(
                                            ClipData
                                                .newPlainText(
                                                    "PesaTrack review-only parser draft",
                                                    draft
                                                )
                                        )
                                    Toast.makeText(
                                        context,
                                        "Review-only parser draft copied",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                },
                onCopyImplementationPacket = {
                    inspectorSavedShapeId
                        ?.let {
                            shapeId ->
                            messageLabStore
                                .exportImplementationWorkPacket(
                                    shapeId
                                )
                                ?.let {
                                    packet ->
                                    val clipboard =
                                        context
                                            .getSystemService(
                                                Context
                                                    .CLIPBOARD_SERVICE
                                            ) as
                                            ClipboardManager
                                    clipboard
                                        .setPrimaryClip(
                                            ClipData
                                                .newPlainText(
                                                    "PesaTrack parser implementation work packet",
                                                    packet
                                                )
                                        )
                                    Toast.makeText(
                                        context,
                                        "Implementation work packet copied",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                },
                onCopySanitized = {
                    messageInspection
                        ?.let {
                            inspection ->
                            val clipboard =
                                context
                                    .getSystemService(
                                        Context
                                            .CLIPBOARD_SERVICE
                                    ) as
                                    ClipboardManager
                            clipboard
                                .setPrimaryClip(
                                    ClipData
                                        .newPlainText(
                                            "PesaTrack sanitized message inspection",
                                            inspection
                                                .sanitizedExport()
                                        )
                                )
                            Toast.makeText(
                                context,
                                "Sanitized diagnostic copied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            )
        }

        item {
            SectionHeader(
                "Learning outcome"
            )
        }

        item {
            ReviewBurdenCard(
                snapshot =
                    reviewBurden
            )
        }

        item {
            SectionHeader(
                "Coverage by source"
            )
        }

        if (sourceCoverage.isEmpty()) {
            item {
                PesaCard(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No source-level financial observations yet. Import history or receive a supported financial notification to build this view.",
                        modifier =
                            Modifier.padding(
                                16.dp
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                count =
                    sourceCoverage.size,
                key = {
                    sourceCoverage[it]
                        .sourceKey
                }
            ) { index ->
                val source =
                    sourceCoverage[index]
                SourceCoverageCard(
                    source = source,
                    selected =
                        selectedCoverageSource
                            ?.sourceKey ==
                        source.sourceKey,
                    onInspect = {
                        if (
                            source
                                .unsupportedFinancial >
                            0
                        ) {
                            selectedCoverageSource =
                                source
                        }
                    }
                )
            }
        }

        item {
            PesaCard(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            16.dp
                        )
                ) {
                    Text(
                        "Kenya Message Pack v1",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                    Spacer(
                        Modifier.height(4.dp)
                    )
                    Text(
                        KenyaMessagePack
                            .templateCount
                            .toString() +
                            " active templates loaded",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                    if (
                        KenyaMessagePack
                            .loadErrors
                            .isNotEmpty()
                    ) {
                        Text(
                            KenyaMessagePack
                                .loadErrors
                                .joinToString(
                                    separator =
                                        "\n"
                                ),
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
                }
            }
        }

        item {
            SectionHeader(
                "Identity intelligence"
            )
        }

        item {
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
                        "Identity Graph",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )
                    Text(
                        "PesaTrack is building stable identities for your accounts, counterparties and aliases without guessing whether a repeated name is a person or business.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                    HealthMetric(
                        label = "Identity nodes",
                        value =
                            identitySnapshot
                                .nodeCount
                    )
                    HealthMetric(
                        label = "Known aliases",
                        value =
                            identitySnapshot
                                .aliasCount
                    )
                    HealthMetric(
                        label = "Confirmed self aliases",
                        value =
                            identitySnapshot
                                .selfAliasCount
                    )
                    HealthMetric(
                        label = "Questions waiting",
                        value =
                            identitySnapshot
                                .pendingPromptCount
                    )
                    HealthMetric(
                        label = "Still unclassified",
                        value =
                            identitySnapshot
                                .unclassifiedIdentityCount
                    )
                    HealthMetric(
                        label = "Learned category identities",
                        value =
                            learningPreferenceCount
                    )
                    HealthMetric(
                        label = "Auto-applied by learning",
                        value =
                            learningApplicationCount
                    )
                    Button(
                        onClick =
                            onIdentityReviewClick,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (
                                identitySnapshot
                                    .pendingPromptCount >
                                0
                            ) {
                                "Review " +
                                    identitySnapshot
                                        .pendingPromptCount +
                                    " identity question" +
                                    if (
                                        identitySnapshot
                                            .pendingPromptCount ==
                                        1
                                    ) {
                                        ""
                                    } else {
                                        "s"
                                    }
                            } else {
                                "Open Identity Review"
                            }
                        )
                    }

                    if (
                        identityBackfillCount > 0
                    ) {
                        Text(
                            "Learned from " +
                                identityBackfillCount +
                                " older transaction" +
                                (
                                    if (
                                        identityBackfillCount ==
                                        1
                                    ) "" else "s"
                                ) +
                                " during this scan.",
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

        item {
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
                    SectionHeader(
                        if (
                            selectedCoverageSource ==
                            null
                        ) {
                            "Message Lab"
                        } else {
                            "Message Lab • " +
                                selectedCoverageSource
                                    ?.displayName
                                    .orEmpty()
                        }
                    )
                    if (
                        selectedCoverageSource !=
                        null
                    ) {
                        Text(
                            "Showing only unsupported shapes from this source.",
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
                    selectedCoverageSource !=
                    null
                ) {
                    TextButton(
                        onClick = {
                            selectedCoverageSource =
                                null
                        }
                    ) {
                        Text("Show all")
                    }
                }
            }
        }

        val confirmedFinancialCount =
            unsupported.count {
                it.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL
            }
        val semanticallyLabeledCount =
            unsupported.count {
                it.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL &&
                    it.semanticLabel != null
            }
        val parserReadiness =
            MessageLabParserReadiness
                .snapshot(
                    unsupported
                )
        val implementationReview =
            MessageLabImplementationReview
                .snapshot(
                    unsupported
                )

        if (
            confirmedFinancialCount > 0
        ) {
            item {
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
                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    "Confirmed financial evidence",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )
                                Text(
                                    confirmedFinancialCount
                                        .toString() +
                                        " unsupported shape" +
                                        if (
                                            confirmedFinancialCount ==
                                            1
                                        ) {
                                            ""
                                        } else {
                                            "s"
                                        } +
                                        " prioritized • " +
                                        semanticallyLabeledCount +
                                        " meaning-labeled",
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
                            SmallStatusPill(
                                text =
                                    confirmedFinancialCount
                                        .toString(),
                                tone = "green"
                            )
                        }

                        Button(
                            onClick = {
                                val fixtures =
                                    messageLabStore
                                        .exportConfirmedFinancialFixtures(
                                            limit = 50
                                        )
                                if (
                                    fixtures != null
                                ) {
                                    val clipboard =
                                        context
                                            .getSystemService(
                                                Context
                                                    .CLIPBOARD_SERVICE
                                            ) as
                                            ClipboardManager
                                    clipboard
                                        .setPrimaryClip(
                                            ClipData
                                                .newPlainText(
                                                    "PesaTrack confirmed financial fixtures",
                                                    fixtures
                                                )
                                        )
                                    Toast.makeText(
                                        context,
                                        "Confirmed financial evidence copied",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Copy confirmed financial bundle"
                            )
                        }
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            HealthMetric(
                                label =
                                    "Need meaning",
                                value =
                                    parserReadiness
                                        .needsMeaningCount
                            )
                        }
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            HealthMetric(
                                label =
                                    "Evidence-ready",
                                value =
                                    parserReadiness
                                        .evidenceReadyCount
                            )
                        }
                        if (
                            parserReadiness
                                .evidenceReadyCount >
                            0
                        ) {
                            Button(
                                onClick = {
                                    val fixtures =
                                        messageLabStore
                                            .exportEvidenceReadyParserCandidates(
                                                limit = 50
                                            )
                                    if (
                                        fixtures != null
                                    ) {
                                        val clipboard =
                                            context
                                                .getSystemService(
                                                    Context
                                                        .CLIPBOARD_SERVICE
                                                ) as
                                                ClipboardManager
                                        clipboard
                                            .setPrimaryClip(
                                                ClipData
                                                    .newPlainText(
                                                        "PesaTrack evidence-ready parser candidates",
                                                        fixtures
                                                    )
                                            )
                                        Toast.makeText(
                                            context,
                                            "Evidence-ready bundle copied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Copy evidence-ready bundle"
                                )
                            }
                            Button(
                                onClick = {
                                    val drafts =
                                        messageLabStore
                                            .exportEvidenceReadyParserDrafts(
                                                limit = 50
                                            )
                                    if (
                                        drafts != null
                                    ) {
                                        val clipboard =
                                            context
                                                .getSystemService(
                                                    Context
                                                        .CLIPBOARD_SERVICE
                                                ) as
                                                ClipboardManager
                                        clipboard
                                            .setPrimaryClip(
                                                ClipData
                                                    .newPlainText(
                                                        "PesaTrack review-only parser drafts",
                                                        drafts
                                                    )
                                            )
                                        Toast.makeText(
                                            context,
                                            "Review-only parser drafts copied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Copy parser draft bundle"
                                )
                            }
                            Text(
                                "Drafts are review artifacts. Mark a candidate below as Ready for parser work only after checking the proposed mechanics.",
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
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            HealthMetric(
                                label =
                                    "Awaiting implementation review",
                                value =
                                    implementationReview
                                        .unreviewedCount
                            )
                        }
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            HealthMetric(
                                label =
                                    "Needs more examples",
                                value =
                                    implementationReview
                                        .needsMoreEvidenceCount
                            )
                        }
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            HealthMetric(
                                label =
                                    "Ready for parser work",
                                value =
                                    implementationReview
                                        .readyForParserWorkCount
                            )
                        }
                        if (
                            implementationReview
                                .readyForParserWorkCount >
                            0
                        ) {
                            Button(
                                onClick = {
                                    val fixtures =
                                        messageLabStore
                                            .exportImplementationReadyParserCandidates(
                                                limit = 50
                                            )
                                    if (
                                        fixtures != null
                                    ) {
                                        val clipboard =
                                            context
                                                .getSystemService(
                                                    Context
                                                        .CLIPBOARD_SERVICE
                                                ) as
                                                ClipboardManager
                                        clipboard
                                            .setPrimaryClip(
                                                ClipData
                                                    .newPlainText(
                                                        "PesaTrack implementation-ready parser candidates",
                                                        fixtures
                                                    )
                                            )
                                        Toast.makeText(
                                            context,
                                            "Implementation-ready bundle copied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Copy implementation-ready bundle"
                                )
                            }
                            Button(
                                onClick = {
                                    val packets =
                                        messageLabStore
                                            .exportImplementationReadyWorkPackets(
                                                limit = 50
                                            )
                                    if (
                                        packets != null
                                    ) {
                                        val clipboard =
                                            context
                                                .getSystemService(
                                                    Context
                                                        .CLIPBOARD_SERVICE
                                                ) as
                                                ClipboardManager
                                        clipboard
                                            .setPrimaryClip(
                                                ClipData
                                                    .newPlainText(
                                                        "PesaTrack parser implementation work packets",
                                                        packets
                                                    )
                                            )
                                        Toast.makeText(
                                            context,
                                            "Implementation work packets copied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Copy implementation work packets"
                                )
                            }
                            Text(
                                "Work packets combine the approved representative, all active family variants, the review-only parser draft, and the safety/test checklist needed before production activation.",
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
            }
        }

        val visibleUnsupported =
            selectedCoverageSource
                ?.let { selected ->
                    unsupported.filter {
                        FinancialCoverageAnalysis
                            .sourceKey(
                                sourceKind =
                                    it.sourceKind,
                                sourcePackage =
                                    it.sourcePackage,
                                displayName =
                                    it.sender
                            ) ==
                            selected.sourceKey
                    }
                }
                ?: unsupported.take(25)

        if (
            selectedCoverageSource !=
                null &&
            visibleUnsupported
                .isNotEmpty()
        ) {
            item {
                Button(
                    onClick = {
                        val source =
                            selectedCoverageSource
                                ?: return@Button
                        val fixtures =
                            messageLabStore
                                .exportDeveloperFixturesForSource(
                                    sourceKey =
                                        source.sourceKey,
                                    limit = 25
                                )
                        if (
                            fixtures != null
                        ) {
                            val clipboard =
                                context
                                    .getSystemService(
                                        Context
                                            .CLIPBOARD_SERVICE
                                    ) as
                                    ClipboardManager
                            clipboard
                                .setPrimaryClip(
                                    ClipData
                                        .newPlainText(
                                            "PesaTrack source fixtures",
                                            fixtures
                                        )
                                )
                            Toast.makeText(
                                context,
                                "Source fixtures copied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Copy redacted source fixtures"
                    )
                }
            }
        }

        if (visibleUnsupported.isEmpty()) {
            item {
                PesaCard(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No unsupported financial message shapes are stored right now. Unknown financial messages will appear here only after local redaction.",
                        modifier =
                            Modifier.padding(
                                16.dp
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                count =
                    visibleUnsupported.size,
                key = {
                    visibleUnsupported[it]
                        .shapeId
                }
            ) { index ->
                val shape =
                    visibleUnsupported[index]
                UnsupportedShapeCard(
                    shape = shape,
                    onConfirmFinancial = {
                        messageLabStore
                            .setUserDecision(
                                shape.shapeId,
                                MessageLabUserDecision
                                    .CONFIRMED_FINANCIAL
                            )
                        diagnosticsStore
                            .restoreShapeUnsupported(
                                shape.shapeId
                            )
                        unsupported =
                            messageLabStore
                                .prioritizedUnsupported(100)
                        ignoredUnsupported =
                            messageLabStore
                                .recentIgnored(100)
                        coverage =
                            diagnosticsStore
                                .coverageSnapshot()
                        sourceCoverage =
                            diagnosticsStore
                                .sourceCoverage(
                                    sourceLimit = 8
                                )
                        Toast.makeText(
                            context,
                            "Kept as financial evidence",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSemanticLabelSelected = {
                            label ->
                        messageLabStore
                            .setSemanticLabel(
                                shape.shapeId,
                                label
                            )
                        unsupported =
                            messageLabStore
                                .prioritizedUnsupported(100)
                        Toast.makeText(
                            context,
                            "Meaning saved: " +
                                label.displayLabel,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSemanticLabelCleared = {
                        messageLabStore
                            .clearSemanticLabel(
                                shape.shapeId
                            )
                        unsupported =
                            messageLabStore
                                .prioritizedUnsupported(100)
                    },
                    onImplementationReviewSelected = {
                            review ->
                        messageLabStore
                            .setImplementationReview(
                                shape.shapeId,
                                review
                            )
                        unsupported =
                            messageLabStore
                                .prioritizedUnsupported(100)
                        Toast.makeText(
                            context,
                            review.displayLabel,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onImplementationReviewCleared = {
                        messageLabStore
                            .clearImplementationReview(
                                shape.shapeId
                            )
                        unsupported =
                            messageLabStore
                                .prioritizedUnsupported(100)
                    },
                    onIgnoreNonFinancial = {
                        messageLabStore
                            .setUserDecision(
                                shape.shapeId,
                                MessageLabUserDecision
                                    .NOT_FINANCIAL
                            )
                        diagnosticsStore
                            .markShapeIgnored(
                                shape.shapeId
                            )
                        unsupported =
                            messageLabStore
                                .prioritizedUnsupported(100)
                        ignoredUnsupported =
                            messageLabStore
                                .recentIgnored(100)
                        coverage =
                            diagnosticsStore
                                .coverageSnapshot()
                        sourceCoverage =
                            diagnosticsStore
                                .sourceCoverage(
                                    sourceLimit = 8
                                )
                        Toast.makeText(
                            context,
                            "This shape will be ignored as non-financial",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onClearDecision = {
                        messageLabStore
                            .clearUserDecision(
                                shape.shapeId
                            )
                        diagnosticsStore
                            .restoreShapeUnsupported(
                                shape.shapeId
                            )
                        unsupported =
                            messageLabStore
                                .prioritizedUnsupported(100)
                        ignoredUnsupported =
                            messageLabStore
                                .recentIgnored(100)
                        coverage =
                            diagnosticsStore
                                .coverageSnapshot()
                        sourceCoverage =
                            diagnosticsStore
                                .sourceCoverage(
                                    sourceLimit = 8
                                )
                    },
                    onCopyParserDraft = {
                        val draft =
                            messageLabStore
                                .exportParserDraft(
                                    shape.shapeId
                                )
                        if (
                            draft != null
                        ) {
                            val clipboard =
                                context.getSystemService(
                                    Context
                                        .CLIPBOARD_SERVICE
                                ) as
                                    ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData
                                    .newPlainText(
                                        "PesaTrack review-only parser draft",
                                        draft
                                    )
                            )
                            Toast.makeText(
                                context,
                                "Review-only parser draft copied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onCopyFixture = {
                        val fixture =
                            messageLabStore
                                .exportDeveloperFixture(
                                    shape.shapeId
                                )
                        if (
                            fixture != null
                        ) {
                            val clipboard =
                                context.getSystemService(
                                    Context
                                        .CLIPBOARD_SERVICE
                                ) as
                                    ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData
                                    .newPlainText(
                                        "PesaTrack redacted fixture",
                                        fixture
                                    )
                            )
                            Toast.makeText(
                                context,
                                "Redacted fixture copied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }

        if (ignoredUnsupported.isNotEmpty()) {
            item {
                SectionHeader(
                    "Ignored non-financial shapes"
                )
            }

            item {
                Text(
                    "These redacted message shapes were explicitly marked as non-financial. They no longer count as financial coverage failures. You can undo any decision.",
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

            items(
                count =
                    ignoredUnsupported
                        .take(10)
                        .size,
                key = {
                    "ignored-" +
                        ignoredUnsupported[it]
                            .shapeId
                }
            ) { index ->
                val shape =
                    ignoredUnsupported[index]
                IgnoredShapeCard(
                    shape = shape,
                    onUndo = {
                        messageLabStore
                            .clearUserDecision(
                                shape.shapeId
                            )
                        diagnosticsStore
                            .restoreShapeUnsupported(
                                shape.shapeId
                            )
                        unsupported =
                            messageLabStore
                                .prioritizedUnsupported(100)
                        ignoredUnsupported =
                            messageLabStore
                                .recentIgnored(100)
                        coverage =
                            diagnosticsStore
                                .coverageSnapshot()
                        sourceCoverage =
                            diagnosticsStore
                                .sourceCoverage(
                                    sourceLimit = 8
                                )
                    }
                )
            }
        }
    }
}

private data class DataHealthLoadResult(
    val coverage:
        FinancialMessageCoverageSnapshot,
    val sourceCoverage:
        List<FinancialSourceCoverageSnapshot>,
    val unsupported:
        List<UnsupportedMessageShape>,
    val ignoredUnsupported:
        List<UnsupportedMessageShape>,
    val identitySnapshot:
        IdentityGraphSnapshot,
    val identityBackfillCount: Int,
    val learningPreferenceCount: Int,
    val learningApplicationCount: Int,
    val retiredMessageLabCount: Int,
    val reviewBurden:
        ReviewBurdenSnapshot,
    val reprocessFreshness:
        FinancialHistoryReprocessFreshness
)

@Composable
private fun MessageInspectorCard(
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
private fun HistoryReprocessCard(
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
private fun ReviewBurdenCard(
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
private fun SourceCoverageCard(
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
private fun HealthMetric(
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

@Composable
private fun UnsupportedShapeCard(
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
            MessageLabRelevanceExplanation(
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
private fun MessageLabRelevanceExplanation(
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
private fun IgnoredShapeCard(
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
            MessageLabRelevanceExplanation(
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
