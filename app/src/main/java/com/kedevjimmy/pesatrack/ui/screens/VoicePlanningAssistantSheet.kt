package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.data.local.VoicePlanningRepository
import com.kedevjimmy.pesatrack.domain.planning.VoiceBillDraft
import com.kedevjimmy.pesatrack.domain.planning.VoiceGoalDraft
import com.kedevjimmy.pesatrack.domain.planning.VoicePlanDraft
import com.kedevjimmy.pesatrack.domain.planning.VoicePlanKind
import com.kedevjimmy.pesatrack.domain.planning.VoicePlanningParser
import com.kedevjimmy.pesatrack.planning.BillCadence
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoicePlanningAssistantSheet(
    kind: VoicePlanKind,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context =
        LocalContext.current
    val scope =
        rememberCoroutineScope()
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    val repository =
        remember {
            VoicePlanningRepository(context)
        }

    var transcript by remember {
        mutableStateOf("")
    }
    var draft by remember {
        mutableStateOf<VoicePlanDraft?>(
            null
        )
    }
    var status by remember {
        mutableStateOf(
            "Tap the microphone and speak naturally."
        )
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var listening by remember {
        mutableStateOf(false)
    }
    var saving by remember {
        mutableStateOf(false)
    }
    var recognizer by remember {
        mutableStateOf<SpeechRecognizer?>(
            null
        )
    }
    var startRequest by remember {
        mutableIntStateOf(0)
    }

    val offlineAvailable =
        remember {
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S &&
                onDeviceSpeechAvailable(
                    context
                )
        }

    DisposableEffect(
        offlineAvailable
    ) {
        if (!offlineAvailable) {
            onDispose { }
        } else {
            val local =
                createOnDeviceRecognizer(
                    context
                )
            recognizer = local
            local.setRecognitionListener(
                object :
                    RecognitionListener {
                    override fun onReadyForSpeech(
                        params: Bundle?
                    ) {
                        listening = true
                        error = null
                        status =
                            "Listening…"
                    }

                    override fun onBeginningOfSpeech() {
                        status =
                            "Listening…"
                    }

                    override fun onRmsChanged(
                        rmsdB: Float
                    ) = Unit

                    override fun onBufferReceived(
                        buffer: ByteArray?
                    ) = Unit

                    override fun onEndOfSpeech() {
                        status =
                            "Processing speech…"
                    }

                    override fun onError(
                        errorCode: Int
                    ) {
                        listening = false
                        error =
                            speechErrorMessage(
                                errorCode
                            )
                        status =
                            "Tap the microphone to try again."
                    }

                    override fun onResults(
                        results: Bundle?
                    ) {
                        listening = false
                        val heard =
                            results
                                ?.getStringArrayList(
                                    SpeechRecognizer
                                        .RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()
                                .orEmpty()
                        if (
                            heard.isNotBlank()
                        ) {
                            transcript = heard
                            draft = null
                            error = null
                            status =
                                "Review what I heard, then tap Send."
                        } else {
                            error =
                                "I couldn't hear a complete command."
                        }
                    }

                    override fun onPartialResults(
                        partialResults: Bundle?
                    ) {
                        partialResults
                            ?.getStringArrayList(
                                SpeechRecognizer
                                    .RESULTS_RECOGNITION
                            )
                            ?.firstOrNull()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let {
                                transcript = it
                            }
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) = Unit
                }
            )

            onDispose {
                runCatching {
                    local.cancel()
                }
                runCatching {
                    local.destroy()
                }
                recognizer = null
            }
        }
    }

    LaunchedEffect(
        startRequest,
        recognizer
    ) {
        if (
            startRequest <= 0 ||
            recognizer == null
        ) {
            return@LaunchedEffect
        }

        transcript = ""
        draft = null
        error = null
        status =
            "Starting offline speech recognition…"
        recognizer?.startListening(
            offlineRecognitionIntent()
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) {
            granted ->
            if (granted) {
                startRequest++
            } else {
                error =
                    "Microphone permission is required for voice add."
            }
        }

    fun startVoice() {
        if (!offlineAvailable) {
            error =
                "Offline speech recognition isn't available on this phone. PesaTrack won't fall back to cloud speech."
            return
        }

        val granted =
            ContextCompat
                .checkSelfPermission(
                    context,
                    Manifest.permission
                        .RECORD_AUDIO
                ) ==
                PackageManager
                    .PERMISSION_GRANTED
        if (granted) {
            startRequest++
        } else {
            permissionLauncher.launch(
                Manifest.permission
                    .RECORD_AUDIO
            )
        }
    }

    fun sendTranscript() {
        val parsed =
            VoicePlanningParser.parse(
                kind = kind,
                transcript =
                    transcript
            )
        draft = parsed.draft
        error = parsed.error
        if (parsed.draft != null) {
            status =
                "I understood this. Confirm before anything is added."
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (
                !saving
            ) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor =
            MaterialTheme
                .colorScheme
                .surface,
        shape =
            RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 4.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    14.dp
                )
        ) {
            Text(
                if (
                    kind ==
                    VoicePlanKind.GOAL
                ) {
                    "Add goal by voice"
                } else {
                    "Add bill by voice"
                },
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                "Your voice is processed with Android's on-device speech recognizer. PesaTrack does not send this command to an AI service.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        20.dp
                    ),
                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            16.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (
                                listening
                            ) {
                                recognizer
                                    ?.stopListening()
                            } else {
                                startVoice()
                            }
                        },
                        modifier =
                            Modifier.size(
                                76.dp
                            ),
                        shape =
                            CircleShape,
                        contentPadding =
                            androidx.compose.foundation.layout
                                .PaddingValues(
                                    0.dp
                                ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        if (
                                            listening
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .secondary
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        }
                                )
                    ) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription =
                                if (
                                    listening
                                ) {
                                    "Stop listening"
                                } else {
                                    "Start voice command"
                                },
                            modifier =
                                Modifier.size(
                                    32.dp
                                )
                        )
                    }

                    Text(
                        status,
                        modifier =
                            Modifier.padding(
                                top = 10.dp
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

            if (
                transcript.isNotBlank()
            ) {
                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(
                                alpha = .55f
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                14.dp
                            )
                    ) {
                        Text(
                            "I heard",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                        Text(
                            transcript,
                            modifier =
                                Modifier.padding(
                                    top = 4.dp
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyLarge,
                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick =
                        ::sendTranscript,
                    enabled =
                        !listening &&
                            !saving,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.Send,
                        contentDescription =
                            null
                    )
                    Spacer(
                        Modifier.width(
                            8.dp
                        )
                    )
                    Text("Send")
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

            draft?.let {
                parsed ->
                VoicePlanConfirmationCard(
                    draft = parsed
                )

                Button(
                    onClick = {
                        if (saving) {
                            return@Button
                        }
                        saving = true
                        error = null
                        scope.launch {
                            try {
                                val saved =
                                    withContext(
                                        Dispatchers.IO
                                    ) {
                                        repository
                                            .save(
                                                parsed
                                            )
                                    }
                                onSaved(
                                    saved.displayName
                                )
                                onDismiss()
                            } catch (
                                cancelled:
                                    kotlinx.coroutines
                                        .CancellationException
                            ) {
                                throw cancelled
                            } catch (
                                failure: Exception
                            ) {
                                error =
                                    failure.message
                                        ?: "Couldn't add this yet."
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    18.dp
                                ),
                            strokeWidth =
                                2.dp
                        )
                        Spacer(
                            Modifier.width(
                                8.dp
                            )
                        )
                    } else {
                        Icon(
                            Icons.Rounded
                                .CheckCircle,
                            contentDescription =
                                null
                        )
                        Spacer(
                            Modifier.width(
                                8.dp
                            )
                        )
                    }
                    Text(
                        if (
                            saving
                        ) {
                            "Adding…"
                        } else if (
                            kind ==
                            VoicePlanKind.GOAL
                        ) {
                            "Confirm & add goal"
                        } else {
                            "Confirm & add bill"
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        transcript = ""
                        draft = null
                        error = null
                        status =
                            "Tap the microphone and say it again."
                    },
                    enabled = !saving,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription =
                            null
                    )
                    Spacer(
                        Modifier.width(
                            8.dp
                        )
                    )
                    Text("Say it again")
                }
            }

            Spacer(
                Modifier.size(
                    10.dp
                )
            )
        }
    }
}

@Composable
private fun VoicePlanConfirmationCard(
    draft: VoicePlanDraft
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .primaryContainer
                .copy(
                    alpha = .55f
                )
    ) {
        Column(
            modifier =
                Modifier.padding(
                    15.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    7.dp
                )
        ) {
            Text(
                "Please confirm",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )

            when (draft) {
                is VoiceGoalDraft -> {
                    ConfirmationRow(
                        "Goal",
                        draft.name
                    )
                    ConfirmationRow(
                        "Target",
                        formatVoiceMoney(
                            draft.totalTargetMinor
                        )
                    )
                    ConfirmationRow(
                        "Target date",
                        draft.targetDateEpochMs
                            ?.let(
                                ::formatVoiceDate
                            )
                            ?: "—"
                    )
                    ConfirmationRow(
                        "Monthly contribution",
                        formatVoiceMoney(
                            draft.monthlyContributionMinor
                        )
                    )
                }

                is VoiceBillDraft -> {
                    ConfirmationRow(
                        "Bill",
                        draft.name
                    )
                    ConfirmationRow(
                        "Amount",
                        formatVoiceMoney(
                            draft.amountMinor
                        )
                    )
                    ConfirmationRow(
                        "Repeats",
                        if (
                            draft.cadence ==
                            BillCadence.WEEKLY
                        ) {
                            "Weekly"
                        } else {
                            "Monthly"
                        }
                    )
                    ConfirmationRow(
                        "Due",
                        if (
                            draft.cadence ==
                            BillCadence.WEEKLY
                        ) {
                            weekdayLabel(
                                draft.dueDayOfWeek
                            )
                        } else {
                            "Day " +
                                draft.dueDay
                                    .toString()
                        }
                    )
                    ConfirmationRow(
                        "Match payee",
                        draft.payee
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmationRow(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            label,
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
            value,
            modifier =
                Modifier.padding(
                    start = 14.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

private fun formatVoiceMoney(
    minor: Long
): String =
    "KES " +
        NumberFormat
            .getNumberInstance()
            .format(
                minor / 100L
            )

private fun formatVoiceDate(
    epochMs: Long
): String =
    SimpleDateFormat(
        "d MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(epochMs)
    )

private fun weekdayLabel(
    day: Int?
): String =
    when (day) {
        Calendar.MONDAY ->
            "Monday"
        Calendar.TUESDAY ->
            "Tuesday"
        Calendar.WEDNESDAY ->
            "Wednesday"
        Calendar.THURSDAY ->
            "Thursday"
        Calendar.FRIDAY ->
            "Friday"
        Calendar.SATURDAY ->
            "Saturday"
        Calendar.SUNDAY ->
            "Sunday"
        else ->
            "—"
    }

private fun offlineRecognitionIntent():
    Intent =
    Intent(
        RecognizerIntent
            .ACTION_RECOGNIZE_SPEECH
    )
        .putExtra(
            RecognizerIntent
                .EXTRA_LANGUAGE_MODEL,
            RecognizerIntent
                .LANGUAGE_MODEL_FREE_FORM
        )
        .putExtra(
            RecognizerIntent
                .EXTRA_PARTIAL_RESULTS,
            true
        )
        .putExtra(
            RecognizerIntent
                .EXTRA_PREFER_OFFLINE,
            true
        )
        .putExtra(
            RecognizerIntent
                .EXTRA_MAX_RESULTS,
            3
        )

private fun speechErrorMessage(
    code: Int
): String =
    when (code) {
        SpeechRecognizer.ERROR_NO_MATCH ->
            "I couldn't understand that. Try speaking a little slower."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "I didn't hear anything. Tap the microphone and try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission is required for voice add."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "Voice recognition is busy. Try again in a moment."
        else ->
            "Voice recognition stopped. Tap the microphone to try again."
    }

private fun onDeviceSpeechAvailable(
    context:
        android.content.Context
): Boolean =
    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.S
    ) {
        isOnDeviceSpeechAvailable(
            context
        )
    } else {
        false
    }

@RequiresApi(
    Build.VERSION_CODES.S
)
private fun isOnDeviceSpeechAvailable(
    context:
        android.content.Context
): Boolean =
    SpeechRecognizer
        .isOnDeviceRecognitionAvailable(
            context
        )

@RequiresApi(
    Build.VERSION_CODES.S
)
private fun createOnDeviceRecognizer(
    context:
        android.content.Context
): SpeechRecognizer =
    SpeechRecognizer
        .createOnDeviceSpeechRecognizer(
            context
        )
