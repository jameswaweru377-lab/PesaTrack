package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

@Composable
internal fun ReviewDiscoveryPage(
    sources: List<DetectedSetupSource>,
    ownershipCandidates:
        List<DiscoveredFinancialEntity>,
    selectedOwnershipEntityKeys:
        Set<String>,
    salarySuggestion:
        OnboardingSalarySuggestion?,
    salaryAccepted: Boolean,
    importResult: HistoricalImportResult?,
    applying: Boolean,
    onSalaryAccepted: (Boolean) -> Unit,
    onToggleAccount:
        (DiscoveredFinancialEntity) -> Unit,
    onManualSalary: () -> Unit,
    onContinue: () -> Unit
) {
    var showOptions by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        SmartSetupHero(
            icon = Icons.Rounded.CheckCircle,
            title = "Here's what we found",
            body =
                "Your transactions are organized automatically. You can start now and add personal details later."
        )

        if (sources.isEmpty()) {
            PesaCard {
                Column(
                    Modifier.padding(18.dp)
                ) {
                    Text(
                        "No money accounts confirmed yet",
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                    )
                    Text(
                        if (importResult == null) {
                            "That's fine. PesaTrack can start empty and learn from future financial activity."
                        } else {
                            "PesaTrack imported your history but did not find enough strong sender evidence to safely name an account."
                        },
                        modifier =
                            Modifier.padding(top = 5.dp),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        } else {
            sources.forEach { source ->
                DiscoveredSourceCard(source)
            }
        }

        if (salarySuggestion != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSalaryAccepted(
                            !salaryAccepted
                        )
                    },
                shape =
                    RoundedCornerShape(22.dp),
                color =
                    if (salaryAccepted) {
                        pesaToneContainer("amber")
                    } else {
                        MaterialTheme.colorScheme
                            .surface
                    }
            ) {
                Row(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    IconBubble(
                        Icons.Rounded.Payments,
                        size = 46.dp,
                        background =
                            pesaToneContainer(
                                "amber"
                            ),
                        tint =
                            pesaToneContent("amber")
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            "Possible salary",
                            style =
                                MaterialTheme.typography
                                    .labelLarge,
                            color =
                                MaterialTheme.colorScheme
                                    .primary
                        )
                        Text(
                            salarySuggestion.label,
                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                        )
                        Text(
                            salarySuggestion
                                .monthCount
                                .toString() +
                                " months • recent " +
                                formatSetupMoney(
                                    salarySuggestion
                                        .representativeAmountMinor
                                ),
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                    Icon(
                        if (salaryAccepted) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.Payments
                        },
                        contentDescription =
                            if (salaryAccepted) {
                                "Marked as salary"
                            } else {
                                "Not marked as salary"
                            },
                        tint =
                            if (salaryAccepted) {
                                MaterialTheme.colorScheme
                                    .primary
                            } else {
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                            }
                    )
                }
            }

            Text(
                "Tap the salary card if this is not your pay. PesaTrack creates the salary rule only after your confirmation.",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        } else if (showOptions) {
            PesaCard {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    IconBubble(
                        Icons.Rounded.Payments,
                        size = 44.dp
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(
                                start = 11.dp
                            )
                    ) {
                        Text(
                            "Salary wasn't found",
                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                        )
                        Text(
                            "Salary setup is optional. Add it later if you want planning around payday; accounts and spending work without it.",
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = onContinue,
            enabled = !applying,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape =
                RoundedCornerShape(18.dp)
        ) {
            if (applying) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    "Saving accounts…",
                    modifier =
                        Modifier.padding(
                            start = 10.dp
                        )
                )
            } else {
                Text("Continue")
            }
        }

        TextButton(
            onClick = { showOptions = !showOptions },
            enabled = !applying,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(if (showOptions) "Hide optional setup" else "Personalize accounts and salary (optional)")
        }
        if (
            showOptions && ownershipCandidates.isNotEmpty()
        ) {
            Text(
                "Confirm accounts you recognize",
                style =
                    MaterialTheme.typography
                        .titleMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurface,
                fontWeight =
                    FontWeight.SemiBold
            )
            Text(
                "PesaTrack can identify a bank or savings product without assuming it belongs to you. Confirm only accounts you actually use; anything you skip can be reviewed later in Activity.",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            ownershipCandidates
                .forEach { entity ->
                    OwnershipCandidateCard(
                        entity = entity,
                        selected =
                            entity.entityKey in
                                selectedOwnershipEntityKeys,
                        enabled = !applying,
                        onToggle = {
                            onToggleAccount(
                                entity
                            )
                        }
                    )
                }
        }

        if (
            showOptions && (salarySuggestion == null || !salaryAccepted)
        ) {
            OutlinedButton(
                onClick = onManualSalary,
                enabled = !applying,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                shape =
                    RoundedCornerShape(18.dp)
            ) {
                Text("Set up salary manually")
            }
        }

    }
}

