package com.kedevjimmy.pesatrack.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.IdentityGraphStore
import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphSnapshot
import com.kedevjimmy.pesatrack.domain.messagepack.KenyaMessagePack
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageCoverageSnapshot
import com.kedevjimmy.pesatrack.ingestion.MessageLabStore
import com.kedevjimmy.pesatrack.ingestion.UnsupportedMessageShape
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var unsupported by remember {
        mutableStateOf<
            List<UnsupportedMessageShape>
        >(emptyList())
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

    LaunchedEffect(Unit) {
        val loaded =
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
                    unsupported =
                        messageLabStore
                            .recentUnsupported(25),
                    identitySnapshot =
                        identityGraphStore
                            .snapshot(),
                    identityBackfillCount =
                        backfilled
                )
            }
        coverage = loaded.coverage
        unsupported = loaded.unsupported
        identitySnapshot =
            loaded.identitySnapshot
        identityBackfillCount =
            loaded.identityBackfillCount
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
                            "Unsupported financial shapes",
                        value =
                            coverage
                                .unsupportedFinancial
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
            SectionHeader(
                "Message Lab"
            )
        }

        if (unsupported.isEmpty()) {
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
                    unsupported.size,
                key = {
                    unsupported[it]
                        .shapeId
                }
            ) { index ->
                val shape =
                    unsupported[index]
                UnsupportedShapeCard(
                    shape = shape,
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
    }
}

private data class DataHealthLoadResult(
    val coverage:
        FinancialMessageCoverageSnapshot,
    val unsupported:
        List<UnsupportedMessageShape>,
    val identitySnapshot:
        IdentityGraphSnapshot,
    val identityBackfillCount: Int
)

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
            Text(
                shape.sender,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )
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
            Button(
                onClick =
                    onCopyFixture
            ) {
                Text(
                    "Copy redacted fixture"
                )
            }
        }
    }
}
