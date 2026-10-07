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
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.data.local.EntityEnrichmentPreferences
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentFetchResult
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentHttpClient
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingDiscoveryEngine
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingTransactionObservation
import com.kedevjimmy.pesatrack.domain.intelligence.EntityDescriptorPrivacyPolicy
import com.kedevjimmy.pesatrack.domain.intelligence.SalaryCandidateRemotePolicy
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.domain.rules.HistoryImportWindow
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
    IMPORT,
    SMS_PERMISSION,
    ANALYZE,
    REVIEW,
    SALARY_MANUAL,
    MPESA_CONFIRM,
    LIVE_CAPTURE,
    COMPLETE
}

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
            EntityEnrichmentHttpClient()
        }
    val onlineEntityLookupEnabled =
        remember {
            EntityEnrichmentPreferences
                .get(context)
                .isOnlineLookupEnabled()
        }

    var stage by rememberSaveable {
        mutableStateOf(SmartSetupStage.SMS_PERMISSION)
    }
    var importMonths by rememberSaveable {
        mutableStateOf(3)
    }
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
    var selectedOwnershipEntityKeys by remember {
        mutableStateOf<Set<String>>(
            emptySet()
        )
    }
    var reviewSaving by remember {
        mutableStateOf(false)
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
            }
        }

    val settingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
            notificationGranted =
                smartHasNotificationAccess(context)
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

    LaunchedEffect(
        stage,
        smsGranted,
        smsPromptLaunched
    ) {
        if (
            stage ==
            SmartSetupStage.SMS_PERMISSION
        ) {
            if (smsGranted) {
                // Stay on the Connect Messages concept
                // until the user chooses Continue.
            } else if (
                smartIsLikelySideloaded(
                    context
                ) &&
                !smsAppInfoVisited
            ) {
                // Sideloaded builds can be blocked by
                // Android restricted-settings protection.
                // The user must explicitly enable that
                // system setting before we request SMS.
            } else if (!smsPromptLaunched) {
                smsPromptLaunched = true
                smsPermissionLauncher.launch(
                    Manifest.permission.READ_SMS
                )
            }
        }
    }

    LaunchedEffect(
        stage,
        importMonths,
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

        val startEpochMs =
            HistoryImportWindow
                .importStartEpochMs(
                    importMonths
                )
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
                                type == null ||
                                name == null
                            ) {
                                null
                            } else {
                                name to type
                            }
                        }
                        .groupBy {
                            it.first
                                .lowercase(
                                    Locale.ROOT
                                )
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
        selectedOwnershipEntityKeys =
            emptySet()
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
            incomeDiscovery.first != null
        mpesaPhoneSuggestion =
            incomeDiscovery.second
        delay(MIN_PROGRESS_STEP_MS)

        analysisStep =
            AnalysisStep.DUPLICATES
        withContext(Dispatchers.IO) {
            transactionStore
                .onboardingDuplicateCandidateCount(
                    cutoff
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
        importMonths
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
                    importMonths
                        .coerceAtLeast(1)
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

        val startEpochMs =
            HistoryImportWindow
                .importStartEpochMs(
                    importMonths
                        .coerceAtLeast(1)
                )
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
                importMonths
                    .coerceAtLeast(1)
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
            financialUnderstandingStore
                .confirmProviderOwned(
                    providerKey =
                        "NCBA_MSHWARI_SAVINGS"
                )
            financialUnderstandingStore
                .confirmProviderOwned(
                    providerKey =
                        "NCBA_MSHWARI_LOCK_SAVINGS"
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
            val selectedKeys =
                selectedOwnershipEntityKeys
            withContext(Dispatchers.IO) {
                selectedKeys.forEach {
                        entityKey ->
                    financialUnderstandingStore
                        .confirmEntityOwned(
                            entityKey
                        )
                    financialCoordinator
                        .reconcilePendingTransfers(
                            entityKey =
                                entityKey,
                            rebuildPayCycles =
                                false
                        )
                }

                if (
                    selectedKeys.isNotEmpty() &&
                    smsGranted &&
                    importMonths > 0
                ) {
                    HistoryImportWindow
                        .importStartEpochMs(
                            importMonths
                        )
                        ?.let {
                            startEpochMs ->
                            HistoricalSmsImporter(
                                context
                                    .applicationContext
                            ).importSince(
                                sinceEpochMs =
                                    startEpochMs,
                                rebuildPayCycles =
                                    false
                            )
                        }
                }

                if (
                    selectedKeys.isNotEmpty()
                ) {
                    payCycleStore.rebuild()
                }
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
            selectedOwnershipEntityKeys =
                emptySet()

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

            val cutoff =
                HistoryImportWindow
                    .importStartEpochMs(
                        importMonths
                            .coerceAtLeast(1)
                    )
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
                                    cutoff
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

            salarySuggestion =
                refreshedSalary
            salaryAccepted =
                refreshedSalary != null
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
        SmartSetupStage.COMPLETE

    val onboardingBusy =
        reviewSaving ||
            mpesaConfirmationBusy ||
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
            stage !=
                SmartSetupStage.SMS_PERMISSION
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

                SmartSetupStage
                    .SALARY_MANUAL ->
                    SmartSetupStage.REVIEW

                SmartSetupStage
                    .MPESA_CONFIRM ->
                    SmartSetupStage.REVIEW

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
                    if (
                        mpesaPhoneSuggestion !=
                        null
                    ) {
                        SmartSetupStage
                            .MPESA_CONFIRM
                    } else {
                        SmartSetupStage.REVIEW
                    }

                SmartSetupStage.IMPORT ->
                    SmartSetupStage.SMS_PERMISSION
            }
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
                    SmartSmsPermissionPage(
                        smsGranted = smsGranted,
                        denied = smsDenied,
                        restrictedHelp =
                            !smsGranted &&
                                smartIsLikelySideloaded(
                                    context
                                ) &&
                                (
                                    !smsAppInfoVisited ||
                                        smsDenied
                                    ),
                        notificationEnabled =
                            notificationGranted,
                        onOpenAppInfo = {
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
                        },
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
                        onEnableNotifications = {
                            if (notificationGranted) {
                                Unit
                            } else if (
                                smartIsLikelySideloaded(
                                    context
                                ) &&
                                !smsAppInfoVisited
                            ) {
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
                        onContinue = {
                            stage =
                                SmartSetupStage.IMPORT
                        },
                        onContinueWithout = {
                            importMonths = 0
                            stage =
                                SmartSetupStage.IMPORT
                        }
                    )
                }
            }

            SmartSetupStage.ANALYZE -> {
                item {
                    AnalysisPage(
                        importing =
                            importMonths > 0 &&
                                smsGranted,
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
                        selectedOwnershipEntityKeys =
                            selectedOwnershipEntityKeys,
                        salarySuggestion =
                            salarySuggestion,
                        salaryAccepted =
                            salaryAccepted,
                        importResult =
                            importResult,
                        applying =
                            reviewSaving,
                        onSalaryAccepted = {
                            salaryAccepted = it
                        },
                        onToggleAccount = {
                                entity ->
                            selectedOwnershipEntityKeys =
                                if (
                                    entity.entityKey in
                                    selectedOwnershipEntityKeys
                                ) {
                                    selectedOwnershipEntityKeys -
                                        entity.entityKey
                                } else {
                                    selectedOwnershipEntityKeys +
                                        entity.entityKey
                                }
                        },
                        onManualSalary = {
                            setupScope.launch {
                                applySelectedAccountOwnership()
                                stage =
                                    SmartSetupStage
                                        .SALARY_MANUAL
                            }
                        },
                        onContinue = {
                            setupScope.launch {
                                applySelectedAccountOwnership()
                                applySalaryIfConfirmed()
                                stage =
                                    if (
                                        mpesaPhoneSuggestion !=
                                        null
                                    ) {
                                        SmartSetupStage
                                            .MPESA_CONFIRM
                                    } else {
                                        nextAfterIdentitySetup()
                                    }
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
                            importMonths,
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
                                            stage =
                                                if (
                                                    mpesaPhoneSuggestion !=
                                                    null
                                                ) {
                                                    SmartSetupStage
                                                        .MPESA_CONFIRM
                                                } else {
                                                    nextAfterIdentitySetup()
                                                }
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
                            stage =
                                if (
                                    mpesaPhoneSuggestion !=
                                    null
                                ) {
                                    SmartSetupStage
                                        .MPESA_CONFIRM
                                } else {
                                    nextAfterIdentitySetup()
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
                        busy =
                            mpesaConfirmationBusy,
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
                                            applySalaryIfConfirmed()
                                            stage =
                                                nextAfterIdentitySetup()
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
                                            applySalaryIfConfirmed()
                                            stage =
                                                nextAfterIdentitySetup()
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
                                    applySalaryIfConfirmed()
                                    stage =
                                        nextAfterIdentitySetup()
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
                        sources =
                            detectedSources,
                        salaryAccepted =
                            salaryApplied ||
                                (
                                    salaryAccepted &&
                                        salarySuggestion !=
                                        null
                                    ),
                        notificationEnabled =
                            notificationGranted,
                        navigating =
                            completionNavigating,
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
    val visibleIndex = when (stage) {
        SmartSetupStage.SMS_PERMISSION -> 0
        SmartSetupStage.IMPORT, SmartSetupStage.ANALYZE,
        SmartSetupStage.REVIEW, SmartSetupStage.SALARY_MANUAL,
        SmartSetupStage.MPESA_CONFIRM -> 1
        SmartSetupStage.LIVE_CAPTURE, SmartSetupStage.COMPLETE -> 2
    }
    Text(
        text = "${visibleIndex + 1} of 3 • " +
            listOf("Connect messages", "Import history", "Ready")[visibleIndex],
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.Center
    ) {
        repeat(3) { index ->
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SmartSetupHero(
            icon = Icons.Rounded.History,
            title = "Start with your recent money",
            body = "We'll identify transactions and match the accounts already visible in your messages."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1, 2, 3).forEach { months ->
                val selected = selectedMonths == months
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 92.dp)
                        .clickable { onSelected(months) },
                    shape = RoundedCornerShape(18.dp),
                    color =
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                ) {
                    Column(
                        Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            months.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (months == 1) "month" else "months",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(top = 5.dp).size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (selectedMonths > 0) {
                        "We'll scan messages from the last $selectedMonths " +
                            if (selectedMonths == 1) "month." else "months."
                    } else {
                        "History import is off. You can start fresh."
                    },
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedButton(
            onClick = { onSelected(0) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(if (selectedMonths == 0) "Starting fresh" else "Not now — start fresh")
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(if (selectedMonths == 0) "Continue" else "Scan my messages")
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            "You can change the history window later. Raw message text is not kept as your ledger.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
internal fun OwnershipCandidateCard(
    entity: DiscoveredFinancialEntity,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    PesaCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        onClick = onToggle
                    )
                    .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon =
                    when (
                        runCatching {
                            AccountType
                                .valueOf(
                                    entity
                                        .accountTypeHint
                                )
                        }.getOrDefault(
                            AccountType.OTHER
                        )
                    ) {
                        AccountType.BANK ->
                            Icons.Rounded
                                .AccountBalance
                        AccountType.MOBILE_WALLET ->
                            Icons.Rounded
                                .PhoneAndroid
                        else ->
                            Icons.Rounded
                                .Savings
                    },
                size = 44.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(
                    entity.displayName,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    "Detected account • ownership not assumed",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription =
                        "Selected as my account",
                    tint =
                        MaterialTheme.colorScheme
                            .primary
                )
            } else {
                Text(
                    "This is mine",
                    color =
                        MaterialTheme.colorScheme
                            .primary,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun DiscoveredSourceCard(
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
private fun MpesaConfirmationPage(
    suggestedPhone: String,
    manualMode: Boolean,
    manualPhone: String,
    error: String?,
    busy: Boolean,
    onManualChanged: (String) -> Unit,
    onConfirmSuggested: () -> Unit,
    onChooseAnother: () -> Unit,
    onSaveManual: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
    SmartSetupHero(
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
                "M-Pesa (" +
                    maskPhone(
                        suggestedPhone
                    ) +
                    ")",
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
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    "Confirming…",
                    modifier =
                        Modifier.padding(
                            start = 10.dp
                        )
                )
            } else {
                Text("Yes, this is mine")
            }
        }

        OutlinedButton(
            onClick = onChooseAnother,
            enabled = !busy,
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
            enabled = !busy,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    "Confirming…",
                    modifier =
                        Modifier.padding(
                            start = 10.dp
                        )
                )
            } else {
                Text("Save and continue")
            }
        }
    }

    TextButton(
        onClick = onSkip,
        enabled = !busy,
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
}

@Composable
private fun SetupCompletePage(
    sources: List<DetectedSetupSource>,
    salaryAccepted: Boolean,
    notificationEnabled: Boolean,
    navigating: Boolean,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(104.dp),
            shape = CircleShape,
            color = pesaToneContainer("green")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = pesaToneContent("green"),
                    modifier = Modifier.size(62.dp)
                )
            }
        }

        Text(
            "Your money picture\nis ready",
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "We've matched what PesaTrack can prove from your transaction messages.",
            modifier = Modifier.padding(top = 7.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (sources.isNotEmpty()) {
            PesaCard {
                Column(
                    Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    sources.take(4).forEach { source ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBubble(
                                if (source.type.contains("BANK", true)) Icons.Rounded.AccountBalance
                                else Icons.Rounded.AccountBalanceWallet,
                                size = 40.dp
                            )
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(source.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    source.messageCount.toString() + " matching message" +
                                        if (source.messageCount == 1) "" else "s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            PesaCard {
                CompletionRow("Accounts will be learned as messages arrive")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompletionRow(if (salaryAccepted) "Salary source matched" else "Salary can be added later")
                CompletionRow(if (notificationEnabled) "Live tracking enabled" else "Live tracking can be enabled later")
            }
        }

        Button(
            onClick = onHome,
            enabled = !navigating,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            if (navigating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Opening dashboard…", modifier = Modifier.padding(start = 10.dp))
            } else {
                Text("Open my dashboard")
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
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

