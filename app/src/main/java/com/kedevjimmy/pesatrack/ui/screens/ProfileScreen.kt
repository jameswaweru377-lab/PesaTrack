package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaCard


import com.kedevjimmy.pesatrack.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProfileScreen(scaffoldPadding: PaddingValues, onBack: () -> Unit, onSettingsClick: () -> Unit,
    onAccountsClick: (() -> Unit)? = null, onDataHealthClick: (() -> Unit)? = null, onGoalsClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pesatrack_preferences",0) }
    var edit by rememberSaveable { mutableStateOf(false) }
    var progress by remember { mutableStateOf<FinancialProgress?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val ledger by LedgerUpdateBus.state.collectAsState()
    LaunchedEffect(ledger.revision, edit, retry) {
        try { progress = withContext(Dispatchers.IO) { FinancialProgressRepository(context).read() }; error = null }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Couldn't load your progress. Please try again." }
    }
    if (edit) { PersonalDetailsScreen(scaffoldPadding) { edit = false }; return }
    EverydayPage("Profile", scaffoldPadding, onBack) {
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(prefs.getString("profile_avatar", "🌿") ?: "🌿", style = MaterialTheme.typography.headlineSmall) }
            }
            Column(Modifier.padding(start = 14.dp)) { Text(prefs.getString("profile_name", "").orEmpty().ifBlank { "Your profile" }, style = MaterialTheme.typography.titleLarge); Text("Your money, your plans", style = MaterialTheme.typography.bodyMedium) }
        } }
        item { EverydayRow(Icons.Rounded.Person, "Personal details", "Name, avatar and optional email") { edit = true } }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { retry++ }) { Text("Try again") } } }
        if (progress == null && error == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        progress?.let { data ->
            item { EverydayRow(Icons.Rounded.AccountBalanceWallet, "${data.accounts} confirmed accounts", "See balances and account details", onAccountsClick ?: onSettingsClick) }
            item { Text("Your financial progress", style = MaterialTheme.typography.titleLarge); Text("Budget use is for this month. Savings show recorded contributions towards each goal.", style = MaterialTheme.typography.bodyMedium) }
            if (data.budgets.isEmpty() && data.goals.isEmpty()) item { Text("Set a budget or create a savings goal in Plan to see your progress here.") }
            data.budgets.forEach { budget -> item { FinancialProgressCard(budget, false) } }
            val active = data.goals.count { it.saved < it.target }
            item { EverydayRow(Icons.Rounded.Savings, "$active active savings goals", "Open your savings goals", onGoalsClick ?: onSettingsClick) }
            data.goals.forEach { goal -> item { Box(Modifier.clickable { (onGoalsClick ?: onSettingsClick)() }) { FinancialProgressCard(goal, true) } } }
        }
        item { EverydayRow(Icons.Rounded.Settings, "Settings", "Privacy, reminders and appearance", onSettingsClick) }
    }
}
@Composable
private fun FinancialProgressCard(item: ProgressItem, saving: Boolean) {
    val ratio = if (item.target <= 0) 0f else (item.saved.toDouble() / item.target).toFloat().coerceIn(0f,1f)
    val money = NumberFormat.getNumberInstance(Locale.UK).apply { minimumFractionDigits=2; maximumFractionDigits=2 }
    PesaCard { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(item.name, style = MaterialTheme.typography.titleMedium)
        Text("KES " + money.format(item.saved / 100.0) + (if(saving) " saved of " else " spent of ") + "KES " + money.format(item.target / 100.0), style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
        Text(if (!saving && item.saved > item.target) "Over this month's budget" else if (saving && item.saved >= item.target) "Goal reached" else "${(item.saved.toDouble()/item.target.coerceAtLeast(1)*100).toInt()}% " + if(saving) "saved" else "used", style = MaterialTheme.typography.bodyMedium)
    } }
}
