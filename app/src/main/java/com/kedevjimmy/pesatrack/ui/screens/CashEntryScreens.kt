package com.kedevjimmy.pesatrack.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.everyday.*
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.domain.recurring.RecurringPatternCandidate
import com.kedevjimmy.pesatrack.planning.*
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.*
import java.util.Calendar
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Keeps the money timeline above utilities. Legacy import/statement/payroll
 * actions are still available, unchanged, through one explicitly labelled
 * disclosure rather than four large buttons above every transaction.
 */
@Composable internal fun ActivityToolsDisclosure(
    onChanged: () -> Unit,
    onPaycheckClick: () -> Unit = {},
    onStatementsClick: (() -> Unit)? = null
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Your transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { expanded = !expanded },
                modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.Tune,
                    contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (expanded) "Hide tools" else "Add & tools")
            }
        }
        if (expanded) ActivityMoneyActions(
            onChanged = onChanged,
            onPaycheckClick = onPaycheckClick,
            onStatementsClick = onStatementsClick
        )
    }
}

@Composable internal fun ActivityMoneyActions(
    onChanged: () -> Unit,
    onPaycheckClick: () -> Unit = {},
    onStatementsClick: (() -> Unit)? = null
) {
 var add by rememberSaveable {mutableStateOf(false)};var importing by rememberSaveable {mutableStateOf(false)}
 Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   Button(onClick={add=true},modifier=Modifier.weight(1f).heightIn(min=54.dp),
    colors=ButtonDefaults.buttonColors(containerColor=androidx.compose.ui.graphics.Color(0xFF17564B))) {
    Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(8.dp));Text("Add entry",maxLines=1)
   }
   OutlinedButton(onClick={importing=true},modifier=Modifier.weight(1f).heightIn(min=54.dp)) {
    Icon(Icons.Rounded.UploadFile,null);Spacer(Modifier.width(8.dp));Text("Import",maxLines=1)
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   if (onStatementsClick != null) OutlinedButton(onClick=onStatementsClick,
    modifier=Modifier.weight(1f).heightIn(min=52.dp),
    contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)) {
     Icon(Icons.Rounded.ReceiptLong,null,modifier=Modifier.size(19.dp));Spacer(Modifier.width(6.dp))
     Text("Statements",maxLines=1)
   }
   OutlinedButton(onClick=onPaycheckClick,modifier=Modifier.weight(1f).heightIn(min=52.dp),
    contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)) {
    Icon(Icons.Rounded.Paid,null,modifier=Modifier.size(19.dp));Spacer(Modifier.width(6.dp))
    Text("Paycheck",maxLines=1)
   }
  }
 }
 if(add) ManualEntrySheet(onDismiss={add=false},onChanged=onChanged)
 if(importing) StatementImportSheet(onDismiss={importing=false},onChanged=onChanged,
    onPaycheckClick=onPaycheckClick,onOpenStatements=onStatementsClick)
}
@Composable private fun ManualEntrySheet(onDismiss:()->Unit,onChanged:()->Unit) {
 val c=LocalContext.current;val store=remember {EverydayMoneyStore(c)};val scope=rememberCoroutineScope()
 var accounts by remember {mutableStateOf<List<MoneyAccount>>(emptyList())};var categories by remember {mutableStateOf<List<PesaCategory>>(emptyList())}
 var loaded by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)}
 LaunchedEffect(Unit){try {withContext(Dispatchers.IO){store.ownedAccounts() to CategoryStore.get(c).budgetCategories()}.let {accounts=it.first;categories=it.second};loaded=true}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not load accounts. Close and try again."}}
 val token=rememberSaveable {UUID.randomUUID().toString()};var accountId by rememberSaveable {mutableStateOf(AccountStore.DEFAULT_CASH_ID)}
 var income by rememberSaveable {mutableStateOf(false)};var amount by rememberSaveable {mutableStateOf("")};var label by rememberSaveable {mutableStateOf("")};var category by rememberSaveable {mutableStateOf(TransactionCategory.OTHER.name)};var date by rememberSaveable {mutableLongStateOf(System.currentTimeMillis())};var busy by remember {mutableStateOf(false)}
 MoneyToolSheet("Add entry","Use this for money without a message, such as cash spending. A cash withdrawal is a transfer, not another expense. Only add money that actually came in or went out.",busy,onDismiss) {
  item {ChoiceField("Type",if(income)"Income"else"Expense",listOf("expense" to "Expense","income" to "Income"),!busy){income=it=="income"}}
  item {ChoiceField("Account",accounts.firstOrNull{it.id==accountId}?.name ?: "Choose account",accounts.map {it.id to it.name},!busy){accountId=it}}
  item {MoneyAmount(amount,enabled=!busy){amount=it;error=null}}
  item {OutlinedTextField(value=label,onValueChange={label=it.take(200)},label={Text("Description")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())}
  if(!income)item {ChoiceField("Category",categories.firstOrNull{it.id==category}?.label ?: "Choose category",categories.map {it.id to it.label},!busy){category=it}}
  item {OutlinedButton(onClick={chooseDate(c,date){date=it}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(toolDate(date))}}
  item {ToolError(error);Button(onClick={val minor=EverydayMoneyPolicy.money(amount);if(minor==null||minor<=0){error="Enter an amount above zero."}else{busy=true;scope.launch{try {val added=withContext(Dispatchers.IO){store.addEntry(token,accountId,minor,date,label,if(income)TransactionCategory.INCOME.name else category,income)};if(added){onChanged();onDismiss()}else error="This entry is already saved."}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not save. Try again."}finally{busy=false}}}},enabled=loaded&&!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(if(busy)"Saving…"else"Save entry")}}
 }
}

