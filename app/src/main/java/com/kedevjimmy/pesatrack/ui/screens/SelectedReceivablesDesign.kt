package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ReceivablePerson
import com.kedevjimmy.pesatrack.data.local.ReceivableEvent
import com.kedevjimmy.pesatrack.data.local.PersonalReceivableStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SelectedReceivablesSummary(people: List<ReceivablePerson>, total: Long?, unknown: Int) {
    Surface(shape = RoundedCornerShape(24.dp), color = AccountCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(62.dp).background(AccountMint, RoundedCornerShape(19.dp)),
                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ReceiptLong, null, tint = AccountBlack) }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Open personal loans", style = MaterialTheme.typography.titleMedium)
                    Text(total?.let(::receivableMoney) ?: "Balance unavailable",
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Text("${people.size} person(s) · $unknown balance(s) need review",
                style = MaterialTheme.typography.bodyMedium)
            if (unknown > 0) Text("Unknown values are excluded from the total.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SelectedReceivablePersonRow(person: ReceivablePerson, onOpen: () -> Unit) {
    Surface(onClick = onOpen, color = Color.White, shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).background(AccountMint, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, tint = AccountBlack)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(person.name, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text(when (person.outstandingMinor) {
                    null -> "Review recorded evidence"
                    0L -> "Repaid"
                    else -> "Still owed to you"
                }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(person.outstandingMinor?.let(::receivableMoney) ?: "Unknown",
                fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
internal fun SelectedPersonalLoanHero(person: ReceivablePerson, history: List<ReceivableEvent>) {
    val lent = runCatching { history.filter { it.active && it.kind == PersonalReceivableStore.LENT }
        .fold(0L) { sum, event -> Math.addExact(sum, event.amountMinor) } }.getOrNull()
    val repaid = runCatching { history.filter { it.active && it.kind == PersonalReceivableStore.REPAID }
        .fold(0L) { sum, event -> Math.addExact(sum, event.amountMinor) } }.getOrNull()
    Surface(shape = RoundedCornerShape(24.dp), color = AccountMint) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Still owed to you", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = AccountBlack)
            Text(person.outstandingMinor?.let(::receivableMoney) ?: "Balance unavailable",
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("${lent?.let(::receivableMoney) ?: "Unknown"} lent · ${repaid?.let(::receivableMoney) ?: "Unknown"} repaid",
                style = MaterialTheme.typography.bodyMedium)
            if (lent != null && lent > 0 && repaid != null) {
                val share = (repaid.toDouble() / lent.toDouble()).coerceIn(0.0, 1.0).toFloat()
                Text("${(share * 100).toInt()}% repaid", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = AccountBlack, trackColor = Color(0xFFC9DDCE))
            }
            Text("Based on recorded transactions, not a live wallet balance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
