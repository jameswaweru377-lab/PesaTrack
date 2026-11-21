package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.data.local.EntityEnrichmentPreferences
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentClientIdentity
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentFetchResult
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentHttpClient
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.onboarding.MessageAccessPolicy
import com.kedevjimmy.pesatrack.domain.onboarding.MessageAccessAction
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingDiscoveryEngine
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingTransactionObservation
import com.kedevjimmy.pesatrack.domain.intelligence.EntityDescriptorPrivacyPolicy
import com.kedevjimmy.pesatrack.domain.intelligence.SalaryCandidateRemotePolicy
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.domain.rules.OnboardingHistoryWindow
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingOwnershipChoice
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingReviewPolicy
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingLastFourHintPolicy
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingAccountDetailsPolicy
import com.kedevjimmy.pesatrack.domain.onboarding.LinkedSavingsOnboardingPolicy
import com.kedevjimmy.pesatrack.domain.model.FinancialInstitutionDirectory
import com.kedevjimmy.pesatrack.domain.rules.SalaryHistoryMonthPolicy
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageIngestionCoordinator
import com.kedevjimmy.pesatrack.ingestion.HistoricalImportResult
import com.kedevjimmy.pesatrack.ingestion.HistoricalSmsImporter
import com.kedevjimmy.pesatrack.ingestion.MpesaNotificationListenerService
import com.kedevjimmy.pesatrack.ui.components.BrandWordmark
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private enum class SmartSetupStage {
    WELCOME,
    PROFILE,
    IMPORT,
    SMS_PERMISSION,
    ANALYZE,
    REVIEW,
    MPESA_SERVICES,
    SALARY_OPTIONAL,
    SALARY_MANUAL,
    MPESA_CONFIRM,
    ACCOUNT_DETAILS,
    LIVE_CAPTURE,
    COMPLETE
}

private const val CUSTOM_IMPORT_SELECTION = 3
private const val ALL_HISTORY_SELECTION = 4

private enum class AnalysisStep {
    SCANNING,
    ACCOUNTS,
    INCOME,
    DUPLICATES,
    COMPLETE
}

private enum class SalaryHistoryMode {
    LATEST_ONLY,
    IMPORT_WINDOW
}

private data class SmartSalaryDraft(
    val monthKey: String,
    val monthLabel: String,
    val amount: String = "",
    val paidAtEpochMs: Long? = null,
    val matchedTransactionId: Long? = null
)


internal data class DetectedSetupSource(
    val name: String,
    val type: String,
    val messageCount: Int,
    val reference: com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
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
    val financialUnderstandingStore =
        remember {
            FinancialUnderstandingStore
                .get(context)
        }
    val financialCoordinator =
        remember {
            FinancialMessageIngestionCoordinator
                .get(context)
        }
    val entityEnrichmentClient =
        remember {
            EntityEnrichmentHttpClient(
                clientId =
                    EntityEnrichmentClientIdentity
                        .get(context)
            )
        }
    val onlineEntityLookupEnabled =
        remember {
            EntityEnrichmentPreferences
                .get(context)
                .isOnlineLookupEnabled()
        }

    var stage by rememberSaveable {
        mutableStateOf(SmartSetupStage.WELCOME)
    }
    var disclosureAccepted by rememberSaveable { mutableStateOf(SmsAccessConsentStore.hasConsent(context)) }
    var consentError by remember { mutableStateOf<String?>(null) }
    var accessStarted by rememberSaveable { mutableStateOf(false) }
    var smsHandled by rememberSaveable { mutableStateOf(false) }
    var notificationsHandled by rememberSaveable { mutableStateOf(false) }
    var importMonths by rememberSaveable {
        mutableStateOf(ALL_HISTORY_SELECTION)
    }
    var customImportStartDateEpochMs by rememberSaveable {
        mutableStateOf(defaultCustomImportStartDateEpochMs())
    }
    var customImportEndDateEpochMs by rememberSaveable {
        mutableStateOf(startOfLocalDayEpochMs(System.currentTimeMillis()))
    }
    val customImportRange = remember(
        importMonths,
        customImportStartDateEpochMs,
        customImportEndDateEpochMs
    ) {
        if (importMonths == CUSTOM_IMPORT_SELECTION) {
            OnboardingHistoryWindow.customRange(
                customImportStartDateEpochMs,
                customImportEndDateEpochMs
            )
        } else null
    }
    val selectedImportStartEpochMs = when (importMonths) {
        1, 2 -> OnboardingHistoryWindow.importStartEpochMs(importMonths)
        CUSTOM_IMPORT_SELECTION -> customImportRange?.startEpochMs
        ALL_HISTORY_SELECTION -> 0L
        else -> null
    }
    val selectedImportEndExclusiveEpochMs = customImportRange?.endExclusiveEpochMs
        .takeIf { importMonths == CUSTOM_IMPORT_SELECTION }
    val selectedHistoryMonths = customImportRange?.calendarMonths ?: importMonths
    val importSelectionValid = importMonths == 0 || selectedImportStartEpochMs != null
    var smsGranted by remember {
        mutableStateOf(smartHasSmsPermission(context))
    }
    var smsPromptLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    var smsDenied by rememberSaveable {
        mutableStateOf(false)
    }
    var notificationPromptLaunched by rememberSaveable { mutableStateOf(false) }
    var accountSaveError by remember { mutableStateOf<String?>(null) }
    var importResult by remember {
        mutableStateOf<HistoricalImportResult?>(null)
    }
    var detectedSources by remember {
        mutableStateOf<List<DetectedSetupSource>>(
            emptyList()
        )
    }
    var pendingOwnershipEntities by remember {
        mutableStateOf<
            List<DiscoveredFinancialEntity>
        >(emptyList())
    }
    // Explicit selections only; detecting a message never checks a box.
    var selectedOwnedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var completedInstitutionKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var completionInstitutionCount by remember { mutableStateOf(0) }
    var completionServiceCount by remember { mutableStateOf(0) }
    var manualInstitutionReferences by remember {
        mutableStateOf<Map<String, com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference>>(emptyMap())
    }
    var chooseInstitutions by rememberSaveable { mutableStateOf(false) }
    var reviewSaving by remember {
        mutableStateOf(false)
    }
    var salarySuggestion by remember {
        mutableStateOf<OnboardingSalarySuggestion?>(null)
    }
    var salaryAccepted by rememberSaveable {
        mutableStateOf(false)
    }
    var mpesaPhoneSuggestion by remember {
        mutableStateOf<String?>(null)
    }
    var mpesaPhoneInput by rememberSaveable { mutableStateOf("") }
    var manualPhoneMode by rememberSaveable {
        mutableStateOf(false)
    }
    var manualMpesaPhone by rememberSaveable {
        mutableStateOf("")
    }
    var salaryConfirmationSaving by remember { mutableStateOf(false) }
    var salaryConfirmationError by remember { mutableStateOf<String?>(null) }
    var linkedSavingsChoices by remember { mutableStateOf<Map<String, OnboardingOwnershipChoice>>(emptyMap()) }
    var mpesaSuggestedNumberAccepted by rememberSaveable { mutableStateOf(false) }
    var mpesaWasSelected by rememberSaveable { mutableStateOf(false) }
    var linkedSavingsSaving by remember { mutableStateOf(false) }
    var linkedSavingsError by remember { mutableStateOf<String?>(null) }
    val linkedSavingsOptions = remember {
        FinancialInstitutionDirectory.options.filter {
            it.reference.providerKey in LinkedSavingsOnboardingPolicy.providerKeys
        }
    }
    var bankReferenceEdits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var bankLastFourHints by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var accountLabels by remember {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var identitySaving by remember { mutableStateOf(false) }
    var identityError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember {
        mutableStateOf<String?>(null)
    }
    var mpesaConfirmationBusy by remember {
        mutableStateOf(false)
    }
    var notificationGranted by remember {
        mutableStateOf(smartHasNotificationAccess(context))
    }
    var notificationSettingsVisited by rememberSaveable {
        mutableStateOf(false)
    }
    var salaryApplied by rememberSaveable {
        mutableStateOf(false)
    }
    var analysisStep by remember {
        mutableStateOf(
            AnalysisStep.SCANNING
        )
    }
    var analysisReviewMode by rememberSaveable {
        mutableStateOf(false)
    }
    var smsAppInfoVisited by rememberSaveable {
        mutableStateOf(false)
    }
    var availableAccounts by remember {
        mutableStateOf<List<MoneyAccount>>(
            emptyList()
        )
    }
    var manualSalaryAccountId by rememberSaveable {
        mutableStateOf("")
    }
    var manualSalaryPayer by rememberSaveable {
        mutableStateOf("")
    }
    var manualSalaryHistoryMode by rememberSaveable {
        mutableStateOf(
            if (importMonths > 1) {
                SalaryHistoryMode.IMPORT_WINDOW
            } else {
                SalaryHistoryMode.LATEST_ONLY
            }
        )
    }
    var manualSalaryDrafts by remember {
        mutableStateOf(
            defaultSmartSalaryDrafts(
                importMonths
            )
        )
    }
    var salaryHistorySearchStatus by remember {
        mutableStateOf<String?>(null)
    }
    var manualSalaryError by remember {
        mutableStateOf<String?>(null)
    }
    var manualSalarySaving by remember {
        mutableStateOf(false)
    }
    var manualSalarySavingLabel by remember {
        mutableStateOf(
            "Saving salary…"
        )
    }
    var completionNavigating by remember {
        mutableStateOf(false)
    }

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) { granted ->
            smsGranted = granted
            smsDenied = !granted
            smsHandled = true
            if (!granted) {
                // Android denied the grant; never read SMS. Manual bank choice remains available.
                accessStarted = false
                stage = SmartSetupStage.ANALYZE
            }
        }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsHandled = true
        if (stage == SmartSetupStage.ANALYZE && analysisStep == AnalysisStep.COMPLETE) {
            stage = SmartSetupStage.REVIEW
        }
    }

    val appInfoLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
            smsGranted =
                smartHasSmsPermission(context)
            if (
                !smsGranted &&
                stage ==
                    SmartSetupStage.SMS_PERMISSION
            ) {
                smsPromptLaunched = false
                smsHandled = false
                accessStarted = disclosureAccepted && SmsAccessConsentStore.hasConsent(context)
            }
        }

    val settingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
            notificationGranted =
                smartHasNotificationAccess(context)
            if (stage == SmartSetupStage.SMS_PERMISSION) {
                notificationsHandled = true
            }
            if (
                stage ==
                    SmartSetupStage.LIVE_CAPTURE
            ) {
                if (notificationGranted) {
                    stage =
                        SmartSetupStage.COMPLETE
                } else {
                    notificationSettingsVisited = true
                }
            } else if (!notificationGranted) {
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
                        smartHasSmsPermission(context)
                    notificationGranted =
                        smartHasNotificationAccess(
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

    LaunchedEffect(stage, disclosureAccepted, accessStarted, smsHandled, notificationsHandled) {
        if (stage != SmartSetupStage.SMS_PERMISSION) return@LaunchedEffect
        when (MessageAccessPolicy.next(disclosureAccepted, accessStarted,
            smsHandled || smsGranted, notificationsHandled || notificationGranted)) {
            MessageAccessAction.WAIT -> Unit
            MessageAccessAction.REQUEST_SMS -> {
                if (!smsPromptLaunched) {
                    smsPromptLaunched = true
                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                }
            }
            MessageAccessAction.OPEN_NOTIFICATIONS -> {
                // Notification access is optional; do not add another first-run screen.
                accessStarted = false
                stage = SmartSetupStage.ANALYZE
            }
            MessageAccessAction.CONTINUE -> {
                accessStarted = false
                stage = SmartSetupStage.ANALYZE
            }
        }
    }

    LaunchedEffect(
        stage,
        importMonths,
        selectedImportStartEpochMs,
        selectedImportEndExclusiveEpochMs,
        smsGranted,
        analysisReviewMode
    ) {
        if (
            stage !=
                SmartSetupStage.ANALYZE ||
            analysisReviewMode
        ) {
            return@LaunchedEffect
        }

        val startEpochMs = selectedImportStartEpochMs
        val cutoff =
            startEpochMs
                ?: (
                    System.currentTimeMillis() -
                        SEVEN_DAYS_MS
                    )

        analysisStep =
            AnalysisStep.SCANNING
        importResult =
            withContext(Dispatchers.IO) {
                if (
                    importSelectionValid &&
                    MessageAccessPolicy.canImport(disclosureAccepted, smsGranted, importMonths) &&
                    startEpochMs != null
                ) {
                    runCatching {
                        HistoricalSmsImporter(
                            context.applicationContext
                        ).importSince(
                            sinceEpochMs = startEpochMs,
                            untilEpochMsExclusive = selectedImportEndExclusiveEpochMs
                        )
                    }.getOrNull()
                } else {
                    null
                }
            }
        delay(MIN_PROGRESS_STEP_MS)

        analysisStep =
            AnalysisStep.ACCOUNTS
        val accountDiscovery =
            withContext(Dispatchers.IO) {
                val diagnostics =
                    diagnosticsStore
                        .recent(500)
                        .filter {
                            it.receivedAtEpochMs >= cutoff &&
                                OnboardingInstitutionPresentationPolicy.isTransactionEvidence(it)
                        }

                val sources =
                    diagnostics
                        .mapNotNull { item ->
                            val resolved =
                                FinancialProviderRegistry
                                    .resolveMessageSourceAccount(
                                        title =
                                            item.title,
                                        bankLike =
                                            item.sourceType ==
                                                "Bank"
                                    )
                            val type =
                                when (
                                    resolved
                                        ?.accountTypeHint
                                ) {
                                    AccountType.BANK ->
                                        "Bank"
                                    AccountType.MOBILE_WALLET ->
                                        "Wallet"
                                    AccountType.SAVINGS ->
                                        "Savings"
                                    else -> null
                                }
                            val name =
                                resolved
                                    ?.displayName
                                    ?.trim()
                                    ?.takeIf(
                                        String::isNotBlank
                                    )

                            if (
                                resolved == null ||
                                type == null ||
                                name == null
                            ) {
                                null
                            } else {
                                item to Triple(name, type, resolved)
                            }
                        }
                        .groupBy { it.second.third.entityKey }
                        .values
                        .map { entries ->
                            val latest =
                                entries.first()
                            DetectedSetupSource(
                                name = latest.second.first,
                                type = latest.second.second,
                                messageCount =
                                    OnboardingInstitutionPresentationPolicy
                                        .uniqueObservationCount(
                                            entries.map { it.first }
                                        ),
                                reference = latest.second.third
                            )
                        }
                        .sortedWith(
                            compareBy(
                                {
                                    sourceTypeOrder(
                                        it.type
                                    )
                                },
                                {
                                    it.name.lowercase(
                                        Locale.ROOT
                                    )
                                }
                            )
                        )

                val accounts =
                    ownedSetupAccounts(
                        accountStore =
                            accountStore,
                        financialStore =
                            financialUnderstandingStore
                    )

                sources to accounts
            }

        detectedSources =
            accountDiscovery.first
        availableAccounts =
            accountDiscovery.second
        pendingOwnershipEntities =
            withContext(
                Dispatchers.IO
            ) {
                financialUnderstandingStore.pendingEntities()
            }
        selectedOwnedKeys = emptySet()
        if (
            manualSalaryAccountId.isBlank()
        ) {
            manualSalaryAccountId =
                availableAccounts
                    .firstOrNull {
                        it.type ==
                            AccountType.BANK
                    }
                    ?.id
                    ?: availableAccounts
                        .firstOrNull {
                            it.id ==
                                AccountStore
                                    .DEFAULT_MPESA_ID
                        }
                        ?.id
                    ?: availableAccounts
                        .firstOrNull()
                        ?.id
                        .orEmpty()
        }
        delay(MIN_PROGRESS_STEP_MS)

        analysisStep =
            AnalysisStep.INCOME
        val incomeDiscovery =
            withContext(Dispatchers.IO) {
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

                Pair(
                    verifiedSalarySuggestion(
                        local =
                            OnboardingDiscoveryEngine
                                .salarySuggestion(
                                    observations
                                ),
                        client =
                            entityEnrichmentClient,
                        onlineLookupEnabled =
                            onlineEntityLookupEnabled
                    ),
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

        salarySuggestion =
            incomeDiscovery.first
        salaryAccepted =
            false // An inferred income pattern is not consent to create a salary rule.
        mpesaPhoneSuggestion =
            incomeDiscovery.second
        delay(MIN_PROGRESS_STEP_MS)

        analysisStep =
            AnalysisStep.DUPLICATES
        withContext(Dispatchers.IO) {
            transactionStore
                .onboardingDuplicateCandidateCount(
                    cutoff,
                    selectedImportEndExclusiveEpochMs
                )
        }
        delay(MIN_PROGRESS_STEP_MS)

        analysisStep =
            AnalysisStep.COMPLETE
        delay(180)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPromptLaunched && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPromptLaunched = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            stage = SmartSetupStage.REVIEW
        }
    }

    LaunchedEffect(
        stage,
        importMonths,
        selectedHistoryMonths,
        selectedImportStartEpochMs
    ) {
        if (
            stage ==
            SmartSetupStage.SALARY_MANUAL
        ) {
            val desired =
                if (
                    manualSalaryHistoryMode ==
                    SalaryHistoryMode.LATEST_ONLY
                ) {
                    1
                } else {
                    selectedHistoryMonths.coerceIn(1, 3)
                }
            if (
                manualSalaryDrafts.size !=
                desired
            ) {
                manualSalaryDrafts =
                    defaultSmartSalaryDrafts(
                        desired
                    )
            }
        }
    }

    suspend fun searchImportedSalaryHistory() {
        val payer =
            manualSalaryPayer.trim()
        if (payer.isBlank()) {
            manualSalaryError =
                "Enter the payroll or statement name first."
            return
        }

        val startEpochMs = selectedImportStartEpochMs
                ?: (
                    System.currentTimeMillis() -
                        31L * 24L * 60L *
                            60L * 1000L
                    )

        val candidates =
            withContext(
                Dispatchers.IO
            ) {
                transactionStore
                    .salaryHistoryCandidates(
                        payerQuery = payer,
                        sinceEpochMs =
                            startEpochMs,
                        accountId =
                            manualSalaryAccountId
                                .takeIf {
                                    it.isNotBlank()
                                }
                    )
                    .filter {
                        selectedImportEndExclusiveEpochMs == null ||
                            it.occurredAtEpochMs < selectedImportEndExclusiveEpochMs
                    }
            }

        if (candidates.isEmpty()) {
            salaryHistorySearchStatus =
                "No matching salary credits were found in the imported history. You can enter the paydays below."
            manualSalaryError = null
            return
        }

        val byMonth =
            candidates
                .groupBy {
                    salaryMonthKey(
                        it.occurredAtEpochMs
                    )
                }
                .mapValues {
                        (_, matches) ->
                    matches.maxBy {
                        it.amountMinor
                    }
                }

        val desired =
            if (
                manualSalaryHistoryMode ==
                SalaryHistoryMode.LATEST_ONLY
            ) {
                1
            } else {
                selectedHistoryMonths.coerceIn(1, 3)
            }

        val baseDrafts =
            defaultSmartSalaryDrafts(
                desired
            )
        var found = 0
        manualSalaryDrafts =
            baseDrafts.map { draft ->
                val match =
                    byMonth[
                        draft.monthKey
                    ]
                if (match != null) {
                    found += 1
                    draft.copy(
                        amount =
                            formatSetupAmountInput(
                                match.amountMinor
                            ),
                        paidAtEpochMs =
                            match.occurredAtEpochMs,
                        matchedTransactionId =
                            match.transactionId
                    )
                } else {
                    draft
                }
            }

        if (
            manualSalaryHistoryMode ==
                SalaryHistoryMode.LATEST_ONLY &&
            found == 0
        ) {
            val latest =
                candidates.maxByOrNull {
                    it.occurredAtEpochMs
                }
            if (latest != null) {
                manualSalaryDrafts =
                    listOf(
                        SmartSalaryDraft(
                            monthKey =
                                salaryMonthKey(
                                    latest.occurredAtEpochMs
                                ),
                            monthLabel =
                                "Most recent salary",
                            amount =
                                formatSetupAmountInput(
                                    latest.amountMinor
                                ),
                            paidAtEpochMs =
                                latest.occurredAtEpochMs,
                            matchedTransactionId =
                                latest.transactionId
                        )
                    )
                found = 1
            }
        }

        salaryHistorySearchStatus =
            if (found > 0) {
                "Found $found salary payment" +
                    if (found == 1) {
                        ""
                    } else {
                        "s"
                    } +
                    " in your imported history. Confirm or edit them below."
            } else {
                "Matching income exists, but it did not line up with the selected months. Enter the paydays below."
            }
        manualSalaryError = null
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

    suspend fun applyManualSalary(): Boolean {
        val account =
            availableAccounts
                .firstOrNull {
                    it.id ==
                        manualSalaryAccountId
                }
        if (account == null) {
            manualSalaryError =
                "Choose where the salary arrived."
            return false
        }

        val selectedDrafts =
            if (
                manualSalaryHistoryMode ==
                SalaryHistoryMode.LATEST_ONLY
            ) {
                manualSalaryDrafts.take(1)
            } else {
                manualSalaryDrafts
            }

        val completed =
            mutableListOf<
                Triple<
                    SmartSalaryDraft,
                    Long,
                    Long
                >
            >()
        for (draft in selectedDrafts) {
            val amountText =
                draft.amount.trim()
            val hasAny =
                amountText.isNotBlank() ||
                    draft.paidAtEpochMs !=
                    null
            if (!hasAny) {
                continue
            }

            val amountMinor =
                parseSetupMoneyMinor(
                    amountText
                )
            val paidAt =
                draft.paidAtEpochMs
            if (
                amountMinor == null ||
                amountMinor <= 0L ||
                paidAt == null
            ) {
                manualSalaryError =
                    "Each salary you add needs both the amount and the date it actually arrived."
                return false
            }

            completed +=
                Triple(
                    draft,
                    amountMinor,
                    paidAt
                )
        }

        if (completed.isEmpty()) {
            manualSalaryError =
                "Add at least one salary payment."
            return false
        }

        val payer =
            manualSalaryPayer.trim()
        val ruleLabel =
            payer.ifBlank {
                "Manual salary"
            }

        withContext(Dispatchers.IO) {
            val existing =
                ruleStore
                    .listRules(
                        enabledOnly = false
                    )
                    .firstOrNull {
                        it.preset ==
                            RulePreset.SALARY &&
                            it.startsPayCycle
                    }

            val salaryRule =
                ruleStore.save(
                    name = "Salary",
                    counterpartyContains =
                        payer.ifBlank {
                            existing
                                ?.counterpartyContains
                                .orEmpty()
                        },
                    counterpartyExact =
                        if (
                            payer.isBlank()
                        ) {
                            existing
                                ?.counterpartyExact
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: ruleLabel
                        } else {
                            ""
                        },
                    category =
                        TransactionCategory.INCOME,
                    direction =
                        MoneyDirection.INCOME,
                    notificationTitleContains =
                        existing
                            ?.notificationTitleContains
                            .orEmpty(),
                    messageContains =
                        existing
                            ?.messageContains
                            .orEmpty(),
                    planningMonthOffset =
                        existing
                            ?.planningMonthOffset
                            ?: 0,
                    displayLabel =
                        "Salary",
                    maximumAmountMinor =
                        existing
                            ?.maximumAmountMinor,
                    enabled = true,
                    preset =
                        RulePreset.SALARY,
                    plannedAmountMinor =
                        existing
                            ?.plannedAmountMinor,
                    startsPayCycle = true,
                    accountId = account.id,
                    linkedAccountId =
                        existing
                            ?.linkedAccountId,
                    payWindowStartDay =
                        existing
                            ?.payWindowStartDay,
                    payWindowEndDay =
                        existing
                            ?.payWindowEndDay,
                    counterpartyPhoneEquals =
                        existing
                            ?.counterpartyPhoneEquals
                            .orEmpty(),
                    id =
                        existing?.id
                            ?: java.util.UUID
                                .randomUUID()
                                .toString()
                )

            if (payer.isNotBlank()) {
                transactionStore
                    .reapplyRuleForCounterparty(
                        rule = salaryRule,
                        counterpartyQuery =
                            payer,
                        rebuildPayCycles =
                            false
                    )
            }

            val historyMatches =
                if (payer.isBlank()) {
                    emptyList()
                } else {
                    val oldest =
                        completed.minOf {
                            it.third
                        } -
                            ONE_DAY_MS
                    transactionStore
                        .salaryHistoryCandidates(
                            payerQuery = payer,
                            sinceEpochMs =
                                oldest,
                            accountId =
                                account.id,
                            limit = 100
                        )
                }

            completed.forEach {
                    (draft, amountMinor, paidAt) ->
                val backingTransactionId =
                    draft.matchedTransactionId
                        ?: historyMatches
                            .firstOrNull {
                                match ->
                                match.amountMinor ==
                                    amountMinor &&
                                    kotlin.math.abs(
                                        match.occurredAtEpochMs -
                                            paidAt
                                    ) <=
                                    ONE_DAY_MS
                            }
                            ?.transactionId

                if (
                    backingTransactionId !=
                    null
                ) {
                    transactionStore
                        .applyRuleToTransaction(
                            transactionId =
                                backingTransactionId,
                            rule =
                                salaryRule
                        )
                } else {
                    transactionStore
                        .insertManualIncome(
                            amountMinor =
                                amountMinor,
                            occurredAtEpochMs =
                                paidAt,
                            label =
                                ruleLabel,
                            matchedRule =
                                salaryRule,
                            rebuildPayCycles =
                                false
                        )
                }
            }

        }

        manualSalarySavingLabel =
            "Building your payday picture…"
        withContext(Dispatchers.IO) {
            payCycleStore.rebuild()
        }

        salaryApplied = true
        salaryAccepted = true
        manualSalaryError = null
        return true
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
            financialUnderstandingStore
                .confirmProviderOwned(
                    providerKey =
                        "SAFARICOM_MPESA",
                    accountId =
                        AccountStore
                            .DEFAULT_MPESA_ID
                )
            transactionStore
                .reconcileOwnAccountTransfers(
                    rebuildPayCycles =
                        false
                )
            payCycleStore.rebuild()
        }
        phoneError = null
        return true
    }

    suspend fun applySelectedAccountOwnership() {
        if (reviewSaving) {
            return
        }

        reviewSaving = true
        try {
            val selectedKeys = selectedOwnedKeys
            val pendingKeys = pendingOwnershipEntities.map { it.entityKey }.toSet()
            val directSources = (detectedSources.map { it.reference } + manualInstitutionReferences.values)
                .filter { it.entityKey in selectedKeys && it.entityKey !in pendingKeys }
                .distinctBy { it.entityKey }
            withContext(Dispatchers.IO) {
                pendingOwnershipEntities.filter { it.entityKey in selectedKeys }.forEach { entity ->
                    financialUnderstandingStore.confirmEntityOwned(entity.entityKey)
                    financialCoordinator.reconcilePendingTransfers(
                        entityKey = entity.entityKey, rebuildPayCycles = false
                    )
                }
                directSources.forEach { reference ->
                    financialUnderstandingStore.setInstitutionOwned(reference)
                    financialCoordinator.reconcilePendingTransfers(
                        entityKey = reference.entityKey, rebuildPayCycles = false
                    )
                }
                // The profile number becomes an owned M-PESA identifier only after
                // the user explicitly selects M-PESA on this account screen.
                if (directSources.any { it.providerKey == "SAFARICOM_MPESA" }) {
                    val phone = context.getSharedPreferences(
                        "pesatrack_preferences", Context.MODE_PRIVATE
                    ).getString("profile_mpesa_phone", null)
                    if (phone != null) {
                        val wallet = accountStore.listAccounts(enabledOnly = false)
                            .firstOrNull { it.id == AccountStore.DEFAULT_MPESA_ID }
                        accountStore.updateDetails(
                            id = AccountStore.DEFAULT_MPESA_ID,
                            identifier = phone,
                            openingBalanceMinor = wallet?.openingBalanceMinor,
                            openingBalanceAsOfEpochMs = wallet?.openingBalanceAsOfEpochMs
                        )
                    }
                }
                // The first scan already preserved financial evidence. Ownership
                // confirmation above reconciles pending records without a second
                // full read of the SMS inbox.
                if (directSources.any { it.providerKey == "SAFARICOM_MPESA" }) {
                    transactionStore.reconcileOwnAccountTransfers(rebuildPayCycles = false)
                }
                if (selectedKeys.isNotEmpty()) payCycleStore.rebuild()
            }

            availableAccounts =
                ownedSetupAccounts(
                    accountStore =
                        accountStore,
                    financialStore =
                        financialUnderstandingStore
                )
            pendingOwnershipEntities =
                financialUnderstandingStore.pendingEntities()
            selectedOwnedKeys = emptySet()

            if (
                availableAccounts.none {
                    it.id ==
                        manualSalaryAccountId
                }
            ) {
                manualSalaryAccountId =
                    availableAccounts
                        .firstOrNull {
                            it.type ==
                                AccountType.BANK
                        }
                        ?.id
                        ?: availableAccounts
                            .firstOrNull {
                                it.id ==
                                    AccountStore
                                        .DEFAULT_MPESA_ID
                            }
                            ?.id
                        ?: availableAccounts
                            .firstOrNull()
                            ?.id
                            .orEmpty()
            }

            val cutoff = selectedImportStartEpochMs
                    ?: (
                        System.currentTimeMillis() -
                            31L * ONE_DAY_MS
                        )
            val refreshedSalary =
                withContext(
                    Dispatchers.IO
                ) {
                    val observations =
                        transactionStore
                            .recent(500)
                            .filter {
                                it.receivedAtEpochMs >=
                                    cutoff &&
                                    (selectedImportEndExclusiveEpochMs == null ||
                                        it.receivedAtEpochMs < selectedImportEndExclusiveEpochMs)
                            }
                            .map { item ->
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
                                            MoneyDirection
                                                .NEUTRAL
                                        ),
                                    accountId =
                                        item.accountId
                                )
                            }

                    verifiedSalarySuggestion(
                        local =
                            OnboardingDiscoveryEngine
                                .salarySuggestion(
                                    observations
                                ),
                        client =
                            entityEnrichmentClient,
                        onlineLookupEnabled =
                            onlineEntityLookupEnabled
                    )
                }

            val previousSuggestion = salarySuggestion
            val userAccepted = salaryAccepted
            salarySuggestion =
                refreshedSalary
            salaryAccepted =
                OnboardingReviewPolicy.retainSalaryAcceptance(
                    previousSuggestion,
                    refreshedSalary,
                    userAccepted
                )
            if (
                refreshedSalary != null
            ) {
                manualSalaryAccountId =
                    refreshedSalary.accountId
            }
        } finally {
            reviewSaving = false
        }
    }

    fun nextAfterIdentitySetup(): SmartSetupStage =
        SmartSetupStage.ACCOUNT_DETAILS

    fun nextAfterMpesaSetup(): SmartSetupStage =
        SmartSetupStage.ACCOUNT_DETAILS

    suspend fun applyLinkedSavingsChoices(): Boolean {
        if (linkedSavingsSaving) return false
        linkedSavingsSaving = true
        linkedSavingsError = null
        try {
            val plan = LinkedSavingsOnboardingPolicy.plan(linkedSavingsChoices)
            withContext(Dispatchers.IO) {
                linkedSavingsOptions.forEach { option ->
                    val ref = option.reference
                    val providerKey = ref.providerKey ?: return@forEach
                    when (providerKey) {
                        in plan.mineKeys -> {
                            financialUnderstandingStore.setInstitutionOwned(ref)
                            financialCoordinator.reconcilePendingTransfers(
                                entityKey = ref.entityKey,
                                rebuildPayCycles = false
                            )
                        }
                        in plan.notMineKeys -> {
                            // Only a discovered existing entity can be marked external.
                            // Do not invent a financial account from a negative response.
                            financialUnderstandingStore.markEntityExternal(ref.entityKey)
                        }
                        else -> Unit
                    }
                }
                if (plan.mineKeys.isNotEmpty() || plan.notMineKeys.isNotEmpty()) {
                    transactionStore.reconcileOwnAccountTransfers(rebuildPayCycles = false)
                    payCycleStore.rebuild()
                }
            }
            availableAccounts = ownedSetupAccounts(accountStore, financialUnderstandingStore)
            return true
        } catch (_: Exception) {
            linkedSavingsError = "Could not save savings choices. Try again or skip for now."
            return false
        } finally {
            linkedSavingsSaving = false
        }
    }

    fun detectedMpesaServices() = linkedSavingsOptions.filter { option ->
        val detectedKeys = pendingOwnershipEntities.map { it.entityKey }.toSet() +
            detectedSources.map { it.reference.entityKey }
        option.reference.entityKey in detectedKeys
    }

    fun saveAccountDetails() {
        if (identitySaving) return
        val validation = OnboardingAccountDetailsPolicy.validate(
            availableAccounts, bankReferenceEdits
        )
        if (validation.error != null) {
            identityError = validation.error
            return
        }
        val hintValidation = OnboardingLastFourHintPolicy.validate(
            availableAccounts.filter { it.type == AccountType.BANK }.map { it.id }.toSet(),
            bankLastFourHints
        )
        if (hintValidation.error != null) {
            identityError = hintValidation.error
            return
        }
        identityError = null
        identitySaving = true
        setupScope.launch {
            try {
                val selectedMpesaPhone = mpesaPhoneInput.takeIf { it.isNotBlank() }
                    ?: mpesaPhoneSuggestion
                if (mpesaWasSelected && mpesaSuggestedNumberAccepted &&
                    !confirmMpesaPhone(selectedMpesaPhone.orEmpty())
                ) {
                    identityError = phoneError ?: "Could not confirm this M-Pesa number."
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    validation.entries.forEach { (id, ref) ->
                        accountStore.updateIdentifier(id, ref)
                    }
                    val hintsEditor = context.getSharedPreferences(
                        "pesatrack_account_last_four_hints", Context.MODE_PRIVATE
                    ).edit()
                    hintValidation.entries.forEach { (accountId, digits) ->
                        hintsEditor.putString(accountId, digits)
                    }
                    require(hintsEditor.commit())
                    val labels = context.getSharedPreferences(
                        "pesatrack_preferences", Context.MODE_PRIVATE
                    ).edit()
                    accountLabels.forEach { (accountId, label) ->
                        labels.putString("account_label_$accountId", label.trim())
                    }
                    require(labels.commit())
                }
                stage = SmartSetupStage.COMPLETE
            } catch (_: Exception) {
                identityError = "Could not save your account details. Try again or skip."
            } finally {
                identitySaving = false
            }
        }
    }

    val onboardingBusy =
        reviewSaving ||
            mpesaConfirmationBusy ||
            linkedSavingsSaving ||
            salaryConfirmationSaving ||
            identitySaving ||
            manualSalarySaving ||
            completionNavigating ||
            (
                stage ==
                    SmartSetupStage.ANALYZE &&
                    !analysisReviewMode &&
                    analysisStep !=
                        AnalysisStep.COMPLETE
                )

    BackHandler(
        enabled =
            stage != SmartSetupStage.SMS_PERMISSION &&
                stage != SmartSetupStage.WELCOME &&
                stage != SmartSetupStage.PROFILE &&
                stage != SmartSetupStage.ANALYZE &&
                stage != SmartSetupStage.REVIEW
    ) {
        if (onboardingBusy) {
            return@BackHandler
        }

        stage =
            when (stage) {
                SmartSetupStage
                    .SMS_PERMISSION ->
                    SmartSetupStage.WELCOME

                SmartSetupStage
                    .ANALYZE -> {
                    analysisReviewMode = false
                    SmartSetupStage.SMS_PERMISSION
                }

                SmartSetupStage
                    .REVIEW -> {
                    analysisReviewMode = true
                    analysisStep =
                        AnalysisStep.COMPLETE
                    SmartSetupStage.ANALYZE
                }

                SmartSetupStage.MPESA_SERVICES -> SmartSetupStage.REVIEW

                SmartSetupStage.SALARY_OPTIONAL ->
                    SmartSetupStage.ACCOUNT_DETAILS

                SmartSetupStage
                    .SALARY_MANUAL ->
                    SmartSetupStage.SALARY_OPTIONAL

                SmartSetupStage
                    .MPESA_CONFIRM ->
                    SmartSetupStage.REVIEW

                SmartSetupStage.ACCOUNT_DETAILS ->
                    if (mpesaWasSelected) SmartSetupStage.MPESA_SERVICES
                    else SmartSetupStage.REVIEW

                SmartSetupStage
                    .LIVE_CAPTURE ->
                    if (
                        mpesaPhoneSuggestion !=
                        null
                    ) {
                        SmartSetupStage
                            .MPESA_CONFIRM
                    } else {
                        SmartSetupStage.REVIEW
                    }

                SmartSetupStage
                    .COMPLETE ->
                    SmartSetupStage.ACCOUNT_DETAILS

                SmartSetupStage.IMPORT ->
                    SmartSetupStage.SMS_PERMISSION

                SmartSetupStage.WELCOME ->
                    SmartSetupStage.WELCOME

                SmartSetupStage.PROFILE ->
                    SmartSetupStage.WELCOME
            }
    }

    // Four-screen first-run route. Legacy optional detail steps are available after setup,
    // but they cannot interrupt the approved profile → scan → ownership path.
    if (stage == SmartSetupStage.WELCOME) {
        PesaTrackWelcomePage(onGetStarted = { stage = SmartSetupStage.PROFILE })
        return
    }
    if (stage == SmartSetupStage.PROFILE) {
        OnboardingLocalProfilePage(onProceed = {
            disclosureAccepted = SmsAccessConsentStore.hasConsent(context)
            importMonths = ALL_HISTORY_SELECTION
            accessStarted = true
            smsPromptLaunched = false
            smsHandled = smartHasSmsPermission(context)
            stage = SmartSetupStage.SMS_PERMISSION
        })
        return
    }
    if (stage == SmartSetupStage.SMS_PERMISSION || stage == SmartSetupStage.ANALYZE) {
        val phase = when (analysisStep) {
            AnalysisStep.SCANNING -> "Importing transactions…"
            AnalysisStep.ACCOUNTS -> "Finding your accounts…"
            AnalysisStep.INCOME -> "Checking transfers…"
            AnalysisStep.DUPLICATES -> "Checking for duplicates…"
            AnalysisStep.COMPLETE -> "Finishing up…"
        }
        OnboardingDarkScanPage(importing = smsGranted, phase = phase)
        return
    }
    if (stage == SmartSetupStage.REVIEW) {
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxSize().background(com.kedevjimmy.pesatrack.ui.theme.PremiumVisualTokens.darkCanvas)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(scaffoldPadding)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            ReviewDiscoveryPage(
                sources = detectedSources,
                selectedOwnedKeys = selectedOwnedKeys,
                mpesaIdentifierHint = mpesaPhoneSuggestion,
                importResult = importResult,
                applying = reviewSaving,
                error = accountSaveError,
                onToggleAccount = { key, checked ->
                    selectedOwnedKeys = if (checked) selectedOwnedKeys + key else selectedOwnedKeys - key
                    accountSaveError = null
                },
                onSelectInstitution = { reference, checked ->
                    manualInstitutionReferences = if (checked)
                        manualInstitutionReferences + (reference.entityKey to reference)
                    else manualInstitutionReferences - reference.entityKey
                    selectedOwnedKeys = if (checked)
                        selectedOwnedKeys + reference.entityKey
                    else selectedOwnedKeys - reference.entityKey
                    accountSaveError = null
                },
                onContinue = {
                    if (!reviewSaving && selectedOwnedKeys.isNotEmpty()) setupScope.launch {
                        accountSaveError = null
                        try {
                            completedInstitutionKeys = selectedOwnedKeys
                            completionInstitutionCount = selectedOwnedKeys.size
                            applySelectedAccountOwnership()
                            onDone()
                        } catch (_: Exception) {
                            accountSaveError = "Could not save your accounts. Please try again."
                        }
                    }
                }
            )
        }
        return
    }

}

@Composable
internal fun MpesaConfirmationPage(
    suggestedPhone: String,
    manualMode: Boolean,
    manualPhone: String,
    error: String?,
    busy: Boolean,
    lockedAccountChecked: Boolean,
    onLockedAccountChanged: (Boolean) -> Unit,
    onManualChanged: (String) -> Unit,
    onConfirmSuggested: () -> Unit,
    onChooseAnother: () -> Unit,
    onSaveManual: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("M-Pesa account details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Text("Help us match your transactions accurately.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PesaCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.PhoneAndroid, size = 42.dp)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text("M-Pesa", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium)
                        Text("Detected from messages",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!manualMode && suggestedPhone.isNotBlank()) {
                    OutlinedTextField(
                        value = suggestedPhone.filter(Char::isDigit).takeLast(4),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Phone number (last 4 digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Confirm the suggested number, or choose a different one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onChooseAnother, enabled = !busy) {
                        Text("Use a different number")
                    }
                } else {
                    OutlinedTextField(
                        value = manualPhone,
                        onValueChange = onManualChanged,
                        label = { Text("Your M-Pesa phone number") },
                        placeholder = { Text("07XX XXX XXX") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("The complete number is required to verify self-transfers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Do you have a locked M-Pesa account?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Text("M-Shwari Lock Savings is tracked separately.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = lockedAccountChecked,
                        onCheckedChange = onLockedAccountChanged,
                        enabled = !busy,
                        modifier = Modifier.testTag("mpesa-lock-savings-toggle")
                    )
                }
                if (lockedAccountChecked) {
                    Text("We'll also look for Lock Savings transactions. This does not create a balance.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = if (manualMode || suggestedPhone.isBlank()) onSaveManual else onConfirmSuggested,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (busy) "Saving…" else "Continue") }
        TextButton(onClick = onSkip, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Back to this later")
        }
    }
}

private fun ownedSetupAccounts(
    accountStore: AccountStore,
    financialStore:
        FinancialUnderstandingStore
): List<MoneyAccount> {
    val confirmedIds =
        financialStore
            .confirmedAccountIds()

    return accountStore
        .listAccounts()
        .filter {
            it.type in
                setOf(
                    AccountType.BANK,
                    AccountType.MOBILE_WALLET,
                    AccountType.CASH,
                    AccountType.SAVINGS
                )
        }
        .filter { account ->
            AccountDisplayPolicy
                .isConfirmedOwned(
                    account = account,
                    confirmedAccountIds =
                        confirmedIds,
                    includeCash = true
                )
        }
}

private fun verifiedSalarySuggestion(
    local: OnboardingSalarySuggestion?,
    client: EntityEnrichmentHttpClient,
    onlineLookupEnabled: Boolean
): OnboardingSalarySuggestion? {
    val suggestion =
        local ?: return null
    if (!onlineLookupEnabled) {
        return suggestion
    }

    val descriptor =
        EntityDescriptorPrivacyPolicy
            .sanitize(
                suggestion.label
            )
            ?: return suggestion

    if (!client.isConfigured()) {
        return suggestion
    }

    return when (
        val result =
            client.fetch(
                descriptor
            )
    ) {
        is EntityEnrichmentFetchResult
            .Resolved ->
            if (
                SalaryCandidateRemotePolicy
                    .shouldReject(
                        result.value
                    )
            ) {
                null
            } else {
                suggestion
            }

        else -> suggestion
    }
}

private fun onboardingOwnershipCandidates(
    entities: List<DiscoveredFinancialEntity>
): List<DiscoveredFinancialEntity> =
    entities
        .filter { entity ->
            AccountDisplayPolicy
                .isMeaningfulAccountName(
                    entity.displayName
                )
        }
        .filter { entity ->
            runCatching {
                AccountType.valueOf(
                    entity.accountTypeHint
                )
            }.getOrDefault(
                AccountType.OTHER
            ) in
                setOf(
                    AccountType.BANK,
                    AccountType.MOBILE_WALLET,
                    AccountType.SAVINGS
                )
        }
        .filterNot { entity ->
            entity.providerKey in
                setOf(
                    "SAFARICOM_MPESA",
                    "NCBA_MSHWARI_SAVINGS",
                    "NCBA_MSHWARI_LOCK_SAVINGS"
                )
        }
        .distinctBy {
            it.entityKey
        }
        .sortedBy {
            it.displayName.lowercase(
                Locale.ROOT
            )
        }

private fun defaultSmartSalaryDrafts(
    months: Int,
    nowEpochMs: Long =
        System.currentTimeMillis()
): List<SmartSalaryDraft> {
    val count =
        months.coerceIn(1, 3)
    val calendar =
        SalaryHistoryMonthPolicy
            .anchorCalendar(
                nowEpochMs
            )
    val keyFormatter =
        SimpleDateFormat(
            "yyyy-MM",
            Locale.ROOT
        )
    val labelFormatter =
        SimpleDateFormat(
            "MMMM yyyy",
            Locale.getDefault()
        )

    return List(count) {
            index ->
        val month =
            calendar.clone()
                as Calendar
        month.add(
            Calendar.MONTH,
            -index
        )
        SmartSalaryDraft(
            monthKey =
                keyFormatter.format(
                    month.time
                ),
            monthLabel =
                if (count == 1) {
                    "Most recent salary"
                } else {
                    labelFormatter.format(
                        month.time
                    )
                }
        )
    }
}

private fun salaryMonthKey(
    epochMs: Long
): String =
    SimpleDateFormat(
        "yyyy-MM",
        Locale.ROOT
    ).format(
        Date(epochMs)
    )

private fun formatSetupAmountInput(
    amountMinor: Long
): String =
    java.math.BigDecimal(
        amountMinor
    )
        .movePointLeft(2)
        .stripTrailingZeros()
        .toPlainString()

private fun parseSetupMoneyMinor(
    raw: String
): Long? =
    runCatching {
        raw.replace(",", "")
            .trim()
            .toBigDecimal()
            .movePointRight(2)
            .setScale(
                0,
                java.math.RoundingMode.HALF_UP
            )
            .longValueExact()
    }.getOrNull()

private fun formatSetupDate(
    epochMs: Long
): String =
    SimpleDateFormat(
        "d MMM yyyy",
        Locale.getDefault()
    ).format(
        Date(epochMs)
    )

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

internal fun formatSetupMoney(
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

private const val ONE_DAY_MS =
    24L * 60L * 60L * 1000L

private const val MIN_PROGRESS_STEP_MS =
    180L

private const val SEVEN_DAYS_MS =
    7L * 24L * 60L * 60L * 1000L

private fun startOfLocalDayEpochMs(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun endOfLocalDayEpochMs(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfLocalDayEpochMs(epochMs)
        add(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }.timeInMillis

private fun defaultCustomImportStartDateEpochMs(): Long =
    startOfLocalDayEpochMs(
        Calendar.getInstance().apply {
            add(Calendar.MONTH, -6)
        }.timeInMillis
    )

private fun pickerDateEpochMs(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun formatOnboardingDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))
