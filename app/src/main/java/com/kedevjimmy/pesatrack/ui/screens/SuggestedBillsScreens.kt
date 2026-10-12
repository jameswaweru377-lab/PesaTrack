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

@Composable internal fun SuggestedBillsPanel() {
 val c=LocalContext.current;val store=remember {RecurringPatternStore.get(c)};val scope=rememberCoroutineScope()
 val revision by LedgerUpdateBus.state.collectAsState();var candidates by remember {mutableStateOf<List<RecurringPatternCandidate>>(emptyList())};var expanded by rememberSaveable{mutableStateOf(false)};var chosen by remember{mutableStateOf<RecurringPatternCandidate?>(null)};var error by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
 LaunchedEffect(revision.revision){try{candidates=withContext(Dispatchers.IO){store.suggestions()}}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not check suggested bills."}}
 if(candidates.isNotEmpty()||error!=null)PesaCard{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={expanded=!expanded},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("Suggested bills (${candidates.size})");Icon(if(expanded)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,null)};MoneyHelp("These payments repeat in your recorded history. Add one only if it is a bill you expect again. You choose the amount, date and reminders. PesaTrack does not make payments.")}
  ToolError(error)
  if(expanded)candidates.forEach {item->Column {Text(item.label,style=MaterialTheme.typography.titleSmall);Text("${toolMoney(item.typicalAmountMinor)} · ${item.occurrenceCount} payments",style=MaterialTheme.typography.bodySmall);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilledTonalButton(onClick={chosen=item},enabled=!busy){Text("Set up bill")};TextButton(onClick={busy=true;scope.launch{try{withContext(Dispatchers.IO){store.dismiss(item)};candidates=candidates.filterNot{it.patternKey==item.patternKey}}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not dismiss this suggestion."}finally{busy=false}}},enabled=!busy){Text("Not a bill")}}}}
 }}
 chosen?.let {candidate->SuggestedBillSheet(candidate,onDismiss={chosen=null})}
}
/** Shared confirmation sheet used by both Plan and Bills suggestions. */
@Composable internal fun SuggestedBillSheet(candidate:RecurringPatternCandidate,onDismiss:()->Unit) {
 val c=LocalContext.current;val scope=rememberCoroutineScope();var amount by rememberSaveable(candidate.patternKey){mutableStateOf(amountText(candidate.typicalAmountMinor))};var cadence by rememberSaveable(candidate.patternKey){mutableStateOf(if(candidate.medianIntervalDays in 5..9)"WEEKLY"else"MONTHLY")};val expected=Calendar.getInstance().apply{timeInMillis=candidate.nextExpectedAtEpochMs};var day by rememberSaveable(candidate.patternKey){mutableStateOf(expected.get(Calendar.DAY_OF_MONTH).toString())};var weekday by rememberSaveable(candidate.patternKey){mutableStateOf(expected.get(Calendar.DAY_OF_WEEK).toString())};var reminders by rememberSaveable{mutableStateOf(true)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
 MoneyToolSheet("Set up bill","Check the amount and due date. This creates a bill reminder from the repeated payments you confirmed. It does not add another payment or charge your account.",busy,onDismiss){
  item{Text(candidate.label,style=MaterialTheme.typography.titleLarge)}
  item{MoneyAmount(amount,enabled=!busy){amount=it}}
  item{ChoiceField("Repeats",if(cadence=="WEEKLY")"Weekly"else"Monthly",listOf("MONTHLY" to "Monthly","WEEKLY" to "Weekly"),!busy){cadence=it}}
  if(cadence=="MONTHLY")item{OutlinedTextField(value=day,onValueChange={day=it.take(2)},label={Text("Due day (1–31)")},singleLine=true,enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())}
  else item{val days=(1..7).map {n->val cal=Calendar.getInstance().apply{set(Calendar.DAY_OF_WEEK,n)};n.toString() to SimpleDateFormat("EEEE",androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(cal.time)};ChoiceField("Due day",days.firstOrNull{it.first==weekday}?.second?:"Choose day",days,!busy){weekday=it}}
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Remind me",Modifier.weight(1f));Switch(checked=reminders,onCheckedChange={reminders=it},enabled=!busy)}}
  item{ToolError(error);Button(onClick={val minor=EverydayMoneyPolicy.money(amount);val due=day.toIntOrNull();val week=weekday.toIntOrNull();if(minor==null||minor<=0||due==null||due !in 1..31||week==null||week !in 1..7){error="Check the amount and due day."}else{busy=true;scope.launch{try{withContext(Dispatchers.IO){val latest=RecurringPatternStore.get(c).suggestions(limit=20).firstOrNull{it.patternKey==candidate.patternKey} ?: error("This suggestion changed. Close and check again.");val rule=RecurringPatternStore.get(c).accept(latest.copy(typicalAmountMinor=minor)) ?: error("This suggestion is no longer available.");BillSchedules(c).saveSchedule(rule.id,if(cadence=="WEEKLY")BillCadence.WEEKLY else BillCadence.MONTHLY,dueDay=due,dueDayOfWeek=week,enabled=reminders)};onDismiss()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not save this bill."}finally{busy=false}}}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Add bill")}}
 }
}
