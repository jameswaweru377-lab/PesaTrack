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

@Composable internal fun BudgetTimingAction(budget:MonthlyBudget) {
 var open by rememberSaveable(budget.categoryId){mutableStateOf(false)}
 TextButton(onClick={open=true},modifier=Modifier.heightIn(min=48.dp)){Icon(Icons.Rounded.DateRange,null);Spacer(Modifier.width(6.dp));Text(budget.periodLabel+" · Change period")}
 if(open)BudgetTimingSheet(budget){open=false}
}
@Composable private fun BudgetTimingSheet(budget:MonthlyBudget,onDismiss:()->Unit) {
 val c=LocalContext.current;val repo=remember{FlexibleBudgetRepository(c)};val scope=rememberCoroutineScope()
 var cadence by rememberSaveable{mutableStateOf(BudgetCadence.MONTHLY.name)};var anchor by rememberSaveable{mutableStateOf("1")};var carry by rememberSaveable{mutableStateOf(false)};var loaded by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
 LaunchedEffect(budget.categoryId){try{val options=withContext(Dispatchers.IO){repo.options(budget.categoryId)};cadence=options.cadence.name;anchor=options.anchor.toString();carry=options.carryUnused;loaded=true}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not load budget settings."}}
 MoneyToolSheet("Budget period","Your amount applies to each selected period. Payday periods start on the day you choose; shorter months use their last day. Carry unused money adds the previous period's remainder. Changing the period, amount or carry option starts a new carry history from this period.",busy,onDismiss){
  item{Text("Base amount: ${toolMoney(budget.baseLimitMinor)}",style=MaterialTheme.typography.titleMedium)}
  item{ChoiceField("Period",when(cadence){"WEEKLY"->"Weekly";"PAYDAY"->"Payday to payday";else->"Monthly"},listOf("MONTHLY" to "Monthly","WEEKLY" to "Weekly","PAYDAY" to "Payday to payday"),!busy){cadence=it;anchor=if(it=="WEEKLY")Calendar.MONDAY.toString()else"1"}}
  if(cadence=="WEEKLY")item{val days=(Calendar.SUNDAY..Calendar.SATURDAY).map{day->val cal=Calendar.getInstance().apply{set(Calendar.DAY_OF_WEEK,day)};day.toString() to SimpleDateFormat("EEEE",androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(cal.time)};ChoiceField("Week starts",days.firstOrNull{it.first==anchor}?.second?:"Monday",days,!busy){anchor=it}}
  if(cadence=="PAYDAY")item{OutlinedTextField(value=anchor,onValueChange={anchor=it.take(2)},label={Text("Payday (1–31)")},singleLine=true,enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())}
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Carry unused money",Modifier.weight(1f));Switch(checked=carry,onCheckedChange={carry=it},enabled=!busy)}}
  item{ToolError(error);Button(onClick={val day=anchor.toIntOrNull();val mode=BudgetCadence.valueOf(cadence);if(day==null||day !in (if(mode==BudgetCadence.WEEKLY)1..7 else 1..31)){error="Choose a valid starting day."}else{busy=true;scope.launch{try{withContext(Dispatchers.IO){repo.save(budget.categoryId,mode,day,carry)};onDismiss()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not save budget period."}finally{busy=false}}}},enabled=loaded&&!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Save period")}}
 }
}

