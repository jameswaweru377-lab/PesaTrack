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

@Composable internal fun AccountBalanceCheckAction() {
 var open by rememberSaveable {mutableStateOf(false)}
 OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Rounded.FactCheck,null);Spacer(Modifier.width(8.dp));Text("Check a balance")}
 if(open)BalanceCheckSheet{open=false}
}
@Composable private fun BalanceCheckSheet(onDismiss:()->Unit) {
 val c=LocalContext.current;val store=remember {EverydayMoneyStore(c)};val scope=rememberCoroutineScope()
 var accounts by remember {mutableStateOf<List<MoneyAccount>>(emptyList())};var accountId by rememberSaveable{mutableStateOf("")}
 var recorded by remember {mutableStateOf<Long?>(null)};var recent by remember {mutableStateOf<List<StoredTransaction>>(emptyList())};var loaded by remember {mutableStateOf(false)};var actual by rememberSaveable{mutableStateOf("")};var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)}
 LaunchedEffect(Unit){try{accounts=withContext(Dispatchers.IO){store.ownedAccounts()}}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not load accounts."}}
 LaunchedEffect(accountId){loaded=false;actual="";error=null;if(accountId.isNotEmpty()){try {val account=accounts.firstOrNull{it.id==accountId}?:return@LaunchedEffect;val data=withContext(Dispatchers.IO){PaydaySnapshotStore.get(c).currentAccountSnapshot(account).balanceMinor to TransactionStore.get(c).recent(500).filter{it.accountId==accountId||it.linkedAccountId==accountId}.take(8)};recorded=data.first;recent=data.second;loaded=true}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not load this balance."}}}
 MoneyToolSheet("Check balance","Compare the balance shown by your bank or M-Pesa with PesaTrack. Check recent entries if they differ. Saving records the balance you saw now; it does not create income or an expense, or erase earlier messages.",busy,onDismiss){
  item{ChoiceField("Account",accounts.firstOrNull{it.id==accountId}?.name ?: "Choose account",accounts.map{it.id to it.name},!busy){accountId=it}}
  if(loaded){item {Text("Recorded: ${recorded?.let(::toolMoney) ?: "Not known yet"}",style=MaterialTheme.typography.titleMedium)}
   item {MoneyAmount(actual,"Balance you see now (KES)",!busy){actual=it;error=null}}
   item {val value=EverydayMoneyPolicy.money(actual);if(value!=null && recorded!=null){val delta=java.math.BigInteger.valueOf(value)-java.math.BigInteger.valueOf(recorded!!);Text(if(delta.signum()==0)"Balances match"else "Difference: KES ${java.math.BigDecimal(delta,2).toPlainString()}",style=MaterialTheme.typography.titleMedium)}}
   item {ToolError(error);Button(onClick={val value=EverydayMoneyPolicy.money(actual);if(value==null){error="Enter a valid balance."}else{busy=true;scope.launch{try{withContext(Dispatchers.IO){store.saveObservedBalance(accountId,value,recorded)};onDismiss()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not save balance."}finally{busy=false}}}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Save observed balance")}}
   if(recent.isNotEmpty())item{Text("Recent entries",style=MaterialTheme.typography.titleMedium)}
   items(recent,key={it.id}){row->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(row.counterparty ?: "Entry",style=MaterialTheme.typography.bodyMedium);Text(toolDate(row.occurredAtEpochMs),style=MaterialTheme.typography.bodySmall)};Text(toolMoney(row.amountMinor),style=MaterialTheme.typography.bodyMedium)}}
  }else item{ToolError(error);if(accountId.isNotEmpty()&&error==null)LinearProgressIndicator(Modifier.fillMaxWidth())}
 }
}

