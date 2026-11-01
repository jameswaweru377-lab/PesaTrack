package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.ui.components.MainTab
import com.kedevjimmy.pesatrack.ui.components.PesaLogoMark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The attached 21 Oct 2026 light Home reference is the presentation contract.
 * All amounts remain sourced from the financial ledger: the screenshot's April
 * 2025 amounts must never become fake financial records in the real application.
 */
internal object HomeReferencePalette {
    val canvas = Color(0xFFF7F7FF)
    val ink = Color(0xFF0B103A)
    val muted = Color(0xFF626B98)
    val purple = Color(0xFF5D00EB)
    val violet = Color(0xFF6B18FF)
    val green = Color(0xFF087E54)
    val red = Color(0xFFDC1551)
    val blue = Color(0xFF003BCD)
    val line = Color(0xFFE6E8F2)
}

@Composable
internal fun ReferenceHomeHeader(onNotifications: () -> Unit, onProfile: () -> Unit, profileInitial: String) {
    val p = HomeReferencePalette
    val locale = LocalConfiguration.current.locales[0]
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 18.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PesaLogoMark(size = 30.dp)
        Spacer(Modifier.width(9.dp))
        Text("PesaTrack", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.6).sp, color = Color(0xFF191050), modifier = Modifier.weight(1f))
        Box(Modifier.size(42.dp).clickable(onClick = onNotifications), contentAlignment = Alignment.Center) {
            Surface(Modifier.size(38.dp), shape = CircleShape, color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E1FF))) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.NotificationsNone, "Notifications", Modifier.size(23.dp), tint = p.ink)
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 2.dp)
                .size(7.dp).background(p.purple, CircleShape))
        }
        Spacer(Modifier.width(11.dp))
        Surface(onClick = onProfile, modifier = Modifier.size(40.dp), shape = CircleShape,
            color = Color(0xFFE9E4FF)) {
            Box(contentAlignment = Alignment.Center) {
                Text(profileInitial.ifBlank { "J" }.take(1).uppercase(locale),
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
            }
        }
    }
}

@Composable
internal fun ReferenceHomeBottomBar(currentTab: MainTab, onSelected: (MainTab) -> Unit) {
    val p = HomeReferencePalette
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The approved More reference restores the Accounts shortcut as a
            // visible primary tab instead of highlighting More for Accounts.
            val tabs = listOf(
                Triple(MainTab.HOME, Icons.Rounded.Home, "Home"),
                Triple(MainTab.ACTIVITY, Icons.Rounded.FormatListBulleted, "Activity"),
                Triple(MainTab.PLAN, Icons.Rounded.PieChartOutline, "Plan"),
                Triple(MainTab.ACCOUNTS, Icons.Rounded.AccountBalanceWallet, "Accounts"),
                Triple(MainTab.MORE, Icons.Rounded.MoreHoriz, "More")
            )
            tabs.forEach { (tab, icon, title) ->
                val active = currentTab == tab
                val surface = if (active) Modifier.background(
                    Brush.verticalGradient(listOf(Color(0xFF7625FF), Color(0xFF4300D8))),
                    RoundedCornerShape(15.dp)
                ) else Modifier
                Column(
                    Modifier.weight(1f).fillMaxHeight().then(surface).clip(RoundedCornerShape(15.dp))
                        .clickable { onSelected(tab) }
                        .padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Icon(icon, title, Modifier.size(22.dp),
                        tint = if (active) Color.White else Color(0xFF6E7397))
                    Text(title, fontSize = 11.sp, maxLines = 1,
                        color = if (active) Color.White else Color(0xFF5F668C),
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
internal fun ReferenceHomeGreeting(name: String) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(greeting + name.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty(),
            fontSize = 24.sp, letterSpacing = (-0.8).sp, lineHeight = 29.sp,
            fontWeight = FontWeight.Bold, color = HomeReferencePalette.ink)
        Text("Here’s your money at a glance.", fontSize = 14.sp,
            color = HomeReferencePalette.muted, lineHeight = 20.sp)
    }
}

@Composable
internal fun ReferenceHomeHero(amount: String, month: String, onMonth: () -> Unit,
                               onPlan: () -> Unit, onAccounts: () -> Unit) {
    val p = HomeReferencePalette
    var visible by rememberSaveable { mutableStateOf(true) }
    Box(
        Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(21.dp))
            .background(Brush.linearGradient(
                listOf(Color(0xFF6408EC), Color(0xFF4416CB), Color(0xFF071B8C)),
                start = Offset.Zero, end = Offset(1100f, 620f)))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val top = Offset(size.width * .91f, -size.height * .14f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF00D5EE), Color(0xFF136EFF), Color(0xFF5433EE)),
                    center = top, radius = size.width * .39f
                ), radius = size.width * .30f, center = top
            )
            drawCircle(color = Color(0xFF3151F5).copy(alpha = .40f),
                radius = size.width * .26f,
                center = Offset(size.width * .79f, size.height * 1.07f))
            drawCircle(color = Color(0xFF8E8DFF).copy(alpha = .23f),
                radius = size.width * .26f,
                center = Offset(size.width * .79f, size.height * 1.07f),
                style = Stroke(width = 1.5.dp.toPx()))
        }
        Column(Modifier.fillMaxSize().padding(start = 18.dp, end = 16.dp, top = 15.dp, bottom = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Available after commitments", color = Color.White,
                    fontSize = 13.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { visible = !visible }, modifier = Modifier.size(27.dp)) {
                    Icon(if (visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        if (visible) "Hide balance" else "Show balance",
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Text(if (visible) amount else "••••••",
                fontSize = if (amount.length > 16) 23.sp else if (amount.length > 12) 27.sp else 31.sp,
                lineHeight = 35.sp,
                letterSpacing = (-1.2).sp, color = Color.White, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.clickable(onClick = onMonth).padding(top = 1.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(month, fontSize = 13.sp, color = Color.White.copy(alpha = .93f))
                Icon(Icons.Rounded.KeyboardArrowDown, "Choose month",
                    Modifier.size(18.dp).padding(start = 2.dp), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(onClick = onPlan, modifier = Modifier.weight(1.18f).fillMaxHeight(),
                    color = Color.White, shape = CircleShape) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.BarChart, null, Modifier.size(18.dp), tint = p.purple)
                        Spacer(Modifier.width(7.dp))
                        Text("Review plan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.purple)
                    }
                }
                Surface(onClick = onAccounts, modifier = Modifier.weight(.92f).fillMaxHeight(),
                    shape = CircleShape, color = Color.White.copy(alpha = .17f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .21f))) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(17.dp), tint = Color.White)
                        Spacer(Modifier.width(7.dp))
                        Text("Accounts", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), content = content)
    }
}

@Composable
internal fun ReferenceHomeMonth(
    range: String, income: String, spending: String, budgetLeft: String,
    budgetTotal: String, incomeVsPrior: String, spendingVsPrior: String,
    budgetProgress: Float?, onMonth: () -> Unit
) {
    val p = HomeReferencePalette
    ReferenceSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("This month", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                color = p.ink, modifier = Modifier.weight(1f))
            Text(range, fontSize = 11.sp, color = p.muted,
                modifier = Modifier.clickable { onMonth() })
        }
        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
            @Composable fun RowScope.metric(icon: ImageVector, label: String, amount: String,
                                            foot: String, tint: Color, bg: Color,
                                            progress: Float? = null) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Surface(Modifier.size(27.dp), shape = CircleShape, color = bg) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, null, Modifier.size(17.dp), tint = tint)
                            }
                        }
                        Text(label, fontSize = 10.sp, maxLines = 1,
                            color = p.ink, overflow = TextOverflow.Ellipsis)
                    }
                    Text(amount, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold,
                        color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        buildAnnotatedString {
                            val lastSpace = foot.lastIndexOf(' ')
                            if (foot.startsWith("vs ") && lastSpace > 0) {
                                append(foot.substring(0, lastSpace + 1))
                                withStyle(SpanStyle(color = tint, fontWeight = FontWeight.SemiBold)) {
                                    append(foot.substring(lastSpace + 1))
                                }
                            } else append(foot)
                        },
                        fontSize = 10.sp, color = p.muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (progress != null) {
                        Box(Modifier.fillMaxWidth().padding(end = 3.dp, top = 2.dp)
                            .height(7.dp).clip(CircleShape).background(Color(0xFFE6E7F1))) {
                            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                                .background(Brush.horizontalGradient(
                                    listOf(Color(0xFF166DFF), Color(0xFF0616D5))), CircleShape))
                        }
                    }
                }
            }
            metric(Icons.Rounded.South, "Money in", income, incomeVsPrior,
                p.green, Color(0xFFD0FBF0))
            androidx.compose.material3.VerticalDivider(color = p.line,
                modifier = Modifier.padding(horizontal = 7.dp).fillMaxHeight(), thickness = 1.dp)
            metric(Icons.Rounded.North, "Money out", spending, spendingVsPrior,
                p.red, Color(0xFFFFE0E9))
            androidx.compose.material3.VerticalDivider(color = p.line,
                modifier = Modifier.padding(horizontal = 7.dp).fillMaxHeight(), thickness = 1.dp)
            metric(Icons.Rounded.BarChart, "Budget left", budgetLeft, budgetTotal,
                p.blue, Color(0xFFE3E7FF), progress = budgetProgress)
        }
    }
}

@Composable
internal fun ReferenceHomeAttention(count: Int, onClick: () -> Unit) {
    val p = HomeReferencePalette
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp),
        shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF3E9)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFFFFE3CC), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, Modifier.size(19.dp),
                        tint = Color(0xFFDB6700))
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Needs attention", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = p.ink)
                Text(if (count > 0) "$count transactions need categories" else "No categories need attention",
                    fontSize = 11.sp, color = p.muted)
            }
            Icon(Icons.Rounded.ChevronRight, "Review transactions", Modifier.size(20.dp),
                tint = Color(0xFF828CAF))
        }
    }
}

internal data class ReferenceUpcomingEntry(val title: String, val whenText: String,
                                           val amount: String, val isSalary: Boolean)

@Composable
internal fun ReferenceHomeUpcoming(
    items: List<ReferenceUpcomingEntry>,
    onSeeAll: () -> Unit,
    onSalary: () -> Unit = onSeeAll
) {
    val p = HomeReferencePalette
    ReferenceSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Upcoming", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = p.ink, modifier = Modifier.weight(1f))
            Text("See all", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = p.violet, modifier = Modifier.clickable(onClick = onSeeAll))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val shown = items.take(2)
            for (index in 0..1) {
                val entry = shown.getOrNull(index)
                val salary = entry?.isSalary ?: (index == 1)
                val tint = if (salary) Color(0xFF078B77) else p.purple
                val bg = if (salary) Color(0xFFECFBF7) else Color(0xFFF5F2FF)
                Surface(onClick = if (salary) onSalary else onSeeAll,
                    modifier = Modifier.weight(1f).height(53.dp),
                    color = bg, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(30.dp), color = if (salary) Color(0xFFD8F8EE)
                            else Color(0xFFE9DFFF), shape = CircleShape) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(17.dp), tint = tint)
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(entry?.title ?: if (salary) "Salary" else "Bills",
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = p.ink,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry?.whenText ?: if (salary) "Add salary" else "Not scheduled", fontSize = 9.sp,
                                color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (entry != null) {
                            Spacer(Modifier.width(3.dp))
                            Text(entry.amount, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun referenceDate(epochMs: Long, now: Long, locale: Locale): String {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val calendarYesterday = Calendar.getInstance().apply {
        timeInMillis = start
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
    val time = SimpleDateFormat("h:mm a", locale).format(Date(epochMs))
    return when {
        epochMs >= start -> "Today, $time"
        epochMs >= calendarYesterday -> "Yesterday, $time"
        else -> SimpleDateFormat("d MMM yyyy", locale).format(Date(epochMs))
    }
}

@Composable
internal fun ReferenceHomeRecent(
    rows: List<StoredTransaction>, money: (Long) -> String,
    onOpen: (Long) -> Unit, onSeeAll: () -> Unit
) {
    val p = HomeReferencePalette
    val now = remember { System.currentTimeMillis() }
    val locale = LocalConfiguration.current.locales[0]
    ReferenceSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent activity", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = p.ink, modifier = Modifier.weight(1f))
            Text("See all", fontSize = 12.sp, color = p.violet,
                modifier = Modifier.clickable(onClick = onSeeAll))
        }
        Spacer(Modifier.height(5.dp))
        if (rows.isEmpty()) {
            Text("No transactions recorded this month.", fontSize = 12.sp, color = p.muted,
                modifier = Modifier.padding(vertical = 15.dp))
        }
        rows.take(4).forEachIndexed { i, transaction ->
            val title = transaction.counterparty?.takeIf { it.isNotBlank() }
                ?: if (transaction.direction == MoneyDirection.INCOME.name) "Income" else "M-Pesa Transfer"
            val lower = title.lowercase(locale)
            val isIncome = transaction.direction == MoneyDirection.INCOME.name
            val (icon, tint, bubble) = when {
                isIncome || lower.contains("salary") ->
                    Triple(Icons.Rounded.Work, Color(0xFF0744D5), Color(0xFFE4EAFF))
                lower.contains("rent") || lower.contains("house") ->
                    Triple(Icons.Rounded.Home, Color(0xFF007E65), Color(0xFFDEFAF2))
                lower.contains("quickmart") || lower.contains("shop") || lower.contains("market") ->
                    Triple(Icons.Rounded.ShoppingCart, Color(0xFFDB1D55), Color(0xFFFFE0E8))
                else -> Triple(Icons.Rounded.PhoneAndroid, p.purple, Color(0xFFF0E9FF))
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 41.dp).clickable { onOpen(transaction.id) }
                .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(37.dp), shape = RoundedCornerShape(12.dp), color = bubble) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(19.dp), tint = tint)
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = p.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(referenceDate(transaction.occurredAtEpochMs, now, locale),
                        fontSize = 10.sp, color = p.muted, maxLines = 1)
                }
                Text((if (isIncome) "+ " else if (transaction.direction == MoneyDirection.EXPENSE.name) "− " else "") +
                    money(transaction.amountMinor),
                    color = if (isIncome) p.green else p.ink,
                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Rounded.ChevronRight, "Open transaction",
                    Modifier.padding(start = 5.dp).size(17.dp), tint = Color(0xFF919BBA))
            }
            if (i < rows.take(4).lastIndex) {
                HorizontalDivider(Modifier.padding(start = 48.dp), thickness = 1.dp, color = p.line)
            }
        }
    }
}
