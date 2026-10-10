package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.receivables.ReceivableHistoryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The payer's merchant is never used as a default borrower. Identity results require a tap. */
@Composable
internal fun SharedBillPersonFields(
    people: List<ReceivablePerson>, selectedId: String?, name: String, phone: String, busy: Boolean,
    onExisting: (ReceivablePerson?) -> Unit, onName: (String) -> Unit, onPhone: (String) -> Unit,
    onIdentity: (String,String?) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ReceivableHistoryRepository(context) }
    var menu by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<ReceivableHistoryCandidate>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var pickedName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(name,selectedId) {
        rows = emptyList(); searchError = false
        if (selectedId == null && name.trim().length in 2..100 && pickedName != name) {
            delay(350)
            searching = true
            try {
                rows = withContext(Dispatchers.IO) {
                    repository.search(name,null,name,null).filter { !it.row.counterparty.isNullOrBlank() }
                        .distinctBy { ReceivableHistoryPolicy.normalizedName(it.row.counterparty.orEmpty()) to
                            (ReceivableHistoryPolicy.normalizedPhone(it.row.counterpartyPhone) ?: it.row.counterpartyPhone.orEmpty()) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { searchError = true }
            finally { searching = false }
        }
    }
    Box {
        TextButton(enabled = !busy,onClick = { menu = true }) {
            Text(people.firstOrNull { it.id == selectedId }?.let { "Person: " + it.name } ?: "Choose an existing person")
        }
        DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
            people.forEach { person ->
                DropdownMenuItem(text = { Text(person.name + (person.phone?.let { " • " + it } ?: "")) },
                    onClick = { menu = false; pickedName = person.name; onExisting(person) })
            }
            DropdownMenuItem(text = { Text("Search records / new person") },
                onClick = { menu = false; pickedName = null; onExisting(null) })
        }
    }
    if (selectedId != null) Text(name + (phone.takeIf { it.isNotBlank() }?.let { " • " + it } ?: ""),
        style = MaterialTheme.typography.bodySmall)
    if (selectedId == null) {
        OutlinedTextField(value = name,onValueChange = {
            if (it.length <= 100) { pickedName = null; onName(it) }
        },singleLine = true,enabled = !busy,label = { Text("Person's name — search records or enter new") },
            modifier = Modifier.fillMaxWidth().testTag("shared-person-name"))
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (searchError) Text("Could not search records. You can enter the person's details manually.",
            style = MaterialTheme.typography.bodySmall)
        if (rows.isNotEmpty()) {
            Text("People found in transactions — choose the right person. Similar names may be different people.",
                style = MaterialTheme.typography.bodySmall)
            rows.forEach { candidate ->
                val row = candidate.row
                TextButton(enabled = !busy,onClick = {
                    pickedName = row.counterparty
                    rows = emptyList()
                    onIdentity(row.counterparty.orEmpty(),row.counterpartyPhone)
                },modifier = Modifier.fillMaxWidth().testTag("shared-identity-" + row.id)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(row.counterparty.orEmpty())
                        Text(listOfNotNull(row.counterpartyPhone,candidate.accountName).joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text("Up to 50 recent matching records. Refine the name or search by phone for other matches.",
                style = MaterialTheme.typography.labelSmall)
        }
        OutlinedTextField(value = phone,onValueChange = { if (it.length <= 20) onPhone(it) },
            singleLine = true,enabled = !busy,label = { Text("Phone (optional)") },modifier = Modifier.fillMaxWidth())
    }
}
