package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ingestion.HistoricalSmsImporter
import com.kedevjimmy.pesatrack.ingestion.SalarySmsCandidate
import com.kedevjimmy.pesatrack.ingestion.SalarySmsCandidateFinder
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class IncomeSourceChoice(val label: String, val helper: String) {
    SALARY("Salary", "Regular pay from an employer"),
    BUSINESS("Business income", "Profit or drawings from a business"),
    FREELANCE("Freelance / side income", "Client or project payments"),
    MULTIPLE("Multiple income sources", "More than one regular source"),
    OTHER("Other", "Something different")
}

private enum class ArrivalChoice(val label: String, val helper: String) {
    BANK("Bank account", "Salary or income lands in a bank"),
    MPESA("M-Pesa", "Money arrives directly in M-Pesa"),
    CASH("Cash", "Income is received physically")
}

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

    var step by remember { mutableStateOf(0) }
    var incomeSource by remember { mutableStateOf<IncomeSourceChoice?>(null) }
    var arrival by remember { mutableStateOf<ArrivalChoice?>(null) }
    var selectedBank by remember { mutableStateOf<KenyanBank?>(null) }
    var bankQuery by remember { mutableStateOf("") }
    var customBank by remember { mutableStateOf("") }
    var employer by remember { mutableStateOf("") }
    var selectedCandidate by remember { mutableStateOf<SalarySmsCandidate?>(null) }
    var candidates by remember { mutableStateOf<List<SalarySmsCandidate>>(emptyList()) }
    var loadingCandidates by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var smsGranted by remember {
        mutableStateOf(hasSmsPermission(context))
    }
    val setupScope = rememberCoroutineScope()

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsGranted = granted
        if (!granted) {
            error = "SMS access was not granted. You can still finish setup manually."
        }
    }

    LaunchedEffect(step, selectedBank, smsGranted) {
        if (
            step == 2 &&
            arrival == ArrivalChoice.BANK &&
            selectedBank != null &&
            smsGranted
        ) {
            loadingCandidates = true
            candidates = runCatching {
                withContext(Dispatchers.IO) {
                    SalarySmsCandidateFinder(context.applicationContext).find(
                        bank = selectedBank!!,
                        sinceEpochMs = daysAgo(100)
                    )
                }
            }.getOrDefault(emptyList())
            loadingCandidates = false
        }
    }

    fun goNext() {
        error = null
        when (step) {
            0 -> {
                if (incomeSource == null) {
                    error = "Choose the option that best describes your main income."
                } else {
                    step = 1
                }
            }
            1 -> {
                if (arrival == null) {
                    error = "Choose where the money normally arrives."
                    return
                }
                if (arrival == ArrivalChoice.BANK &&
                    selectedBank == null &&
                    customBank.isBlank()
                ) {
                    error = "Choose your bank or enter it manually."
                    return
                }
                step = 2
            }
        }
    }

    suspend fun finishSetup() {
        if (finishing) return
        error = null

        val source = incomeSource ?: return
        val arrivalMethod = arrival ?: return

        context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("primary_income_source", source.name)
            .putString("primary_income_arrival", arrivalMethod.name)
            .apply()

        val account = when (arrivalMethod) {
            ArrivalChoice.BANK -> {
                val bankName = selectedBank?.name ?: customBank.trim()
                accountStore.findOrCreate(bankName, AccountType.BANK)
            }
            ArrivalChoice.MPESA ->
                accountStore.listAccounts().first { it.id == AccountStore.DEFAULT_MPESA_ID }
            ArrivalChoice.CASH ->
                accountStore.listAccounts().first { it.id == AccountStore.DEFAULT_CASH_ID }
        }

        if (source == IncomeSourceChoice.SALARY) {
            val salaryIdentifier = employer.trim().takeIf { it.isNotBlank() }
                ?: selectedCandidate?.originator?.takeIf { it.isNotBlank() }

            if (arrivalMethod != ArrivalChoice.CASH && salaryIdentifier == null) {
                error = "Tell PesaTrack who pays you so future salary payments can be recognized safely."
                finishing = false
                return
            }

            if (arrivalMethod != ArrivalChoice.CASH) {
                val rule = when (arrivalMethod) {
                    ArrivalChoice.BANK -> {
                        val bank = selectedBank
                        val title = selectedCandidate?.address
                            ?.takeIf { it.isNotBlank() }
                            ?: bank?.senderHints?.firstOrNull().orEmpty()

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
                            accountId = account.id
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
                            accountId = account.id
                        )
                    ArrivalChoice.CASH -> null
                }

                if (rule != null) {
                    transactionStore.reapplyRule(rule)
                }

                if (smsGranted) {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            HistoricalSmsImporter(context.applicationContext)
                                .importSince(daysAgo(100))
                        }
                    }
                }
                payCycleStore.rebuild()
            }
        }

        onDone()
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) {
                    IconButton(onClick = {
                        error = null
                        step -= 1
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Set up PesaTrack",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Step " + (step + 1) + " of 3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(6.dp)
            )
        }

        when (step) {
            0 -> item {
                SetupQuestion(
                    title = "What usually funds your month?",
                    body = "Start with your main source. You can add more later."
                ) {
                    IncomeSourceChoice.entries.forEach { choice ->
                        SetupChoice(
                            icon = when (choice) {
                                IncomeSourceChoice.SALARY -> Icons.Rounded.Payments
                                IncomeSourceChoice.BUSINESS -> Icons.Rounded.Storefront
                                IncomeSourceChoice.FREELANCE -> Icons.Rounded.Person
                                IncomeSourceChoice.MULTIPLE -> Icons.Rounded.Wallet
                                IncomeSourceChoice.OTHER -> Icons.Rounded.Business
                            },
                            title = choice.label,
                            helper = choice.helper,
                            selected = incomeSource == choice,
                            onClick = { incomeSource = choice }
                        )
                    }
                }
            }

            1 -> item {
                SetupQuestion(
                    title = if (incomeSource == IncomeSourceChoice.SALARY) {
                        "Where does your salary arrive?"
                    } else {
                        "Where does this money usually arrive?"
                    },
                    body = "PesaTrack uses this to understand transfers between your own money."
                ) {
                    ArrivalChoice.entries.forEach { choice ->
                        SetupChoice(
                            icon = when (choice) {
                                ArrivalChoice.BANK -> Icons.Rounded.AccountBalance
                                ArrivalChoice.MPESA -> Icons.Rounded.Payments
                                ArrivalChoice.CASH -> Icons.Rounded.Wallet
                            },
                            title = choice.label,
                            helper = choice.helper,
                            selected = arrival == choice,
                            onClick = {
                                arrival = choice
                                error = null
                            }
                        )
                    }

                    if (arrival == ArrivalChoice.BANK) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Which bank?",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = bankQuery,
                            onValueChange = { bankQuery = it },
                            label = { Text("Search Kenyan banks") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        KenyanBankDirectory.search(bankQuery)
                            .take(6)
                            .forEach { bank ->
                                BankChoice(
                                    bank = bank,
                                    selected = selectedBank?.id == bank.id,
                                    onClick = {
                                        selectedBank = bank
                                        customBank = ""
                                        bankQuery = bank.name
                                    }
                                )
                            }

                        OutlinedTextField(
                            value = customBank,
                            onValueChange = {
                                customBank = it
                                if (it.isNotBlank()) selectedBank = null
                            },
                            label = { Text("Can't find it? Type bank name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            2 -> item {
                SalaryIdentificationStep(
                    incomeSource = incomeSource,
                    arrival = arrival,
                    selectedBank = selectedBank,
                    smsGranted = smsGranted,
                    loadingCandidates = loadingCandidates,
                    candidates = candidates,
                    selectedCandidate = selectedCandidate,
                    employer = employer,
                    onEmployerChanged = { employer = it },
                    onCandidateSelected = {
                        selectedCandidate = it
                        if (!it.originator.isNullOrBlank()) {
                            employer = it.originator
                        }
                    },
                    onRequestSms = {
                        error = null
                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
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
            if (step < 2) {
                Button(
                    onClick = ::goNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Next")
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
private fun SalaryIdentificationStep(
    incomeSource: IncomeSourceChoice?,
    arrival: ArrivalChoice?,
    selectedBank: KenyanBank?,
    smsGranted: Boolean,
    loadingCandidates: Boolean,
    candidates: List<SalarySmsCandidate>,
    selectedCandidate: SalarySmsCandidate?,
    employer: String,
    onEmployerChanged: (String) -> Unit,
    onCandidateSelected: (SalarySmsCandidate) -> Unit,
    onRequestSms: () -> Unit
) {
    SetupQuestion(
        title = if (incomeSource == IncomeSourceChoice.SALARY) {
            "Let's identify your salary"
        } else {
            "One last detail"
        },
        body = if (incomeSource == IncomeSourceChoice.SALARY) {
            "Show PesaTrack one example. It will use that knowledge for older and future matching payments."
        } else {
            "You can teach detailed income rules later from Settings."
        }
    ) {
        if (incomeSource != IncomeSourceChoice.SALARY) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "Your main source is saved conceptually. Detailed automation can be added later without blocking setup.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            return@SetupQuestion
        }

        if (arrival == ArrivalChoice.BANK) {
            Text(
                selectedBank?.name ?: "Your bank",
                style = MaterialTheme.typography.titleMedium
            )

            if (!smsGranted) {
                PesaCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Find the salary automatically",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Allow SMS access and PesaTrack will look for recent incoming payments from this bank on your phone.",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRequestSms,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .heightIn(min = 52.dp)
                        ) {
                            Text("Allow SMS history")
                        }
                    }
                }
            } else if (loadingCandidates) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Looking for recent incoming payments…",
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            } else if (candidates.isNotEmpty()) {
                Text(
                    "Which one is your salary?",
                    style = MaterialTheme.typography.titleMedium
                )
                candidates.take(5).forEach { candidate ->
                    SalaryCandidateCard(
                        candidate = candidate,
                        selected = selectedCandidate?.smsId == candidate.smsId,
                        onClick = { onCandidateSelected(candidate) }
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "No clear incoming bank payment was found. You can identify the employer manually below.",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (arrival != ArrivalChoice.CASH) {
            OutlinedTextField(
                value = employer,
                onValueChange = onEmployerChanged,
                label = { Text("Who pays you?") },
                placeholder = { Text("Employer or regular payer name") },
                supportingText = {
                    Text("Used as the stable identifier, not the salary amount or payday.")
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
                    "Cash cannot be detected from phone messages. PesaTrack will not invent a salary event; cash income can be recorded manually in a later workflow.",
                    modifier = Modifier.padding(16.dp),
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
                    "This example will teach future matching salary payments automatically.",
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
            Text(title, style = MaterialTheme.typography.headlineSmall)
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
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(icon, size = 42.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
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
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                    candidate.originator ?: "Incoming bank credit",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                        .format(Date(candidate.receivedAtEpochMs)),
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

private fun hasSmsPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED

private fun daysAgo(days: Int): Long =
    Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -days)
    }.timeInMillis

private fun formatWizardMoney(minor: Long): String =
    "KES " + NumberFormat.getNumberInstance(Locale.US).format(minor / 100.0)
