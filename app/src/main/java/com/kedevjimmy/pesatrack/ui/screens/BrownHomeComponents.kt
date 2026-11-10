package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.local.PaydayAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.rules.TrackedAccountGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.launch

/** Reference photography is decorative; all displayed financial figures remain ledger-backed. */
@Composable
internal fun BrownHomeHero(available: String, onAccounts: () -> Unit) {
    val p = BrownHomePalette
    val context = LocalContext.current
    val name = remember(context) {
        context.getSharedPreferences("pesatrack_preferences", android.content.Context.MODE_PRIVATE)
            .getString("profile_name", "").orEmpty().trim().substringBefore(" ")
                .takeIf(String::isNotBlank) ?: "there"
    }
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        else -> "Good evening,"
    }
    val pager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(24.dp))) {
        Image(painterResource(R.drawable.home_brown_money_hero), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to Color(0xCC1C0903), .52f to Color(0x551C0903), 1f to Color.Transparent)))
        HorizontalPager(pager, modifier = Modifier.fillMaxSize()) { page ->
            Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 12.dp,
                top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.Center) {
                Text(if (page == 0) greeting else "Your available money",
                    color = p.white, fontFamily = FontFamily.Cursive,
                    fontStyle = FontStyle.Italic, fontSize = if (page == 0) 22.sp else 20.sp,
                    lineHeight = 26.sp)
                Text(if (page == 0) "$name." else available, color = p.white,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                    fontSize = if (page == 0) 28.sp else 24.sp, lineHeight = 30.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(if (page == 0) "Your money, your plans.\nAll in one place."
                     else "After unpaid bills · check Accounts for balance",
                    color = p.white, fontSize = 11.sp, lineHeight = 16.sp,
                    maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.heightIn(min = 48.dp).clickable {
                        if (page == 0) scope.launch { pager.animateScrollToPage(1) }
                        else onAccounts()
                    }, contentAlignment = Alignment.CenterStart) {
                    Row(Modifier.height(40.dp).clip(CircleShape)
                        .background(p.coffee).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (page == 0) "VIEW MY MONEY" else "YOUR ACCOUNTS",
                            color = p.white, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = p.white)
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { index ->
                Box(Modifier.size(8.dp).clip(CircleShape)
                    .background(if (pager.currentPage == index) p.white else p.white.copy(alpha = .5f)))
            }
        }
    }
}

private data class BrownShortcut(val label: String, val icon: ImageVector)

@Composable
internal fun BrownHomeShortcuts(
    onActivity: () -> Unit, onAccounts: () -> Unit, onBudgets: () -> Unit,
    onGoals: () -> Unit, onDebts: () -> Unit, onReports: () -> Unit
) {
    val p = BrownHomePalette
    val shortcuts = listOf(
        BrownShortcut("Activity", Icons.Rounded.BarChart),
        BrownShortcut("Accounts", Icons.Rounded.AccountBalanceWallet),
        BrownShortcut("Budgets", Icons.Rounded.PieChartOutline),
        BrownShortcut("Goals", Icons.Rounded.TrackChanges),
        BrownShortcut("Debts", Icons.Rounded.Layers),
        BrownShortcut("Reports", Icons.Rounded.Assessment)
    )
    val actions = listOf(onActivity, onAccounts, onBudgets, onGoals, onDebts, onReports)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        shortcuts.forEachIndexed { index, shortcut ->
            Column(Modifier.weight(1f).clickable(onClick = actions[index]),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(Modifier.size(48.dp), shape = CircleShape,
                    color = if (index == 0) p.coffee else p.white,
                    shadowElevation = 2.dp) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(shortcut.icon, shortcut.label, Modifier.size(24.dp),
                            tint = if (index == 0) p.white else p.coffee)
                    }
                }
                Text(shortcut.label, color = p.ink, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun BrownHomeSectionTitle(title: String, action: String, onAction: () -> Unit) {
    val p = BrownHomePalette
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = p.ink, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
        Row(Modifier.heightIn(min = 48.dp).clickable(onClick = onAction),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(action, color = p.coffee, fontSize = 12.sp)
            Icon(Icons.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = p.coffee)
        }
    }
}

/** Absence, unknown quality and incomplete account groups must never become a fake balance. */
internal fun trustedBrownHomeBalance(
    group: TrackedAccountGroup, snapshots: List<PaydayAccountSnapshot>
): Long? = VerifiedAccountBalancePolicy.groupTotal(group.accountIds, snapshots)

@Composable
internal fun BrownHomeAccounts(
    groups: List<TrackedAccountGroup>, balances: List<PaydayAccountSnapshot>,
    money: (Long) -> String, onAccounts: () -> Unit
) {
    val p = BrownHomePalette
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BrownHomeSectionTitle("Your accounts", "View all", onAccounts)
        if (groups.isEmpty()) {
            Text("Connect or confirm your accounts", Modifier.fillMaxWidth()
                .clickable(onClick = onAccounts).padding(vertical = 12.dp),
                color = p.muted, fontSize = 13.sp)
        } else {
            groups.take(3).chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { group ->
                        BrownAccountTile(group.displayName,
                            trustedBrownHomeBalance(group, balances)?.let(money)
                                ?: "Balance unavailable",
                            Modifier.weight(1f), onAccounts)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrownAccountTile(name: String, balance: String,
    modifier: Modifier, onClick: () -> Unit
) {
    val p = BrownHomePalette
    Surface(modifier.heightIn(min = 76.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp), color = p.white,
        border = androidx.compose.foundation.BorderStroke(1.dp, p.outline)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountProviderMark(name, Modifier.size(38.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(balance, color = p.ink, fontSize = if (balance.length > 15) 10.sp else 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
internal fun BrownHomeUpcoming(
    items: List<ReferenceUpcomingEntry>, onPlan: () -> Unit,
    onBills: () -> Unit, onSalary: () -> Unit
) {
    val p = BrownHomePalette
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BrownHomeSectionTitle("Coming up", "View plan", onPlan)
        Surface(color = p.cream, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2DBC4)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(27.dp), tint = p.coffee)
                }
                Column(Modifier.weight(1f)) {
                    if (items.isEmpty()) {
                        Text("Nothing scheduled yet", Modifier.padding(vertical = 10.dp),
                            color = p.muted, fontSize = 13.sp)
                    }
                    items.take(2).forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = p.outline.copy(alpha = .65f))
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .clickable(onClick = if (item.isSalary) onSalary else onBills),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = p.ink, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(item.whenText, color = p.coffee, fontSize = 10.sp)
                            }
                            Text(item.amount, color = p.ink, fontWeight = FontWeight.Bold,
                                fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BrownHomeMonthlySnapshot(
    month: String, spending: String, onReports: () -> Unit, onMonth: () -> Unit
) {
    val p = BrownHomePalette
    Box(Modifier.fillMaxWidth().height(124.dp).clip(RoundedCornerShape(20.dp))) {
        Image(painterResource(R.drawable.home_brown_monthly_coins), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to Color(0x88FFE8D1), .52f to Color(0x33FFE8D1), 1f to Color.Transparent)))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("MONTHLY SNAPSHOT", color = p.coffee, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = .4.sp)
            Text("${month.substringBefore(' ')} spending", color = p.ink,
                fontFamily = FontFamily.Serif, fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("$spending recorded", color = p.ink, fontSize = 12.sp,
                fontWeight = FontWeight.Medium)
            Box(Modifier.heightIn(min = 48.dp).clickable(onClick = onReports),
                contentAlignment = Alignment.CenterStart) {
                Row(Modifier.height(40.dp).clip(CircleShape).background(p.coffee)
                    .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("VIEW REPORT", color = p.white, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.ArrowForward, null, Modifier.size(17.dp), tint = p.white)
                }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(48.dp)
            .clickable(onClick = onMonth), contentAlignment = Alignment.Center) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(p.coffee),
                contentAlignment = Alignment.Center) {
                Text(month.take(3).uppercase(), fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = p.white)
            }
        }
    }
}

@Composable
internal fun BrownHomeRecent(
    rows: List<StoredTransaction>, money: (Long) -> String,
    onOpen: (Long) -> Unit, onSeeAll: () -> Unit
) {
    val p = BrownHomePalette
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(locale) { SimpleDateFormat("d MMM · h:mm a", locale) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BrownHomeSectionTitle("Recent activity", "View all", onSeeAll)
        if (rows.isEmpty()) {
            Text("No recorded activity this month", Modifier.clickable(onClick = onSeeAll)
                .padding(vertical = 10.dp), color = p.muted, fontSize = 13.sp)
        }
        rows.take(4).forEach { row ->
            val income = row.direction == MoneyDirection.INCOME.name
            val expense = row.direction == MoneyDirection.EXPENSE.name
            val title = row.counterparty?.takeIf(String::isNotBlank)
                ?: if (income) "Income" else if (expense) "Expense" else "Transfer"
            val icon = when {
                title.contains("safaricom", true) || title.contains("airtime", true) -> Icons.Rounded.PhoneAndroid
                title.contains("shop", true) || title.contains("mart", true) -> Icons.Rounded.ShoppingBasket
                income -> Icons.Rounded.SouthWest
                else -> Icons.Rounded.HelpOutline
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp)
                .clickable(onClick = { onOpen(row.id) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(p.cream), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(21.dp), tint = p.ink)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 13.sp, color = p.ink,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text("${row.category} · ${timeFormat.format(Date(row.occurredAtEpochMs))}",
                        color = p.muted, fontSize = 11.sp, maxLines = 1)
                }
                Text((if (income) "+" else if (expense) "−" else "") + money(row.amountMinor),
                    color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = p.outline.copy(alpha = .35f))
        }
    }
}

@Composable
internal fun BrownHomeReviewStatus(pending: Int?, onReview: () -> Unit) {
    val p = BrownHomePalette
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .clip(RoundedCornerShape(12.dp)).background(p.cream)
        .clickable(onClick = onReview).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(if (pending != null && pending > 0) Icons.Rounded.NotificationsActive
             else Icons.Rounded.CheckCircleOutline, null, Modifier.size(21.dp), tint = p.coffee)
        Column(Modifier.weight(1f)) {
            Text(if (pending == null) "Review status unavailable" else if (pending > 0)
                "$pending item${if (pending == 1) "" else "s"} need review"
                else "You're all caught up", color = p.ink, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold)
            Text(if (pending == 0) "Nothing currently needs review" else "Review your transactions",
                color = p.muted, fontSize = 10.sp)
        }
        Icon(Icons.Rounded.ChevronRight, "Review", Modifier.size(19.dp), tint = p.coffee)
    }
}
