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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
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
    IMPORT,
    SMS_PERMISSION,
    ANALYZE,
    REVIEW,
    SALARY_OPTIONAL,
    SALARY_MANUAL,
    MPESA_CONFIRM,
    ACCOUNT_DETAILS,
    LIVE_CAPTURE,
    COMPLETE
}

private const val CUSTOM_IMPORT_SELECTION = 3

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
        mutableStateOf(2)
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
    var manualPhoneMode by rememberSaveable {
        mutableStateOf(false)
    }
    var manualMpesaPhone by rememberSaveable {
        mutableStateOf("")
    }
    var salaryConfirmationSaving by remember { mutableStateOf(false) }
    var salaryConfirmationError by remember { mutableStateOf<String?>(null) }
    var linkedSavingsChoices by remember { mutableStateOf<Map<String, OnboardingOwnershipChoice>>(emptyMap()) }
    var mpesaWasSelected by rememberSaveable { mutableStateOf(false) }
    var linkedSavingsSaving by remember { mutableStateOf(false) }
    var linkedSavingsError by remember { mutableStateOf<String?>(null) }
    val linkedSavingsOptions = remember {
        FinancialInstitutionDirectory.options.filter {
            it.reference.providerKey in LinkedSavingsOnboardingPolicy.providerKeys
        }
    }
    var profileName by rememberSaveable {
        mutableStateOf(context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
            .getString("profile_name", "").orEmpty())
    }
    var bankReferenceEdits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var bankLastFourHints by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
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
            if (!granted) accessStarted = false
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
                if (!notificationSettingsVisited) {
                    notificationSettingsVisited = true
                    runCatching { settingsLauncher.launch(smartNotificationAccessIntent(context)) }
                        .onFailure { notificationsHandled = true }
                }
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
                            it.receivedAtEpochMs >=
                                cutoff
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
                                Triple(name, type, resolved)
                            }
                        }
                        .groupBy { it.third.entityKey }
                        .values
                        .map { entries ->
                            val latest =
                                entries.first()
                            DetectedSetupSource(
                                name = latest.first,
                                type = latest.second,
                                messageCount = entries.size,
                                reference = latest.third
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
                onboardingOwnershipCandidates(
                    financialUnderstandingStore
                        .pendingEntities()
                )
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
        stage =
            SmartSetupStage.REVIEW
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
            val directSources = detectedSources.map { it.reference }
                .filter { it.entityKey in selectedKeys && it.entityKey !in pendingKeys }
                .filterNot { it.providerKey == "SAFARICOM_MPESA" }
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
                if (selectedKeys.isNotEmpty() && smsGranted && selectedImportStartEpochMs != null) {
                    selectedImportStartEpochMs?.let { startEpochMs ->
                        HistoricalSmsImporter(context.applicationContext).importSince(
                            sinceEpochMs = startEpochMs,
                            rebuildPayCycles = false,
                            untilEpochMsExclusive = selectedImportEndExclusiveEpochMs
                        )
                    }
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
                onboardingOwnershipCandidates(
                    financialUnderstandingStore
                        .pendingEntities()
                )
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
                    require(context.getSharedPreferences(
                        "pesatrack_preferences", Context.MODE_PRIVATE
                    ).edit().putString("profile_name", profileName.trim()).commit())
                }
                stage = SmartSetupStage.SALARY_OPTIONAL
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
                stage != SmartSetupStage.WELCOME
    ) {
        if (onboardingBusy) {
            return@BackHandler
        }

        stage =
            when (stage) {
                SmartSetupStage
                    .SMS_PERMISSION ->
                    SmartSetupStage.SMS_PERMISSION

                SmartSetupStage
                    .ANALYZE -> {
                    analysisReviewMode = false
                    SmartSetupStage.IMPORT
                }

                SmartSetupStage
                    .REVIEW -> {
                    analysisReviewMode = true
                    analysisStep =
                        AnalysisStep.COMPLETE
                    SmartSetupStage.ANALYZE
                }

                SmartSetupStage.SALARY_OPTIONAL ->
                    SmartSetupStage.ACCOUNT_DETAILS

                SmartSetupStage
                    .SALARY_MANUAL ->
                    SmartSetupStage.SALARY_OPTIONAL

                SmartSetupStage
                    .MPESA_CONFIRM ->
                    SmartSetupStage.REVIEW

                SmartSetupStage.ACCOUNT_DETAILS ->
                    if (mpesaWasSelected) SmartSetupStage.MPESA_CONFIRM
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
                    SmartSetupStage.SALARY_OPTIONAL

                SmartSetupStage.IMPORT ->
                    SmartSetupStage.WELCOME

                SmartSetupStage.WELCOME ->
                    SmartSetupStage.WELCOME
            }
    }

    BackHandler(enabled = chooseInstitutions) { chooseInstitutions = false }
    if (chooseInstitutions) {
        FinancialInstitutionsScreen(scaffoldPadding) { chooseInstitutions = false }
        return
    }
    if (stage == SmartSetupStage.SMS_PERMISSION) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(scaffoldPadding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandWordmark(compact = true)
            StageProgress(stage)
            SmartSmsPermissionPage(
                accepted = disclosureAccepted, busy = accessStarted, denied = smsDenied, consentError = consentError,
                selectedRangeDescription = when (importMonths) {
                    1 -> "1 completed month plus this month to date"
                    2 -> "2 completed months plus this month to date"
                    CUSTOM_IMPORT_SELECTION -> customImportRange?.let {
                        "${formatOnboardingDate(it.startEpochMs)} – ${formatOnboardingDate(customImportEndDateEpochMs)}"
                    }
                    else -> null
                },
                restrictedHelp = smsDenied && smartIsLikelySideloaded(context),
                onAccepted = {
                    val revoked = if (it) true else {
                        val sourcesSaved = com.kedevjimmy.pesatrack.ingestion.NotificationSourceTrustStore(context).revokeAll()
                        val smsSaved = SmsAccessConsentStore.setAccepted(context, false)
                        sourcesSaved && smsSaved
                    }
                    if (revoked) {
                        disclosureAccepted = it
                        consentError = null
                    } else consentError = "Your choice could not be saved. Please try again."
                },
                onOpenAppInfo = {
                    smsAppInfoVisited = true
                    appInfoLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.packageName)))
                },
                onTryAgain = {
                    if (disclosureAccepted && SmsAccessConsentStore.setAccepted(context, true)) {
                        smsDenied = false
                        smsHandled = false
                        smsPromptLaunched = false
                        accessStarted = true
                    } else consentError = "Your choice could not be saved. Please try again."
                },
                onContinue = {
                    if (disclosureAccepted && SmsAccessConsentStore.setAccepted(context, true)) {
                        consentError = null
                        accessStarted = true
                    } else consentError = "Your choice could not be saved. Please try again."
                },
                onContinueWithout = {
                    val sourcesSaved = com.kedevjimmy.pesatrack.ingestion.NotificationSourceTrustStore(context).revokeAll()
                    val smsSaved = SmsAccessConsentStore.setAccepted(context, false)
                    if (sourcesSaved && smsSaved) {
                        disclosureAccepted = false
                        importMonths = 0
                        accessStarted = false
                        stage = SmartSetupStage.ANALYZE
                    } else consentError = "Your choice could not be saved. Please try again."
                }
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme
                    .background
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
            )
            .padding(scaffoldPadding),
        contentPadding =
            PaddingValues(
                horizontal = 18.dp,
                vertical = 20.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        item {
            BrandWordmark(
                compact = true
            )
        }

        item {
            StageProgress(stage)
        }

        when (stage) {
            SmartSetupStage.WELCOME -> {
                item {
                    PesaTrackWelcomePage(
                        onGetStarted = { stage = SmartSetupStage.IMPORT },
                        onSkip = {
                            importMonths = 0
                            stage = SmartSetupStage.ANALYZE
                        }
                    )
                }
            }

            SmartSetupStage.IMPORT -> {
                item {
                    ImportChoicePage(
                        selectedMonths =
                            importMonths,
                        customStartDateEpochMs = customImportStartDateEpochMs,
                        customEndDateEpochMs = customImportEndDateEpochMs,
                        canContinue = importSelectionValid,
                        onSelected = {
                            importMonths = it
                            smsDenied = false
                        },
                        onCustomStartDateSelected = {
                            customImportStartDateEpochMs = it
                            importMonths = CUSTOM_IMPORT_SELECTION
                            smsDenied = false
                        },
                        onCustomEndDateSelected = {
                            customImportEndDateEpochMs = it
                            importMonths = CUSTOM_IMPORT_SELECTION
                            smsDenied = false
                        },
                        onLater = {
                            importMonths = 0
                            smsDenied = false
                            stage = SmartSetupStage.ANALYZE
                        },
                        onContinue = {
                            if (
                                importMonths == 0 ||
                                (importSelectionValid && MessageAccessPolicy.canImport(
                                    disclosureAccepted, smsGranted, importMonths
                                ))
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

            SmartSetupStage.SMS_PERMISSION -> Unit

            SmartSetupStage.ANALYZE -> {
                item {
                    AnalysisPage(
                        importing =
                            importSelectionValid && MessageAccessPolicy.canImport(
                                disclosureAccepted, smsGranted, importMonths
                            ),
                        step = analysisStep,
                        reviewMode =
                            analysisReviewMode,
                        onContinue =
                            if (
                                analysisReviewMode
                            ) {
                                {
                                    analysisReviewMode =
                                        false
                                    stage =
                                        SmartSetupStage
                                            .REVIEW
                                }
                            } else {
                                null
                            }
                    )
                }
            }

            SmartSetupStage.REVIEW -> {
                item {
                    ReviewDiscoveryPage(
                        sources =
                            detectedSources,
                        ownershipCandidates =
                            pendingOwnershipEntities,
                        selectedOwnedKeys = selectedOwnedKeys,
                        importResult = importResult,
                        applying = reviewSaving,
                        onToggleAccount = { key, checked ->
                            selectedOwnedKeys = if (checked) selectedOwnedKeys + key
                            else selectedOwnedKeys - key
                        },
                        onAddMissingInstitution = { chooseInstitutions = true },
                        onContinue = {
                            setupScope.launch {
                                val wantsMpesa = detectedSources.any {
                                    it.reference.providerKey == "SAFARICOM_MPESA" &&
                                        it.reference.entityKey in selectedOwnedKeys
                                }
                                mpesaWasSelected = wantsMpesa
                                applySelectedAccountOwnership()
                                if (wantsMpesa && mpesaPhoneSuggestion.isNullOrBlank()) {
                                    manualPhoneMode = true
                                }
                                stage = if (wantsMpesa) SmartSetupStage.MPESA_CONFIRM
                                else nextAfterIdentitySetup()
                            }
                        }
                    )
                }
            }

            SmartSetupStage.SALARY_MANUAL -> {
                item {
                    ManualSalaryPage(
                        context = context,
                        accounts =
                            availableAccounts,
                        selectedAccountId =
                            manualSalaryAccountId,
                        payer =
                            manualSalaryPayer,
                        importMonths =
                            selectedHistoryMonths.coerceIn(0, 3),
                        historyMode =
                            manualSalaryHistoryMode,
                        drafts =
                            manualSalaryDrafts,
                        searchStatus =
                            salaryHistorySearchStatus,
                        error =
                            manualSalaryError,
                        saving =
                            manualSalarySaving,
                        savingLabel =
                            manualSalarySavingLabel,
                        onAccountSelected = {
                            manualSalaryAccountId =
                                it
                            manualSalaryError =
                                null
                            salaryHistorySearchStatus =
                                null
                        },
                        onPayerChanged = {
                            manualSalaryPayer =
                                it
                            manualSalaryError =
                                null
                            salaryHistorySearchStatus =
                                null
                        },
                        onHistoryModeChanged = {
                                mode ->
                            manualSalaryHistoryMode =
                                mode
                            manualSalaryDrafts =
                                defaultSmartSalaryDrafts(
                                    if (
                                        mode ==
                                        SalaryHistoryMode
                                            .LATEST_ONLY
                                    ) {
                                        1
                                    } else {
                                        importMonths
                                            .coerceAtLeast(
                                                1
                                            )
                                    }
                                )
                            salaryHistorySearchStatus =
                                null
                            manualSalaryError =
                                null
                        },
                        onSearchHistory = {
                            setupScope.launch {
                                searchImportedSalaryHistory()
                            }
                        },
                        onDraftAmountChanged = {
                                index,
                                value ->
                            manualSalaryDrafts =
                                manualSalaryDrafts
                                    .mapIndexed {
                                            itemIndex,
                                            draft ->
                                        if (
                                            itemIndex ==
                                            index
                                        ) {
                                            draft.copy(
                                                amount =
                                                    value,
                                                matchedTransactionId =
                                                    null
                                            )
                                        } else {
                                            draft
                                        }
                                    }
                            manualSalaryError =
                                null
                        },
                        onDraftDateSelected = {
                                index,
                                value ->
                            manualSalaryDrafts =
                                manualSalaryDrafts
                                    .mapIndexed {
                                            itemIndex,
                                            draft ->
                                        if (
                                            itemIndex ==
                                            index
                                        ) {
                                            draft.copy(
                                                paidAtEpochMs =
                                                    value,
                                                matchedTransactionId =
                                                    null
                                            )
                                        } else {
                                            draft
                                        }
                                    }
                            manualSalaryError =
                                null
                        },
                        onSave = {
                            if (!manualSalarySaving) {
                                manualSalarySaving =
                                    true
                                manualSalarySavingLabel =
                                    "Saving salary entries…"
                                manualSalaryError =
                                    null
                                setupScope.launch {
                                    try {
                                        if (
                                            applyManualSalary()
                                        ) {
                                            stage = SmartSetupStage.COMPLETE
                                        }
                                    } catch (
                                        error: Throwable
                                    ) {
                                        manualSalaryError =
                                            "PesaTrack could not save your salary yet. Please try again."
                                    } finally {
                                        manualSalarySaving =
                                            false
                                    }
                                }
                            }
                        },
                        onSkip = {
                            stage = SmartSetupStage.SALARY_OPTIONAL
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
                        busy =
                            mpesaConfirmationBusy,
                        lockedAccountChecked =
                            linkedSavingsChoices["NCBA_MSHWARI_LOCK_SAVINGS"] == OnboardingOwnershipChoice.MINE,
                        onLockedAccountChanged = { enabled ->
                            linkedSavingsChoices = if (enabled) {
                                linkedSavingsChoices + (
                                    "NCBA_MSHWARI_LOCK_SAVINGS" to OnboardingOwnershipChoice.MINE
                                )
                            } else {
                                linkedSavingsChoices - "NCBA_MSHWARI_LOCK_SAVINGS"
                            }
                        },
                        onManualChanged = {
                            manualMpesaPhone = it
                            phoneError = null
                        },
                        onConfirmSuggested = {
                            val suggested =
                                mpesaPhoneSuggestion
                                    .orEmpty()
                            if (
                                !mpesaConfirmationBusy
                            ) {
                                mpesaConfirmationBusy =
                                    true
                                setupScope.launch {
                                    try {
                                        if (
                                            confirmMpesaPhone(
                                                suggested
                                            )
                                        ) {
                                            if (linkedSavingsChoices["NCBA_MSHWARI_LOCK_SAVINGS"] != OnboardingOwnershipChoice.MINE ||
                                                applyLinkedSavingsChoices()
                                            ) {
                                                stage = nextAfterMpesaSetup()
                                            } else {
                                                phoneError = linkedSavingsError
                                            }
                                        }
                                    } finally {
                                        mpesaConfirmationBusy =
                                            false
                                    }
                                }
                            }
                        },
                        onChooseAnother = {
                            manualPhoneMode = true
                        },
                        onSaveManual = {
                            if (
                                !mpesaConfirmationBusy
                            ) {
                                mpesaConfirmationBusy =
                                    true
                                setupScope.launch {
                                    try {
                                        if (
                                            confirmMpesaPhone(
                                                manualMpesaPhone
                                            )
                                        ) {
                                            if (linkedSavingsChoices["NCBA_MSHWARI_LOCK_SAVINGS"] != OnboardingOwnershipChoice.MINE ||
                                                applyLinkedSavingsChoices()
                                            ) {
                                                stage = nextAfterMpesaSetup()
                                            } else {
                                                phoneError = linkedSavingsError
                                            }
                                        }
                                    } finally {
                                        mpesaConfirmationBusy =
                                            false
                                    }
                                }
                            }
                        },
                        onSkip = {
                            setupScope.launch {
                                    stage =
                                        nextAfterMpesaSetup()
                                }
                        }
                    )
                }
            }

            SmartSetupStage.ACCOUNT_DETAILS -> {
                item {
                    PesaTrackAccountDetailsPage(
                        accounts = availableAccounts,
                        profileName = profileName,
                        editedReferences = bankReferenceEdits,
                        lastFourHints = bankLastFourHints,
                        saving = identitySaving,
                        error = identityError,
                        onNameChanged = { profileName = it },
                        onReferenceChanged = { id, ref ->
                            bankReferenceEdits = bankReferenceEdits + (id to ref)
                            identityError = null
                        },
                        onLastFourChanged = { id, suffix ->
                            bankLastFourHints = bankLastFourHints + (id to suffix)
                            identityError = null
                        },
                        onSaveAndContinue = { saveAccountDetails() },
                        onSkip = { stage = SmartSetupStage.SALARY_OPTIONAL }
                    )
                }
            }

            SmartSetupStage.SALARY_OPTIONAL -> {
                item {
                    OnboardingSalaryChoicePage(
                        suggestion = salarySuggestion,
                        saving = salaryConfirmationSaving,
                        error = salaryConfirmationError,
                        onConfirmDetected = {
                            if (!salaryConfirmationSaving && salarySuggestion != null) {
                                salaryConfirmationSaving = true
                                salaryConfirmationError = null
                                salaryAccepted = true
                                setupScope.launch {
                                    try {
                                        applySalaryIfConfirmed()
                                        stage = SmartSetupStage.COMPLETE
                                    } catch (_: Exception) {
                                        salaryAccepted = false
                                        salaryConfirmationError = "Could not save salary yet. Please try again or skip."
                                    } finally {
                                        salaryConfirmationSaving = false
                                    }
                                }
                            }
                        },
                        onEnterManually = {
                            if (!salaryConfirmationSaving) {
                                salaryAccepted = false
                                stage = SmartSetupStage.SALARY_MANUAL
                            }
                        },
                        onNotMySalary = {
                            if (!salaryConfirmationSaving) {
                                salaryAccepted = false
                                stage = SmartSetupStage.COMPLETE
                            }
                        },
                        onSkip = {
                            if (!salaryConfirmationSaving) {
                                salaryAccepted = false
                                stage = SmartSetupStage.COMPLETE
                            }
                        }
                    )
                }
            }

            SmartSetupStage.LIVE_CAPTURE -> {
                item {
                    SmartLiveCapturePage(
                        alreadyEnabled =
                            notificationGranted,
                        settingsVisited =
                            notificationSettingsVisited,
                        restrictedInstall =
                            smartIsLikelySideloaded(
                                context
                            ),
                        restrictedPrepared =
                            !smartIsLikelySideloaded(
                                context
                            ) ||
                                smsAppInfoVisited,
                        onEnable = {
                            if (
                                notificationGranted
                            ) {
                                stage =
                                    SmartSetupStage
                                        .COMPLETE
                            } else if (
                                smartIsLikelySideloaded(
                                    context
                                ) &&
                                !smsAppInfoVisited
                            ) {
                                smsAppInfoVisited =
                                    true
                                appInfoLauncher.launch(
                                    Intent(
                                        Settings
                                            .ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse(
                                            "package:" +
                                                context.packageName
                                        )
                                    )
                                )
                            } else {
                                notificationSettingsVisited =
                                    true
                                settingsLauncher.launch(
                                    smartNotificationAccessIntent(
                                        context
                                    )
                                )
                            }
                        },
                        onOpenAppInfo = {
                            smsAppInfoVisited = true
                            appInfoLauncher.launch(
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
                        navigating = completionNavigating,
                        onHome = {
                            if (!completionNavigating) {
                                completionNavigating =
                                    true
                                onDone()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StageProgress(
    stage: SmartSetupStage
) {
    val steps = listOf(
        "Welcome", "Scan messages", "Review accounts", "M-Pesa identity",
        "Bank details", "Salary (optional)", "Ready"
    )
    val visibleIndex = when (stage) {
        SmartSetupStage.WELCOME -> 0
        SmartSetupStage.IMPORT, SmartSetupStage.SMS_PERMISSION,
        SmartSetupStage.ANALYZE -> 1
        SmartSetupStage.REVIEW -> 2
        SmartSetupStage.MPESA_CONFIRM -> 3
        SmartSetupStage.ACCOUNT_DETAILS -> 4
        SmartSetupStage.SALARY_OPTIONAL, SmartSetupStage.SALARY_MANUAL -> 5
        SmartSetupStage.LIVE_CAPTURE -> 5
        SmartSetupStage.COMPLETE -> 6
    }
    Text(
        text = "${visibleIndex + 1} of ${steps.size} • ${steps[visibleIndex]}",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(steps.size) { index ->
            Surface(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (index == visibleIndex) 25.dp else 7.dp, height = 7.dp),
                shape = CircleShape,
                color = if (index <= visibleIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            ) {}
        }
    }
}

@Composable
private fun ImportChoicePage(
    selectedMonths: Int,
    customStartDateEpochMs: Long,
    customEndDateEpochMs: Long,
    canContinue: Boolean,
    onSelected: (Int) -> Unit,
    onCustomStartDateSelected: (Long) -> Unit,
    onCustomEndDateSelected: (Long) -> Unit,
    onLater: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val today = startOfLocalDayEpochMs(System.currentTimeMillis())
    val customRange = OnboardingHistoryWindow.customRange(
        customStartDateEpochMs,
        customEndDateEpochMs
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(142.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(112.dp), shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.Message, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(62.dp))
                }
            }
        }
        Text("Choose your SMS history",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Text(
            "We'll look for financial activity in messages from M-Pesa, banks and other services.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Text("Scanning runs on this phone. Full message text stays here. Optional online business-name lookup is separate.",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("How far back should we check?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()) {
            listOf(1, 2).forEach { n ->
                FilterChip(
                    selected = selectedMonths == n,
                    onClick = { onSelected(n) },
                    label = { Text(if (n == 1) "1 month" else "2 months") },
                    modifier = Modifier.weight(1f)
                )
            }
            FilterChip(
                selected = selectedMonths == CUSTOM_IMPORT_SELECTION,
                onClick = { onSelected(CUSTOM_IMPORT_SELECTION) },
                label = { Text("Custom") },
                modifier = Modifier.weight(1f)
            )
        }

        if (selectedMonths == CUSTOM_IMPORT_SELECTION) {
            Text("Choose your dates", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val selected = Calendar.getInstance().apply {
                            timeInMillis = customStartDateEpochMs
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                onCustomStartDateSelected(pickerDateEpochMs(year, month, day))
                            },
                            selected.get(Calendar.YEAR),
                            selected.get(Calendar.MONTH),
                            selected.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            datePicker.maxDate = minOf(customEndDateEpochMs, today)
                        }.show()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                ) {
                    Text("From\n${formatOnboardingDate(customStartDateEpochMs)}",
                        textAlign = TextAlign.Center)
                }
                OutlinedButton(
                    onClick = {
                        val selected = Calendar.getInstance().apply {
                            timeInMillis = customEndDateEpochMs
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                onCustomEndDateSelected(pickerDateEpochMs(year, month, day))
                            },
                            selected.get(Calendar.YEAR),
                            selected.get(Calendar.MONTH),
                            selected.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            datePicker.minDate = customStartDateEpochMs
                            datePicker.maxDate = System.currentTimeMillis()
                        }.show()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                ) {
                    Text("To\n${formatOnboardingDate(customEndDateEpochMs)}",
                        textAlign = TextAlign.Center)
                }
            }
            Text(
                if (customRange != null) "Any past date range can be selected, including five or six months."
                else "Choose a valid past date range. Future dates aren't available.",
                style = MaterialTheme.typography.bodySmall,
                color = if (customRange == null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Includes completed previous months plus this month to date. You can change the range later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp)) {
            Text(if (selectedMonths == 0) "Continue" else "Review privacy & continue")
        }
        TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
            Text("Maybe later")
        }
    }
}

@Composable
private fun AnalysisPage(
    importing: Boolean,
    step: AnalysisStep,
    reviewMode: Boolean = false,
    onContinue: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp),
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
                if (
                    reviewMode &&
                    step ==
                        AnalysisStep.COMPLETE
                ) {
                    Icon(
                        Icons.Rounded
                            .CheckCircle,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                58.dp
                            ),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                } else {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(58.dp),
                        strokeWidth = 5.dp
                    )
                }
            }
        }

        Text(
            "Understanding your money…",
            modifier =
                Modifier.padding(top = 24.dp),
            style =
                MaterialTheme.typography
                    .headlineMedium,
            color =
                MaterialTheme.colorScheme
                    .onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            if (importing) {
                "PesaTrack is working through your selected history one step at a time."
            } else {
                "PesaTrack is preparing a clean starting point and will learn as new activity arrives."
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
                    Arrangement.spacedBy(16.dp)
            ) {
                AnalysisRow(
                    label =
                        "Scanning financial messages",
                    target =
                        AnalysisStep.SCANNING,
                    current = step
                )
                AnalysisRow(
                    label =
                        "Finding your accounts",
                    target =
                        AnalysisStep.ACCOUNTS,
                    current = step
                )
                AnalysisRow(
                    label =
                        "Looking for recurring income",
                    target =
                        AnalysisStep.INCOME,
                    current = step
                )
                AnalysisRow(
                    label =
                        "Checking duplicate evidence",
                    target =
                        AnalysisStep.DUPLICATES,
                    current = step
                )
            }
        }

        if (
            reviewMode &&
            onContinue != null
        ) {
            Button(
                onClick = onContinue,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .heightIn(
                            min = 54.dp
                        ),
                shape =
                    RoundedCornerShape(
                        18.dp
                    )
            ) {
                Text(
                    "Continue to what we found"
                )
            }
        }
    }
}

@Composable
private fun AnalysisRow(
    label: String,
    target: AnalysisStep,
    current: AnalysisStep
) {
    val complete =
        current == AnalysisStep.COMPLETE ||
            current.ordinal >
                target.ordinal
    val active =
        !complete &&
            current == target

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment =
                Alignment.Center
        ) {
            when {
                complete -> {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        modifier =
                            Modifier.size(24.dp)
                    )
                }

                active -> {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                }

                else -> {
                    Surface(
                        modifier =
                            Modifier.size(10.dp),
                        shape = CircleShape,
                        color =
                            MaterialTheme
                                .colorScheme
                                .outlineVariant
                    ) {}
                }
            }
        }

        Text(
            label,
            modifier =
                Modifier.padding(start = 12.dp),
            color =
                if (active || complete) {
                    MaterialTheme.colorScheme
                        .onSurface
                } else {
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
                },
            fontWeight =
                if (active) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
        )
    }
}

@Composable
private fun ManualSalaryPage(
    context: Context,
    accounts: List<MoneyAccount>,
    selectedAccountId: String,
    payer: String,
    importMonths: Int,
    historyMode: SalaryHistoryMode,
    drafts: List<SmartSalaryDraft>,
    searchStatus: String?,
    error: String?,
    saving: Boolean,
    savingLabel: String,
    onAccountSelected: (String) -> Unit,
    onPayerChanged: (String) -> Unit,
    onHistoryModeChanged:
        (SalaryHistoryMode) -> Unit,
    onSearchHistory: () -> Unit,
    onDraftAmountChanged:
        (Int, String) -> Unit,
    onDraftDateSelected:
        (Int, Long) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        SmartSetupHero(
            icon = Icons.Rounded.Payments,
            title = "Set up your salary history",
            body =
                "Payday starts a cash-flow cycle. Add the dates and amounts PesaTrack could not reliably learn from your messages."
        )

        PesaCard {
            Column(
                modifier =
                    Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Where does salary arrive?",
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurface
                )

                accounts.forEach { account ->
                    SalaryAccountChoice(
                        account = account,
                        selected =
                            account.id ==
                                selectedAccountId,
                        onClick = {
                            onAccountSelected(
                                account.id
                            )
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = payer,
            onValueChange = onPayerChanged,
            label = {
                Text(
                    "Payroll / statement name"
                )
            },
            placeholder = {
                Text(
                    "Example: XYZ PAYROLL"
                )
            },
            supportingText = {
                Text(
                    "Use the wording shown by your bank or M-Pesa. It can be different from your employer's legal name."
                )
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (
            importMonths > 0 &&
            payer.isNotBlank()
        ) {
            OutlinedButton(
                onClick = onSearchHistory,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                shape =
                    RoundedCornerShape(18.dp)
            ) {
                Text(
                    "Search imported history for this payroll name"
                )
            }
        }

        if (importMonths > 1) {
            Text(
                "How much salary history do you want to add?",
                style =
                    MaterialTheme.typography
                        .titleMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurface
            )

            SmartSelectableSetupCard(
                selected =
                    historyMode ==
                        SalaryHistoryMode
                            .IMPORT_WINDOW,
                title =
                    "Use the selected history",
                body =
                    "Add up to $importMonths paydays so PesaTrack can reconstruct earlier pay cycles.",
                icon =
                    Icons.Rounded.History,
                onClick = {
                    onHistoryModeChanged(
                        SalaryHistoryMode
                            .IMPORT_WINDOW
                    )
                }
            )

            SmartSelectableSetupCard(
                selected =
                    historyMode ==
                        SalaryHistoryMode
                            .LATEST_ONLY,
                title =
                    "Start with my latest salary",
                body =
                    "Skip older paydays and begin the cash-flow model from the most recent salary.",
                icon =
                    Icons.Rounded.Payments,
                onClick = {
                    onHistoryModeChanged(
                        SalaryHistoryMode
                            .LATEST_ONLY
                    )
                }
            )
        }

        searchStatus?.let { message ->
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(16.dp),
                color =
                    pesaToneContainer(
                        if (
                            message.startsWith(
                                "Found"
                            )
                        ) {
                            "green"
                        } else {
                            "amber"
                        }
                    )
            ) {
                Text(
                    message,
                    modifier =
                        Modifier.padding(14.dp),
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurface
                )
            }
        }

        drafts.forEachIndexed {
                index,
                draft ->
            PesaCard {
                Column(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
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
                                draft.monthLabel,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                            if (
                                draft.matchedTransactionId !=
                                null
                            ) {
                                Text(
                                    "Found in imported history",
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
                        }
                        if (
                            draft.matchedTransactionId !=
                            null
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription =
                                    "Matched from history",
                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = draft.amount,
                        onValueChange = {
                            onDraftAmountChanged(
                                index,
                                it
                            )
                        },
                        label = {
                            Text(
                                "Salary amount"
                            )
                        },
                        prefix = {
                            Text("KES ")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType
                                        .Decimal
                            ),
                        modifier =
                            Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedButton(
                        onClick = {
                            val initial =
                                Calendar
                                    .getInstance()
                                    .apply {
                                        draft
                                            .paidAtEpochMs
                                            ?.let {
                                                timeInMillis =
                                                    it
                                            }
                                    }

                            DatePickerDialog(
                                context,
                                {
                                        _,
                                        year,
                                        month,
                                        dayOfMonth ->
                                    val selected =
                                        Calendar
                                            .getInstance()
                                            .apply {
                                                clear()
                                                set(
                                                    year,
                                                    month,
                                                    dayOfMonth,
                                                    12,
                                                    0,
                                                    0
                                                )
                                            }
                                            .timeInMillis
                                    onDraftDateSelected(
                                        index,
                                        selected
                                    )
                                },
                                initial.get(
                                    Calendar.YEAR
                                ),
                                initial.get(
                                    Calendar.MONTH
                                ),
                                initial.get(
                                    Calendar
                                        .DAY_OF_MONTH
                                )
                            ).apply {
                                datePicker.maxDate =
                                    System
                                        .currentTimeMillis()
                            }.show()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 52.dp
                                )
                    ) {
                        Text(
                            draft.paidAtEpochMs
                                ?.let(
                                    ::formatSetupDate
                                )
                                ?: "Choose the actual payday"
                        )
                    }
                }
            }
        }

        Text(
            if (
                historyMode ==
                SalaryHistoryMode.IMPORT_WINDOW
            ) {
                "Leave an older month empty if you cannot recover it. PesaTrack will use only the paydays you confirm."
            } else {
                "PesaTrack will start from this payday. Older calendar activity remains visible, but it will not be treated as an earlier salary cycle."
            },
            style =
                MaterialTheme.typography
                    .bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        error?.let { message ->
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(16.dp),
                color =
                    MaterialTheme.colorScheme
                        .errorContainer
            ) {
                Text(
                    message,
                    modifier =
                        Modifier.padding(14.dp),
                    color =
                        MaterialTheme.colorScheme
                            .onErrorContainer
                )
            }
        }

        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape =
                RoundedCornerShape(18.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    savingLabel,
                    modifier =
                        Modifier.padding(
                            start = 10.dp
                        )
                )
            } else {
                Text(
                    if (
                        historyMode ==
                        SalaryHistoryMode
                            .IMPORT_WINDOW &&
                        drafts.size > 1
                    ) {
                        "Save salary history and continue"
                    } else {
                        "Save salary and continue"
                    }
                )
            }
        }

        TextButton(
            onClick = onSkip,
            enabled = !saving,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}

@Composable
private fun SalaryAccountChoice(
    account: MoneyAccount,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape =
            RoundedCornerShape(16.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme
                    .primaryContainer
            } else {
                MaterialTheme.colorScheme
                    .surfaceVariant
                    .copy(alpha = .55f)
            }
    ) {
        Row(
            modifier =
                Modifier.padding(13.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon =
                    when (account.type) {
                        AccountType.BANK ->
                            Icons.Rounded
                                .AccountBalance
                        AccountType.MOBILE_WALLET ->
                            Icons.Rounded
                                .PhoneAndroid
                        AccountType.CASH ->
                            Icons.Rounded.Wallet
                        else ->
                            Icons.Rounded.Payments
                    },
                size = 40.dp
            )
            Text(
                AccountIdentityPresentation
                    .title(account),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                style =
                    MaterialTheme.typography
                        .titleSmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurface
            )
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription =
                        "Selected",
                    tint =
                        MaterialTheme.colorScheme
                            .primary
                )
            }
        }
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

@Composable
private fun SetupCompletePage(
    navigating: Boolean,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Surface(
            modifier = Modifier.size(112.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp))
            }
        }
        Text("You're all set!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Text("We've set up your accounts and are ready to start tracking your financial data.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center)
        PesaCard {
            Column(Modifier.fillMaxWidth().padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("What happens next?",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                listOf(
                    "Import your transactions",
                    "Automatically categorise your income and expenses",
                    "Track balances, debts and savings",
                    "You can always add or edit accounts later"
                ).forEach { label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp))
                        Text(label,
                            modifier = Modifier.padding(start = 10.dp),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Button(
            onClick = onHome, enabled = !navigating,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (navigating) "Opening…" else "Go to dashboard") }
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
