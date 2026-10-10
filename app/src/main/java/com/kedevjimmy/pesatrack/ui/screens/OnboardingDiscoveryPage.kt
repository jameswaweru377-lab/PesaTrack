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
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingOwnershipChoice
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
    ownershipChoices:
        Map<String, OnboardingOwnershipChoice>,
    importResult: HistoricalImportResult?,
    applying: Boolean,
    onOwnershipChoice:
        (DiscoveredFinancialEntity, OnboardingOwnershipChoice) -> Unit,
    onContinue: () -> Unit,
    onAddMissingInstitution: () -> Unit = {}
) {
    var showOptions by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        SmartSetupHero(
            icon = Icons.Rounded.CheckCircle,
            title = "Here's what we found",
            body =
                "These accounts were mentioned in your messages. Confirm which accounts are yours; names alone do not prove ownership."
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

        Text(
            "Salary setup comes after confirming your accounts. You can skip it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextButton(
            onClick = { showOptions = !showOptions },
            enabled = !applying,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(if (showOptions) "Hide account options" else "Review account options")
        }
        if (
            showOptions && ownershipCandidates.isNotEmpty()
        ) {
            Text(
                "Which accounts belong to you?",
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
                "Mine confirms ownership. Not mine excludes a recipient's account. Unsure leaves ownership unchanged. You can skip this for now.",
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
                        choice =
                            ownershipChoices[entity.entityKey]
                                ?: OnboardingOwnershipChoice.UNSURE,
                        enabled = !applying,
                        onChoice = { choice ->
                            onOwnershipChoice(entity, choice)
                        }
                    )
                }
        }

        if (showOptions) {
            OutlinedButton(
                onClick = onAddMissingInstitution,
                enabled = !applying,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Add a missing bank or service")
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


    }
}
