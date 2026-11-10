package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import java.text.SimpleDateFormat
import java.util.Date

/** Approved phone-sized Plan surface. All money and events are sourced by PlanScreen. */
@Composable
internal fun ApprovedCoffeePlanOverview(
    data: PlanJourneyView,
    selectedPeriod: String,
    onPeriod: (String) -> Unit,
    onOtherMonths: () -> Unit,
    onBudget: () -> Unit,
    onGoals: () -> Unit,
    onBills: () -> Unit,
    onShopping: (() -> Unit)?,
    onDetails: () -> Unit,
    onSalary: () -> Unit
) {
    val p = BrownHomePalette
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Plan your money", fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, fontSize = 19.sp, color = p.ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        ApprovedPlanWeek(data.week)
        Surface(shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
            color = Color.White, border = BorderStroke(1.dp, p.outline),
            modifier = Modifier.heightIn(min = 48.dp).clickable(onClickLabel = "Other months", onClick = onOtherMonths)) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(data.month, color = p.ink, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = p.ink)
            }
        }
        ApprovedPlanMoneyCard(data, onDetails)
        Text("Your plans", color = p.ink, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ApprovedPlanProgress("Budget", data.budgetPercent, Icons.Rounded.Wallet,
                onBudget, Modifier.weight(1f))
            ApprovedPlanProgress("Goals", data.goalPercent, Icons.Rounded.Flag,
                onGoals, Modifier.weight(1f))
        }
        if (onShopping != null) ShoppingListsPlanEntry(onShopping)
        ApprovedPlanLink("Bills", when (data.bills.size) {
            0 -> "No upcoming payments"
            1 -> "1 upcoming"
            else -> "${data.bills.size} upcoming"
        }, Icons.Rounded.CalendarMonth, onBills)
    }
}

@Composable
private fun ApprovedPlanWeek(days: List<PlanCalendarDay>) {
    val p = BrownHomePalette
    var chosen by remember(days) { mutableStateOf(days.firstOrNull { it.isToday }?.startEpochMs) }
    Surface(color = p.cream.copy(alpha = 0.55f),
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius)) {
        Row(Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            days.forEach { day ->
                val active = chosen == day.startEpochMs
                Column(Modifier.weight(1f).heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(PesaDesignTokens.innerRadius))
                    .background(if (active) p.coffee else Color.Transparent)
                    .clickable(role = Role.Button) { chosen = day.startEpochMs }
                    .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(day.weekday, fontSize = 11.sp,
                        color = if (active) Color.White else p.ink, maxLines = 1)
                    Text(day.dayOfMonth, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else p.ink)
                }
            }
        }
    }
}

@Composable
private fun ApprovedPlanMoneyCard(data: PlanJourneyView, onDetails: () -> Unit) {
    val p = BrownHomePalette
    var info by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        border = BorderStroke(1.dp, p.outline.copy(alpha = 0.5f))) {
        Box {
            Image(painterResource(R.drawable.plan_coffee_notebook), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SPENDABLE", color = p.coffee, fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp, letterSpacing = 1.sp)
                    IconButton(onClick = { info = true }, modifier = Modifier.size(48.dp)) {
                        Icon(if (data.spendableNeedsReview) Icons.Rounded.WarningAmber
                            else Icons.Rounded.Info,
                            if (data.spendableNeedsReview) "Spendable needs balance review"
                            else "How is spendable calculated?",
                            tint = if (data.spendableNeedsReview) p.attention else p.coffee,
                            modifier = Modifier.size(18.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().clickable(
                    role = Role.Button, onClickLabel = "View Plan breakdown", onClick = onDetails),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(data.spendable ?: "Unavailable", modifier = Modifier.weight(1f),
                        color = p.ink, fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 32.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = p.coffee,
                        modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(color = p.coffee.copy(alpha = 0.33f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ApprovedPlanMiniMoney("Accounts", data.liquid ?: "—", Modifier.weight(1f))
                    ApprovedPlanMiniMoney("Bills", data.billsTotal ?: "—", Modifier.weight(1f))
                    ApprovedPlanMiniMoney("Saved", data.protectedSavings ?: "—", Modifier.weight(1f))
                }
            }
        }
    }
    if (info) AlertDialog(onDismissRequest = { info = false },
        title = { Text("About spendable money") },
        text = { Text("Spendable uses recorded balances from your accounts, excludes protected " +
            "savings and reserves unpaid bills for this month. It is an estimate, not a live " +
            "bank balance. If an account balance is missing or old, check it in Accounts.") },
        confirmButton = { TextButton(onClick = { info = false }) { Text("Got it") } })
}

@Composable
private fun ApprovedPlanMiniMoney(label: String, amount: String, modifier: Modifier) {
    val p = BrownHomePalette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = p.coffee, fontSize = 12.sp)
        Text(amount, color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ApprovedPlanProgress(title: String, percent: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val p = BrownHomePalette
    Surface(modifier = modifier.heightIn(min = 88.dp)
        .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = Color.White, border = BorderStroke(1.dp, p.outline)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = p.coffee, modifier = Modifier.size(20.dp))
                Text(title, color = p.ink, fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(percent?.let { "$it%" } ?: "Set up", color = p.ink,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(progress = { (percent ?: 0).coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = p.coffee,
                trackColor = p.cream)
        }
    }
}

@Composable
internal fun ApprovedPlanLink(title: String, detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val p = BrownHomePalette
    Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = Color.White, border = BorderStroke(1.dp, p.outline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(PesaDesignTokens.iconRadius), color = p.cream) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = p.coffee)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = p.ink, fontWeight = FontWeight.SemiBold)
                Text(detail, color = p.muted, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = p.ink)
        }
    }
}

@Composable
internal fun ApprovedPlanComingUp(bills: List<PlanJourneyBill>, onBills: () -> Unit) {
    val p = BrownHomePalette
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Coming up", color = p.ink, fontSize = 22.sp,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        if (bills.isEmpty()) Text("Nothing due soon", color = p.muted, fontSize = 13.sp)
        else bills.forEach { bill ->
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
                .clickable(role = Role.Button, onClick = onBills),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
                    color = p.coffee, modifier = Modifier.size(8.dp)) {}
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val date = bill.dueEpochMs?.let {
                        SimpleDateFormat("EEE · d MMM", locale).format(Date(it))
                    } ?: "UPCOMING"
                    Text(date.uppercase(locale), color = p.coffee,
                        fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Text(bill.label, color = p.ink, fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(bill.amount, color = p.ink, fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp, maxLines = 1)
            }
            if (bill != bills.last()) HorizontalDivider(color = p.outline.copy(alpha = 0.5f))
        }
    }
}
