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

private fun goalMoney(minor: Long): String {
    val absolute = abs(minor)
    val hasCents = absolute % 100L != 0L
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (hasCents) 2 else 0
        maximumFractionDigits = if (hasCents) 2 else 0
    }
    return (if (minor < 0L) "-KSh " else "KSh ") +
        formatter.format(absolute / 100.0)
}

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
                listOf(Color(0xFF292929), Color(0xFF404040)),
                Icons.Rounded.SportsEsports,
                Color(0xFF525252)
            )
        "table" in text || "furniture" in text || "sofa" in text ->
            GoalArtwork(
                Icons.Rounded.TableRestaurant,
                listOf(Color(0xFF3A3A3A), Color(0xFF656565)),
                Icons.Rounded.Storefront,
                Color(0xFFA56813)
            )
        "emergency" in text || "fund" in text ->
            GoalArtwork(
                Icons.Rounded.HealthAndSafety,
                listOf(Color(0xFF292929), Color(0xFF555555)),
                Icons.Rounded.HealthAndSafety,
                Color(0xFF287A5B)
            )
        "vacation" in text || "holiday" in text || "travel" in text || "diani" in text ->
            GoalArtwork(
                Icons.Rounded.BeachAccess,
                listOf(Color(0xFF3F3F3F), Color(0xFF181818)),
                Icons.Rounded.Flight,
                Color(0xFFA6CAE3)
            )
        "land" in text || "house" in text || "home" in text ->
            GoalArtwork(
                Icons.Rounded.HomeWork,
                listOf(Color(0xFF292929), Color(0xFF4C4C4C)),
                Icons.Rounded.Home,
                Color(0xFF287A5B)
            )
        else ->
            GoalArtwork(
                Icons.Rounded.Savings,
                listOf(Color(0xFF1E1E1E), Color(0xFF181818)),
                Icons.Rounded.TrackChanges,
                Color(0xFF287A5B)
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
private fun GoalPageHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Goals",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Turn your dreams into a plan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Goals", style = MaterialTheme.typography.labelMedium)
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp).size(18.dp)
                )
            }
        }
        Surface(
            modifier = Modifier.padding(start = 8.dp).size(42.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Notifications, contentDescription = null)
            }
        }
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
                            listOf(Color(0xFF9AD2BB), Color(0xFF181818))
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
    tone: Color = Color(0xFF525252)
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GoalHero(
    totalSavedMinor: Long,
    currentMonthMinor: Long,
    previousMonthMinor: Long,
    averageProgress: Int,
    totalTargetMinor: Long
) {
    val growthLabel = when {
        previousMonthMinor > 0L -> {
            val percent = (
                (currentMonthMinor - previousMonthMinor) * 100.0 /
                    previousMonthMinor.toDouble()
                ).roundToInt()
            val direction = if (percent >= 0) "↑" else "↓"
            "$direction ${abs(percent)}% from last month"
        }
        currentMonthMinor > 0L -> "New savings this month"
        else -> "No change this month"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF191919),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Total Goal Savings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = .74f)
                        )
                        Icon(
                            Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 7.dp).size(19.dp),
                            tint = Color(0xFFEEEEEE)
                        )
                    }
                    Text(
                        goalMoney(totalSavedMinor),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        growthLabel,
                        modifier = Modifier.padding(top = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF9AD2BB)
                    )
                }
                if (totalSavedMinor > 0L) GoalSummaryBars()
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 13.dp),
                color = Color.White.copy(alpha = .12f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoalHeroMetric(
                    Icons.Rounded.DonutLarge,
                    "$averageProgress%",
                    "Average Progress",
                    Modifier.weight(1f)
                )
                GoalHeroMetric(
                    Icons.Rounded.EmojiEvents,
                    goalMoney(totalTargetMinor),
                    "Total Target",
                    Modifier.weight(1f),
                    Color(0xFFE8C18C)
                )
            }
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
        color = if (selected) Color(0xFF303030)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Color(0xFF9AD2BB)
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
                tint = if (selected) Color(0xFFDEDEDE)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                group.label,
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color(0xFFF5F5F5)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalReferenceCard(
    model: GoalCardModel,
    locale: Locale,
    onClick: () -> Unit
) {
    val percent = (model.progress * 100).roundToInt().coerceIn(0, 100)
    PesaCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.Top
        ) {
            GoalArtworkTile(model.title, Modifier.size(52.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        model.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Target: " + goalMoney(model.targetMinor),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    model.targetDateEpochMs?.let {
                        Row(
                            modifier = Modifier.padding(start = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                null,
                                Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                goalDate(it, locale),
                                modifier = Modifier.padding(start = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { model.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        color = Color(0xFF525252),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "$percent%",
                        modifier = Modifier.padding(start = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    goalMoney(model.savedMinor) + " / " + goalMoney(model.targetMinor),
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(.44f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(26.dp),
                            shape = CircleShape,
                            color = Color(0xFFE8C18C).copy(alpha = .12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.MonetizationOn,
                                    null,
                                    Modifier.size(16.dp),
                                    tint = Color(0xFFE8C18C)
                                )
                            }
                        }
                        Column(Modifier.padding(start = 5.dp)) {
                            Text(
                                goalMoney(model.monthlyTargetMinor),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                "Monthly contribution",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(.56f),
                        shape = RoundedCornerShape(11.dp),
                        color = Color(0xFF191919)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                null,
                                Modifier.size(15.dp),
                                tint = Color(0xFF9AD2BB)
                            )
                            Text(
                                goalProgressMessage(model),
                                modifier = Modifier.padding(start = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
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
                GoalArtworkTile(model.title, Modifier.size(64.dp))
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
                color = Color(0xFF525252),
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
                color = Color(0xFF191919)
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF9AD2BB))
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
            android.util.Log.e("PesaTrackScreens", "Could not load goals", failure)
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
        item { GoalPageHeader() }
        item {
            GoalHero(
                totalSavedMinor = totalSaved,
                currentMonthMinor = loaded.currentMonthSavings.values.sum(),
                previousMonthMinor = loaded.previousMonthSavings.values.sum(),
                averageProgress = averageProgress,
                totalTargetMinor = totalTarget
            )
        }
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
            OutlinedButton(
                onClick = {
                    voiceAdd = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 11.dp
                )
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add goal by voice",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Button(
                onClick = {
                    editingGoal = null
                    goalEditor = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFF9AD2BB)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = Color(0xFF101010)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Create Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Set a new goal and start saving",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = .76f)
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
            }
        }
    }
}
