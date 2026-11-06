package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.model.AccountType

private data class KnownProvider(val name: String, val kind: AccountType)
private val popularProviders = listOf(
    KnownProvider("Equity Bank", AccountType.BANK),
    KnownProvider("M-PESA", AccountType.MOBILE_WALLET),
    KnownProvider("KCB", AccountType.BANK),
    KnownProvider("Co-operative Bank", AccountType.BANK),
    KnownProvider("I&M Bank", AccountType.BANK),
    KnownProvider("M-Shwari", AccountType.SAVINGS)
)

/** Provider selection is local and does not imply a live bank connection. */
@Composable
internal fun SelectedAccountPicker(onSelect: (String, AccountType) -> Unit, onOther: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Choose an account", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text("Select where your money is held", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = search, onValueChange = { search = it },
            placeholder = { Text("Search bank or wallet") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("All", "Banks", "Mobile", "Savings").forEach { option ->
                Surface(onClick = { filter = option }, shape = RoundedCornerShape(25.dp),
                    color = if (filter == option) AccountBlack else AccountMint,
                    modifier = Modifier.weight(1f)) {
                    Text(option, Modifier.padding(vertical = 11.dp),
                        color = if (filter == option) Color.White else AccountBlack,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Text("Popular in Kenya", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        popularProviders.filter { provider ->
            provider.name.contains(search, ignoreCase = true) && when (filter) {
                "Banks" -> provider.kind == AccountType.BANK
                "Mobile" -> provider.kind == AccountType.MOBILE_WALLET
                "Savings" -> provider.kind == AccountType.SAVINGS
                else -> true
            }
        }.forEach { provider ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(provider.name, provider.kind) }
                .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AccountProviderMark(provider.name)
                Text(provider.name, Modifier.weight(1f).padding(start = 12.dp),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.ChevronRight, null, tint = AccountBlack)
            }
            HorizontalDivider(color = Color(0xFFE6E6E3))
        }
        OutlinedButton(onClick = onOther, modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, AccountBlack)) {
            Icon(Icons.Rounded.Add, null, tint = AccountBlack)
            Text("Other bank or wallet", Modifier.padding(start = 8.dp), color = AccountBlack)
        }
        Surface(shape = RoundedCornerShape(18.dp), color = AccountMint) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, null, tint = AccountBlack)
                Text("Your account details stay on this phone", Modifier.padding(start = 12.dp),
                    fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
