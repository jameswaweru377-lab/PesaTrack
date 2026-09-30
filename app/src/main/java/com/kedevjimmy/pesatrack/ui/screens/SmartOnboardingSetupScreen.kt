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
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Message
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
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingDiscoveryEngine
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingSalarySuggestion
import com.kedevjimmy.pesatrack.domain.onboarding.OnboardingTransactionObservation
import com.kedevjimmy.pesatrack.domain.onboarding.SalarySignalStrength
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.HistoryImportWindow
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
    var pendingOwnershipEntities by remember {
        mutableStateOf<
            List<DiscoveredFinancialEntity>
        >(emptyList())
    }
    var selectedOwnershipKeys by rememberSaveable {
        mutableStateOf<List<String>>(
            emptyList()
        )
    }
    var reviewApplying by remember {
        mutableStateOf(false)
    }
    var reviewError by remember {
        mutableStateOf<String?>(null)
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
    var mpesaConfirming by remember {
        mutableStateOf(false)
    }
    var stageBeforeMpesa by rememberSaveable {
        mutableStateOf(
            SmartSetupStage.REVIEW
        )
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
    var analysisStep by remember {
        mutableStateOf(
            AnalysisStep.SCANNING
        )
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

    val appInfoLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
            smsGranted =
                hasSmsPermission(context)
            if (smsGranted) {
                stage =
                    SmartSetupStage.ANALYZE
            } else if (
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
                stage =
                    SmartSetupStage.ANALYZE
            } else if (
                isLikelySideloaded(
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
                    OnboardingDiscoveryEngine
                        .salarySuggestion(
                            observations
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
            incomeDiscovery.first
                ?.signalStrength ==
                SalarySignalStrength
                    .EXPLICIT_PAYROLL
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

    suspend fun refreshSalarySuggestionFromLedger() {
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

        val refreshed =
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

                OnboardingDiscoveryEngine
                    .salarySuggestion(
                        observations
                    )
            }

        salarySuggestion = refreshed
        salaryAccepted =
            refreshed?.signalStrength ==
                SalarySignalStrength
                    .EXPLICIT_PAYROLL
        refreshed?.accountId?.let {
            accountId ->
            if (
                availableAccounts.any {
                    it.id == accountId
                }
            ) {
                manualSalaryAccountId =
                    accountId
            }
        }
    }

    suspend fun applySelectedOwnerships() {
        val selected =
            selectedOwnershipKeys
                .toSet()
        val entities =
            pendingOwnershipEntities
                .filter {
                    it.entityKey in
                        selected
                }

        if (entities.isNotEmpty()) {
            withContext(
                Dispatchers.IO
            ) {
                entities.forEach {
                        entity ->
                    financialUnderstandingStore
                        .confirmEntityOwned(
                            entity.entityKey
                        )
                    financialCoordinator
                        .reconcilePendingTransfers(
                            entity.entityKey
                        )
                }

                if (
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
                                startEpochMs
                            )
                        }
                }

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
        selectedOwnershipKeys =
            emptyList()

        if (
            manualSalaryAccountId
                .isBlank() ||
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

        refreshSalarySuggestionFromLedger()
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

        val latestByMonth =
            candidates
                .groupBy {
                    salaryMonthKey(
                        it.occurredAtEpochMs
                    )
                }
                .values
                .mapNotNull { matches ->
                    matches.maxByOrNull {
                        it.occurredAtEpochMs
                    }
                }
                .sortedByDescending {
                    it.occurredAtEpochMs
                }
                .take(desired)

        val baseDrafts =
            defaultSmartSalaryDrafts(
                desired
            )
        var found = 0
        manualSalaryDrafts =
            baseDrafts.mapIndexed {
                    index,
                    draft ->
                val match =
                    latestByMonth
                        .getOrNull(index)
                if (match == null) {
                    draft
                } else {
                    found += 1
                    draft.copy(
                        monthKey =
                            salaryMonthKey(
                                match.occurredAtEpochMs
                            ),
                        amount =
                            formatSetupAmountInput(
                                match.amountMinor
                            ),
                        paidAtEpochMs =
                            match.occurredAtEpochMs,
                        matchedTransactionId =
                            match.transactionId
                    )
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

            transactionStore
                .reapplyRule(
                    salaryRule
                )

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
                                salaryRule
                        )
                }
            }

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
                .reconcileOwnAccountTransfers()
            payCycleStore.rebuild()
        }
        phoneError = null
        return true
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
                    SmsPermissionPage(
                        denied = smsDenied,
                        restrictedHelp =
                            isLikelySideloaded(
                                context
                            ) &&
                                (
                                    !smsAppInfoVisited ||
                                        smsDenied
                                    ),
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
                                smsGranted,
                        step = analysisStep
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
                        salarySuggestion =
                            salarySuggestion,
                        salaryAccepted =
                            salaryAccepted,
                        importResult =
                            importResult,
                        selectedOwnershipKeys =
                            selectedOwnershipKeys
                                .toSet(),
                        applying =
                            reviewApplying,
                        error =
                            reviewError,
                        onSalaryAccepted = {
                            salaryAccepted = it
                        },
                        onToggleAccount = {
                                entity ->
                            val current =
                                selectedOwnershipKeys
                                    .toMutableSet()
                            if (
                                entity.entityKey in
                                current
                            ) {
                                current.remove(
                                    entity.entityKey
                                )
                            } else {
                                current.add(
                                    entity.entityKey
                                )
                            }
                            selectedOwnershipKeys =
                                current.toList()
                            reviewError = null
                        },
                        onManualSalary = {
                            if (!reviewApplying) {
                                setupScope.launch {
                                    reviewApplying =
                                        true
                                    reviewError = null
                                    try {
                                        applySelectedOwnerships()
                                        stage =
                                            SmartSetupStage
                                                .SALARY_MANUAL
                                    } catch (
                                        _: Exception
                                    ) {
                                        reviewError =
                                            "Could not save those account choices. Please try again."
                                    } finally {
                                        reviewApplying =
                                            false
                                    }
                                }
                            }
                        },
                        onContinue = {
                            if (!reviewApplying) {
                                setupScope.launch {
                                    reviewApplying =
                                        true
                                    reviewError = null
                                    try {
                                        applySelectedOwnerships()
                                        applySalaryIfConfirmed()
                                        stageBeforeMpesa =
                                            SmartSetupStage.REVIEW
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
                                    } catch (
                                        _: Exception
                                    ) {
                                        reviewError =
                                            "Could not finish these setup choices. Please try again."
                                    } finally {
                                        reviewApplying =
                                            false
                                    }
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
                            setupScope.launch {
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
                                            SmartSetupStage
                                                .LIVE_CAPTURE
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
                                    SmartSetupStage
                                        .LIVE_CAPTURE
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
                        restrictedInstall =
                            isLikelySideloaded(
                                context
                            ),
                        restrictedPrepared =
                            !isLikelySideloaded(
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
                                isLikelySideloaded(
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
                                    notificationAccessIntent(
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
                        onHome = onDone
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
    val visibleIndex =
        when (stage) {
            SmartSetupStage.IMPORT -> 0
            SmartSetupStage.SMS_PERMISSION -> 1
            SmartSetupStage.ANALYZE -> 2
            SmartSetupStage.REVIEW -> 3
            SmartSetupStage.SALARY_MANUAL -> 4
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
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
                2,
                "Last 2 months",
                "A balanced amount of recent history"
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
                    if (option.first == 0) {
                        Icons.Rounded.Lock
                    } else {
                        Icons.Rounded.History
                    },
                onClick = {
                    onSelected(option.first)
                }
            )
        }

        error?.let { message ->
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        16.dp
                    ),
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
            onClick = onContinue,
            enabled = !applying,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape =
                RoundedCornerShape(18.dp)
        ) {
            Text("Continue")
            Icon(
                Icons.Rounded.ArrowForward,
                contentDescription = null,
                modifier =
                    Modifier.padding(
                        start = 8.dp
                    )
            )
        }

        Text(
            "You can import or re-import history later from Settings.",
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
private fun SmsPermissionPage(
    denied: Boolean,
    restrictedHelp: Boolean,
    onOpenAppInfo: () -> Unit,
    onTryAgain: () -> Unit,
    onContinueWithout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        SetupHero(
            icon = Icons.Rounded.Message,
            title = "Allow access to your messages",
            body =
                "Android controls this permission. PesaTrack uses it only to read the history you chose."
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
                    when {
                        restrictedHelp ->
                            "Android blocked SMS access"
                        denied ->
                            "SMS access wasn't allowed"
                        else ->
                            "Waiting for Android permission"
                    },
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    color =
                        MaterialTheme.colorScheme
                            .onSurface,
                    textAlign =
                        TextAlign.Center
                )
                Text(
                    when {
                        restrictedHelp ->
                            "This debug APK was installed outside a trusted app store. On this phone, Android requires you to enable “Allow restricted settings” in App info first."
                        denied ->
                            "You can try again or continue without importing old messages."
                        else ->
                            "The system Allow / Don't allow dialog should appear automatically."
                    },
                    textAlign =
                        TextAlign.Center,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }

        if (restrictedHelp) {
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(18.dp),
                color =
                    pesaToneContainer("amber")
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "One Android step is required",
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                    )
                    Text(
                        "Open App info → turn on Allow restricted settings → return to PesaTrack. The SMS permission request will open again automatically.",
                        style =
                            MaterialTheme.typography
                                .bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenAppInfo,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                    ) {
                        Text("Open App info")
                    }
                }
            }
        } else if (denied) {
            Button(
                onClick = onTryAgain,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("Try again")
            }
        }

        if (denied) {
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
            "PesaTrack cannot switch Android's restricted-settings protection on by itself. That decision always belongs to you.",
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
private fun AnalysisPage(
    importing: Boolean,
    step: AnalysisStep
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
private fun ReviewDiscoveryPage(
    sources: List<DetectedSetupSource>,
    ownershipCandidates:
        List<DiscoveredFinancialEntity>,
    salarySuggestion:
        OnboardingSalarySuggestion?,
    salaryAccepted: Boolean,
    importResult: HistoricalImportResult?,
    selectedOwnershipKeys: Set<String>,
    applying: Boolean,
    error: String?,
    onSalaryAccepted: (Boolean) -> Unit,
    onToggleAccount:
        (DiscoveredFinancialEntity) -> Unit,
    onManualSalary: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        SetupHero(
            icon = Icons.Rounded.CheckCircle,
            title = "Here's what we found",
            body =
                "These are signals from your real messages. Confirm what looks right; PesaTrack will not invent missing accounts."
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

        if (
            ownershipCandidates.isNotEmpty()
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
                                selectedOwnershipKeys,
                        enabled = !applying,
                        onToggle = {
                            onToggleAccount(
                                entity
                            )
                        }
                    )
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
                            "Some banks don't send a useful salary SMS. Add one recent salary manually so Money In and your current pay cycle start correctly.",
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

        if (
            salarySuggestion == null ||
            !salaryAccepted
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

        Button(
            onClick = onContinue,
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
                    "Saving…",
                    modifier =
                        Modifier.padding(
                            start = 8.dp
                        )
                )
            } else {
                Text(
                    if (
                        salarySuggestion == null ||
                        !salaryAccepted
                    ) {
                        "Continue without salary"
                    } else {
                        "Looks good"
                    }
                )
            }
        }
    }
}

@Composable
private fun OwnershipCandidateCard(
    entity: DiscoveredFinancialEntity,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onToggle
                ),
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
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            start = 11.dp
                        )
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
                    if (selected) {
                        "Selected as one of your accounts"
                    } else {
                        "Detected account • ownership not assumed"
                    },
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
                        "Selected",
                    tint =
                        MaterialTheme.colorScheme
                            .primary
                )
            } else {
                Text(
                    "This is mine",
                    style =
                        MaterialTheme.typography
                            .labelLarge,
                    color =
                        MaterialTheme.colorScheme
                            .primary
                )
            }
        }
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
        SetupHero(
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

            SelectableSetupCard(
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

            SelectableSetupCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape =
                RoundedCornerShape(18.dp)
        ) {
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

        TextButton(
            onClick = onSkip,
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
                account.name,
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
}

@Composable
private fun LiveCapturePage(
    alreadyEnabled: Boolean,
    settingsVisited: Boolean,
    restrictedInstall: Boolean,
    restrictedPrepared: Boolean,
    onEnable: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onLater: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
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
            when {
                alreadyEnabled ->
                    "Continue"
                restrictedInstall &&
                    !restrictedPrepared ->
                    "Open App info first"
                else ->
                    "Enable live tracking"
            }
        )
    }

    if (
        restrictedInstall &&
        !restrictedPrepared &&
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
                    "Debug install protection",
                    style =
                        MaterialTheme.typography
                            .titleMedium
                )
                Text(
                    "Because this APK was installed outside Google Play, Android may block Notification Access until you manually turn on “Allow restricted settings” in App info.",
                    modifier =
                        Modifier.padding(top = 4.dp),
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
            color =
                MaterialTheme.colorScheme
                    .onBackground,
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
            color =
                MaterialTheme.colorScheme
                    .onBackground,
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

    return List(count) {
            index ->
        SmartSalaryDraft(
            monthKey =
                "payday-slot-$index",
            monthLabel =
                when (index) {
                    0 ->
                        "Most recent salary"
                    1 ->
                        "Previous salary"
                    else ->
                        "$index salaries ago"
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

private fun isLikelySideloaded(
    context: Context
): Boolean {
    val installer =
        runCatching {
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {
                context.packageManager
                    .getInstallSourceInfo(
                        context.packageName
                    )
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager
                    .getInstallerPackageName(
                        context.packageName
                    )
            }
        }.getOrNull()

    return installer
        ?.contains(
            "vending",
            ignoreCase = true
        ) != true
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

private const val ONE_DAY_MS =
    24L * 60L * 60L * 1000L

private const val MIN_PROGRESS_STEP_MS =
    180L

private const val SEVEN_DAYS_MS =
    7L * 24L * 60L * 60L * 1000L
