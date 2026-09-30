package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingDiscoveryEngine
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingTransactionObservation
import com.kedevjimmy.pesatrack.domain.rules.HistoryImportWindow
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ingestion.HistoricalSmsImporter
import com.kedevjimmy.pesatrack.ingestion.MpesaNotificationListenerService
import com.kedevjimmy.pesatrack.ui.components.BrandWordmark
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private enum class SmartSetupStage {
    IMPORT,
    SMS_PERMISSION,
    ANALYZE,
    REVIEW,
    MPESA_CONFIRM,
    LIVE_CAPTURE,
    COMPLETE
}

private data class DetectedSetupSource(
    val name: String,
    val type: String,
    val messageCount: Int
)

@Composable
fun SmartOnboardingSetupScreen(
    scaffoldPadding: PaddingValues,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val setupScope = rememberCoroutineScope()
    val accountStore = remember { AccountStore.get(context) }
    val transactionStore = remember {
        TransactionStore.get(context)
    }
    val ruleStore = remember { RuleStore.get(context) }
    val payCycleStore = remember {
        PayCycleStore.get(context)
    }
    val diagnosticsStore = remember {
        FinancialIngestionDiagnosticsStore.get(context)
    }

    var stage by rememberSaveable {
        mutableStateOf(SmartSetupStage.IMPORT)
    }
    var importMonths by rememberSaveable {
        mutableStateOf(3)
    }
    var smsGranted by remember {
        mutableStateOf(hasSmsPermission(context))
    }
    var smsPromptLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    var smsDenied by rememberSaveable {
        mutableStateOf(false)
    }
    var importResult by remember {
        mutableStateOf<HistoricalImportResult?>(null)
    }
    var detectedSources by remember {
        mutableStateOf<List<DetectedSetupSource>>(
            emptyList()
        )
    }
    var salarySuggestion by remember {
        mutableStateOf<OnboardingSalarySuggestion?>(null)
    }
    var salaryAccepted by rememberSaveable {
        mutableStateOf(true)
    }
    var mpesaPhoneSuggestion by remember {
        mutableStateOf<String?>(null)
    }
    var manualPhoneMode by rememberSaveable {
        mutableStateOf(false)
    }
    var manualMpesaPhone by rememberSaveable {
        mutableStateOf("")
    }
    var phoneError by remember {
        mutableStateOf<String?>(null)
    }
    var notificationGranted by remember {
        mutableStateOf(hasNotificationAccess(context))
    }
    var notificationSettingsVisited by rememberSaveable {
        mutableStateOf(false)
    }
    var salaryApplied by rememberSaveable {
        mutableStateOf(false)
    }

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) { granted ->
            smsGranted = granted
            smsDenied = !granted
            if (granted) {
                stage = SmartSetupStage.ANALYZE
            }
        }

    val settingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
            notificationGranted =
                hasNotificationAccess(context)
            if (notificationGranted) {
                stage = SmartSetupStage.COMPLETE
            } else {
                notificationSettingsVisited = true
            }
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (
                    event ==
                    Lifecycle.Event.ON_RESUME
                ) {
                    smsGranted =
                        hasSmsPermission(context)
                    notificationGranted =
                        hasNotificationAccess(
                            context
                        )
                    if (
                        stage ==
                        SmartSetupStage.LIVE_CAPTURE &&
                        notificationGranted &&
                        notificationSettingsVisited
                    ) {
                        stage =
                            SmartSetupStage.COMPLETE
                    }
                }
            }
        lifecycleOwner.lifecycle
            .addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle
                .removeObserver(observer)
        }
    }

    LaunchedEffect(stage, smsGranted) {
        if (
            stage ==
            SmartSetupStage.SMS_PERMISSION
        ) {
            if (smsGranted) {
                stage =
                    SmartSetupStage.ANALYZE
            } else if (!smsPromptLaunched) {
                smsPromptLaunched = true
                smsPermissionLauncher.launch(
                    Manifest.permission.READ_SMS
                )
            }
        }
    }

    LaunchedEffect(stage, importMonths, smsGranted) {
        if (
            stage !=
            SmartSetupStage.ANALYZE
        ) {
            return@LaunchedEffect
        }

        val startEpochMs =
            HistoryImportWindow
                .importStartEpochMs(
                    importMonths
                )

        val discovery =
            withContext(Dispatchers.IO) {
                val result =
                    if (
                        importMonths > 0 &&
                        smsGranted &&
                        startEpochMs != null
                    ) {
                        runCatching {
                            HistoricalSmsImporter(
                                context.applicationContext
                            ).importSince(
                                startEpochMs
                            )
                        }.getOrNull()
                    } else {
                        null
                    }

                val cutoff =
                    startEpochMs
                        ?: (
                            System.currentTimeMillis() -
                                SEVEN_DAYS_MS
                            )

                val diagnostics =
                    diagnosticsStore
                        .recent(500)
                        .filter {
                            it.receivedAtEpochMs >=
                                cutoff
                        }

                val sources =
                    diagnostics
                        .asSequence()
                        .filter {
                            it.sourceType in
                                setOf(
                                    "Bank",
                                    "Wallet",
                                    "Savings"
                                )
                        }
                        .mapNotNull { item ->
                            item.sourceDisplayName
                                ?.trim()
                                ?.takeIf(
                                    String::isNotBlank
                                )
                                ?.let { name ->
                                    name to
                                        item.sourceType
                                }
                        }
                        .groupBy {
                            it.first
                                .lowercase()
                        }
                        .values
                        .map { entries ->
                            val latest =
                                entries.first()
                            DetectedSetupSource(
                                name = latest.first,
                                type = latest.second,
                                messageCount =
                                    entries.size
                            )
                        }
                        .sortedWith(
                            compareBy(
                                { sourceTypeOrder(it.type) },
                                { it.name.lowercase() }
                            )
                        )

                val transactions =
                    transactionStore
                        .recent(500)
                        .filter {
                            it.receivedAtEpochMs >=
                                cutoff
                        }

                val observations =
                    transactions.map { item ->
                        OnboardingTransactionObservation(
                            counterparty =
                                item.counterparty,
                            counterpartyPhone =
                                item.counterpartyPhone,
                            amountMinor =
                                item.amountMinor,
                            occurredAtEpochMs =
                                item.occurredAtEpochMs,
                            direction =
                                runCatching {
                                    MoneyDirection
                                        .valueOf(
                                            item.direction
                                        )
                                }.getOrDefault(
                                    MoneyDirection.NEUTRAL
                                ),
                            accountId =
                                item.accountId
                        )
                    }

                val existingMpesaNumber =
                    accountStore
                        .listAccounts(
                            enabledOnly = false
                        )
                        .firstOrNull {
                            it.id ==
                                AccountStore
                                    .DEFAULT_MPESA_ID
                        }
                        ?.identifier

                SetupDiscoveryResult(
                    importResult = result,
                    sources = sources,
                    salary =
                        OnboardingDiscoveryEngine
                            .salarySuggestion(
                                observations
                            ),
                    mpesaPhone =
                        if (
                            existingMpesaNumber
                                .isNullOrBlank()
                        ) {
                            OnboardingDiscoveryEngine
                                .mpesaPhoneSuggestion(
                                    observations =
                                        observations,
                                    mpesaAccountId =
                                        AccountStore
                                            .DEFAULT_MPESA_ID
                                )
                        } else {
                            null
                        }
                )
            }

        // Keep the analysis page visible long enough to feel
        // deliberate without turning setup into a waiting screen.
        delay(700)

        importResult =
            discovery.importResult
        detectedSources =
            discovery.sources
        salarySuggestion =
            discovery.salary
        salaryAccepted =
            discovery.salary != null
        mpesaPhoneSuggestion =
            discovery.mpesaPhone
        stage =
            SmartSetupStage.REVIEW
    }

    suspend fun applySalaryIfConfirmed() {
        val suggestion =
            salarySuggestion
                ?.takeIf {
                    salaryAccepted &&
                        !salaryApplied
                }
                ?: return

        withContext(Dispatchers.IO) {
            val existing =
                ruleStore.listRules(
                    enabledOnly = false
                )
                    .firstOrNull {
                        it.preset ==
                            RulePreset.SALARY &&
                            it.startsPayCycle
                    }

            if (existing == null) {
                val rule =
                    ruleStore.save(
                        name = "Salary",
                        counterpartyContains = "",
                        counterpartyExact =
                            suggestion.label,
                        category =
                            TransactionCategory
                                .INCOME,
                        direction =
                            MoneyDirection.INCOME,
                        displayLabel = "Salary",
                        preset =
                            RulePreset.SALARY,
                        startsPayCycle = true,
                        accountId =
                            suggestion.accountId
                    )
                transactionStore
                    .reapplyRule(rule)
                payCycleStore.rebuild()
            }
        }
        salaryApplied = true
    }

    suspend fun confirmMpesaPhone(
        rawPhone: String
    ): Boolean {
        val normalized =
            OwnAccountTransferDetector
                .normalizeKenyanPhone(
                    rawPhone
                )
        if (normalized == null) {
            phoneError =
                "Enter a valid Kenyan mobile number."
            return false
        }

        withContext(Dispatchers.IO) {
            val mpesaAccount =
                accountStore
                    .listAccounts(
                        enabledOnly = false
                    )
                    .firstOrNull {
                        it.id ==
                            AccountStore
                                .DEFAULT_MPESA_ID
                    }

            accountStore.updateDetails(
                id =
                    AccountStore
                        .DEFAULT_MPESA_ID,
                identifier = normalized,
                openingBalanceMinor =
                    mpesaAccount
                        ?.openingBalanceMinor,
                openingBalanceAsOfEpochMs =
                    mpesaAccount
                        ?.openingBalanceAsOfEpochMs
            )
            transactionStore
                .reconcileOwnAccountTransfers()
        }
        phoneError = null
        return true
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(
                MaterialTheme.colorScheme
                    .background
            ),
        contentPadding =
            PaddingValues(
                horizontal = 18.dp,
                vertical = 20.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        item {
            BrandWordmark()
        }

        item {
            StageProgress(stage)
        }

        when (stage) {
            SmartSetupStage.IMPORT -> {
                item {
                    ImportChoicePage(
                        selectedMonths =
                            importMonths,
                        onSelected = {
                            importMonths = it
                            smsDenied = false
                        },
                        onContinue = {
                            if (
                                importMonths == 0 ||
                                smsGranted
                            ) {
                                stage =
                                    SmartSetupStage
                                        .ANALYZE
                            } else {
                                smsPromptLaunched =
                                    false
                                stage =
                                    SmartSetupStage
                                        .SMS_PERMISSION
                            }
                        }
                    )
                }
            }

            SmartSetupStage.SMS_PERMISSION -> {
                item {
                    SmsPermissionPage(
                        denied = smsDenied,
                        onTryAgain = {
                            smsDenied = false
                            smsPromptLaunched =
                                true
                            smsPermissionLauncher
                                .launch(
                                    Manifest.permission
                                        .READ_SMS
                                )
                        },
                        onContinueWithout = {
                            importMonths = 0
                            stage =
                                SmartSetupStage
                                    .ANALYZE
                        }
                    )
                }
            }

            SmartSetupStage.ANALYZE -> {
                item {
                    AnalysisPage(
                        importing =
                            importMonths > 0 &&
                                smsGranted
                    )
                }
            }

            SmartSetupStage.REVIEW -> {
                item {
                    ReviewDiscoveryPage(
                        sources =
                            detectedSources,
                        salarySuggestion =
                            salarySuggestion,
                        salaryAccepted =
                            salaryAccepted,
                        importResult =
                            importResult,
                        onSalaryAccepted = {
                            salaryAccepted = it
                        },
                        onContinue = {
                            setupScope.launch {
                                applySalaryIfConfirmed()
                                stage =
                                    if (
                                        mpesaPhoneSuggestion !=
                                        null
                                    ) {
                                        SmartSetupStage
                                            .MPESA_CONFIRM
                                    } else {
                                        SmartSetupStage
                                            .LIVE_CAPTURE
                                    }
                            }
                        }
                    )
                }
            }

            SmartSetupStage.MPESA_CONFIRM -> {
                item {
                    MpesaConfirmationPage(
                        suggestedPhone =
                            mpesaPhoneSuggestion
                                .orEmpty(),
                        manualMode =
                            manualPhoneMode,
                        manualPhone =
                            manualMpesaPhone,
                        error = phoneError,
                        onManualChanged = {
                            manualMpesaPhone = it
                            phoneError = null
                        },
                        onConfirmSuggested = {
                            val suggested =
                                mpesaPhoneSuggestion
                                    .orEmpty()
                            setupScope.launch {
                                    if (
                                        confirmMpesaPhone(
                                            suggested
                                        )
                                    ) {
                                        applySalaryIfConfirmed()
                                        stage =
                                            SmartSetupStage
                                                .LIVE_CAPTURE
                                    }
                                }
                        },
                        onChooseAnother = {
                            manualPhoneMode = true
                        },
                        onSaveManual = {
                            setupScope.launch {
                                    if (
                                        confirmMpesaPhone(
                                            manualMpesaPhone
                                        )
                                    ) {
                                        applySalaryIfConfirmed()
                                        stage =
                                            SmartSetupStage
                                                .LIVE_CAPTURE
                                    }
                                }
                        },
                        onSkip = {
                            setupScope.launch {
                                    applySalaryIfConfirmed()
                                    stage =
                                        SmartSetupStage
                                            .LIVE_CAPTURE
                                }
                        }
                    )
                }
            }

            SmartSetupStage.LIVE_CAPTURE -> {
                item {
                    LiveCapturePage(
                        alreadyEnabled =
                            notificationGranted,
                        settingsVisited =
                            notificationSettingsVisited,
                        onEnable = {
                            if (
                                notificationGranted
                            ) {
                                stage =
                                    SmartSetupStage
                                        .COMPLETE
                            } else {
                                notificationSettingsVisited =
                                    true
                                settingsLauncher.launch(
                                    notificationAccessIntent(
                                        context
                                    )
                                )
                            }
                        },
                        onOpenAppInfo = {
                            settingsLauncher.launch(
                                Intent(
                                    Settings
                                        .ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse(
                                        "package:" +
                                            context.packageName
                                    )
                                )
                            )
                        },
                        onLater = {
                            stage =
                                SmartSetupStage
                                    .COMPLETE
                        }
                    )
                }
            }

            SmartSetupStage.COMPLETE -> {
                item {
                    SetupCompletePage(
                        sources =
                            detectedSources,
                        salaryAccepted =
                            salaryAccepted &&
                                salarySuggestion !=
                                null,
                        notificationEnabled =
                            notificationGranted,
                        onHome = onDone
                    )
                }
            }
        }
    }
}

private data class SetupDiscoveryResult(
    val importResult: HistoricalImportResult?,
    val sources: List<DetectedSetupSource>,
    val salary: OnboardingSalarySuggestion?,
    val mpesaPhone: String?
)

@Composable
private fun StageProgress(
    stage: SmartSetupStage
) {
    val visibleIndex =
        when (stage) {
            SmartSetupStage.IMPORT -> 0
            SmartSetupStage.SMS_PERMISSION -> 1
            SmartSetupStage.ANALYZE -> 2
            SmartSetupStage.REVIEW -> 3
            SmartSetupStage.MPESA_CONFIRM -> 4
            SmartSetupStage.LIVE_CAPTURE -> 5
            SmartSetupStage.COMPLETE -> 6
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.Center
    ) {
        repeat(7) { index ->
            Surface(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(
                        width =
                            if (
                                index ==
                                visibleIndex
                            ) {
                                26.dp
                            } else {
                                7.dp
                            },
                        height = 7.dp
                    ),
                shape = CircleShape,
                color =
                    if (
                        index <=
                        visibleIndex
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                    }
            ) {}
        }
    }
}

@Composable
private fun ImportChoicePage(
    selectedMonths: Int,
    onSelected: (Int) -> Unit,
    onContinue: () -> Unit
) {
    SetupHero(
        icon = Icons.Rounded.History,
        title = "Let's find your money messages",
        body =
            "Choose how much recent history PesaTrack should use to understand your accounts and patterns."
    )

    listOf(
        Triple(
            3,
            "Last 3 months",
            "Recommended for the clearest picture"
        ),
        Triple(
            1,
            "Last 1 month",
            "A quick way to get started"
        ),
        Triple(
            0,
            "Not now",
            "Start without reading old SMS"
        )
    ).forEach { option ->
        SelectableSetupCard(
            selected =
                selectedMonths ==
                    option.first,
            title = option.second,
            body = option.third,
            icon =
                if (
                    option.first == 0
                ) {
                    Icons.Rounded.Lock
                } else {
                    Icons.Rounded.History
                },
            onClick = {
                onSelected(option.first)
            }
        )
    }

    Button(
        onClick = onContinue,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text("Continue")
        Icon(
            Icons.Rounded.ArrowForward,
            contentDescription = null,
            modifier =
                Modifier.padding(start = 8.dp)
        )
    }

    Text(
        "You can import or re-import history later from Settings.",
        style = MaterialTheme.typography.bodySmall,
        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant
    )
}

@Composable
private fun SmsPermissionPage(
    denied: Boolean,
    onTryAgain: () -> Unit,
    onContinueWithout: () -> Unit
) {
    SetupHero(
        icon = Icons.Rounded.Message,
        title = "Allow access to your messages",
        body =
            "Android asks for this permission directly. PesaTrack uses it only to read the history you chose."
    )

    PesaCard {
        Column(
            Modifier.padding(18.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            IconBubble(
                Icons.Rounded.Shield,
                size = 56.dp,
                background =
                    pesaToneContainer("green"),
                tint =
                    pesaToneContent("green")
            )
            Text(
                if (denied) {
                    "SMS access wasn't allowed"
                } else {
                    "Waiting for Android permission"
                },
                style =
                    MaterialTheme.typography
                        .titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                if (denied) {
                    "That's okay. You can continue without importing old messages."
                } else {
                    "The system Allow / Don't allow dialog should appear automatically."
                },
                textAlign = TextAlign.Center,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }

    if (denied) {
        Button(
            onClick = onTryAgain,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("Try again")
        }
        OutlinedButton(
            onClick =
                onContinueWithout,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("Continue without import")
        }
    }

    Text(
        "Raw SMS text stays on this phone. PesaTrack's optional online merchant enrichment only receives privacy-filtered business descriptors.",
        style = MaterialTheme.typography.bodySmall,
        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant
    )
}

@Composable
private fun AnalysisPage(
    importing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color =
                MaterialTheme.colorScheme
                    .primaryContainer
        ) {
            Box(
                contentAlignment =
                    Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(58.dp),
                    strokeWidth = 5.dp
                )
            }
        }

        Text(
            "Understanding your money…",
            modifier =
                Modifier.padding(top = 24.dp),
            style =
                MaterialTheme.typography
                    .headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            if (importing) {
                "PesaTrack is scanning your selected history, finding accounts and looking for recurring income."
            } else {
                "PesaTrack is preparing a clean starting point. It will learn from new activity as it arrives."
            },
            modifier =
                Modifier.padding(top = 8.dp),
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        PesaCard(
            modifier =
                Modifier.padding(top = 26.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(13.dp)
            ) {
                AnalysisRow(
                    "Scanning financial messages"
                )
                AnalysisRow(
                    "Finding your accounts"
                )
                AnalysisRow(
                    "Looking for recurring income"
                )
                AnalysisRow(
                    "Checking duplicate evidence"
                )
            }
        }
    }
}

@Composable
private fun AnalysisRow(
    label: String
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint =
                MaterialTheme.colorScheme
                    .primary
        )
        Text(
            label,
            modifier =
                Modifier.padding(start = 10.dp),
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewDiscoveryPage(
    sources: List<DetectedSetupSource>,
    salarySuggestion:
        OnboardingSalarySuggestion?,
    salaryAccepted: Boolean,
    importResult: HistoricalImportResult?,
    onSalaryAccepted: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    SetupHero(
        icon = Icons.Rounded.CheckCircle,
        title = "Here's what we found",
        body =
            "Confirm the useful signals. You can change account and rule details later."
    )

    if (sources.isEmpty()) {
        PesaCard {
            Column(
                Modifier.padding(18.dp)
            ) {
                Text(
                    "No accounts confirmed yet",
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
                Text(
                    if (importResult == null) {
                        "That's fine. PesaTrack will discover M-Pesa, banks and other money sources as messages arrive."
                    } else {
                        "PesaTrack imported what it could, but there wasn't enough evidence to confidently name an account yet."
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
            shape = RoundedCornerShape(22.dp),
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
                                .titleMedium
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
            "Tap the salary card to include or exclude it. PesaTrack only creates the salary rule after your confirmation.",
            style =
                MaterialTheme.typography
                    .bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    } else {
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
                    Modifier.padding(
                        start = 11.dp
                    )
                ) {
                    Text(
                        "Salary can be learned later",
                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )
                    Text(
                        "We didn't see a repeated payer across multiple months, so PesaTrack won't guess.",
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text("Looks good")
    }
}

@Composable
private fun DiscoveredSourceCard(
    source: DetectedSetupSource
) {
    PesaCard {
        Row(
            modifier =
                Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon =
                    when (source.type) {
                        "Bank" ->
                            Icons.Rounded
                                .AccountBalance
                        "Wallet" ->
                            Icons.Rounded.Wallet
                        else ->
                            Icons.Rounded.Payments
                    },
                size = 46.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    source.name,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    source.type +
                        " • " +
                        source.messageCount +
                        " captured message" +
                        if (
                            source.messageCount ==
                            1
                        ) {
                            ""
                        } else {
                            "s"
                        },
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Detected",
                tint =
                    MaterialTheme.colorScheme
                        .primary
            )
        }
    }
}

@Composable
private fun MpesaConfirmationPage(
    suggestedPhone: String,
    manualMode: Boolean,
    manualPhone: String,
    error: String?,
    onManualChanged: (String) -> Unit,
    onConfirmSuggested: () -> Unit,
    onChooseAnother: () -> Unit,
    onSaveManual: () -> Unit,
    onSkip: () -> Unit
) {
    SetupHero(
        icon = Icons.Rounded.PhoneAndroid,
        title = "Is this your M-Pesa number?",
        body =
            "We noticed this destination repeatedly. Confirming ownership helps PesaTrack separate your transfers from money sent to someone else."
    )

    PesaCard {
        Column(
            modifier =
                Modifier.padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                maskPhone(suggestedPhone),
                style =
                    MaterialTheme.typography
                        .headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Only ownership is being confirmed.",
                modifier =
                    Modifier.padding(top = 5.dp),
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }

    if (!manualMode) {
        Button(
            onClick = onConfirmSuggested,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Yes, this is mine")
        }

        OutlinedButton(
            onClick = onChooseAnother,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("No, choose another")
        }
    } else {
        OutlinedTextField(
            value = manualPhone,
            onValueChange = onManualChanged,
            label = {
                Text("Your M-Pesa number")
            },
            placeholder = {
                Text("0712345678")
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Phone
                ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        error?.let {
            Text(
                it,
                color =
                    MaterialTheme.colorScheme
                        .error,
                style =
                    MaterialTheme.typography
                        .bodySmall
            )
        }
        Button(
            onClick = onSaveManual,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("Save and continue")
        }
    }

    TextButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("I'll do this later")
    }

    Text(
        "PesaTrack never treats a number as yours without your confirmation.",
        style =
            MaterialTheme.typography
                .bodySmall,
        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant
    )
}

@Composable
private fun LiveCapturePage(
    alreadyEnabled: Boolean,
    settingsVisited: Boolean,
    onEnable: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onLater: () -> Unit
) {
    SetupHero(
        icon = Icons.Rounded.Notifications,
        title = "Keep PesaTrack updated automatically",
        body =
            "Notification access lets PesaTrack understand new financial confirmations as they arrive."
    )

    PesaCard {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            FeatureRow(
                icon =
                    Icons.Rounded.Message,
                title = "New money messages",
                body =
                    "Processed locally when they arrive"
            )
            FeatureRow(
                icon =
                    Icons.Rounded.Shield,
                title = "No extra daily work",
                body =
                    "Your Activity view keeps itself current"
            )
        }
    }

    if (alreadyEnabled) {
        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(18.dp),
            color =
                pesaToneContainer("green")
        ) {
            Row(
                Modifier.padding(15.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint =
                        pesaToneContent(
                            "green"
                        )
                )
                Text(
                    "Live tracking is already enabled.",
                    modifier =
                        Modifier.padding(
                            start = 10.dp
                        )
                )
            }
        }
    }

    Button(
        onClick = onEnable,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            if (alreadyEnabled) {
                "Continue"
            } else {
                "Enable live tracking"
            }
        )
    }

    if (
        settingsVisited &&
        !alreadyEnabled
    ) {
        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(18.dp),
            color =
                pesaToneContainer("amber")
        ) {
            Column(
                Modifier.padding(15.dp)
            ) {
                Text(
                    "Still not enabled?",
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
                Text(
                    "Sideloaded debug builds can require Android's “Allow restricted settings” switch first.",
                    modifier =
                        Modifier.padding(top = 4.dp),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
                TextButton(
                    onClick =
                        onOpenAppInfo
                ) {
                    Text("Open App info")
                }
            }
        }
    }

    OutlinedButton(
        onClick = onLater,
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text("Maybe later")
    }

    Text(
        "Live tracking is optional. PesaTrack remains usable without it.",
        style =
            MaterialTheme.typography
                .bodySmall,
        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant
    )
}

@Composable
private fun SetupCompletePage(
    sources: List<DetectedSetupSource>,
    salaryAccepted: Boolean,
    notificationEnabled: Boolean,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(92.dp),
            shape = CircleShape,
            color =
                pesaToneContainer("green")
        ) {
            Box(
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint =
                        pesaToneContent(
                            "green"
                        ),
                    modifier =
                        Modifier.size(54.dp)
                )
            }
        }
        Text(
            "All set!",
            modifier =
                Modifier.padding(top = 18.dp),
            style =
                MaterialTheme.typography
                    .headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "PesaTrack is ready to turn your financial messages into one clear money picture.",
            modifier =
                Modifier.padding(
                    top = 7.dp,
                    bottom = 18.dp
                ),
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        PesaCard {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                CompletionRow(
                    if (sources.isEmpty()) {
                        "Accounts will be learned as you use PesaTrack"
                    } else {
                        sources.size
                            .toString() +
                            " money source" +
                            if (
                                sources.size == 1
                            ) {
                                ""
                            } else {
                                "s"
                            } +
                            " detected"
                    }
                )
                if (salaryAccepted) {
                    CompletionRow(
                        "Salary pattern confirmed"
                    )
                }
                CompletionRow(
                    if (
                        notificationEnabled
                    ) {
                        "Live tracking enabled"
                    } else {
                        "Live tracking can be enabled later"
                    }
                )
            }
        }

        Button(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .heightIn(min = 56.dp),
            shape =
                RoundedCornerShape(18.dp)
        ) {
            Text("Go to Home")
            Icon(
                Icons.Rounded.ArrowForward,
                contentDescription = null,
                modifier =
                    Modifier.padding(
                        start = 8.dp
                    )
            )
        }
    }
}

@Composable
private fun CompletionRow(
    text: String
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint =
                MaterialTheme.colorScheme
                    .primary
        )
        Text(
            text,
            modifier =
                Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun SetupHero(
    icon:
        androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        IconBubble(
            icon,
            size = 58.dp
        )
        Text(
            title,
            modifier =
                Modifier.padding(top = 14.dp),
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            body,
            modifier =
                Modifier.padding(top = 7.dp),
            style =
                MaterialTheme.typography
                    .bodyLarge,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectableSetupCard(
    selected: Boolean,
    title: String,
    body: String,
    icon:
        androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape =
            RoundedCornerShape(20.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme
                    .primaryContainer
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
                icon,
                size = 44.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    title,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    body,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
            Icon(
                if (selected) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.History
                },
                contentDescription = null,
                tint =
                    if (selected) {
                        MaterialTheme.colorScheme
                            .primary
                    } else {
                        MaterialTheme.colorScheme
                            .outline
                    }
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon:
        androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        IconBubble(
            icon,
            size = 44.dp
        )
        Column(
            Modifier.padding(start = 11.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
            Text(
                body,
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

private fun hasSmsPermission(
    context: Context
): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) ==
        PackageManager.PERMISSION_GRANTED

private fun hasNotificationAccess(
    context: Context
): Boolean =
    NotificationManagerCompat
        .getEnabledListenerPackages(
            context
        )
        .contains(context.packageName)

private fun notificationAccessIntent(
    context: Context
): Intent {
    val component =
        ComponentName(
            context,
            MpesaNotificationListenerService::
                class.java
        )

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.R
    ) {
        val detail =
            Intent(
                Settings
                    .ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS
            ).putExtra(
                Settings
                    .EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                component.flattenToString()
            )

        if (
            detail.resolveActivity(
                context.packageManager
            ) != null
        ) {
            return detail
        }
    }

    return Intent(
        Settings
            .ACTION_NOTIFICATION_LISTENER_SETTINGS
    )
}

private fun sourceTypeOrder(
    type: String
): Int =
    when (type) {
        "Wallet" -> 0
        "Bank" -> 1
        "Savings" -> 2
        else -> 3
    }

private fun maskPhone(
    raw: String
): String {
    val digits =
        raw.filter(Char::isDigit)
    return if (digits.length >= 4) {
        "•••• " +
            digits.takeLast(4)
    } else {
        raw
    }
}

private fun formatSetupMoney(
    amountMinor: Long
): String {
    val formatter =
        NumberFormat.getNumberInstance(
            Locale.getDefault()
        ).apply {
            maximumFractionDigits = 0
        }
    return "KES " +
        formatter.format(
            amountMinor / 100.0
        )
}

private const val SEVEN_DAYS_MS =
    7L * 24L * 60L * 60L * 1000L
