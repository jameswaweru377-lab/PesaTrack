package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBank
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.HistoryImportWindow
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import com.kedevjimmy.pesatrack.domain.rules.HistoryMonth
import com.kedevjimmy.pesatrack.ingestion.HistoricalSmsImporter
import com.kedevjimmy.pesatrack.ingestion.SalarySmsCandidate
import com.kedevjimmy.pesatrack.ingestion.SalarySmsCandidateFinder
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SetupStage {
    SOURCES,
    ROUTES,
    HISTORY,
    IDENTIFY,
    PAY_WINDOW,
    SALARY_HISTORY,
    ACCOUNTS
}

private enum class IncomeSourceChoice(
    val label: String,
    val helper: String
) {
    SALARY("Salary", "Regular pay from an employer"),
    BUSINESS("Business income", "Profit or drawings from a business"),
    FREELANCE("Freelance / side income", "Client or project payments"),
    OTHER("Other regular income", "Another source you receive repeatedly")
}

private enum class ArrivalChoice(
    val label: String,
    val helper: String
) {
    BANK("Bank account", "Money lands in a bank account"),
    MPESA("M-Pesa", "Money arrives directly in M-Pesa"),
    CASH("Cash", "Money is received physically")
}

private enum class PayWindowChoice(
    val label: String,
    val helper: String,
    val startDay: Int?,
    val endDay: Int?
) {
    VARIES("My payday varies", "Don't assume a fixed payday.", null, null),
    LATE_MONTH("Usually 24th–28th", "Salary normally arrives near month end.", 24, 28),
    MONTH_END("Around month end", "Usually between the 24th and the 1st.", 24, 1),
    MID_MONTH("Usually 14th–18th", "Salary normally arrives mid-month.", 14, 18),
    CUSTOM("Choose my own range", "Set the earliest and latest usual payday.", null, null)
}

private data class IncomeRouteDraft(
    val arrival: ArrivalChoice? = null,
    val selectedBank: KenyanBank? = null,
    val customBank: String = "",
    val bankQuery: String = ""
)

private data class ManualSalaryDraft(
    val amount: String = "",
    val day: String = ""
)

private data class ManualSalaryEntry(
    val amountMinor: Long,
    val occurredAtEpochMs: Long
)

@Composable
fun InitialMoneySetupScreen(
    scaffoldPadding: PaddingValues,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val accountStore = remember { AccountStore.get(context) }
    val ruleStore = remember { RuleStore.get(context) }
    val transactionStore = remember { TransactionStore.get(context) }
    val payCycleStore = remember { PayCycleStore.get(context) }
    val setupScope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(SetupStage.SOURCES) }
    var selectedSources by remember {
        mutableStateOf(setOf<IncomeSourceChoice>())
    }
    var routes by remember {
        mutableStateOf(mapOf<IncomeSourceChoice, IncomeRouteDraft>())
    }
    var routeIndex by remember { mutableStateOf(0) }

    var historyMonthsCount by remember { mutableStateOf(0) }
    var smsGranted by remember {
        mutableStateOf(hasSmsPermission(context))
    }
    var smsDenied by remember { mutableStateOf(false) }

    var employer by remember { mutableStateOf("") }
    var selectedCandidate by remember {
        mutableStateOf<SalarySmsCandidate?>(null)
    }
    var candidates by remember {
        mutableStateOf<List<SalarySmsCandidate>>(emptyList())
    }
    var loadingCandidates by remember { mutableStateOf(false) }
    var manualHistoryEnabled by remember { mutableStateOf(false) }
    var manualSalaryDrafts by remember {
        mutableStateOf(mapOf<String, ManualSalaryDraft>())
    }
    var confirmedManualSalaryMonths by remember {
        mutableStateOf(setOf<String>())
    }
    var activeManualSalaryMonthKey by remember {
        mutableStateOf<String?>(null)
    }
    var payWindowChoice by remember { mutableStateOf(PayWindowChoice.VARIES) }
    var customPayWindowStart by remember { mutableStateOf("") }
    var customPayWindowEnd by remember { mutableStateOf("") }
    var mpesaPhone by remember { mutableStateOf("") }
    var mpesaOpeningBalance by remember { mutableStateOf("") }
    var salaryAccountOpeningBalance by remember { mutableStateOf("") }

    var finishing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val orderedSources = IncomeSourceChoice.entries.filter {
        it in selectedSources
    }
    val currentRouteSource = orderedSources.getOrNull(routeIndex)
    val selectedHistoryMonths = HistoryImportWindow.months(historyMonthsCount)
    val eligibleManualSalaryMonths = selectedHistoryMonths
        .filter { it.startEpochMs <= System.currentTimeMillis() }
        .sortedBy(HistoryMonth::startEpochMs)
    val salaryRoute = routes[IncomeSourceChoice.SALARY]
    val salaryBank = salaryRoute?.bankForSearch()
    val salaryBankKey = salaryBank?.name.orEmpty()

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsGranted = granted
        smsDenied = !granted
        error = if (granted) {
            null
        } else {
            "SMS access was not granted. You can continue without importing messages and add salary history manually."
        }
    }

    LaunchedEffect(
        stage,
        historyMonthsCount,
        smsGranted,
        salaryBankKey
    ) {
        candidates = emptyList()
        selectedCandidate = null

        if (
            stage == SetupStage.IDENTIFY &&
            IncomeSourceChoice.SALARY in selectedSources &&
            salaryRoute?.arrival == ArrivalChoice.BANK &&
            historyMonthsCount > 0 &&
            smsGranted &&
            salaryBank != null
        ) {
            val since = HistoryImportWindow.importStartEpochMs(
                historyMonthsCount
            )
            if (since != null) {
                loadingCandidates = true
                candidates = runCatching {
                    withContext(Dispatchers.IO) {
                        SalarySmsCandidateFinder(
                            context.applicationContext
                        ).find(
                            bank = salaryBank,
                            sinceEpochMs = since
                        )
                    }
                }.getOrDefault(emptyList())
                loadingCandidates = false

                if (candidates.isEmpty()) {
                    manualHistoryEnabled = true
                }
            }
        } else {
            loadingCandidates = false
        }
    }

    fun updateRoute(
        source: IncomeSourceChoice,
        transform: (IncomeRouteDraft) -> IncomeRouteDraft
    ) {
        val current = routes[source] ?: IncomeRouteDraft()
        routes = routes + (source to transform(current))
    }

    fun validateCurrentRoute(): Boolean {
        val source = currentRouteSource ?: return false
        val route = routes[source] ?: IncomeRouteDraft()

        if (route.arrival == null) {
            error = "Choose where " + source.label.lowercase() + " normally arrives."
            return false
        }

        if (
            route.arrival == ArrivalChoice.BANK &&
            route.selectedBank == null &&
            route.customBank.isBlank()
        ) {
            error = "Choose the bank or type its name."
            return false
        }

        return true
    }

    fun needsManualSalaryHistory(): Boolean =
        IncomeSourceChoice.SALARY in selectedSources &&
            eligibleManualSalaryMonths.isNotEmpty() &&
            (
                manualHistoryEnabled ||
                    !smsGranted ||
                    salaryRoute?.arrival != ArrivalChoice.BANK ||
                    (
                        !loadingCandidates &&
                            candidates.isEmpty()
                        )
                )

    fun validateSalaryIdentity(): Boolean {
        if (IncomeSourceChoice.SALARY !in selectedSources) return true
        if (salaryRoute?.arrival == ArrivalChoice.CASH) return true

        val identity = employer.trim().takeIf { it.isNotBlank() }
            ?: selectedCandidate?.originator?.takeIf {
                !it.isNullOrBlank()
            }

        if (identity == null) {
            error = "Tell PesaTrack who normally pays your salary."
            return false
        }
        return true
    }

    fun resolvedPayWindow(): Pair<Int?, Int?> =
        if (payWindowChoice == PayWindowChoice.CUSTOM) {
            customPayWindowStart.toIntOrNull() to
                customPayWindowEnd.toIntOrNull()
        } else {
            payWindowChoice.startDay to payWindowChoice.endDay
        }

    fun validatePayWindow(): Boolean {
        if (payWindowChoice != PayWindowChoice.CUSTOM) return true
        val (startDay, endDay) = resolvedPayWindow()
        if (
            startDay == null || endDay == null ||
            startDay !in 1..31 || endDay !in 1..31
        ) {
            error = "Enter a valid payday range using days 1 to 31."
            return false
        }
        return true
    }

    fun goForward() {
        error = null

        when (stage) {
            SetupStage.SOURCES -> {
                if (selectedSources.isEmpty()) {
                    error = "Choose at least one way money normally comes in."
                    return
                }
                routeIndex = 0
                stage = SetupStage.ROUTES
            }

            SetupStage.ROUTES -> {
                if (!validateCurrentRoute()) return

                if (routeIndex < orderedSources.lastIndex) {
                    routeIndex += 1
                } else {
                    stage = SetupStage.HISTORY
                }
            }

            SetupStage.HISTORY -> {
                stage = if (IncomeSourceChoice.SALARY in selectedSources) {
                    SetupStage.IDENTIFY
                } else {
                    SetupStage.ACCOUNTS
                }
            }

            SetupStage.IDENTIFY -> {
                if (!validateSalaryIdentity()) return
                stage = SetupStage.PAY_WINDOW
            }

            SetupStage.PAY_WINDOW -> {
                if (!validatePayWindow()) return
                if (needsManualSalaryHistory()) {
                    activeManualSalaryMonthKey =
                        eligibleManualSalaryMonths
                            .firstOrNull {
                                it.key() !in confirmedManualSalaryMonths
                            }
                            ?.key()
                            ?: eligibleManualSalaryMonths
                                .firstOrNull()
                                ?.key()
                    stage = SetupStage.SALARY_HISTORY
                } else {
                    stage = SetupStage.ACCOUNTS
                }
            }

            SetupStage.SALARY_HISTORY -> {
                stage = SetupStage.ACCOUNTS
            }

            SetupStage.ACCOUNTS -> Unit
        }
    }

    fun goBack() {
        error = null

        when (stage) {
            SetupStage.SOURCES -> Unit
            SetupStage.ROUTES -> {
                if (routeIndex > 0) {
                    routeIndex -= 1
                } else {
                    stage = SetupStage.SOURCES
                }
            }
            SetupStage.HISTORY -> {
                routeIndex = orderedSources.lastIndex.coerceAtLeast(0)
                stage = SetupStage.ROUTES
            }
            SetupStage.IDENTIFY -> {
                stage = SetupStage.HISTORY
            }
            SetupStage.PAY_WINDOW -> {
                stage = SetupStage.IDENTIFY
            }
            SetupStage.SALARY_HISTORY -> {
                stage = SetupStage.PAY_WINDOW
            }
            SetupStage.ACCOUNTS -> {
                stage = if (
                    IncomeSourceChoice.SALARY in selectedSources
                ) {
                    if (needsManualSalaryHistory()) {
                        SetupStage.SALARY_HISTORY
                    } else {
                        SetupStage.PAY_WINDOW
                    }
                } else {
                    SetupStage.HISTORY
                }
            }
        }
    }

    fun validateManualSalaryEntries():
        Pair<List<ManualSalaryEntry>, String?> {
        if (confirmedManualSalaryMonths.isEmpty()) {
            return emptyList<ManualSalaryEntry>() to null
        }

        val entries = mutableListOf<ManualSalaryEntry>()
        val now = System.currentTimeMillis()

        eligibleManualSalaryMonths
            .filter { it.key() in confirmedManualSalaryMonths }
            .forEach { month ->
                val draft = manualSalaryDrafts[month.key()]
                    ?: ManualSalaryDraft()

                val amountMinor = parseMoneyMinor(draft.amount)
                    ?: return emptyList<ManualSalaryEntry>() to
                        ("Enter a valid salary amount for " + month.label + ".")

                if (amountMinor <= 0L) {
                    return emptyList<ManualSalaryEntry>() to
                        ("Salary for " + month.label + " must be above zero.")
                }

                val day = draft.day.toIntOrNull()
                    ?: return emptyList<ManualSalaryEntry>() to
                        ("Enter the day salary arrived in " + month.label + ".")

                val occurred = HistoryImportWindow.epochForDay(month, day)
                    ?: return emptyList<ManualSalaryEntry>() to
                        ("That day does not exist in " + month.label + ".")

                if (occurred > now) {
                    return emptyList<ManualSalaryEntry>() to
                        ("Salary date for " + month.label + " cannot be in the future.")
                }

                entries += ManualSalaryEntry(
                    amountMinor = amountMinor,
                    occurredAtEpochMs = occurred
                )
            }

        return entries to null
    }

    fun confirmManualSalaryMonth(month: HistoryMonth) {
        val draft = manualSalaryDrafts[month.key()] ?: ManualSalaryDraft()
        val amountMinor = parseMoneyMinor(draft.amount)
        if (amountMinor == null || amountMinor <= 0L) {
            error = "Enter a valid salary amount for " + month.label + "."
            return
        }

        val day = draft.day.toIntOrNull()
        if (day == null) {
            error = "Enter the day salary arrived in " + month.label + "."
            return
        }

        val occurred = HistoryImportWindow.epochForDay(month, day)
        if (occurred == null) {
            error = "That day does not exist in " + month.label + "."
            return
        }

        if (occurred > System.currentTimeMillis()) {
            error = "Salary date for " + month.label + " cannot be in the future."
            return
        }

        confirmedManualSalaryMonths =
            confirmedManualSalaryMonths + month.key()
        activeManualSalaryMonthKey =
            eligibleManualSalaryMonths
                .firstOrNull {
                    it.key() !in confirmedManualSalaryMonths
                }
                ?.key()
                ?: month.key()
        error = null
    }

    suspend fun finishSetup() {
        error = null

        val manualResult = validateManualSalaryEntries()
        val manualEntries = manualResult.first
        manualResult.second?.let {
            error = it
            return
        }

        val salarySelected =
            IncomeSourceChoice.SALARY in selectedSources
        val salaryRouteValue = routes[IncomeSourceChoice.SALARY]

        val normalizedMpesaPhone = mpesaPhone
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let(OwnAccountTransferDetector::normalizeKenyanPhone)

        if (mpesaPhone.isNotBlank() && normalizedMpesaPhone == null) {
            error = "Enter a valid Kenyan M-Pesa number, for example 0712345678."
            return
        }

        val mpesaOpeningMinor = mpesaOpeningBalance
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let(::parseMoneyMinor)
        if (mpesaOpeningBalance.isNotBlank() && mpesaOpeningMinor == null) {
            error = "Enter the current M-Pesa balance as a number."
            return
        }

        val salaryAccountOpeningMinor = salaryAccountOpeningBalance
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let(::parseMoneyMinor)
        if (
            salaryAccountOpeningBalance.isNotBlank() &&
            salaryAccountOpeningMinor == null
        ) {
            error = "Enter the current bank balance as a number."
            return
        }

        val salaryIdentifier = employer.trim().takeIf {
            it.isNotBlank()
        } ?: selectedCandidate?.originator?.takeIf {
            !it.isNullOrBlank()
        }

        if (
            salarySelected &&
            salaryRouteValue?.arrival != ArrivalChoice.CASH &&
            salaryIdentifier == null
        ) {
            error = "Tell PesaTrack who normally pays your salary."
            return
        }

        val accounts = mutableMapOf<IncomeSourceChoice, MoneyAccount>()

        orderedSources.forEach { source ->
            val route = routes[source] ?: return@forEach
            val account = when (route.arrival) {
                ArrivalChoice.BANK -> {
                    accountStore.findOrCreate(
                        route.bankName(),
                        AccountType.BANK
                    )
                }
                ArrivalChoice.MPESA ->
                    accountStore.listAccounts().first {
                        it.id == AccountStore.DEFAULT_MPESA_ID
                    }
                ArrivalChoice.CASH ->
                    accountStore.listAccounts().first {
                        it.id == AccountStore.DEFAULT_CASH_ID
                    }
                null -> return@forEach
            }
            accounts[source] = account
        }

        val setupObservedAt = System.currentTimeMillis()
        accountStore.updateDetails(
            id = AccountStore.DEFAULT_MPESA_ID,
            identifier = normalizedMpesaPhone,
            openingBalanceMinor = mpesaOpeningMinor,
            openingBalanceAsOfEpochMs = setupObservedAt
        )

        if (salaryRouteValue?.arrival == ArrivalChoice.BANK) {
            accounts[IncomeSourceChoice.SALARY]?.let { salaryAccount ->
                accountStore.updateDetails(
                    id = salaryAccount.id,
                    identifier = salaryAccount.identifier,
                    openingBalanceMinor = salaryAccountOpeningMinor,
                    openingBalanceAsOfEpochMs = setupObservedAt
                )
            }
        }

        transactionStore.reconcileOwnAccountTransfers()

        saveSetupPreferences(
            context = context,
            sources = selectedSources,
            routes = routes,
            historyMonthsCount = historyMonthsCount
        )

        if (salarySelected && salaryRouteValue != null) {
            val salaryAccount = accounts[IncomeSourceChoice.SALARY]
                ?: return

            val (payWindowStartDay, payWindowEndDay) =
                resolvedPayWindow()

            val salaryRule = when (salaryRouteValue.arrival) {
                ArrivalChoice.BANK -> {
                    val title = selectedCandidate?.address
                        ?.takeIf { it.isNotBlank() }
                        ?: salaryRouteValue.selectedBank
                            ?.senderHints
                            ?.firstOrNull()
                            .orEmpty()
                            .ifBlank {
                                salaryRouteValue.bankName()
                            }

                    ruleStore.save(
                        name = "Salary",
                        counterpartyContains = "",
                        category = TransactionCategory.INCOME,
                        direction = MoneyDirection.INCOME,
                        notificationTitleContains = title,
                        messageContains = salaryIdentifier.orEmpty(),
                        displayLabel = "Salary",
                        preset = RulePreset.SALARY,
                        startsPayCycle = true,
                        accountId = salaryAccount.id,
                        payWindowStartDay = payWindowStartDay,
                        payWindowEndDay = payWindowEndDay
                    )
                }

                ArrivalChoice.MPESA ->
                    ruleStore.save(
                        name = "Salary",
                        counterpartyContains = "",
                        counterpartyExact = salaryIdentifier.orEmpty(),
                        category = TransactionCategory.INCOME,
                        direction = MoneyDirection.INCOME,
                        displayLabel = "Salary",
                        preset = RulePreset.SALARY,
                        startsPayCycle = true,
                        accountId = salaryAccount.id,
                        payWindowStartDay = payWindowStartDay,
                        payWindowEndDay = payWindowEndDay
                    )

                ArrivalChoice.CASH ->
                    ruleStore.save(
                        name = "Salary",
                        counterpartyContains = "",
                        counterpartyExact = "Manual salary",
                        category = TransactionCategory.INCOME,
                        direction = MoneyDirection.INCOME,
                        displayLabel = "Salary",
                        preset = RulePreset.SALARY,
                        startsPayCycle = true,
                        accountId = salaryAccount.id,
                        payWindowStartDay = payWindowStartDay,
                        payWindowEndDay = payWindowEndDay
                    )

                null -> return
            }

            transactionStore.reapplyRule(salaryRule)

            val importStart = HistoryImportWindow.importStartEpochMs(
                historyMonthsCount
            )
            if (
                historyMonthsCount > 0 &&
                smsGranted &&
                importStart != null
            ) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        HistoricalSmsImporter(
                            context.applicationContext
                        ).importSince(importStart)
                    }
                }
            }

            manualEntries.forEach { entry ->
                transactionStore.insertManualIncome(
                    amountMinor = entry.amountMinor,
                    occurredAtEpochMs = entry.occurredAtEpochMs,
                    label = if (
                        salaryRouteValue.arrival == ArrivalChoice.CASH
                    ) {
                        "Manual salary"
                    } else {
                        salaryIdentifier ?: "Salary"
                    },
                    matchedRule = salaryRule
                )
            }

            payCycleStore.rebuild()
        } else if (
            historyMonthsCount > 0 &&
            smsGranted
        ) {
            HistoryImportWindow.importStartEpochMs(
                historyMonthsCount
            )?.let { importStart ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        HistoricalSmsImporter(
                            context.applicationContext
                        ).importSince(importStart)
                    }
                }
            }
        }

        onDone()
    }

    val salarySelected =
        IncomeSourceChoice.SALARY in selectedSources
    val totalStages = if (salarySelected) 7 else 4
    val stageNumber = when (stage) {
        SetupStage.SOURCES -> 1
        SetupStage.ROUTES -> 2
        SetupStage.HISTORY -> 3
        SetupStage.IDENTIFY -> 4
        SetupStage.PAY_WINDOW -> 5
        SetupStage.SALARY_HISTORY -> 6
        SetupStage.ACCOUNTS -> if (salarySelected) 7 else 4
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = 18.dp,
            vertical = 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stage != SetupStage.SOURCES) {
                    IconButton(onClick = ::goBack) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Set up PesaTrack",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        if (stage == SetupStage.ROUTES) {
                            "Step 2 of " + totalStages + " • " +
                                (routeIndex + 1) +
                                " of " +
                                orderedSources.size +
                                " income sources"
                        } else {
                            "Step " + stageNumber + " of " + totalStages
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { stageNumber / totalStages.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(6.dp)
            )
        }

        when (stage) {
            SetupStage.SOURCES -> item {
                IncomeSourcesStep(
                    selectedSources = selectedSources,
                    onToggle = { source ->
                        selectedSources =
                            if (source in selectedSources) {
                                selectedSources - source
                            } else {
                                selectedSources + source
                            }
                    }
                )
            }

            SetupStage.ROUTES -> item {
                val source = currentRouteSource
                if (source != null) {
                    IncomeRouteStep(
                        source = source,
                        draft = routes[source] ?: IncomeRouteDraft(),
                        onArrival = { arrival ->
                            updateRoute(source) {
                                it.copy(arrival = arrival)
                            }
                        },
                        onBankQuery = { query ->
                            updateRoute(source) {
                                it.copy(
                                    bankQuery = query,
                                    selectedBank = if (
                                        query == it.selectedBank?.name
                                    ) {
                                        it.selectedBank
                                    } else {
                                        null
                                    }
                                )
                            }
                        },
                        onBankSelected = { bank ->
                            updateRoute(source) {
                                it.copy(
                                    selectedBank = bank,
                                    bankQuery = bank.name,
                                    customBank = ""
                                )
                            }
                        },
                        onCustomBank = { name ->
                            updateRoute(source) {
                                it.copy(
                                    customBank = name,
                                    selectedBank = if (name.isBlank()) {
                                        it.selectedBank
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    )
                }
            }

            SetupStage.HISTORY -> item {
                HistoryChoiceStep(
                    selectedCount = historyMonthsCount,
                    smsGranted = smsGranted,
                    smsDenied = smsDenied,
                    onCountSelected = { count ->
                        historyMonthsCount = count
                        manualHistoryEnabled = false
                        manualSalaryDrafts = emptyMap()
                        confirmedManualSalaryMonths = emptySet()
                        activeManualSalaryMonthKey = null
                        error = null
                    },
                    onRequestSms = {
                        smsDenied = false
                        error = null
                        smsPermissionLauncher.launch(
                            Manifest.permission.READ_SMS
                        )
                    },
                    onOpenAppSettings = {
                        openAppSettings(context)
                    }
                )
            }

            SetupStage.IDENTIFY -> item {
                IdentifyIncomeStep(
                    salaryRoute = salaryRoute,
                    historyMonthsCount = historyMonthsCount,
                    smsGranted = smsGranted,
                    loadingCandidates = loadingCandidates,
                    candidates = candidates,
                    selectedCandidate = selectedCandidate,
                    employer = employer,
                    onEmployerChanged = {
                        employer = it
                    },
                    onCandidateSelected = { candidate ->
                        selectedCandidate = candidate
                        manualHistoryEnabled = false
                        manualSalaryDrafts = emptyMap()
                        confirmedManualSalaryMonths = emptySet()
                        activeManualSalaryMonthKey = null
                        candidate.originator
                            ?.takeIf { it.isNotBlank() }
                            ?.let { employer = it }
                    },
                    onManualHistory = {
                        selectedCandidate = null
                        manualHistoryEnabled = true
                    }
                )
            }

            SetupStage.PAY_WINDOW -> item {
                PaydayWindowStep(
                    selected = payWindowChoice,
                    customStart = customPayWindowStart,
                    customEnd = customPayWindowEnd,
                    onSelected = { choice ->
                        payWindowChoice = choice
                        error = null
                    },
                    onCustomStartChanged = {
                        customPayWindowStart = it
                        error = null
                    },
                    onCustomEndChanged = {
                        customPayWindowEnd = it
                        error = null
                    }
                )
            }

            SetupStage.SALARY_HISTORY -> item {
                ManualSalaryHistoryStep(
                    months = eligibleManualSalaryMonths,
                    drafts = manualSalaryDrafts,
                    confirmedMonthKeys = confirmedManualSalaryMonths,
                    activeMonthKey = activeManualSalaryMonthKey,
                    onMonthSelected = { month ->
                        activeManualSalaryMonthKey = month.key()
                        error = null
                    },
                    onDraftChanged = { month, draft ->
                        manualSalaryDrafts =
                            manualSalaryDrafts +
                                (month.key() to draft)
                        error = null
                    },
                    onConfirmMonth = ::confirmManualSalaryMonth
                )
            }

            SetupStage.ACCOUNTS -> item {
                OwnedAccountsSetupFields(
                    salaryRoute = salaryRoute,
                    mpesaPhone = mpesaPhone,
                    mpesaOpeningBalance = mpesaOpeningBalance,
                    salaryAccountOpeningBalance =
                        salaryAccountOpeningBalance,
                    onMpesaPhoneChanged = { mpesaPhone = it },
                    onMpesaOpeningBalanceChanged = {
                        mpesaOpeningBalance = it
                    },
                    onSalaryAccountOpeningBalanceChanged = {
                        salaryAccountOpeningBalance = it
                    }
                )
            }
        }

        error?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item {
            if (stage != SetupStage.ACCOUNTS) {
                Button(
                    onClick = ::goForward,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        if (
                            stage == SetupStage.ROUTES &&
                            routeIndex < orderedSources.lastIndex
                        ) {
                            "Next income source"
                        } else {
                            "Next"
                        }
                    )
                    Icon(
                        Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                Button(
                    onClick = {
                        if (!finishing) {
                            setupScope.launch {
                                finishing = true
                                try {
                                    finishSetup()
                                } finally {
                                    finishing = false
                                }
                            }
                        }
                    },
                    enabled = !finishing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (finishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text("Finish setup")
                }
            }
        }
    }
}

@Composable
private fun IncomeSourcesStep(
    selectedSources: Set<IncomeSourceChoice>,
    onToggle: (IncomeSourceChoice) -> Unit
) {
    SetupQuestion(
        title = "What usually brings money in?",
        body = "Choose one or more. PesaTrack will only ask questions that apply to what you select."
    ) {
        IncomeSourceChoice.entries.forEach { choice ->
            SetupChoice(
                icon = when (choice) {
                    IncomeSourceChoice.SALARY ->
                        Icons.Rounded.Payments
                    IncomeSourceChoice.BUSINESS ->
                        Icons.Rounded.Storefront
                    IncomeSourceChoice.FREELANCE ->
                        Icons.Rounded.Person
                    IncomeSourceChoice.OTHER ->
                        Icons.Rounded.Business
                },
                title = choice.label,
                helper = choice.helper,
                selected = choice in selectedSources,
                onClick = { onToggle(choice) }
            )
        }

        if (selectedSources.size > 1) {
            Text(
                selectedSources.size.toString() +
                    " income sources selected. You'll set up each one next.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun IncomeRouteStep(
    source: IncomeSourceChoice,
    draft: IncomeRouteDraft,
    onArrival: (ArrivalChoice) -> Unit,
    onBankQuery: (String) -> Unit,
    onBankSelected: (KenyanBank) -> Unit,
    onCustomBank: (String) -> Unit
) {
    SetupQuestion(
        title = "Where does " +
            source.label.lowercase() +
            " arrive?",
        body = "Only the choices relevant to this income source are shown."
    ) {
        ArrivalChoice.entries.forEach { choice ->
            SetupChoice(
                icon = when (choice) {
                    ArrivalChoice.BANK ->
                        Icons.Rounded.AccountBalance
                    ArrivalChoice.MPESA ->
                        Icons.Rounded.Payments
                    ArrivalChoice.CASH ->
                        Icons.Rounded.Wallet
                },
                title = choice.label,
                helper = choice.helper,
                selected = draft.arrival == choice,
                onClick = { onArrival(choice) }
            )
        }

        if (draft.arrival == ArrivalChoice.BANK) {
            Spacer(Modifier.height(2.dp))
            Text(
                "Which bank?",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = draft.bankQuery,
                onValueChange = onBankQuery,
                label = { Text("Search Kenyan banks") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            bankChoices(draft.bankQuery)
                .take(6)
                .forEach { bank ->
                    BankChoice(
                        bank = bank,
                        selected = draft.selectedBank?.id == bank.id,
                        onClick = { onBankSelected(bank) }
                    )
                }

            OutlinedTextField(
                value = draft.customBank,
                onValueChange = onCustomBank,
                label = { Text("Can't find it? Type bank name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun HistoryChoiceStep(
    selectedCount: Int,
    smsGranted: Boolean,
    smsDenied: Boolean,
    onCountSelected: (Int) -> Unit,
    onRequestSms: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    SetupQuestion(
        title = "Use recent SMS history?",
        body = "This is optional. Choose how far back PesaTrack may look. The maximum is three months."
    ) {
        (0..3).forEach { count ->
            val months = HistoryImportWindow.months(count)
            SetupChoice(
                icon = if (count == 0) {
                    Icons.Rounded.Lock
                } else {
                    Icons.Rounded.History
                },
                title = when (count) {
                    0 -> "Don't import messages"
                    1 -> "Import 1 month"
                    else -> "Import " + count + " months"
                },
                helper = if (count == 0) {
                    "Continue without reading old SMS"
                } else {
                    months.joinToString(" • ") {
                        it.shortLabel()
                    }
                },
                selected = selectedCount == count,
                onClick = { onCountSelected(count) }
            )
        }

        if (selectedCount > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Why PesaTrack asks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "It reads supported money SMS on this phone to find transactions and income. Raw SMS text is not stored in the PesaTrack ledger or sent away.",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (smsGranted) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "SMS access is ready.",
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = onRequestSms,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Allow SMS history")
                }

                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Open app settings")
                }

                if (smsDenied) {
                    Text(
                        "If Android says this permission is restricted, open App info and use the system's “Allow restricted settings” option if your phone offers it. PesaTrack cannot enable that setting for you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "You can still press Next without granting access. If salary history is needed, PesaTrack will give it its own simple step.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IdentifyIncomeStep(
    salaryRoute: IncomeRouteDraft?,
    historyMonthsCount: Int,
    smsGranted: Boolean,
    loadingCandidates: Boolean,
    candidates: List<SalarySmsCandidate>,
    selectedCandidate: SalarySmsCandidate?,
    employer: String,
    onEmployerChanged: (String) -> Unit,
    onCandidateSelected: (SalarySmsCandidate) -> Unit,
    onManualHistory: () -> Unit
) {
    SetupQuestion(
        title = "Who pays your salary?",
        body = "Keep this step focused on the payer. Salary history and account details come next."
    ) {
        if (
            salaryRoute?.arrival == ArrivalChoice.BANK &&
            historyMonthsCount > 0 &&
            smsGranted
        ) {
            when {
                loadingCandidates -> {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Looking for incoming payments from " +
                                salaryRoute.bankName() +
                                "…",
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                candidates.isNotEmpty() -> {
                    Text(
                        "Did PesaTrack find it?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    candidates.take(5).forEach { candidate ->
                        SalaryCandidateCard(
                            candidate = candidate,
                            selected =
                                selectedCandidate?.smsId ==
                                    candidate.smsId,
                            onClick = {
                                onCandidateSelected(candidate)
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onManualHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("None of these — I'll add salary history")
                    }
                }

                else -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = pesaToneContainer("amber")
                    ) {
                        Text(
                            "No clear salary payment was found. Type the payer below, then add only the salary months that actually arrived.",
                            modifier = Modifier.padding(14.dp),
                            color = pesaToneContent("amber")
                        )
                    }
                }
            }
        }

        if (salaryRoute?.arrival != ArrivalChoice.CASH) {
            OutlinedTextField(
                value = employer,
                onValueChange = onEmployerChanged,
                label = { Text("Employer or regular payer") },
                placeholder = {
                    Text("Example: Braeburn")
                },
                supportingText = {
                    Text(
                        "Used to recognize future salary payments. The amount and payday can change."
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = pesaToneContainer("amber")
            ) {
                Text(
                    "Cash salary cannot be detected from phone messages. On the next step, add the months that were actually paid.",
                    modifier = Modifier.padding(14.dp),
                    color = pesaToneContent("amber")
                )
            }
        }

        if (selectedCandidate != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Salary example selected. PesaTrack will use it to learn the payer.",
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ManualSalaryHistoryStep(
    months: List<HistoryMonth>,
    drafts: Map<String, ManualSalaryDraft>,
    confirmedMonthKeys: Set<String>,
    activeMonthKey: String?,
    onMonthSelected: (HistoryMonth) -> Unit,
    onDraftChanged: (HistoryMonth, ManualSalaryDraft) -> Unit,
    onConfirmMonth: (HistoryMonth) -> Unit
) {
    SetupQuestion(
        title = "Add salary history",
        body = "Add one paid month at a time. A month that has not been paid yet should stay unadded."
    ) {
        if (months.isEmpty()) {
            Text(
                "You did not choose any history months.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@SetupQuestion
        }

        Text(
            "Choose month",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEach { month ->
                val key = month.key()
                val selected = key == activeMonthKey
                val confirmed = key in confirmedMonthKeys
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 58.dp)
                        .clickable { onMonthSelected(month) },
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        selected -> MaterialTheme.colorScheme.primaryContainer
                        confirmed -> pesaToneContainer("green")
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 10.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            month.shortLabel(),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            month.year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (confirmed) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = "Added",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        val activeMonth = months.firstOrNull {
            it.key() == activeMonthKey
        } ?: months.first()

        ManualSalaryMonthEditor(
            month = activeMonth,
            draft = drafts[activeMonth.key()]
                ?: ManualSalaryDraft(),
            confirmed = activeMonth.key() in confirmedMonthKeys,
            onChanged = {
                onDraftChanged(activeMonth, it)
            },
            onConfirm = {
                onConfirmMonth(activeMonth)
            }
        )

        if (confirmedMonthKeys.isNotEmpty()) {
            Text(
                "Added",
                style = MaterialTheme.typography.titleMedium
            )
            months
                .filter { it.key() in confirmedMonthKeys }
                .forEach { month ->
                    val draft = drafts[month.key()] ?: return@forEach
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = pesaToneContainer("green")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = pesaToneContent("green")
                            )
                            Column(
                                modifier = Modifier.padding(start = 10.dp)
                            ) {
                                Text(
                                    month.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                val amount = parseMoneyMinor(draft.amount)
                                Text(
                                    (amount?.let(::formatWizardMoney)
                                        ?: "Salary") +
                                        " • paid day " +
                                        draft.day,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
        }

        Text(
            "A salary starts a pay cycle on the day it was actually paid. Spending after that date stays in that cycle until the next salary arrives.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ManualSalaryMonthEditor(
    month: HistoryMonth,
    draft: ManualSalaryDraft,
    confirmed: Boolean,
    onChanged: (ManualSalaryDraft) -> Unit,
    onConfirm: () -> Unit
) {
    PesaCard {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                month.label,
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.amount,
                    onValueChange = {
                        onChanged(draft.copy(amount = it))
                    },
                    label = { Text("Salary amount") },
                    prefix = { Text("KES ") },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.day,
                    onValueChange = {
                        onChanged(draft.copy(day = it))
                    },
                    label = { Text("Paid day") },
                    placeholder = { Text("25") },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    if (confirmed) {
                        "Update " + month.shortLabel() + " salary"
                    } else {
                        "Add " + month.shortLabel() + " salary"
                    }
                )
            }
            Text(
                "Do not add this month if salary has not arrived yet. Future dates are blocked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OwnedAccountsSetupFields(
    salaryRoute: IncomeRouteDraft?,
    mpesaPhone: String,
    mpesaOpeningBalance: String,
    salaryAccountOpeningBalance: String,
    onMpesaPhoneChanged: (String) -> Unit,
    onMpesaOpeningBalanceChanged: (String) -> Unit,
    onSalaryAccountOpeningBalanceChanged: (String) -> Unit
) {
    SetupQuestion(
        title = "Which money is already yours?",
        body = "This helps PesaTrack separate real income from money moving between your own accounts."
    ) {
        OutlinedTextField(
            value = mpesaPhone,
            onValueChange = onMpesaPhoneChanged,
            label = { Text("Your M-Pesa number (recommended)") },
            placeholder = { Text("0712345678") },
            supportingText = {
                Text(
                    "If a bank sends money to this number, PesaTrack can treat it as an internal transfer instead of new income."
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = mpesaOpeningBalance,
            onValueChange = onMpesaOpeningBalanceChanged,
            label = { Text("Current M-Pesa balance (optional)") },
            placeholder = { Text("Example: 2,450") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (salaryRoute?.arrival == ArrivalChoice.BANK) {
            OutlinedTextField(
                value = salaryAccountOpeningBalance,
                onValueChange = onSalaryAccountOpeningBalanceChanged,
                label = {
                    Text(
                        salaryRoute.bankName() +
                            " current balance (optional)"
                    )
                },
                placeholder = { Text("Example: 8,000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Text(
            "Current balances are recorded as money you already had. They are never counted as salary or new income.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SetupQuestion(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit
) {
    PesaCard {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun SetupChoice(
    icon: ImageVector,
    title: String,
    helper: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(icon, size = 42.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BankChoice(
    bank: KenyanBank,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                bank.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SalaryCandidateCard(
    candidate: SalarySmsCandidate,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatWizardMoney(candidate.amountMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    candidate.originator
                        ?: "Incoming bank credit",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    SimpleDateFormat(
                        "d MMM yyyy",
                        Locale.getDefault()
                    ).format(
                        Date(candidate.receivedAtEpochMs)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun IncomeRouteDraft.bankName(): String =
    selectedBank?.name
        ?: customBank.trim()

private fun IncomeRouteDraft.bankForSearch(): KenyanBank? {
    selectedBank?.let { return it }

    val manual = customBank.trim()
    if (manual.isBlank()) return null

    return KenyanBank(
        id = "custom",
        name = manual,
        senderHints = listOf(manual)
    )
}

private fun bankChoices(query: String): List<KenyanBank> {
    if (query.isNotBlank()) {
        return KenyanBankDirectory.search(query)
    }

    val preferredIds = listOf(
        "equity",
        "cooperative",
        "kcb",
        "ncba",
        "absa",
        "stanbic"
    )
    val preferred = preferredIds.mapNotNull { id ->
        KenyanBankDirectory.banks.firstOrNull {
            it.id == id
        }
    }
    val rest = KenyanBankDirectory.banks.filterNot {
        it.id in preferredIds
    }
    return preferred + rest
}

private fun saveSetupPreferences(
    context: Context,
    sources: Set<IncomeSourceChoice>,
    routes: Map<IncomeSourceChoice, IncomeRouteDraft>,
    historyMonthsCount: Int
) {
    val editor = context.getSharedPreferences(
        "pesatrack_preferences",
        Context.MODE_PRIVATE
    ).edit()

    editor.putStringSet(
        "income_sources",
        sources.map { it.name }.toSet()
    )
    editor.putInt(
        "history_import_months",
        historyMonthsCount
    )

    sources.forEach { source ->
        val route = routes[source] ?: return@forEach
        editor.putString(
            "income_arrival_" + source.name,
            route.arrival?.name
        )
        if (route.arrival == ArrivalChoice.BANK) {
            editor.putString(
                "income_bank_" + source.name,
                route.bankName()
            )
        }
    }

    editor.apply()
}

private fun HistoryMonth.key(): String =
    year.toString() + "-" + month.toString()

private fun HistoryMonth.shortLabel(): String =
    SimpleDateFormat(
        "MMM",
        Locale.getDefault()
    ).format(Date(startEpochMs))

private fun parseMoneyMinor(raw: String): Long? =
    runCatching {
        raw.replace(",", "")
            .trim()
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()

private fun hasSmsPermission(context: Context): Boolean =
    context.checkSelfPermission(
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:" + context.packageName)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun formatWizardMoney(minor: Long): String =
    "KES " +
        NumberFormat.getNumberInstance(Locale.US)
            .format(minor / 100.0)
