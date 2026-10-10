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

@Composable internal fun SplitPaymentAction(row:StoredTransaction,onChanged:()->Unit) {
 if(row.direction!=MoneyDirection.EXPENSE.name||row.reviewState in setOf(ReviewState.NEEDS_REVIEW.name,ReviewState.EXCLUDED.name)||row.relationType!=null||row.reversalOfCode!=null)return
 val c=LocalContext.current
 val revision by LedgerUpdateBus.state.collectAsState()
 var savedParts by remember(row.id) {mutableStateOf<List<MoneyPart>>(emptyList())}
 LaunchedEffect(row.id,revision.revision) {savedParts=withContext(Dispatchers.IO){EverydayMoneyStore(c).allocations(row.id)}}
 var open by rememberSaveable(row.id){mutableStateOf(false)}
 OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Rounded.CallSplit,null);Spacer(Modifier.width(8.dp));Text(if(savedParts.isEmpty())"Split payment"else"Edit split (${savedParts.size} categories)")}
 if(open)SplitPaymentSheet(row,{open=false},onChanged)
}
@Composable private fun SplitPaymentSheet(row:StoredTransaction,onDismiss:()->Unit,onChanged:()->Unit) {
 val c=LocalContext.current;val store=remember {EverydayMoneyStore(c)};val scope=rememberCoroutineScope();val parts=remember {mutableStateListOf<Pair<String,String>>()}
 var categories by remember {mutableStateOf<List<PesaCategory>>(emptyList())};var loaded by remember {mutableStateOf(false)};var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)};var hadSplit by remember {mutableStateOf(false)}
 LaunchedEffect(row.id){try{val data=withContext(Dispatchers.IO){CategoryStore.get(c).budgetCategories() to store.allocations(row.id)};categories=data.first;hadSplit=data.second.isNotEmpty();parts.addAll(if(hadSplit)data.second.map {it.category to amountText(it.minor)} else listOf(row.category to "","" to ""));loaded=true}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not load this payment."}}
 MoneyToolSheet("Split payment","Use different spending categories for one payment. Parts must match the payment amount. Fees stay separate. Reports and budgets use these parts; your account total does not change. Refunds follow the same proportions.",busy,onDismiss){
  item {Text("Payment: ${toolMoney(row.amountMinor)}",style=MaterialTheme.typography.titleLarge)}
  items(parts.size,key={it}){i->val p=parts[i];PesaCard{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){ChoiceField("Category ${i+1}",categories.firstOrNull{it.id==p.first}?.label ?: "Choose category",categories.map {it.id to it.label},!busy){parts[i]=it to parts[i].second};MoneyAmount(p.second,enabled=!busy){parts[i]=parts[i].first to it};if(parts.size>2)TextButton(onClick={parts.removeAt(i)},enabled=!busy){Text("Remove part")}}}
  if(parts.size<20)item{TextButton(onClick={parts.add("" to "")},enabled=!busy){Icon(Icons.Rounded.Add,null);Text("Add part")}}
  item {val total=parts.fold(java.math.BigInteger.ZERO){sum,p->sum+java.math.BigInteger.valueOf(EverydayMoneyPolicy.money(p.second)?:0)};Text("Parts: KES ${java.math.BigDecimal(total,2).toPlainString()}",style=MaterialTheme.typography.titleSmall);ToolError(error)}
  item {Button(onClick={val values=parts.mapNotNull {(id,a)->EverydayMoneyPolicy.money(a)?.let {MoneyPart(id,it)}};if(values.size!=parts.size||!EverydayMoneyPolicy.validSplit(row.amountMinor,values)){error="Use different categories and match the full amount."}else{busy=true;scope.launch{try{withContext(Dispatchers.IO){store.split(row.id,values)};onChanged();onDismiss()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not save split."}finally{busy=false}}}},enabled=loaded&&!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Save split")}}
  if(hadSplit)item{TextButton(onClick={busy=true;scope.launch{try{withContext(Dispatchers.IO){store.removeSplit(row.id)};onChanged();onDismiss()}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not remove split."}finally{busy=false}}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Use one category again")}}
 }
}

