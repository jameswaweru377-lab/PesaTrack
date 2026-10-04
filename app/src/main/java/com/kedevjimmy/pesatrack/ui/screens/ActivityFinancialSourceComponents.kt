package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivityMonthBucket
import com.kedevjimmy.pesatrack.data.local.ActivityReviewOverview
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import com.kedevjimmy.pesatrack.data.local.AccountStore
import com.kedevjimmy.pesatrack.data.local.CategoryStore
import com.kedevjimmy.pesatrack.data.local.DiscoveredFinancialEntity
import com.kedevjimmy.pesatrack.data.local.FinancialUnderstandingStore
import com.kedevjimmy.pesatrack.data.local.IdentityGraphStore
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PossibleDuplicateGroup
import com.kedevjimmy.pesatrack.data.local.ReviewGroup
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewCandidate
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewItem
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewKind
import com.kedevjimmy.pesatrack.data.local.RelationshipReviewStore
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.KenyanBankDirectory
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.domain.rules.HistoryImportWindow
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import com.kedevjimmy.pesatrack.ingestion.FinancialMessageIngestionCoordinator
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnostic
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticCode
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticsStore
import com.kedevjimmy.pesatrack.ingestion.HistoricalSmsImporter
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialProviderRegistry
import com.kedevjimmy.pesatrack.ui.components.IconBubble
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.components.PesaTrackTopBar
import com.kedevjimmy.pesatrack.ui.components.SmallStatusPill
import com.kedevjimmy.pesatrack.ui.components.isPesaDarkTheme
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

data class FinancialSourceSummary(
    val key: String,
    val displayName: String,
    val sourceType: String,
    val messageCount: Int
)

@Composable
private fun FinancialSourceNavigator(
    sources: List<FinancialSourceSummary>,
    selectedSourceKey: String?,
    onSourceSelected: (String?) -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(7.dp)
    ) {
        Text(
            "Your account messages",
            style =
                MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Only confirmed accounts are shown. The badge is the number of captured messages for this month.",
            style =
                MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected =
                        selectedSourceKey == null,
                    onClick = {
                        onSourceSelected(null)
                    },
                    label = {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text("All accounts")
                            val total =
                                sources.sumOf {
                                    it.messageCount
                                }
                            if (total > 0) {
                                Badge(
                                    modifier =
                                        Modifier.padding(
                                            start = 6.dp
                                        )
                                ) {
                                    Text(
                                        total.toString()
                                    )
                                }
                            }
                        }
                    }
                )
            }
            sources.forEach { source ->
                item(key = source.key) {
                    FilterChip(
                        selected =
                            selectedSourceKey ==
                                source.key,
                        onClick = {
                            onSourceSelected(
                                source.key
                            )
                        },
                        label = {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    source.displayName
                                )
                                Badge(
                                    modifier =
                                        Modifier.padding(
                                            start = 6.dp
                                        )
                                ) {
                                    Text(
                                        source.messageCount
                                            .toString()
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun FinancialSourceFeedHeader(
    source: FinancialSourceSummary?,
    messageCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color =
            MaterialTheme.colorScheme
                .surfaceVariant
                .copy(alpha = .48f)
    ) {
        Column(
            Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            )
        ) {
            Text(
                source?.displayName
                    ?: "Financial source",
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                (
                    source?.sourceType
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Financial"
                    ) +
                    " • " +
                    messageCount +
                    " captured message" +
                    if (messageCount == 1) {
                        ""
                    } else {
                        "s"
                    },
                modifier =
                    Modifier.padding(top = 2.dp),
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

@Composable
internal fun EmptyFinancialSourceFeed() {
    PesaCard {
        Text(
            "No captured financial messages for this source in the selected month.",
            modifier = Modifier.padding(18.dp),
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
internal fun FinancialMessageDateHeader(
    dayEpochMs: Long,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            dayLabel(dayEpochMs),
            modifier = Modifier.weight(1f),
            style =
                MaterialTheme.typography
                    .titleSmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
        Text(
            count.toString() +
                " message" +
                if (count == 1) "" else "s",
            style =
                MaterialTheme.typography
                    .labelMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
internal fun CapturedFinancialMessageRow(
    diagnostic: FinancialIngestionDiagnostic
) {
    val currentSourceType =
        financialSourceType(
            diagnostic
        )
    val currentEventLabel =
        if (
            currentSourceType != "Bank" &&
            diagnostic.eventLabel
                .startsWith(
                    "Bank ",
                    ignoreCase = true
                )
        ) {
            diagnostic.eventLabel
                .removePrefix("Bank ")
                .replaceFirstChar {
                    it.uppercase()
                }
        } else {
            diagnostic.eventLabel
        }

    val tone =
        when (diagnostic.code) {
            FinancialIngestionDiagnosticCode
                .INGESTED -> "green"
            FinancialIngestionDiagnosticCode
                .PENDING_REVIEW -> "amber"
            FinancialIngestionDiagnosticCode
                .UNDERSTOOD_NO_LEDGER_EFFECT ->
                "blue"
            FinancialIngestionDiagnosticCode
                .RESOLVED_BY_CURRENT_COVERAGE ->
                "green"
            FinancialIngestionDiagnosticCode
                .IGNORED_NON_FINANCIAL ->
                "blue"
            FinancialIngestionDiagnosticCode
                .LINKED_OBSERVATION -> "green"
            FinancialIngestionDiagnosticCode
                .OBSERVATION_ONLY -> "blue"
            FinancialIngestionDiagnosticCode
                .UNRECOGNIZED_FINANCIAL_LANGUAGE ->
                "amber"
        }
    val status =
        when (diagnostic.code) {
            FinancialIngestionDiagnosticCode
                .INGESTED -> "Captured"
            FinancialIngestionDiagnosticCode
                .PENDING_REVIEW ->
                "Needs review"
            FinancialIngestionDiagnosticCode
                .UNDERSTOOD_NO_LEDGER_EFFECT ->
                "Understood"
            FinancialIngestionDiagnosticCode
                .RESOLVED_BY_CURRENT_COVERAGE ->
                "Now understood"
            FinancialIngestionDiagnosticCode
                .IGNORED_NON_FINANCIAL ->
                "Ignored"
            FinancialIngestionDiagnosticCode
                .LINKED_OBSERVATION -> "Linked"
            FinancialIngestionDiagnosticCode
                .OBSERVATION_ONLY -> "Observed"
            FinancialIngestionDiagnosticCode
                .UNRECOGNIZED_FINANCIAL_LANGUAGE ->
                "Not understood"
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 11.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconBubble(
                icon =
                    when {
                        currentEventLabel
                            .contains(
                                "loan",
                                ignoreCase = true
                            ) ->
                            Icons.AutoMirrored.Rounded.ReceiptLong
                        currentEventLabel
                            .contains(
                                "transfer",
                                ignoreCase = true
                            ) ->
                            Icons.Rounded.SyncAlt
                        currentEventLabel
                            .contains(
                                "received",
                                ignoreCase = true
                            ) ->
                            Icons.AutoMirrored.Rounded.CallReceived
                        else ->
                            Icons.Rounded.AccountBalance
                    },
                size = 40.dp,
                background =
                    pesaToneContainer(tone),
                tint =
                    pesaToneContent(tone)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp)
            ) {
                Text(
                    currentEventLabel,
                    style =
                        MaterialTheme.typography
                            .titleSmall,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    formatTime(
                        diagnostic
                            .receivedAtEpochMs
                    ) +
                        "  •  " +
                        currentSourceType,
                    modifier =
                        Modifier.padding(top = 3.dp),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
            SmallStatusPill(status)
        }
    }
}

internal fun accountGroupTypeLabel(
    group: TrackedAccountGroup
): String =
    when (group.type) {
        AccountType.BANK -> "Bank"
        AccountType.MOBILE_WALLET ->
            "Wallet"
        AccountType.SAVINGS ->
            "Savings"
        else -> "Account"
    }

internal fun financialSourceDisplayName(
    diagnostic: FinancialIngestionDiagnostic
): String =
    FinancialProviderRegistry
        .messageSourceDisplayName(
            title = diagnostic.title,
            bankLike =
                diagnostic.sourceType == "Bank"
        )
        ?.takeIf(String::isNotBlank)
        ?: diagnostic.sourceDisplayName
            ?.takeIf(String::isNotBlank)
        ?: diagnostic.title
            ?.takeIf(String::isNotBlank)
        ?: "Unknown financial source"

private fun financialSourceType(
    diagnostic: FinancialIngestionDiagnostic
): String {
    val source =
        FinancialProviderRegistry
            .resolveMessageSourceAccount(
                title = diagnostic.title,
                bankLike =
                    diagnostic.sourceType ==
                        "Bank"
            )

    return when (
        source?.accountTypeHint
    ) {
        AccountType.BANK -> "Bank"
        AccountType.MOBILE_WALLET ->
            "Wallet"
        AccountType.SAVINGS ->
            "Savings"
        else ->
            if (
                diagnostic.sourceType ==
                "Lender"
            ) {
                "Lender"
            } else {
                "Financial"
            }
    }
}

internal fun financialEntityReviewDisplayName(
    entity: DiscoveredFinancialEntity,
    diagnostics:
        List<FinancialIngestionDiagnostic>,
    accountsById:
        Map<String, com.kedevjimmy.pesatrack.domain.model.MoneyAccount> =
            emptyMap()
): String {
    val linkedAccount =
        entity.confirmedAccountId
            ?.let(accountsById::get)
            ?: when (
                entity.providerKey
            ) {
                "SAFARICOM_MPESA" ->
                    accountsById[
                        AccountStore
                            .DEFAULT_MPESA_ID
                    ]

                else -> null
            }

    if (
        linkedAccount != null &&
        !linkedAccount.identifier
            .isNullOrBlank()
    ) {
        return AccountIdentityPresentation
            .title(linkedAccount)
    }
    KenyanBankDirectory
        .matchSender(
            entity.displayName
        )
        ?.name
        ?.let {
            return it
        }

    val resolved =
        FinancialProviderRegistry
            .resolveAccount(
                entity.displayName
            )
    if (resolved.providerKey != null) {
        return resolved.displayName
    }

    val normalized =
        ownershipReviewKey(
            entity.displayName
        )
    if (
        normalized in
            setOf(
                "locksaving",
                "locksavings",
                "locksavingsaccount"
            ) &&
        diagnostics.any {
            val title =
                it.title.orEmpty()
            val source =
                it.sourceDisplayName
                    .orEmpty()
            val combined =
                (title + " " + source)
                    .uppercase()
                    .replace(
                        Regex("""[^A-Z0-9]+"""),
                        " "
                    )
            combined.contains(
                "M SHWARI"
            )
        }
    ) {
        return "M-Shwari Lock Savings"
    }

    return entity.displayName
}

internal fun ownershipEvidenceLabel(
    entity: DiscoveredFinancialEntity
): String {
    val type =
        runCatching {
            AccountType.valueOf(
                entity.accountTypeHint
            )
        }.getOrDefault(
            AccountType.OTHER
        )
    val typeLabel =
        when (type) {
            AccountType.BANK -> "Bank"
            AccountType.MOBILE_WALLET ->
                "Wallet"
            AccountType.SAVINGS ->
                "Savings"
            else -> "Financial account"
        }

    val seen =
        SimpleDateFormat(
            "d MMM, h:mm a",
            Locale.getDefault()
        ).format(
            Date(
                entity.lastSeenEpochMs
            )
        )

    return "$typeLabel • last seen $seen"
}

internal fun shouldOfferOwnershipReview(
    entity: DiscoveredFinancialEntity,
    diagnostics:
        List<FinancialIngestionDiagnostic>
): Boolean {
    if (
        !AccountDisplayPolicy
            .isMeaningfulAccountName(
                entity.displayName
            )
    ) {
        return false
    }

    val type =
        runCatching {
            AccountType.valueOf(
                entity.accountTypeHint
            )
        }.getOrDefault(
            AccountType.OTHER
        )
    if (
        type !in
            setOf(
                AccountType.BANK,
                AccountType.MOBILE_WALLET,
                AccountType.SAVINGS
            )
    ) {
        return false
    }

    if (entity.providerKey != null) {
        return true
    }

    val display =
        entity.displayName.trim()
    if (
        !display.endsWith(
            " Bank",
            ignoreCase = true
        )
    ) {
        return true
    }

    val base =
        display.substring(
            0,
            display.length - 5
        ).trim()
    val baseKey =
        ownershipReviewKey(base)

    val looksLikeLegacyPseudoBank =
        diagnostics.any { diagnostic ->
            val title =
                diagnostic.title
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
                    ?: return@any false

            if (
                ownershipReviewKey(
                    title
                ) != baseKey
            ) {
                return@any false
            }

            FinancialProviderRegistry
                .resolveMessageSourceAccount(
                    title = title,
                    bankLike = true
                )
                ?.accountTypeHint !=
                AccountType.BANK
        }

    return !looksLikeLegacyPseudoBank
}

private fun ownershipReviewKey(
    value: String
): String =
    value.lowercase(Locale.ROOT)
        .replace(
            Regex("""[^a-z0-9]+"""),
            ""
        )

private fun financialSourceKey(
    displayName: String
): String =
    displayName
        .lowercase(Locale.ROOT)
        .replace(
            Regex("""[^a-z0-9]+"""),
            "-"
        )
        .trim('-')
        .ifBlank { "unknown" }

internal fun financialActivityDisplayName(
    item: StoredTransaction,
    rawDisplayName: String,
    accountName: String?
): String {
    val senderStyleLabel =
        FINANCIAL_SENDER_SUFFIX_REGEX
            .containsMatchIn(rawDisplayName)
    val bankLike =
        senderStyleLabel &&
            (
                item.direction ==
                    MoneyDirection.TRANSFER.name ||
                    item.category ==
                        TransactionCategory
                            .TRANSFER.name
                )

    return if (senderStyleLabel) {
        FinancialProviderRegistry
            .messageSourceDisplayName(
                title = rawDisplayName,
                bankLike = bankLike
            )
            ?: rawDisplayName
    } else {
        rawDisplayName
    }
}

internal fun financialActivityEventLabel(
    item: StoredTransaction,
    categoryLabel: String,
    rawDisplayName: String,
    accountName: String?
): String {
    val bankLike =
        listOfNotNull(
            rawDisplayName,
            accountName
        ).any {
            FINANCIAL_SENDER_SUFFIX_REGEX
                .containsMatchIn(it)
        }

    return when {
        item.category ==
            TransactionCategory.DEBT_LOAN.name &&
            item.direction ==
                MoneyDirection.INCOME.name ->
            "Loan"
        item.category ==
            TransactionCategory.DEBT_LOAN.name &&
            item.direction !=
                MoneyDirection.INCOME.name ->
            "Loan repayment"
        item.direction ==
            MoneyDirection.TRANSFER.name &&
            bankLike ->
            "Bank transfer"
        else -> categoryLabel
    }
}

private val FINANCIAL_SENDER_SUFFIX_REGEX =
    Regex(
        """\s+(?:BULK(?:\s+SMS)?|ALERTS?|SMS|NOTIFICATIONS?)\s*$""",
        RegexOption.IGNORE_CASE
    )

@Composable
internal fun MerchantBadge(
    item: StoredTransaction,
    categoryId: String,
    tone: String
) {
    IconBubble(
        icon = iconFor(
            categoryId,
            item.type
        ),
        size = 40.dp,
        background = pesaToneContainer(tone),
        tint = pesaToneContent(tone)
    )
}

private fun iconFor(
    categoryId: String,
    type: String
): ImageVector = when (categoryId) {
    TransactionCategory.RENT.name ->
        Icons.Rounded.Home
    TransactionCategory.GROCERIES.name ->
        Icons.Rounded.ShoppingCart
    TransactionCategory.EATING_OUT.name,
    "LUNCH" ->
        Icons.Rounded.Restaurant
    TransactionCategory.ELECTRICITY.name ->
        Icons.Rounded.Bolt
    TransactionCategory.INTERNET.name ->
        Icons.Rounded.Wifi
    TransactionCategory.TRANSPORT.name ->
        Icons.Rounded.DirectionsBus
    TransactionCategory.HOME_HOUSEHOLD.name ->
        Icons.Rounded.Home
    TransactionCategory.SHOPPING.name ->
        Icons.Rounded.ShoppingCart
    TransactionCategory.FAMILY.name ->
        Icons.Rounded.Groups
    TransactionCategory.FEES.name,
    TransactionCategory.DEBT_LOAN.name,
    TransactionCategory.EDUCATION.name ->
        Icons.AutoMirrored.Rounded.ReceiptLong
    TransactionCategory.PHONE_AND_DATA.name ->
        Icons.Rounded.PhoneAndroid
    TransactionCategory.INCOME.name ->
        Icons.AutoMirrored.Rounded.CallReceived
    TransactionCategory.TRANSFER.name ->
        Icons.Rounded.SyncAlt
    TransactionCategory.CASH.name ->
        Icons.Rounded.LocalAtm
    TransactionCategory.SAVINGS.name ->
        Icons.Rounded.Savings
    TransactionCategory.INVESTMENT.name ->
        Icons.Rounded.AccountBalance
    TransactionCategory.WATER.name,
    TransactionCategory.HEALTH.name,
    TransactionCategory.ENTERTAINMENT.name,
    TransactionCategory.PERSONAL_CARE.name,
    TransactionCategory.OTHER.name ->
        Icons.Rounded.MoreHoriz
    TransactionCategory.UNCATEGORIZED.name ->
        if (type == MpesaTransactionType.REVERSAL.name) {
            Icons.Rounded.SyncAlt
        } else {
            Icons.AutoMirrored.Rounded.HelpOutline
        }
    else -> Icons.Rounded.MoreHoriz
}

internal fun amountLabel(
    item: StoredTransaction,
    direction: MoneyDirection
): String {
    val sign = when {
        item.relationType ==
            TransactionStore.RELATION_REFUND -> "+"
        direction == MoneyDirection.INCOME -> "+"
        item.type == MpesaTransactionType.REVERSAL.name -> "+"
        direction == MoneyDirection.EXPENSE -> "-"
        else -> ""
    }
    return "$sign ${formatActivityMoney(item.amountMinor)}".trim()
}

internal fun formatActivityMoney(minor: Long): String =
    "KES ${NumberFormat.getNumberInstance(Locale.US).format(minor / 100.0)}"

internal fun formatSignedMoney(minor: Long): String =
    if (minor < 0) "- ${formatActivityMoney(-minor)}" else formatActivityMoney(minor)

internal fun formatTime(epochMs: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(epochMs))

internal fun dayStart(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

internal fun dayLabel(epochMs: Long): String {
    val today = dayStart(System.currentTimeMillis())
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today
        add(Calendar.DAY_OF_MONTH, -1)
    }.timeInMillis

    return when (epochMs) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> SimpleDateFormat(
            "EEE, d MMM",
            Locale.getDefault()
        ).format(Date(epochMs))
    }
}

internal fun monthLabel(epochMs: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(epochMs))

internal fun monthStart(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

internal fun shiftMonth(
    epochMs: Long,
    months: Int
): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        add(Calendar.MONTH, months)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

internal fun monthStart(year: Int, month: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis


internal fun duplicateSourceLabel(
    item: StoredTransaction
): String =
    item.counterparty
        ?.takeIf(String::isNotBlank)
        ?: item.evidenceSource
            ?.takeIf(String::isNotBlank)
        ?: item.sourcePackage
            ?.substringAfterLast('.')
            ?.takeIf(String::isNotBlank)
        ?: humanize(item.sourceKind)

internal fun formatPreciseTime(epochMs: Long): String =
    SimpleDateFormat(
        "d MMM, HH:mm:ss",
        Locale.getDefault()
    ).format(Date(epochMs))

internal fun humanize(value: String): String =
    value.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

private fun TransactionCategory.prettyName(): String =
    name.lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
