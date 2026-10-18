package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val turquoise = Color(0xFF42E5D2)
private val danger = Color(0xFFFF536D)
private val amber = Color(0xFFFFC55C)
private val violet = Color(0xFFB7A4FF)
private val blue = Color(0xFF399CFF)

@Composable
private fun DesignBCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .58f))
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            content = content
        )
    }
}

/** The home palette follows the actual Material theme, including the user's in-app theme choice. */
@Composable
private fun homeAccentColor(): Color = MaterialTheme.colorScheme.primary

@Composable
private fun homeNegativeColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > .5f) Color(0xFFB4233D)
    else MaterialTheme.colorScheme.error

@Composable
internal fun HomeBGreeting(name: String) {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            greeting + name.takeIf(String::isNotBlank)?.let { ", $it" }.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Here's your money at a glance.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun HomeBHealth(health: HomeFinancialSignals, onDetails: () -> Unit) {
    val accent = homeAccentColor()
    val stateColor = when (health.status) {
        "Needs attention", "Worth reviewing" -> MaterialTheme.colorScheme.tertiary
        "On track" -> MaterialTheme.colorScheme.secondary
        else -> accent
    }
    DesignBCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape, color = accent.copy(alpha = .13f),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    Icons.Rounded.Favorite, null,
                    modifier = Modifier.padding(10.dp), tint = accent
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Financial health", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = accent.copy(alpha = .10f),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            "Beta", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp, color = accent
                        )
                    }
                }
                Text(health.status, color = stateColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDetails) {
                Text("View", style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp))
            }
        }
        Text(
            health.explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        val icons = listOf(
            Icons.Rounded.AccountBalanceWallet, Icons.Rounded.Savings,
            Icons.Rounded.AccountBalance, Icons.Rounded.Timeline
        )
        val tints = listOf(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                .5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .38f)
            )
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 11.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                health.indicators.take(4).forEachIndexed { index, signal ->
                    Column(
                        modifier = Modifier.weight(1f).clickable(onClick = onDetails),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = tints[index].copy(alpha = .12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                icons[index], null,
                                Modifier.padding(9.dp), tint = tints[index]
                            )
                        }
                        Text(
                            signal.title, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            signal.score?.let { "$it/100" } ?: "More data",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onDetails),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Info, null, Modifier.size(15.dp), tint = accent)
            Spacer(Modifier.width(6.dp))
            Text(
                "Data confidence: " +
                    (health.confidence.score?.let { "$it/100" } ?: "Unknown"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            if (health.actions.isNotEmpty()) {
                Text("Next steps", style = MaterialTheme.typography.labelSmall, color = accent)
                Icon(Icons.Rounded.ChevronRight, null,
                    Modifier.size(15.dp), tint = accent)
            }
        }
    }
}

@Composable
internal fun HomeBMonthly(
    isCurrent: Boolean, month: String, daysLeft: Int?, moneyIn: String, moneyOut: String,
    netFlow: String, negative: Boolean, inComparison: Int?, outComparison: Int?,
    progress: Float?, onExplain: () -> Unit, onChooseMonth: () -> Unit
) {
    val dangerColor = homeNegativeColor()
    DesignBCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (isCurrent) "This month ($month)" else month,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (daysLeft != null) {
                    Text(
                        "$daysLeft days left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onChooseMonth) {
                Text("Choose month", style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Rounded.CalendarMonth, "Choose month", Modifier.size(16.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeBMoneyFigure(
                "Recorded income", moneyIn, inComparison, false, Modifier.weight(1f)
            )
            androidx.compose.material3.VerticalDivider(
                modifier = Modifier.height(62.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)
            )
            HomeBMoneyFigure(
                "Recorded expenses", moneyOut, outComparison, true, Modifier.weight(1f)
            )
        }
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Net flow", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        netFlow, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (negative) dangerColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
                IconButton(onClick = onExplain, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Info, "About recorded net flow",
                        tint = homeAccentColor())
                }
            }
        }
        Row(verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Info, null, Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Recorded transactions only. Some income or transfers may be missing. " +
                    "Net flow is not your bank balance.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun HomeBMoneyFigure(
    title: String, amount: String, change: Int?, isOutflow: Boolean, modifier: Modifier
) {
    val positiveChange = change != null && if (isOutflow) change < 0 else change > 0
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall, maxLines = 2
        )
        Text(
            amount, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 21.sp
        )
        Text(
            change?.let { (if (it > 0) "+" else "") + it.toString() + "% vs prior" }
                ?: "No prior comparison",
            fontSize = 11.sp, maxLines = 2,
            color = if (change == null || change == 0) MaterialTheme.colorScheme.onSurfaceVariant
            else if (positiveChange) MaterialTheme.colorScheme.secondary else homeNegativeColor()
        )
    }
}

@Composable
internal fun HomeBInsight(
    text: String, onClick: () -> Unit, actionNeeded: Boolean = true
) {
    val accent = if (actionNeeded) MaterialTheme.colorScheme.tertiary
                 else MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick, shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .58f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .16f)) {
                Icon(
                    if (actionNeeded) Icons.Rounded.FactCheck else Icons.Rounded.Lightbulb,
                    null, Modifier.padding(10.dp).size(21.dp), tint = accent
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (actionNeeded) "Action needed" else "Spending insight",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(Icons.Rounded.ChevronRight, "Open review", tint = accent)
        }
    }
}

@Composable
internal fun HomeBTile(
    title: String, amount: String, help: String, icon: ImageVector,
    tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > .5f
    val iconColor = if (isLight) when (title) {
        "Upcoming bills" -> Color(0xFF9A5B0A)
        "Investments" -> Color(0xFF087E73)
        "Enter salary" -> Color(0xFF2454B8)
        else -> MaterialTheme.colorScheme.primary
    } else tint
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 105.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .60f)
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Surface(shape = CircleShape, color = iconColor.copy(alpha = .13f)) {
                    Icon(icon, null, Modifier.padding(8.dp).size(20.dp), tint = iconColor)
                }
                Text(
                    title, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 15.sp,
                    modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                amount, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                help, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun HomeBInvestmentsDialog(
    positions:List<ObservedInvestmentPosition>,onDismiss:()->Unit
) {
    AlertDialog(onDismissRequest=onDismiss,
        title={Text("Observed investments")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("The app has execution observations, not a verified market value or an authoritative brokerage balance.",
                    style=MaterialTheme.typography.bodySmall)
                if(positions.isEmpty()) Text("No executed investments have been detected.")
                positions.take(6).forEach { pos ->
                    Text(pos.label,fontWeight=FontWeight.Bold)
                    Text("Observed net units: "+
                        java.text.NumberFormat.getNumberInstance(Locale.US)
                            .format(pos.quantityMicrounits/1_000_000.0)+
                        " · Partial history",
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(positions.size>6) Text("More investment records are available.")
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Close")}}
    )
}

@Composable
internal fun HomeBRecent(
    rows:List<StoredTransaction>, category:(String)->String,
    money:(Long)->String,onOpen:(Long)->Unit,onAll:()->Unit
) {
    DesignBCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Recent activity",Modifier.weight(1f),
                style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
            TextButton(onClick=onAll,contentPadding=PaddingValues(2.dp)){
                Text("See all",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)
            }
        }
        if(rows.isEmpty()) Text("No transactions for this month.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
        rows.take(3).forEachIndexed { index,row ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(row.id) }
                .padding(vertical=4.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primary.copy(alpha=.12f)) {
                    Icon(Icons.Rounded.ReceiptLong,null,
                        Modifier.padding(8.dp).size(19.dp),tint=MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(row.counterparty?.takeIf(String::isNotBlank)?:"Transaction",
                        style=MaterialTheme.typography.labelMedium,
                        fontWeight=FontWeight.SemiBold,maxLines=1,
                        overflow=TextOverflow.Ellipsis)
                    Text(category(row.category)+" · "+
                        SimpleDateFormat("d MMM, HH:mm",locale)
                            .format(Date(row.occurredAtEpochMs)),
                        fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines=1)
                }
                val incoming=row.direction==MoneyDirection.INCOME.name
                Text((if(incoming)"+" else if(row.direction==MoneyDirection.EXPENSE.name)"−" else "")+
                    money(row.amountMinor),fontSize=11.sp,fontWeight=FontWeight.Bold,
                    color=if(incoming)turquoise else if(row.direction==MoneyDirection.EXPENSE.name)
                        danger else MaterialTheme.colorScheme.onSurface,
                    maxLines=1)
            }
            if(index<rows.take(3).lastIndex)
                HorizontalDivider(thickness=.5.dp,
                    color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f))
        }
    }
}
