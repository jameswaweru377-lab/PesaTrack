package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.IdentityGraphStore
import com.kedevjimmy.pesatrack.domain.identity.IdentityNodeKind
import com.kedevjimmy.pesatrack.domain.identity.IdentityReviewPrompt
import com.kedevjimmy.pesatrack.domain.identity.IdentityReviewPromptType
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun IdentityReviewScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context =
        LocalContext.current
    val store =
        remember {
            IdentityGraphStore.get(context)
        }
    val scope =
        rememberCoroutineScope()

    var prompts by remember {
        mutableStateOf<
            List<IdentityReviewPrompt>
        >(emptyList())
    }
    var loading by remember {
        mutableStateOf(true)
    }
    var busyKey by remember {
        mutableStateOf<String?>(null)
    }
    var errorText by remember {
        mutableStateOf<String?>(null)
    }
    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(refreshKey) {
        loading = true
        prompts =
            withContext(Dispatchers.IO) {
                store.pendingReviewPrompts(
                    limit = 50
                )
            }
        loading = false
    }

    fun performDecision(
        prompt: IdentityReviewPrompt,
        action: () -> Boolean
    ) {
        val key =
            prompt.promptType.name +
                ":" +
                prompt.identityId
        if (busyKey != null) {
            return
        }

        busyKey = key
        errorText = null
        scope.launch {
            val result =
                withContext(
                    Dispatchers.IO
                ) {
                    runCatching(action)
                }

            result.onSuccess {
                saved ->
                if (saved) {
                    refreshKey += 1
                } else {
                    errorText =
                        "PesaTrack could not save that identity decision. Please try again."
                }
            }.onFailure {
                errorText =
                    "PesaTrack could not save that identity decision. Please try again."
            }
            busyKey = null
        }
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
                        "Identity Review",
                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )
                    Text(
                        "Check your names and numbers. Other labels are optional.",
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

        errorText?.let {
            item {
                PesaCard(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        it,
                        modifier =
                            Modifier.padding(
                                16.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }
        }

        if (loading) {
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalArrangement =
                        Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (prompts.isEmpty()) {
            item {
                PesaCard(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            )
                    ) {
                        Text(
                            "No identity questions right now",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )
                        Text(
                            "PesaTrack will ask only when it has enough evidence. Unknown names stay unknown until you confirm them.",
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
            }
        } else {
            items(
                items = prompts,
                key = {
                    it.promptType.name +
                        ":" +
                        it.identityId
                }
            ) {
                prompt ->
                val key =
                    prompt.promptType.name +
                        ":" +
                        prompt.identityId
                IdentityReviewCard(
                    prompt = prompt,
                    busy =
                        busyKey == key,
                    disabled =
                        busyKey != null,
                    onConfirmSelf = {
                        performDecision(
                            prompt
                        ) {
                            store.confirmIdentityAsSelf(
                                prompt.identityId
                            )
                        }
                    },
                    onRejectSelf = {
                        performDecision(
                            prompt
                        ) {
                            store.rejectIdentityAsSelf(
                                prompt
                            )
                        }
                    },
                    onClassify = {
                        kind ->
                        performDecision(
                            prompt
                        ) {
                            store.classifyIdentity(
                                identityId =
                                    prompt.identityId,
                                kind = kind
                            )
                        }
                    },
                    onDefer = {
                        performDecision(
                            prompt
                        ) {
                            store.deferReviewPrompt(
                                prompt
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun IdentityReviewCard(
    prompt: IdentityReviewPrompt,
    busy: Boolean,
    disabled: Boolean,
    onConfirmSelf: () -> Unit,
    onRejectSelf: () -> Unit,
    onClassify: (
        IdentityNodeKind
    ) -> Unit,
    onDefer: () -> Unit
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
                when (
                    prompt.promptType
                ) {
                    IdentityReviewPromptType
                        .CONFIRM_SELF_PHONE ->
                        "Is this your number?"

                    IdentityReviewPromptType
                        .CONFIRM_SELF_NAME ->
                        "Is this your name?"

                    IdentityReviewPromptType
                        .CLASSIFY_COUNTERPARTY ->
                        "How would you label this recipient?"
                },
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Text(
                prompt.displayLabel,
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            Text(
                when (prompt.promptType) {
                    IdentityReviewPromptType.CONFIRM_SELF_PHONE,
                    IdentityReviewPromptType.CONFIRM_SELF_NAME -> "Seen in a possible own-account transfer. Confirm only if it is yours."
                    IdentityReviewPromptType.CLASSIFY_COUNTERPARTY -> prompt.reason
                },
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
                "Seen in " +
                    prompt.observationCount +
                    if (
                        prompt.observationCount ==
                        1
                    ) {
                        " transaction"
                    } else {
                        " transactions"
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

            if (
                prompt.promptType ==
                    IdentityReviewPromptType
                        .CLASSIFY_COUNTERPARTY
            ) {
                IdentityClassificationButtons(
                    enabled =
                        !disabled,
                    onClassify =
                        onClassify
                )
            } else {
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
                            onConfirmSelf,
                        enabled =
                            !disabled,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            if (busy) {
                                "Saving…"
                            } else {
                                "Yes"
                            }
                        )
                    }
                    OutlinedButton(
                        onClick =
                            onRejectSelf,
                        enabled =
                            !disabled,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("No")
                    }
                }
            }

            TextButton(
                onClick = onDefer,
                enabled = !disabled
            ) {
                Text("Not now")
            }
        }
    }
}

@Composable
private fun IdentityClassificationButtons(
    enabled: Boolean,
    onClassify: (
        IdentityNodeKind
    ) -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {
            OutlinedButton(
                onClick = {
                    onClassify(
                        IdentityNodeKind.PERSON
                    )
                },
                enabled = enabled,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text("Person")
            }
            OutlinedButton(
                onClick = {
                    onClassify(
                        IdentityNodeKind.BUSINESS
                    )
                },
                enabled = enabled,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text("Business")
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
            OutlinedButton(
                onClick = {
                    onClassify(
                        IdentityNodeKind.BILLER
                    )
                },
                enabled = enabled,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text("Biller")
            }
            OutlinedButton(
                onClick = {
                    onClassify(
                        IdentityNodeKind
                            .FINANCIAL_INSTITUTION
                    )
                },
                enabled = enabled,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    "Financial institution"
                )
            }
        }
    }
}
