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
internal fun DataHealthHistoryReprocessCard(
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
                DataHealthMetric(
                    label = "SMS scanned",
                    value =
                        result
                            .importResult
                            .scanned
                )
                DataHealthMetric(
                    label =
                        "Understood by current brain",
                    value =
                        result
                            .importResult
                            .understood
                )
                DataHealthMetric(
                    label =
                        "New ledger/events added",
                    value =
                        result
                            .importResult
                            .inserted
                )
                DataHealthMetric(
                    label =
                        "Existing duplicates preserved",
                    value =
                        result
                            .importResult
                            .duplicates
                )
                DataHealthMetric(
                    label =
                        "Existing state refreshed",
                    value =
                        result
                            .importResult
                            .stateRefreshed
                )
                DataHealthMetric(
                    label =
                        "Pending review events",
                    value =
                        result
                            .importResult
                            .pendingReview
                )
                DataHealthMetric(
                    label =
                        "Not applied",
                    value =
                        result
                            .importResult
                            .ignored
                )
                DataHealthMetric(
                    label =
                        "Old unsupported occurrences cleared",
                    value =
                        result
                            .retiredUnsupportedOccurrences
                )

                Text(
                    "Debt outstanding: " +
                        dataHealthHistoryFormatKesMinor(
                            result
                                .totalOutstandingBeforeMinor
                        ) +
                        " → " +
                        dataHealthHistoryFormatKesMinor(
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
                                    dataHealthHistoryFormatKesMinor(
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

private fun dataHealthHistoryFormatKesMinor(
    amountMinor: Long
): String =
    String.format(
        Locale.US,
        "KES %,.2f",
        amountMinor / 100.0
    )
