package com.kedevjimmy.pesatrack.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.planning.VoicePlanKind
import com.kedevjimmy.pesatrack.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import com.kedevjimmy.pesatrack.domain.intelligence.ForecastStatus
import com.kedevjimmy.pesatrack.domain.intelligence.OfflineMoneyPolicy
import kotlin.math.roundToInt

private fun goalMoney(minor: Long): String = formatKesAmount(minor)

private fun goalDate(epochMs: Long, locale: Locale): String =
    SimpleDateFormat("d MMM yyyy", locale).format(Date(epochMs))

private fun monthStart(offset: Int): Long =
    Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, offset)
    }.timeInMillis

private data class ReferenceGoalState(
    val rules: List<SmartRule>,
    val currentMonthSavings: Map<String?, Long>,
    val previousMonthSavings: Map<String?, Long>,
    val totalSavings: Map<String?, Long>,
    val metadata: Map<String, SavingGoalMetadata>,
    val completedSavings: Map<String?, List<Long>> = emptyMap()
)

private enum class GoalGroup(
    val label: String,
    val icon: ImageVector
) {
    ALL("All Goals", Icons.Rounded.TrackChanges),
    FUTURE("Future Purchases", Icons.Rounded.ShoppingBag),
    SAVINGS("Savings Goals", Icons.Rounded.Savings),
    LIFESTYLE("Lifestyle", Icons.Rounded.Flight)
}

private data class GoalCardModel(
    val rule: SmartRule,
    val title: String,
    val group: GoalGroup,
    val monthlyTargetMinor: Long,
    val targetMinor: Long,
    val savedMinor: Long,
    val progress: Float,
    val targetDateEpochMs: Long?,
    val completedSavings: List<Long> = emptyList(),
    val hasTotalTarget: Boolean = false
)

private fun goalGroupFor(rule: SmartRule): GoalGroup {
    val text = (rule.displayLabel ?: rule.name).lowercase(Locale.getDefault())
    return when {
        listOf(
            "ps5", "playstation", "phone", "laptop", "table", "sofa",
            "furniture", "tv", "television", "car", "fridge", "purchase", "buy"
        ).any(text::contains) -> GoalGroup.FUTURE
        listOf(
            "vacation", "holiday", "travel", "diani", "trip",
            "wedding", "lifestyle", "gym"
        ).any(text::contains) -> GoalGroup.LIFESTYLE
        else -> GoalGroup.SAVINGS
    }
}

private data class GoalArtwork(
    val icon: ImageVector,
    val colors: List<Color>,
    val badge: ImageVector,
    val badgeColor: Color
)

private fun goalArtwork(title: String): GoalArtwork {
    val text = title.lowercase(Locale.getDefault())
    return when {
        "ps5" in text || "playstation" in text || "game" in text ->
            GoalArtwork(
                Icons.Rounded.SportsEsports,
                listOf(Color(0xFF243B73), Color(0xFF4A3FB4)),
                Icons.Rounded.SportsEsports,
                Color(0xFF7C6FF2)
            )
        "table" in text || "furniture" in text || "sofa" in text ->
            GoalArtwork(
                Icons.Rounded.TableRestaurant,
                listOf(Color(0xFF62401D), Color(0xFFC18442)),
                Icons.Rounded.Storefront,
                Color(0xFFF0B74A)
            )
        "emergency" in text || "fund" in text ->
            GoalArtwork(
                Icons.Rounded.HealthAndSafety,
                listOf(Color(0xFF533A18), Color(0xFF9A6D25)),
                Icons.Rounded.HealthAndSafety,
                Color(0xFF14B8A6)
            )
        "vacation" in text || "holiday" in text || "travel" in text || "diani" in text ->
            GoalArtwork(
                Icons.Rounded.BeachAccess,
                listOf(Color(0xFF2878B8), Color(0xFF5B55E7)),
                Icons.Rounded.Flight,
                Color(0xFF57C7FF)
            )
        "land" in text || "house" in text || "home" in text ->
            GoalArtwork(
                Icons.Rounded.HomeWork,
                listOf(Color(0xFF29254F), Color(0xFF5A54A6)),
                Icons.Rounded.Home,
                Color(0xFF14B8A6)
            )
        else ->
            GoalArtwork(
                Icons.Rounded.Savings,
                listOf(Color(0xFF1A2135), Color(0xFF4F46D8)),
                Icons.Rounded.TrackChanges,
                Color(0xFF14B8A6)
            )
    }
}

private fun goalProgressMessage(model: GoalCardModel): String {
    if (!model.hasTotalTarget) return "Add the full amount you want to save to see when you might reach it."
    val forecast = OfflineMoneyPolicy.forecast(model.targetMinor, model.savedMinor, model.completedSavings)
    return when (forecast.status) {
        ForecastStatus.REACHED -> "You have reached this goal."
        ForecastStatus.INSUFFICIENT_HISTORY ->
            "Save towards this goal for three full months to see an estimated finish date."
        ForecastStatus.NO_RELIABLE_PROGRESS ->
            "There is not enough savings progress to estimate a finish date yet."
        ForecastStatus.ESTIMATE -> {
            val range = forecast.slowestMonths?.let {
                " It could take ${forecast.fastestMonths}–$it months if your pace changes."
            } ?: " A slower pace could take longer."
            "At your recent pace, about ${forecast.typicalMonths} months to go.$range " +
                "Based on the last ${forecast.sampleMonths} full months of saving and withdrawals. " +
                "This is an estimate, not a promise."
        }
    }
}

@Composable
private fun GoalPageHeader(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Plan")
        }
        VibrantPlanPageTitle("Goals", "Save for what matters to you.",
            Modifier.weight(1f))
    }
}

private fun goalReferenceAsset(
    title: String
): String? {
    val text =
        title.lowercase(
            Locale.getDefault()
        )
    return when {
        "ps5" in text ||
            "playstation" in text ->
            GoalReferenceAssets.PS5

        "table" in text ||
            "furniture" in text ||
            "sofa" in text ->
            GoalReferenceAssets.TABLE

        "emergency" in text ->
            GoalReferenceAssets.EMERGENCY

        "vacation" in text ||
            "diani" in text ||
            "holiday" in text ||
            "travel" in text ->
            GoalReferenceAssets.VACATION

        else -> null
    }
}

@Composable
private fun GoalArtworkTile(
    title: String,
    modifier: Modifier = Modifier
) {
    val encoded =
        goalReferenceAsset(title)
    val bitmap =
        remember(encoded) {
            encoded?.let {
                runCatching {
                    val bytes =
                        Base64.decode(
                            it,
                            Base64.DEFAULT
                        )
                    BitmapFactory
                        .decodeByteArray(
                            bytes,
                            0,
                            bytes.size
                        )
                        ?.asImageBitmap()
                }.getOrNull()
            }
        }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription =
                title + " goal image",
            modifier =
                modifier.clip(
                    RoundedCornerShape(
                        16.dp
                    )
                ),
            contentScale =
                ContentScale.Crop
        )
        return
    }

    val artwork =
        goalArtwork(title)
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    16.dp
                )
            )
            .background(
                Brush.linearGradient(
                    artwork.colors
                )
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            artwork.icon,
            contentDescription = null,
            modifier =
                Modifier.size(48.dp),
            tint =
                Color.White.copy(
                    alpha = .96f
                )
        )
        Surface(
            modifier =
                Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .padding(7.dp)
                    .size(30.dp),
            shape =
                RoundedCornerShape(
                    9.dp
                ),
            color =
                artwork.badgeColor
        ) {
            Box(
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    artwork.badge,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(
                            18.dp
                        ),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun GoalSummaryBars() {
    val heights = listOf(26, 42, 62, 48, 38, 51, 58, 44)
    Row(
        modifier = Modifier.width(118.dp).height(70.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp)
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF55E6D1), Color(0xFF3933A8))
                        )
                    )
            )
        }
    }
}

@Composable
private fun GoalHeroMetric(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tone: Color = Color(0xFFC5BCFF)
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = tone.copy(alpha = .13f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = tone)
            }
        }
        Column(Modifier.padding(start = 7.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFDCE4F4), // Legible on fixed dark hero in both themes.
                maxLines = 2
            )
        }
    }
}

@Composable
private fun GoalHero(
    totalSavedMinor: Long, currentMonthMinor: Long, previousMonthMinor: Long,
    averageProgress: Int, totalTargetMinor: Long
) {
    val progress = if (totalTargetMinor > 0L)
        (totalSavedMinor.toFloat() / totalTargetMinor.toFloat()).coerceIn(0f,1f) else 0f
    VibrantPlanSurface {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Total saved toward goals", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(goalMoney(totalSavedMinor), style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold)
                Text("of " + goalMoney(totalTargetMinor), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VibrantPlanIcon(Icons.Rounded.TrackChanges, "violet", size = 58)
        }
        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(9.dp)
            .clip(RoundedCornerShape(50.dp)),
            color = PlanVibrant.violet, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Goal progress  •  " + (progress * 100).roundToInt() + "%",
                style = MaterialTheme.typography.labelSmall,
                color = PlanVibrant.violet)
            Text("Saved this month  " + goalMoney(currentMonthMinor),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalFilterChip(
    group: GoalGroup,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50.dp),
        color = if (selected) Color(0xFFEDE6FF)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) PlanVibrant.violet
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                group.icon,
                null,
                Modifier.size(17.dp),
                tint = if (selected) PlanVibrant.violet
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                group.label,
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) PlanVibrant.violet
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalReferenceCard(
    model: GoalCardModel, locale: Locale, onClick: () -> Unit
) {
    val percent = (model.progress * 100).roundToInt().coerceIn(0, 100)
    val tone = when(model.group) {
        GoalGroup.SAVINGS -> "mint"
        GoalGroup.LIFESTYLE -> "pink"
        GoalGroup.FUTURE -> "violet"
        else -> "blue"
    }
    val icon = when {
        model.title.contains("ps5",true) || model.title.contains("gaming",true) ->
            Icons.Rounded.SportsEsports
        model.title.contains("laptop",true) || model.title.contains("phone",true) ->
            Icons.Rounded.Devices
        model.title.contains("emergency",true) -> Icons.Rounded.Eco
        else -> model.group.icon
    }
    val (_,barColor) = planVibrantTone(tone)
    VibrantPlanSurface(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VibrantPlanIcon(icon, tone, size = 53)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(model.title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(goalMoney(model.savedMinor) + " / " + goalMoney(model.targetMinor),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    LinearProgressIndicator(progress = {model.progress},
                        modifier = Modifier.weight(1f).height(7.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        color = barColor, trackColor = Color(0xFFEBE7F8))
                    Text(percent.toString() + "%", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall)
                }
                model.targetDateEpochMs?.let {
                    Text("Target: " + goalDate(it, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "View goal",
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailsSheet(
    model: GoalCardModel,
    locale: Locale,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoalArtworkTile(model.title, Modifier.size(82.dp))
                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                    Text(
                        model.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Target " + goalMoney(model.targetMinor),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LinearProgressIndicator(
                progress = { model.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50.dp)),
                color = Color(0xFF7C6FF2),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    goalMoney(model.savedMinor) + " saved",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    ((model.progress * 100).roundToInt().coerceIn(0, 100)).toString() + "%",
                    fontWeight = FontWeight.Bold
                )
            }
            model.targetDateEpochMs?.let {
                Text(
                    "Target date • " + goalDate(it, locale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Monthly contribution • " + goalMoney(model.monthlyTargetMinor),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF171F31)
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF55E6D1))
                    Text(
                        goalProgressMessage(model),
                        modifier = Modifier.padding(start = 9.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Icon(Icons.Rounded.Edit, null)
                Spacer(Modifier.width(7.dp))
                Text("Edit goal")
            }
        }
    }
}

@Composable
fun SavingGoalsScreen(
    scaffoldPadding: PaddingValues,
    onBack: () -> Unit,
    onPlanClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val revision by LedgerUpdateBus.state.collectAsState()

    var loaded by remember {
        mutableStateOf(
            ReferenceGoalState(
                emptyList(),
                emptyMap(),
                emptyMap(),
                emptyMap(),
                emptyMap()
            )
        )
    }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableIntStateOf(0) }
    var selectedGroup by remember { mutableStateOf(GoalGroup.ALL) }
    var goalEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SmartRule?>(null) }
    var selectedGoal by remember { mutableStateOf<GoalCardModel?>(null) }
    var voiceAdd by remember { mutableStateOf(false) }

    LaunchedEffect(revision.revision, retryLoad) {
        loadError = null
        try {
            loaded = withContext(Dispatchers.IO) {
                val rules = RuleStore.get(context).listRules(true).filter {
                    it.category == TransactionCategory.SAVINGS &&
                        (it.plannedAmountMinor ?: 0L) > 0L
                }
                val store = TransactionStore.get(context)
                val types = AccountStore.get(context)
                    .listAccounts(false)
                    .associate { it.id to it.type }

                fun savingsByRule(rows: List<StoredTransaction>): Map<String?, Long> =
                    rules.associate { rule ->
                        rule.id to checkNotNull(GoalSavingHistoryPolicy.netSaved(
                            GoalSavingHistoryPolicy.observations(rows, rule.id, types)
                        )) { "Savings total exceeds supported range" }
                    }

                val now = System.currentTimeMillis()
                val currentRows = store.activitySnapshotBetween(
                    monthStart(0),
                    monthStart(1),
                    now,
                    performMaintenance = false
                ).trustedTransactions
                val previousRows = store.activitySnapshotBetween(
                    monthStart(-1),
                    monthStart(0),
                    now,
                    performMaintenance = false
                ).trustedTransactions
                val totalRows = store.activitySnapshotBetween(
                    0L,
                    now + 1L,
                    now,
                    performMaintenance = false
                ).trustedTransactions

                ReferenceGoalState(
                    rules = rules,
                    currentMonthSavings = savingsByRule(currentRows),
                    previousMonthSavings = savingsByRule(previousRows),
                    totalSavings = savingsByRule(totalRows),
                    metadata = SavingGoalMetadataStore.get(context)
                        .listForRules(rules.map { it.id }),
                    completedSavings = rules.associate { rule ->
                        rule.id to OfflineMoneyPolicy.completedSavings(
                            GoalSavingHistoryPolicy.observations(totalRows, rule.id, types), now)
                    }
                )
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            android.util.Log.e("PesaTrackScreens", "Could not load goals")
            loadError = "Couldn't load this page. Your saved data is unchanged."
        }
    }

    val models = loaded.rules.map { rule ->
        val metadata = loaded.metadata[rule.id]
        val monthlyTarget = rule.plannedAmountMinor ?: 0L
        val target = metadata?.totalTargetMinor?.takeIf { it > 0L } ?: monthlyTarget
        val saved = loaded.totalSavings[rule.id] ?: 0L
        GoalCardModel(
            rule = rule,
            title = rule.displayLabel ?: rule.name,
            group = goalGroupFor(rule),
            monthlyTargetMinor = monthlyTarget,
            targetMinor = target,
            savedMinor = saved,
            progress = if (target <= 0L) 0f
            else (saved.toFloat() / target.toFloat()).coerceIn(0f, 1f),
            targetDateEpochMs = metadata?.targetDateEpochMs,
            completedSavings = loaded.completedSavings[rule.id].orEmpty(),
            hasTotalTarget = (metadata?.totalTargetMinor ?: 0L) > 0L
        )
    }

    val activeGoals = models.filter { it.progress < 1f }
    val totalSaved = models.sumOf { it.savedMinor }
    val totalTarget = models.sumOf { it.targetMinor }
    val averageProgress = if (activeGoals.isEmpty()) {
        if (models.isEmpty()) 0 else 100
    } else {
        (activeGoals.map { it.progress }.average() * 100).roundToInt()
    }
    val filtered = models.filter {
        selectedGroup == GoalGroup.ALL || it.group == selectedGroup
    }

    if (goalEditor) {
        SavingGoalEditor(editingGoal) {
            goalEditor = false
            editingGoal = null
            // Rule/metadata edits are not ledger transactions, so explicitly
            // reload the goal models when the editor closes.
            retryLoad++
        }
    }

    if (voiceAdd) {
        VoicePlanningAssistantSheet(
            kind = VoicePlanKind.GOAL,
            onDismiss = {
                voiceAdd = false
            },
            onSaved = {
                retryLoad++
            }
        )
    }

    selectedGoal?.let { model ->
        GoalDetailsSheet(
            model = model,
            locale = locale,
            onDismiss = { selectedGoal = null },
            onEdit = {
                selectedGoal = null
                editingGoal = model.rule
                goalEditor = true
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background)
            .testTag("saving-goals-list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ConceptLoadError(loadError) { retryLoad++ } }
        item { GoalPageHeader(onBack) }
        item {
            GoalHero(
                totalSavedMinor = totalSaved,
                currentMonthMinor = loaded.currentMonthSavings.values.sum(),
                previousMonthMinor = loaded.previousMonthSavings.values.sum(),
                averageProgress = averageProgress,
                totalTargetMinor = totalTarget
            )
        }
        item { VibrantPlanSectionTitle("Your goals") }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 6.dp)
            ) {
                items(GoalGroup.entries) { group ->
                    GoalFilterChip(
                        group,
                        group == selectedGroup
                    ) { selectedGroup = group }
                }
            }
        }
        items(filtered, key = { it.rule.id }) { model ->
            GoalReferenceCard(
                model = model,
                locale = locale,
                onClick = { selectedGoal = model }
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VibrantActionButton(
                    "+ New goal",
                    onClick = {
                        editingGoal = null
                        goalEditor = true
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { voiceAdd = true },
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8CBFF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFEDE6FF),
                        contentColor = PlanVibrant.violet
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp)
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add by voice", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
