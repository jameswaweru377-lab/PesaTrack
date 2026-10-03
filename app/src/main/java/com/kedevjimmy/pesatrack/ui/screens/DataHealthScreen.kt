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
import com.kedevjimmy.pesatrack.ingestion.FinancialHistoryReprocessReport
import com.kedevjimmy.pesatrack.ingestion.FinancialHistoryReprocessor
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageCoverageSnapshot
import com.kedevjimmy.pesatrack.ingestion.FinancialSourceCoverageSnapshot
import com.kedevjimmy.pesatrack.ingestion.MessageLabStore
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
                        .recentUnsupported(100),
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
                        .reviewBurdenSnapshot()
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
                                .recentUnsupported(100)
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
                                .recentUnsupported(100)
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
                                .recentUnsupported(100)
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
                                .recentUnsupported(100)
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
        ReviewBurdenSnapshot
)

@Composable
private fun HistoryReprocessCard(
    permissionGranted: Boolean,
    permissionDenied: Boolean,
    selectedMonths: Int,
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
            Text(
                "Reprocess financial history",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
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
    onIgnoreNonFinancial: () -> Unit,
    onClearDecision: () -> Unit,
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
}
